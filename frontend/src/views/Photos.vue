<script setup lang="ts">
import { ref, Ref, computed, watch, nextTick, onMounted, onUnmounted } from "vue"
import { useRouter } from "vue-router"
import { language } from "@/functions/languageStore.ts"
import { errorStore } from "@/components/error/errorStore.ts"
import { createPhotoThumbnail } from "@/api/photo.ts"
import { getFoldersByLevel, getFolderChildren, getFolderPhotosPage, getFolderChain, getThumbnailUrl, createFolderThumbnail } from "@/api/folder.ts"
import { searchFolders, searchPhotosPage } from "@/api/search.ts"
import { MediaFile, Folder } from "@/types"
import Error from "@/components/error/Error.vue"
import Loading from "@/components/utilities/Loading.vue"
import PhotoDetail from "@/components/PhotoDetail.vue"

const PAGE_SIZE = 50
/** Larger initial fetch for root-level photos, which are rarely paginated. */
const ROOT_PAGE_SIZE = 200

/**
 * Props for the photo grid.
 *
 * @property folderId id of the folder to open directly; when null the grid opens at
 *           the library root. Supplied via the route query, e.g. when jumping to a
 *           photo's folder from the slideshow
 */
interface PhotosProps {
	folderId?: number | null
}

const props = withDefaults(defineProps<PhotosProps>(), {
	folderId: null
})

const router = useRouter()

const loading = ref(false)
const selectedPhotoId: Ref<number | null> = ref(null)

/** Free-text query matched against folder paths and photo paths/filenames; empty means no search. */
const searchQuery: Ref<string> = ref("")

/** Selected rating filter as a string ("" = all ratings, "1".."5" = exact rating). */
const searchRating: Ref<string> = ref("")

/** True while search results are shown in place of the folder-browsing view. */
const searchActive: Ref<boolean> = ref(false)

/** Pending debounce timer for search input; cleared on each keystroke and on unmount. */
let searchDebounce: ReturnType<typeof setTimeout> | null = null

/** Stack of folders the user has navigated into; empty means root (level 1). */
const folderStack: Ref<Folder[]> = ref([])

const displayFolders: Ref<Folder[]> = ref([])
const displayPhotos: Ref<MediaFile[]> = ref([])

/** Set of photo ids currently awaiting thumbnail generation. */
const loadingThumbnails: Ref<Set<number>> = ref(new Set())

/** Set of folder ids currently awaiting thumbnail generation. */
const loadingFolderThumbnails: Ref<Set<number>> = ref(new Set())

/** ID of the folder whose photos are currently paginated; null at root. */
const currentFolderId: Ref<number | null> = ref(null)

/** Zero-based index of the last page loaded for the current folder. */
const currentPage: Ref<number> = ref(0)

/** Total number of photos in the current folder as reported by the API. */
const totalPhotosCount: Ref<number> = ref(0)

/** Whether more pages are available for the current folder. */
const hasMorePhotos: Ref<boolean> = ref(false)

/** True while a subsequent page is being fetched (not the initial load). */
const loadingMore: Ref<boolean> = ref(false)

/** Sentinel element at the bottom of the grid; observed to trigger page loads. */
const sentinelRef: Ref<HTMLElement | null> = ref(null)

/** Scrollable grid container; used as root for the IntersectionObserver. */
const scrollAreaRef: Ref<HTMLElement | null> = ref(null)

let intersectionObserver: IntersectionObserver | null = null

onMounted(() => {
	if (props.folderId != null) loadFolderById(props.folderId)
	else loadRoot()
})

// Re-open the requested folder when the route query changes while the grid stays mounted.
watch(() => props.folderId, (folderId) => {
	if (folderId != null) loadFolderById(folderId)
	else loadRoot()
})

onUnmounted(() => {
	intersectionObserver?.disconnect()
	if (searchDebounce) clearTimeout(searchDebounce)
})

// Debounce search input: run a search when a query or rating is set, otherwise leave search mode.
watch([searchQuery, searchRating], () => {
	if (searchDebounce) clearTimeout(searchDebounce)
	searchDebounce = setTimeout(() => {
		if (searchQuery.value.trim() === "" && searchRating.value === "") {
			if (searchActive.value) exitSearch()
		} else {
			runSearch()
		}
	}, 300)
})

/** Parses the selected rating into a number, or null when "all ratings" is selected. */
const selectedRating = (): number | null =>
	searchRating.value === "" ? null : Number(searchRating.value)

/**
 * Runs a search for the current query and rating, replacing the grid with the matching
 * folders and the first page of matching photos. Folders carry no rating, so they are only
 * included when no rating filter is applied. Subsequent photo pages load via infinite scroll.
 */
const runSearch = async () => {
	selectedPhotoId.value = null
	searchActive.value = true
	loading.value = true
	displayFolders.value = []
	loadingFolderThumbnails.value = new Set()
	resetPhotoState()

	const query = searchQuery.value.trim()
	const rating = selectedRating()

	const foldersPromise: Promise<Folder[]> = rating === null
		? searchFolders(query).catch((e: unknown) => { handleError(e); return [] as Folder[] })
		: Promise.resolve([])

	const [folders, photosPage] = await Promise.all([
		foldersPromise,
		searchPhotosPage(query, rating, 0, PAGE_SIZE).catch((e: unknown) => { handleError(e); return null })
	])

	displayFolders.value = folders
	generateMissingFolderThumbnails(folders)

	if (photosPage) {
		displayPhotos.value = photosPage.content
		totalPhotosCount.value = photosPage.totalElements
		hasMorePhotos.value = !photosPage.last
		currentPage.value = 0
		generateMissingThumbnails(photosPage.content)
	}

	loading.value = false
	if (hasMorePhotos.value) setupObserver()
}

/** Leaves search mode and restores the folder view the user was browsing before searching. */
const exitSearch = async () => {
	searchActive.value = false
	const current = folderStack.value[folderStack.value.length - 1]
	if (!current) {
		await loadRoot()
		return
	}
	loading.value = true
	displayFolders.value = []
	loadingFolderThumbnails.value = new Set()
	resetPhotoState()
	await loadFolderContent(current)
	loading.value = false
	if (hasMorePhotos.value) setupObserver()
}

/** Clears the search controls and immediately returns to the folder-browsing view. */
const clearSearch = () => {
	searchQuery.value = ""
	searchRating.value = ""
	if (searchActive.value) exitSearch()
}

/** Sets up IntersectionObserver on the sentinel after a tick so the DOM is ready. */
const setupObserver = () => {
	intersectionObserver?.disconnect()
	intersectionObserver = null
	nextTick(() => {
		if (!sentinelRef.value) return
		intersectionObserver = new IntersectionObserver(
			entries => { if (entries[0].isIntersecting) loadMorePhotos() },
			{ root: scrollAreaRef.value, rootMargin: "200px" }
		)
		intersectionObserver.observe(sentinelRef.value)
	})
}

watch(hasMorePhotos, val => {
	if (val) setupObserver()
	else {
		intersectionObserver?.disconnect()
		intersectionObserver = null
	}
})

/** Resets all photo-pagination state and disconnects the observer. */
const resetPhotoState = () => {
	displayPhotos.value = []
	loadingThumbnails.value = new Set()
	hasMorePhotos.value = false
	currentFolderId.value = null
	currentPage.value = 0
	totalPhotosCount.value = 0
	intersectionObserver?.disconnect()
	intersectionObserver = null
}

/** Returns the display name for a folder (last path segment). */
const folderName = (folder: Folder): string => {
	const parts = folder.path.split("/")
	return parts[parts.length - 1]
}

/**
 * Loads the first page of children folders and direct photos for the given folder,
 * then triggers thumbnail generation for any items missing one.
 *
 * @param folder the folder whose contents to load
 */
const loadFolderContent = async (folder: Folder) => {
	currentFolderId.value = folder.id
	currentPage.value = 0

	const [children, photosPage] = await Promise.all([
		getFolderChildren(folder.id).catch((e: unknown) => { handleError(e); return [] as Folder[] }),
		getFolderPhotosPage(folder.id, 0, PAGE_SIZE).catch((e: unknown) => { handleError(e); return null })
	])

	displayFolders.value = children
	generateMissingFolderThumbnails(children)

	if (photosPage) {
		displayPhotos.value = photosPage.content
		totalPhotosCount.value = photosPage.totalElements
		hasMorePhotos.value = !photosPage.last
		generateMissingThumbnails(photosPage.content)
	}
}

/** Loads the top-level folders and the photos sitting directly in the library root. */
const loadRoot = async () => {
	loading.value = true
	folderStack.value = []
	displayFolders.value = []
	loadingFolderThumbnails.value = new Set()
	resetPhotoState()

	// The library root directory itself is persisted as a folder with an empty path.
	// Depending on how the scan computes nesting levels it can land at level 0 or level 1,
	// so gather candidates from both. Folders with an empty path are root containers: their
	// direct photos belong inline at the top level, never hidden behind a folder card.
	const [level0, level1] = await Promise.all([
		getFoldersByLevel(0).catch(() => [] as Folder[]),
		getFoldersByLevel(1).catch((e: unknown) => { handleError(e); return [] as Folder[] })
	])

	const rootContainers = [...level0, ...level1].filter(f => f.path === "")
	const folders = level1.filter(f => f.path !== "")

	displayFolders.value = folders
	generateMissingFolderThumbnails(folders)

	const photoArrays = await Promise.all(
		rootContainers.map(f =>
			getFolderPhotosPage(f.id, 0, ROOT_PAGE_SIZE)
				.then(p => p.content)
				.catch(() => [] as MediaFile[])
		)
	)
	displayPhotos.value = photoArrays.flat()
	totalPhotosCount.value = displayPhotos.value.length
	generateMissingThumbnails(displayPhotos.value)

	loading.value = false
}

/**
 * Opens the grid directly on the folder with the given id, rebuilding the breadcrumb
 * from the folder's ancestor chain so navigating up and Home behave as if the user had
 * drilled in by hand. Falls back to the library root when the chain cannot be resolved
 * (e.g. an unknown id or a photo that sits directly in the root).
 *
 * @param folderId the id of the folder to open
 */
const loadFolderById = async (folderId: number) => {
	loading.value = true
	displayFolders.value = []
	loadingFolderThumbnails.value = new Set()
	resetPhotoState()

	let chain: Folder[]
	try {
		chain = await getFolderChain(folderId)
	} catch (e) {
		handleError(e)
		await loadRoot()
		return
	}

	if (chain.length === 0) {
		await loadRoot()
		return
	}

	folderStack.value = chain
	await loadFolderContent(chain[chain.length - 1])

	loading.value = false
	if (hasMorePhotos.value) setupObserver()
}

/**
 * Handles a click on a folder card. In search mode the breadcrumb is unrelated to the result,
 * so the folder is opened by id (rebuilding the chain) after leaving search mode; otherwise the
 * folder is opened by drilling in from the current position.
 *
 * @param folder the folder whose card was clicked
 */
const onFolderClick = async (folder: Folder) => {
	if (!searchActive.value) {
		await navigateInto(folder)
		return
	}
	// Leave search mode without triggering exitSearch (loadFolderById rebuilds the view itself).
	searchActive.value = false
	searchQuery.value = ""
	searchRating.value = ""
	await loadFolderById(folder.id)
}

/**
 * Navigates into a folder: pushes it onto the breadcrumb stack, then loads its
 * children and direct photos in parallel.
 */
const navigateInto = async (folder: Folder) => {
	loading.value = true
	folderStack.value = [...folderStack.value, folder]
	displayFolders.value = []
	loadingFolderThumbnails.value = new Set()
	resetPhotoState()

	await loadFolderContent(folder)

	loading.value = false
	if (hasMorePhotos.value) setupObserver()
}

/**
 * Jumps to a specific point in the breadcrumb trail.  Passing -1 resets to root.
 */
const navigateTo = async (stackIndex: number) => {
	selectedPhotoId.value = null
	if (stackIndex < 0) {
		await loadRoot()
		return
	}
	const target = folderStack.value[stackIndex]
	folderStack.value = folderStack.value.slice(0, stackIndex + 1)
	loading.value = true
	displayFolders.value = []
	loadingFolderThumbnails.value = new Set()
	resetPhotoState()

	await loadFolderContent(target)

	loading.value = false
	if (hasMorePhotos.value) setupObserver()
}

/**
 * Loads the next page of photos and appends them to the grid. The source is the active search
 * when in search mode, otherwise the current folder.
 */
const loadMorePhotos = async () => {
	if (!hasMorePhotos.value || loadingMore.value) return
	if (!searchActive.value && currentFolderId.value === null) return
	loadingMore.value = true
	const nextPage = currentPage.value + 1
	try {
		const response = searchActive.value
			? await searchPhotosPage(searchQuery.value.trim(), selectedRating(), nextPage, PAGE_SIZE)
			: await getFolderPhotosPage(currentFolderId.value!, nextPage, PAGE_SIZE)
		displayPhotos.value = [...displayPhotos.value, ...response.content]
		currentPage.value = nextPage
		hasMorePhotos.value = !response.last
		generateMissingThumbnails(response.content)
	} catch (e) {
		handleError(e)
	} finally {
		loadingMore.value = false
		// If sentinel is still visible after DOM update, keep loading pages.
		nextTick(() => {
			if (!hasMorePhotos.value || !sentinelRef.value || !scrollAreaRef.value) return
			const containerRect = scrollAreaRef.value.getBoundingClientRect()
			const rect = sentinelRef.value.getBoundingClientRect()
			if (rect.top < containerRect.bottom + 200) loadMorePhotos()
		})
	}
}

/**
 * For each folder in the list that has no thumbnail, fires a concurrent API request to
 * generate one.  Each folder card shows a spinner while its request is in-flight; once the
 * thumbnail id is returned the card switches to displaying the image.
 *
 * @param folders the list of folders to inspect
 */
const generateMissingFolderThumbnails = (folders: Folder[]) => {
	for (const folder of folders) {
		if (folder.thumbnailId != null) continue
		loadingFolderThumbnails.value.add(folder.id)
		createFolderThumbnail(folder.id)
			.then(thumbnailId => {
				folder.thumbnailId = thumbnailId
			})
			.catch(() => { /* silently ignore — card falls back to the folder icon */ })
			.finally(() => {
				loadingFolderThumbnails.value.delete(folder.id)
			})
	}
}

/**
 * For each photo in the list that has no thumbnail, fires a concurrent API request to
 * generate one.  Each photo shows a spinner while its request is in-flight; once the
 * thumbnail id is returned the photo card switches to displaying the image.
 *
 * @param photos the list of photos to inspect
 */
const generateMissingThumbnails = (photos: MediaFile[]) => {
	for (const photo of photos) {
		if (photo.thumbnailId != null) continue
		loadingThumbnails.value.add(photo.id)
		createPhotoThumbnail(photo.id)
			.then(thumbnailId => {
				photo.thumbnailId = thumbnailId
			})
			.catch(() => { /* silently ignore — card falls back to the default icon */ })
			.finally(() => {
				loadingThumbnails.value.delete(photo.id)
			})
	}
}

/**
 * Opens the slideshow scoped to the folder currently being viewed. At the library
 * root no folder id is passed, so the slideshow draws from the entire library.
 */
const launchSlideshow = () => {
	const folder = folderStack.value[folderStack.value.length - 1]
	router.push({
		name: "Slideshow",
		query: folder ? { folderId: folder.id } : {}
	})
}

/**
 * Opens the slideshow scoped to a specific folder card, without first navigating
 * into it.  Invoked from the play overlay on a folder thumbnail.
 *
 * @param folder the folder to play a slideshow from
 */
const launchFolderSlideshow = (folder: Folder) => {
	router.push({
		name: "Slideshow",
		query: { folderId: folder.id }
	})
}

const handleError = (error: unknown) => {
	const err = error as { response?: { data?: { message?: string; status?: number } }; message?: string }
	errorStore.set(true, err.response?.data?.message ?? err.message ?? "", err.response?.data?.status ?? 500)
}

const selectedPhotoIndex = computed(() =>
	displayPhotos.value.findIndex(p => p.id === selectedPhotoId.value)
)

const hasPrevPhoto = computed(() => selectedPhotoIndex.value > 0)

const hasNextPhoto = computed(() => selectedPhotoIndex.value < displayPhotos.value.length - 1)

const goToPrevPhoto = () => {
	const idx = selectedPhotoIndex.value
	if (idx > 0) selectedPhotoId.value = displayPhotos.value[idx - 1].id
}

const goToNextPhoto = () => {
	const idx = selectedPhotoIndex.value
	if (idx < displayPhotos.value.length - 1) selectedPhotoId.value = displayPhotos.value[idx + 1].id
}

const onPhotoDeleted = (id: number) => {
	displayPhotos.value = displayPhotos.value.filter(p => p.id !== id)
	selectedPhotoId.value = null
}
</script>

<template>
	<div class="photos-page">

		<!-- Always-visible header: centered search bar, then breadcrumb/results row -->
		<div class="photos-header">

			<!-- Centered search: free-text query over paths/filenames + rating filter -->
			<div class="photos-search">
				<div class="input-group input-group-sm photos-search__group">
					<span class="input-group-text">
						<svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" fill="currentColor" viewBox="0 0 16 16">
							<path d="M11.742 10.344a6.5 6.5 0 1 0-1.397 1.398h-.001q.044.06.098.115l3.85 3.85a1 1 0 0 0 1.415-1.414l-3.85-3.85a1 1 0 0 0-.115-.1zM12 6.5a5.5 5.5 0 1 1-11 0 5.5 5.5 0 0 1 11 0"/>
						</svg>
					</span>
					<input
						type="text"
						class="form-control"
						v-model="searchQuery"
						:placeholder="language.get('Search by path or filename')"
						:aria-label="language.get('Search')"
					/>
					<select
						class="form-select photos-search__rating"
						v-model="searchRating"
						:title="language.get('Rating')"
						:aria-label="language.get('Rating')"
					>
						<option value="">{{ language.get("All ratings") }}</option>
						<option value="1">1 ★</option>
						<option value="2">2 ★</option>
						<option value="3">3 ★</option>
						<option value="4">4 ★</option>
						<option value="5">5 ★</option>
					</select>
					<button
						v-if="searchActive"
						type="button"
						class="btn btn-outline-secondary"
						@click="clearSearch"
						:title="language.get('Clear')"
						:aria-label="language.get('Clear')"
					>
						<svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" fill="currentColor" viewBox="0 0 16 16">
							<path d="M2.146 2.854a.5.5 0 1 1 .708-.708L8 7.293l5.146-5.147a.5.5 0 0 1 .708.708L8.707 8l5.147 5.146a.5.5 0 0 1-.708.708L8 8.707l-5.146 5.147a.5.5 0 0 1-.708-.708L7.293 8z"/>
						</svg>
					</button>
				</div>
			</div>

			<!-- Breadcrumb (browse) or results label (search) + photo count + slideshow -->
			<div class="photos-subheader">
				<nav v-if="!searchActive" aria-label="breadcrumb">
					<ol class="breadcrumb mb-0">
						<li class="breadcrumb-item">
							<a href="#" class="text-decoration-none breadcrumb-home" @click.prevent="navigateTo(-1)" :title="language.get('Home')">
								<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="currentColor" viewBox="0 0 16 16">
									<path d="M8.354 1.146a.5.5 0 0 0-.708 0l-6 6A.5.5 0 0 0 1.5 8v6a.5.5 0 0 0 .5.5h3.5a.5.5 0 0 0 .5-.5v-3h4v3a.5.5 0 0 0 .5.5H14a.5.5 0 0 0 .5-.5V8a.5.5 0 0 0-.146-.354L13 6.293V2.5a.5.5 0 0 0-.5-.5h-1a.5.5 0 0 0-.5.5v1.293L8.354 1.146z"/>
								</svg>
							</a>
						</li>
						<li
							v-for="(folder, index) in folderStack"
							:key="folder.id"
							class="breadcrumb-item"
							:class="{ active: index === folderStack.length - 1 }"
						>
							<a
								v-if="index < folderStack.length - 1"
								href="#"
								class="text-decoration-none"
								@click.prevent="navigateTo(index)"
							>{{ folderName(folder) }}</a>
							<span v-else>{{ folderName(folder) }}</span>
						</li>
					</ol>
				</nav>
				<div v-else class="text-muted small search-results-label">
					{{ language.get("Search results") }}
				</div>
				<div v-if="totalPhotosCount > 0" class="text-muted small">
					{{ displayPhotos.length }}
					<template v-if="hasMorePhotos">/ {{ totalPhotosCount }}</template>
					{{ language.get("photos") }}
				</div>
				<button
					v-if="!searchActive && (folderStack.length > 0 || displayPhotos.length > 0)"
					type="button"
					class="btn btn-sm btn-outline-primary slideshow-btn"
					@click="launchSlideshow"
					:title="language.get('Slideshow')"
				>
					<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="currentColor" viewBox="0 0 16 16">
						<path d="m11.596 8.697-6.363 3.692c-.54.313-1.233-.066-1.233-.697V4.308c0-.63.692-1.01 1.233-.696l6.363 3.692a.802.802 0 0 1 0 1.393z"/>
					</svg>
					<span class="ms-1">{{ language.get("Slideshow") }}</span>
				</button>
			</div>
		</div>

		<!-- Photo viewer -->
		<PhotoDetail
			v-if="selectedPhotoId !== null"
			:photoId="selectedPhotoId"
			:hasPrev="hasPrevPhoto"
			:hasNext="hasNextPhoto"
			@close="selectedPhotoId = null"
			@prev="goToPrevPhoto"
			@next="goToNextPhoto"
			@deleted="onPhotoDeleted"
		/>

		<!-- Scrollable grid -->
		<div v-else ref="scrollAreaRef" class="photos-scroll-area">

			<!-- Loading -->
			<div v-if="loading">
				<Loading />
			</div>

			<!-- Unified grid: folders first, then photos -->
			<div v-else>
				<div v-if="searchActive && displayFolders.length === 0 && displayPhotos.length === 0" class="text-muted">
					{{ language.get("No results found.") }}
				</div>
				<div v-else-if="!searchActive && displayFolders.length === 0 && displayPhotos.length === 0 && folderStack.length === 0" class="text-muted">
					{{ language.get("No folders found. Scan a folder to import photos.") }}
				</div>
				<div v-else>
					<div class="row row-cols-2 row-cols-sm-3 row-cols-md-4 row-cols-lg-5 row-cols-xl-6 g-3">

						<!-- Go-up card -->
						<div v-if="!searchActive && folderStack.length > 0" class="col">
							<div class="media-card up-card" @click="navigateTo(folderStack.length - 2)">
								<div class="media-thumb">
									<svg xmlns="http://www.w3.org/2000/svg" width="40" height="40" fill="#6c757d" viewBox="0 0 16 16">
										<path fill-rule="evenodd" d="M8 15a.5.5 0 0 0 .5-.5V2.707l3.146 3.147a.5.5 0 0 0 .708-.708l-4-4a.5.5 0 0 0-.708 0l-4 4a.5.5 0 1 0 .708.708L7.5 2.707V14.5a.5.5 0 0 0 .5.5z"/>
									</svg>
								</div>
								<div class="media-name">{{ language.get("..") }}</div>
							</div>
						</div>

						<!-- Folder cards -->
						<div v-for="folder in displayFolders" :key="'f-' + folder.id" class="col">
							<div class="media-card folder-card" @click="onFolderClick(folder)" :title="folder.path">
								<div class="media-thumb">
									<!-- Play overlay: starts a slideshow scoped to this folder -->
									<button
										type="button"
										class="play-overlay"
										@click.stop="launchFolderSlideshow(folder)"
										:title="language.get('Slideshow')"
										:aria-label="language.get('Slideshow')"
									>
										<svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" fill="white" viewBox="0 0 16 16">
											<path d="m11.596 8.697-6.363 3.692c-.54.313-1.233-.066-1.233-.697V4.308c0-.63.692-1.01 1.233-.696l6.363 3.692a.802.802 0 0 1 0 1.393z"/>
										</svg>
									</button>
									<div v-if="loadingFolderThumbnails.has(folder.id)" class="thumb-generating">
										<span class="spinner-border spinner-border-sm text-secondary" role="status"></span>
									</div>
									<img
										v-else-if="folder.thumbnailId"
										:src="getThumbnailUrl(folder.thumbnailId)"
										:alt="folderName(folder)"
										class="thumb-img"
									/>
									<svg v-else xmlns="http://www.w3.org/2000/svg" width="48" height="48" fill="#a08a50" viewBox="0 0 16 16">
										<path d="M9.828 3h3.982a2 2 0 0 1 1.992 2.181l-.637 7A2 2 0 0 1 13.174 14H2.826a2 2 0 0 1-1.991-1.819l-.637-7a1.99 1.99 0 0 1 .342-1.31L.5 3a2 2 0 0 1 2-2h3.672a2 2 0 0 1 1.414.586l.828.828A2 2 0 0 0 9.828 3zm-8.322.12C1.72 3.042 1.95 3 2.19 3h5.396l-.707-.707A1 1 0 0 0 6.172 2H2.5a1 1 0 0 0-1 .981l.006.139z"/>
									</svg>
									<!-- Folder type badge -->
									<div class="type-badge">
										<svg xmlns="http://www.w3.org/2000/svg" width="12" height="12" fill="white" viewBox="0 0 16 16">
											<path d="M9.828 3h3.982a2 2 0 0 1 1.992 2.181l-.637 7A2 2 0 0 1 13.174 14H2.826a2 2 0 0 1-1.991-1.819l-.637-7a1.99 1.99 0 0 1 .342-1.31L.5 3a2 2 0 0 1 2-2h3.672a2 2 0 0 1 1.414.586l.828.828A2 2 0 0 0 9.828 3zm-8.322.12C1.72 3.042 1.95 3 2.19 3h5.396l-.707-.707A1 1 0 0 0 6.172 2H2.5a1 1 0 0 0-1 .981l.006.139z"/>
										</svg>
									</div>
								</div>
								<div class="media-name">{{ folderName(folder) }}</div>
							</div>
						</div>

						<!-- Photo cards -->
						<div v-for="photo in displayPhotos" :key="'p-' + photo.id" class="col">
							<div class="media-card photo-card" @click="selectedPhotoId = photo.id" :title="photo.filename">
								<div class="media-thumb">
									<div v-if="loadingThumbnails.has(photo.id)" class="thumb-generating">
										<span class="spinner-border spinner-border-sm text-secondary" role="status"></span>
									</div>
									<img
										v-else-if="photo.thumbnailId"
										:src="getThumbnailUrl(photo.thumbnailId)"
										:alt="photo.filename"
										class="thumb-img"
									/>
									<svg v-else xmlns="http://www.w3.org/2000/svg" width="48" height="48" fill="#7090b0" viewBox="0 0 16 16">
										<path d="M6.502 7a1.5 1.5 0 1 0 0-3 1.5 1.5 0 0 0 0 3z"/>
										<path d="M14 14a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V2a2 2 0 0 1 2-2h5.5L14 4.5V14zM4 1a1 1 0 0 0-1 1v10l2.224-2.224a.5.5 0 0 1 .61-.075L8 11l2.157-3.02a.5.5 0 0 1 .76-.063L13 10V4.5h-2A1.5 1.5 0 0 1 9.5 3V1H4z"/>
									</svg>
									<!-- Photo type badge -->
									<div class="type-badge">
										<svg xmlns="http://www.w3.org/2000/svg" width="12" height="12" fill="white" viewBox="0 0 16 16">
											<path d="M6.502 7a1.5 1.5 0 1 0 0-3 1.5 1.5 0 0 0 0 3z"/>
											<path d="M14 14a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V2a2 2 0 0 1 2-2h5.5L14 4.5V14zM4 1a1 1 0 0 0-1 1v10l2.224-2.224a.5.5 0 0 1 .61-.075L8 11l2.157-3.02a.5.5 0 0 1 .76-.063L13 10V4.5h-2A1.5 1.5 0 0 1 9.5 3V1H4z"/>
										</svg>
									</div>
								</div>
								<div class="media-name">{{ photo.filename }}</div>
							</div>
						</div>

						<!-- Infinite-scroll sentinel: observed to trigger next-page loads -->
						<div v-if="hasMorePhotos || loadingMore" ref="sentinelRef" class="load-more-sentinel">
							<div v-if="loadingMore" class="text-center py-3">
								<span class="spinner-border spinner-border-sm text-secondary" role="status"></span>
							</div>
						</div>

					</div>
				</div>
			</div>

		</div>

		<Error />

	</div>
</template>

<style scoped>
	.media-card {
		cursor: pointer;
		border: 1px solid #dee2e6;
		border-radius: 8px;
		overflow: hidden;
		transition: border-color 0.15s, box-shadow 0.15s;
		user-select: none;
	}

	.media-card:hover {
		border-color: #0d6efd;
		box-shadow: 0 0 0 2px rgba(13, 110, 253, 0.15);
	}

	.media-thumb {
		aspect-ratio: 1;
		display: flex;
		align-items: center;
		justify-content: center;
		position: relative;
	}

	.folder-card {
		border-color: #868e96;
		border-width: 2px;
	}

	.folder-card .media-thumb {
		background-color: #fdf6e3;
	}

	.photo-card .media-thumb {
		background-color: #eef2f7;
	}

	.up-card .media-thumb {
		background-color: #f8f9fa;
	}

	.up-card .media-name {
		color: #6c757d;
		font-weight: 500;
	}

	.media-name {
		padding: 4px 8px 6px;
		font-size: 0.75rem;
		text-overflow: ellipsis;
		overflow: hidden;
		white-space: nowrap;
		border-top: 1px solid #dee2e6;
		background: #fff;
	}

	.thumb-img {
		width: 100%;
		height: 100%;
		object-fit: cover;
	}

	.thumb-generating {
		display: flex;
		align-items: center;
		justify-content: center;
		width: 100%;
		height: 100%;
	}

	.photos-page {
		display: flex;
		flex-direction: column;
		flex: 1;
		min-height: 0;
		overflow: hidden;
	}

	.photos-header {
		flex-shrink: 0;
		display: flex;
		flex-direction: column;
		gap: 0.5rem;
		padding: 0.75rem 0 0.5rem;
		border-bottom: 1px solid #dee2e6;
		margin-bottom: 0.5rem;
	}

	.photos-search {
		display: flex;
		justify-content: center;
	}

	.photos-search__group {
		width: 100%;
		max-width: 520px;
	}

	.photos-search__rating {
		flex: 0 0 auto;
		width: auto;
		min-width: 7.5rem;
	}

	.photos-subheader {
		display: flex;
		align-items: flex-start;
		justify-content: space-between;
		gap: 1rem;
		min-height: 1.5rem;
	}

	.search-results-label {
		font-weight: 500;
	}

	.slideshow-btn {
		flex-shrink: 0;
		display: inline-flex;
		align-items: center;
		white-space: nowrap;
	}

	.photos-scroll-area {
		flex: 1;
		min-height: 0;
		overflow-x: hidden;
		overflow-y: auto;
		padding-top: 0.5rem;
	}

	.breadcrumb-home {
		display: inline-flex;
		align-items: center;
	}

	.play-overlay {
		position: absolute;
		bottom: 6px;
		right: 6px;
		width: 36px;
		height: 36px;
		padding: 0;
		border: none;
		border-radius: 50%;
		background-color: rgba(13, 110, 253, 0.85);
		display: flex;
		align-items: center;
		justify-content: center;
		opacity: 0;
		transform: scale(0.9);
		transition: opacity 0.15s, transform 0.15s, background-color 0.15s;
		cursor: pointer;
		z-index: 2;
	}

	.play-overlay svg {
		margin-left: 2px;
	}

	.folder-card:hover .play-overlay,
	.play-overlay:focus-visible {
		opacity: 1;
		transform: scale(1);
	}

	.play-overlay:hover {
		background-color: rgba(13, 110, 253, 1);
	}

	.type-badge {
		position: absolute;
		top: 6px;
		right: 6px;
		width: 22px;
		height: 22px;
		border-radius: 4px;
		background-color: rgba(0, 0, 0, 0.45);
		display: flex;
		align-items: center;
		justify-content: center;
		z-index: 1;
	}

	.load-more-sentinel {
		min-height: 48px;
	}
</style>
