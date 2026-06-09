<script setup lang="ts">
import { ref, Ref, onMounted } from "vue"
import { language } from "@/functions/languageStore.ts"
import { errorStore } from "@/components/error/errorStore.ts"
import { scanLibraryFolders, clearLibrary, createPhotoThumbnail } from "@/api/photo.ts"
import { getFoldersByLevel, getFolderChildren, getFolderPhotos, getThumbnailUrl } from "@/api/folder.ts"
import { ScanResult, MediaFile, Folder } from "@/types"
import Error from "@/components/error/Error.vue"
import Loading from "@/components/utilities/Loading.vue"
import PhotoDetail from "@/components/PhotoDetail.vue"

type ViewMode = "folders" | "photos"

const scanning = ref(false)
const loading = ref(false)
const clearing = ref(false)
const scanResult: Ref<ScanResult | null> = ref(null)
const selectedPhotoId: Ref<number | null> = ref(null)

/** Stack of folders the user has navigated into; empty means root (level 1). */
const folderStack: Ref<Folder[]> = ref([])

const viewMode: Ref<ViewMode> = ref("folders")
const displayFolders: Ref<Folder[]> = ref([])
const displayPhotos: Ref<MediaFile[]> = ref([])

/** Set of photo ids currently awaiting thumbnail generation. */
const loadingThumbnails: Ref<Set<number>> = ref(new Set())

onMounted(() => {
	loadRoot()
})

/** Returns the display name for a folder (last path segment). */
const folderName = (folder: Folder): string => {
	const parts = folder.path.split("/")
	return parts[parts.length - 1]
}

/** Loads the level-1 folders (library root). */
const loadRoot = async () => {
	loading.value = true
	folderStack.value = []
	displayFolders.value = []
	displayPhotos.value = []
	loadingThumbnails.value = new Set()

	await getFoldersByLevel(1)
		.then((data: Folder[]) => {
			displayFolders.value = data
			viewMode.value = "folders"
		})
		.catch(handleError)

	loading.value = false
}

/**
 * Navigates into a folder: pushes it onto the breadcrumb stack, then loads its
 * children.  If the folder has no children, its photos are loaded instead.
 */
const navigateInto = async (folder: Folder) => {
	loading.value = true
	folderStack.value = [...folderStack.value, folder]
	displayFolders.value = []
	displayPhotos.value = []

	await getFolderChildren(folder.id)
		.then(async (children: Folder[]) => {
			if (children.length > 0) {
				displayFolders.value = children
				viewMode.value = "folders"
			} else {
				await getFolderPhotos(folder.id)
					.then((photos: MediaFile[]) => {
						displayPhotos.value = photos
						viewMode.value = "photos"
						generateMissingThumbnails(photos)
					})
					.catch(handleError)
			}
		})
		.catch(handleError)

	loading.value = false
}

/**
 * Jumps to a specific point in the breadcrumb trail.  Passing -1 resets to root.
 */
const navigateTo = async (stackIndex: number) => {
	if (stackIndex < 0) {
		await loadRoot()
		return
	}
	const target = folderStack.value[stackIndex]
	folderStack.value = folderStack.value.slice(0, stackIndex + 1)
	loading.value = true
	displayFolders.value = []
	displayPhotos.value = []
	loadingThumbnails.value = new Set()

	await getFolderChildren(target.id)
		.then(async (children: Folder[]) => {
			if (children.length > 0) {
				displayFolders.value = children
				viewMode.value = "folders"
			} else {
				await getFolderPhotos(target.id)
					.then((photos: MediaFile[]) => {
						displayPhotos.value = photos
						viewMode.value = "photos"
						generateMissingThumbnails(photos)
					})
					.catch(handleError)
			}
		})
		.catch(handleError)

	loading.value = false
}

const onScan = async () => {
	scanResult.value = null
	scanning.value = true

	await scanLibraryFolders()
		.then((result: ScanResult | undefined) => {
			if (result) scanResult.value = result
			loadRoot()
		})
		.catch(handleError)

	scanning.value = false
}

const onClearLibrary = async () => {
	if (!confirm(language.get("Are you sure you want to delete all photos from the library?"))) return

	clearing.value = true

	await clearLibrary()
		.then(() => {
			loadRoot()
			scanResult.value = null
		})
		.catch(handleError)

	clearing.value = false
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

const handleError = (error: unknown) => {
	const err = error as { response?: { data?: { message?: string; status?: number } }; message?: string }
	errorStore.set(true, err.response?.data?.message ?? err.message ?? "", err.response?.data?.status ?? 500)
}
</script>

<template>
	<div class="container-fluid mt-4">

		<!-- Header -->
		<div class="d-flex justify-content-between align-items-center mb-3">
			<h4 class="mb-0">{{ language.get("Photos") }}</h4>
			<button
				class="btn btn-outline-danger btn-sm"
				:disabled="clearing"
				@click="onClearLibrary"
			>
				<span v-if="clearing" class="spinner-border spinner-border-sm me-1" role="status"></span>
				{{ language.get("Clean Library") }}
			</button>
		</div>

		<!-- Scan button -->
		<div class="mb-3">
			<button class="btn btn-primary btn-sm" :disabled="scanning" @click="onScan">
				<span v-if="scanning" class="spinner-border spinner-border-sm me-1" role="status"></span>
				{{ scanning ? language.get("Scanning...") : language.get("Scan Library") }}
			</button>
		</div>

		<!-- Scan result -->
		<div v-if="scanResult" class="alert mb-3" :class="scanResult.errors > 0 ? 'alert-warning' : 'alert-success'">
			<strong>{{ scanResult.message }}</strong>
			<ul class="mb-0 mt-1">
				<li>{{ language.get("Added") }}: {{ scanResult.added }}</li>
				<li>{{ language.get("Skipped") }}: {{ scanResult.skipped }}</li>
				<li>{{ language.get("Errors") }}: {{ scanResult.errors }}</li>
				<li>{{ language.get("Folders scanned") }}: {{ scanResult.foldersScanned }}</li>
				<li>{{ language.get("Folders skipped") }}: {{ scanResult.foldersSkipped }}</li>
			</ul>
		</div>

		<!-- Breadcrumb -->
		<nav aria-label="breadcrumb" class="mb-3">
			<ol class="breadcrumb mb-0">
				<li class="breadcrumb-item">
					<a href="#" class="text-decoration-none" @click.prevent="navigateTo(-1)">
						{{ language.get("Home") }}
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

		<!-- Loading -->
		<div v-if="loading">
			<Loading />
		</div>

		<!-- Folder grid -->
		<div v-else-if="viewMode === 'folders'">
			<div v-if="displayFolders.length === 0 && folderStack.length === 0" class="text-muted">
				{{ language.get("No folders found. Scan a folder to import photos.") }}
			</div>
			<div class="row row-cols-2 row-cols-sm-3 row-cols-md-4 row-cols-lg-5 row-cols-xl-6 g-3">
				<!-- Go-up card -->
				<div v-if="folderStack.length > 0" class="col">
					<div class="media-card up-card" @click="navigateTo(folderStack.length - 2)">
						<div class="media-thumb">
							<svg xmlns="http://www.w3.org/2000/svg" width="40" height="40" fill="#6c757d" viewBox="0 0 16 16">
								<path fill-rule="evenodd" d="M8 15a.5.5 0 0 0 .5-.5V2.707l3.146 3.147a.5.5 0 0 0 .708-.708l-4-4a.5.5 0 0 0-.708 0l-4 4a.5.5 0 1 0 .708.708L7.5 2.707V14.5a.5.5 0 0 0 .5.5z"/>
							</svg>
						</div>
						<div class="media-name">{{ language.get("..") }}</div>
					</div>
				</div>
				<div v-for="folder in displayFolders" :key="folder.id" class="col">
					<div class="media-card folder-card" @click="navigateInto(folder)" :title="folder.path">
						<div class="media-thumb">
							<img
								v-if="folder.thumbnailId"
								:src="getThumbnailUrl(folder.thumbnailId)"
								:alt="folderName(folder)"
								class="thumb-img"
							/>
							<svg v-else xmlns="http://www.w3.org/2000/svg" width="48" height="48" fill="#a08a50" viewBox="0 0 16 16">
								<path d="M9.828 3h3.982a2 2 0 0 1 1.992 2.181l-.637 7A2 2 0 0 1 13.174 14H2.826a2 2 0 0 1-1.991-1.819l-.637-7a1.99 1.99 0 0 1 .342-1.31L.5 3a2 2 0 0 1 2-2h3.672a2 2 0 0 1 1.414.586l.828.828A2 2 0 0 0 9.828 3zm-8.322.12C1.72 3.042 1.95 3 2.19 3h5.396l-.707-.707A1 1 0 0 0 6.172 2H2.5a1 1 0 0 0-1 .981l.006.139z"/>
							</svg>
						</div>
						<div class="media-name">{{ folderName(folder) }}</div>
					</div>
				</div>
			</div>
		</div>

		<!-- Photo grid -->
		<div v-else-if="viewMode === 'photos'">
			<div v-if="displayPhotos.length === 0" class="text-muted">
				{{ language.get("No photos found in this folder.") }}
			</div>
			<div>
				<div v-if="displayPhotos.length > 0" class="text-muted small mb-2">{{ displayPhotos.length }} {{ language.get("photos") }}</div>
				<div class="row row-cols-2 row-cols-sm-3 row-cols-md-4 row-cols-lg-5 row-cols-xl-6 g-3">
					<!-- Go-up card -->
					<div v-if="folderStack.length > 0" class="col">
						<div class="media-card up-card" @click="navigateTo(folderStack.length - 2)">
							<div class="media-thumb">
								<svg xmlns="http://www.w3.org/2000/svg" width="40" height="40" fill="#6c757d" viewBox="0 0 16 16">
									<path fill-rule="evenodd" d="M8 15a.5.5 0 0 0 .5-.5V2.707l3.146 3.147a.5.5 0 0 0 .708-.708l-4-4a.5.5 0 0 0-.708 0l-4 4a.5.5 0 1 0 .708.708L7.5 2.707V14.5a.5.5 0 0 0 .5.5z"/>
								</svg>
							</div>
							<div class="media-name">{{ language.get("..") }}</div>
						</div>
					</div>
					<div v-for="photo in displayPhotos" :key="photo.id" class="col">
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
							</div>
							<div class="media-name">{{ photo.filename }}</div>
						</div>
					</div>
				</div>
			</div>
		</div>

		<Error />

		<PhotoDetail :photoId="selectedPhotoId" @close="selectedPhotoId = null" />
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
</style>
