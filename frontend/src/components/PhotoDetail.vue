<script setup lang="ts">
import { ref, watch, computed, onMounted, onBeforeUnmount } from "vue"
import { getPhotoById, getPhotoImageUrl } from "@/api/photo.ts"
import { language } from "@/functions/languageStore.ts"
import { errorStore } from "@/components/error/errorStore.ts"
import type { PhotoDetail } from "@/types"

/**
 * Props accepted by the PhotoDetail viewer component.
 *
 * @property photoId - the id of the photo to display, or null when nothing is selected
 * @property hasPrev - whether a previous photo is available in the current folder
 * @property hasNext - whether a next photo is available in the current folder
 */
const props = defineProps<{
	photoId: number | null
	hasPrev: boolean
	hasNext: boolean
}>()

/**
 * Emitted events for navigation and closing the viewer.
 */
const emit = defineEmits<{
	(e: "close"): void
	(e: "prev"): void
	(e: "next"): void
}>()

const photo = ref<PhotoDetail | null>(null)
const loading = ref(false)
const sidebarVisible = ref(true)

/** Constructs the image src URL for the currently loaded photo. */
const imageUrl = computed(() =>
	photo.value ? getPhotoImageUrl(photo.value.id) : ""
)

/** Right offset for buttons that should stay to the left of the sidebar when it is open. */
const rightOffset = computed(() => sidebarVisible.value ? "282px" : "12px")

/** Formats a file size in bytes into a human-readable string (KB / MB). */
const formatFilesize = (bytes: number | null): string => {
	if (bytes === null) return "—"
	if (bytes < 1024) return `${bytes} B`
	if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
	return `${(bytes / (1024 * 1024)).toFixed(2)} MB`
}

/** Formats an ISO datetime string to a localised display string. */
const formatDate = (iso: string | null): string => {
	if (!iso) return "—"
	return new Date(iso).toLocaleString()
}

const handleKeydown = (e: KeyboardEvent) => {
	if (e.key === "ArrowLeft" && props.hasPrev) emit("prev")
	if (e.key === "ArrowRight" && props.hasNext) emit("next")
	if (e.key === "Escape") emit("close")
}

onMounted(() => window.addEventListener("keydown", handleKeydown))
onBeforeUnmount(() => window.removeEventListener("keydown", handleKeydown))

watch(
	() => props.photoId,
	async (id) => {
		if (id === null) return

		loading.value = true
		photo.value = null

		await getPhotoById(id)
			.then((result: PhotoDetail) => {
				photo.value = result
			})
			.catch((error: unknown) => {
				const err = error as { response?: { data?: { message?: string; status?: number } }; message?: string }
				errorStore.set(true, err.response?.data?.message ?? err.message ?? "", err.response?.data?.status ?? 500)
				emit("close")
			})

		loading.value = false
	},
	{ immediate: true }
)
</script>

<template>
	<div class="photo-viewer">

		<!-- Back button -->
		<button class="viewer-btn viewer-close" @click="emit('close')" :aria-label="language.get('Close')">
			<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="currentColor" viewBox="0 0 16 16">
				<path fill-rule="evenodd" d="M15 8a.5.5 0 0 0-.5-.5H2.707l3.147-3.146a.5.5 0 1 0-.708-.708l-4 4a.5.5 0 0 0 0 .708l4 4a.5.5 0 0 0 .708-.708L2.707 8.5H14.5A.5.5 0 0 0 15 8z"/>
			</svg>
		</button>

		<!-- Loading -->
		<div v-if="loading" class="viewer-loading">
			<div class="spinner-border text-light" role="status">
				<span class="visually-hidden">{{ language.get("Loading...") }}</span>
			</div>
		</div>

		<!-- Photo + controls -->
		<template v-else-if="photo">
			<img :src="imageUrl" :alt="photo.filename" class="viewer-image" />

			<!-- Prev arrow -->
			<button
				v-if="hasPrev"
				class="viewer-btn nav-btn nav-prev"
				@click="emit('prev')"
				:aria-label="language.get('Previous')"
			>
				<svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" fill="currentColor" viewBox="0 0 16 16">
					<path fill-rule="evenodd" d="M11.354 1.646a.5.5 0 0 1 0 .708L5.707 8l5.647 5.646a.5.5 0 0 1-.708.708l-6-6a.5.5 0 0 1 0-.708l6-6a.5.5 0 0 1 .708 0z"/>
				</svg>
			</button>

			<!-- Next arrow -->
			<button
				v-if="hasNext"
				class="viewer-btn nav-btn nav-next"
				:style="{ right: rightOffset }"
				@click="emit('next')"
				:aria-label="language.get('Next')"
			>
				<svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" fill="currentColor" viewBox="0 0 16 16">
					<path fill-rule="evenodd" d="M4.646 1.646a.5.5 0 0 1 .708 0l6 6a.5.5 0 0 1 0 .708l-6 6a.5.5 0 0 1-.708-.708L10.293 8 4.646 2.354a.5.5 0 0 1 0-.708z"/>
				</svg>
			</button>

			<!-- Sidebar toggle -->
			<button
				class="viewer-btn sidebar-toggle"
				:style="{ right: rightOffset }"
				@click="sidebarVisible = !sidebarVisible"
				:title="sidebarVisible ? language.get('Hide details') : language.get('Show details')"
			>
				<svg v-if="sidebarVisible" xmlns="http://www.w3.org/2000/svg" width="14" height="14" fill="currentColor" viewBox="0 0 16 16">
					<path fill-rule="evenodd" d="M4.646 1.646a.5.5 0 0 1 .708 0l6 6a.5.5 0 0 1 0 .708l-6 6a.5.5 0 0 1-.708-.708L10.293 8 4.646 2.354a.5.5 0 0 1 0-.708z"/>
				</svg>
				<svg v-else xmlns="http://www.w3.org/2000/svg" width="14" height="14" fill="currentColor" viewBox="0 0 16 16">
					<path fill-rule="evenodd" d="M11.354 1.646a.5.5 0 0 1 0 .708L5.707 8l5.647 5.646a.5.5 0 0 1-.708.708l-6-6a.5.5 0 0 1 0-.708l6-6a.5.5 0 0 1 .708 0z"/>
				</svg>
			</button>

			<!-- Details sidebar -->
			<Transition name="sidebar">
				<div v-show="sidebarVisible" class="details-sidebar">
					<h6 class="sidebar-filename">{{ photo.filename }}</h6>

					<div class="detail-list">
						<div class="detail-row">
							<span class="detail-label">{{ language.get("Path") }}</span>
							<span class="detail-value detail-value--small">{{ photo.path }}</span>
						</div>
						<div v-if="photo.width || photo.height" class="detail-row">
							<span class="detail-label">{{ language.get("Dimensions") }}</span>
							<span class="detail-value">{{ photo.width }} × {{ photo.height }} px</span>
						</div>
						<div class="detail-row">
							<span class="detail-label">{{ language.get("File Size") }}</span>
							<span class="detail-value">{{ formatFilesize(photo.filesize) }}</span>
						</div>
						<div v-if="photo.mimeType" class="detail-row">
							<span class="detail-label">{{ language.get("MIME Type") }}</span>
							<span class="detail-value">{{ photo.mimeType }}</span>
						</div>
						<div v-if="photo.dateTaken" class="detail-row">
							<span class="detail-label">{{ language.get("Date Taken") }}</span>
							<span class="detail-value">{{ formatDate(photo.dateTaken) }}</span>
						</div>
						<div v-if="photo.cameraMake || photo.cameraModel" class="detail-row">
							<span class="detail-label">{{ language.get("Camera") }}</span>
							<span class="detail-value">{{ [photo.cameraMake, photo.cameraModel].filter(Boolean).join(" ") }}</span>
						</div>
						<div v-if="photo.latitude !== null && photo.longitude !== null" class="detail-row">
							<span class="detail-label">{{ language.get("GPS") }}</span>
							<span class="detail-value">{{ photo.latitude }}, {{ photo.longitude }}</span>
						</div>
						<div v-if="photo.rating" class="detail-row">
							<span class="detail-label">{{ language.get("Rating") }}</span>
							<span class="detail-value">
								<span v-for="n in 5" :key="n" :class="n <= (photo.rating ?? 0) ? 'star-filled' : 'star-empty'">★</span>
							</span>
						</div>
						<div v-if="photo.name" class="detail-row">
							<span class="detail-label">{{ language.get("Name") }}</span>
							<span class="detail-value">{{ photo.name }}</span>
						</div>
						<div v-if="photo.description" class="detail-row">
							<span class="detail-label">{{ language.get("Description") }}</span>
							<span class="detail-value">{{ photo.description }}</span>
						</div>
						<div v-if="photo.album" class="detail-row">
							<span class="detail-label">{{ language.get("Album") }}</span>
							<span class="detail-value">{{ photo.album }}</span>
						</div>
						<div v-if="photo.viewCount !== null" class="detail-row">
							<span class="detail-label">{{ language.get("Views") }}</span>
							<span class="detail-value">{{ photo.viewCount }}</span>
						</div>
						<div v-if="photo.hash" class="detail-row">
							<span class="detail-label">{{ language.get("Hash") }}</span>
							<span class="detail-value detail-value--small">{{ photo.hash }}</span>
						</div>
						<div v-if="photo.dateCreated" class="detail-row">
							<span class="detail-label">{{ language.get("Added") }}</span>
							<span class="detail-value">{{ formatDate(photo.dateCreated) }}</span>
						</div>
					</div>
				</div>
			</Transition>
		</template>

	</div>
</template>

<style scoped>
	.photo-viewer {
		position: relative;
		width: 100%;
		height: calc(100vh - 130px);
		background: #111;
		border-radius: 8px;
		overflow: hidden;
		display: flex;
		align-items: center;
		justify-content: center;
	}

	.viewer-btn {
		position: absolute;
		z-index: 10;
		background: rgba(0, 0, 0, 0.55);
		border: 1px solid rgba(255, 255, 255, 0.2);
		color: #fff;
		border-radius: 6px;
		padding: 6px 10px;
		cursor: pointer;
		display: flex;
		align-items: center;
		gap: 6px;
		font-size: 0.82rem;
		transition: background 0.15s;
	}

	.viewer-btn:hover {
		background: rgba(255, 255, 255, 0.15);
	}

	.viewer-close {
		top: 12px;
		left: 12px;
	}

	.nav-btn {
		top: 50%;
		transform: translateY(-50%);
		padding: 10px 12px;
	}

	.nav-prev {
		left: 12px;
	}

	.nav-next {
		transition: right 0.25s ease, background 0.15s;
	}

	.sidebar-toggle {
		top: 12px;
		transition: right 0.25s ease, background 0.15s;
	}

	.viewer-loading {
		display: flex;
		align-items: center;
		justify-content: center;
		width: 100%;
		height: 100%;
	}

	.viewer-image {
		width: 100%;
		height: 100%;
		object-fit: contain;
	}

	.details-sidebar {
		position: absolute;
		top: 0;
		right: 0;
		width: 270px;
		height: 100%;
		background: rgba(10, 10, 10, 0.7);
		backdrop-filter: blur(14px);
		-webkit-backdrop-filter: blur(14px);
		border-left: 1px solid rgba(255, 255, 255, 0.07);
		overflow: hidden;
		padding: 16px;
		color: rgba(255, 255, 255, 0.9);
		box-sizing: border-box;
	}

	.sidebar-filename {
		font-size: 0.88rem;
		font-weight: 600;
		word-break: break-word;
		overflow-wrap: break-word;
		margin-bottom: 18px;
		margin-top: 32px;
		color: #fff;
		line-height: 1.4;
	}

	.detail-list {
		display: flex;
		flex-direction: column;
		gap: 12px;
	}

	.detail-row {
		display: flex;
		flex-direction: column;
		gap: 2px;
	}

	.detail-label {
		font-size: 0.68rem;
		text-transform: uppercase;
		letter-spacing: 0.06em;
		color: rgba(255, 255, 255, 0.4);
	}

	.detail-value {
		font-size: 0.82rem;
		color: rgba(255, 255, 255, 0.85);
		word-break: break-word;
		overflow-wrap: break-word;
	}

	.detail-value--small {
		font-size: 0.75rem;
	}

	.star-filled {
		color: #f0b429;
	}

	.star-empty {
		color: rgba(255, 255, 255, 0.25);
	}

	.sidebar-enter-active,
	.sidebar-leave-active {
		transition: transform 0.25s ease, opacity 0.25s ease;
	}

	.sidebar-enter-from,
	.sidebar-leave-to {
		transform: translateX(100%);
		opacity: 0;
	}
</style>
