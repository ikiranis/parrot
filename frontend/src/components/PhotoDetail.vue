<script setup lang="ts">
import { ref, watch, computed, onMounted, onBeforeUnmount } from "vue"
import { getPhotoById, getPhotoImageUrl, incrementPhotoView, setPhotoRating, deletePhoto } from "@/api/photo.ts"
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
 * Emitted events for navigation, closing the viewer, and post-delete cleanup.
 *
 * @event close - user dismissed the viewer
 * @event prev - user requested the previous photo
 * @event next - user requested the next photo
 * @event deleted - photo was deleted; carries the deleted photo id
 */
const emit = defineEmits<{
	(e: "close"): void
	(e: "prev"): void
	(e: "next"): void
	(e: "deleted", id: number): void
}>()

const photo = ref<PhotoDetail | null>(null)
const loading = ref(false)
const sidebarVisible = ref(true)
const hoverRating = ref(0)
const showDeleteModal = ref(false)

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

/**
 * Sets the photo's rating optimistically, then confirms via the API.
 *
 * @param rating integer 1–5
 */
const ratePhoto = async (rating: number) => {
	if (!photo.value) return
	const photoId = photo.value.id
	photo.value = { ...photo.value, rating }
	try {
		const updated = await setPhotoRating(photoId, rating)
		if (photo.value?.id === photoId) photo.value = updated
	} catch {
		// best-effort — optimistic update stays
	}
}

/** Deletes the current photo after confirmation modal is accepted. */
const deleteCurrentPhoto = async () => {
	if (!photo.value) return
	const id = photo.value.id
	showDeleteModal.value = false
	try {
		await deletePhoto(id)
		emit("deleted", id)
	} catch (error: unknown) {
		const err = error as { response?: { data?: { message?: string; status?: number } }; message?: string }
		errorStore.set(true, err.response?.data?.message ?? err.message ?? "", err.response?.data?.status ?? 500)
	}
}

const handleKeydown = (e: KeyboardEvent) => {
	if (showDeleteModal.value) {
		if (e.key === "Escape") showDeleteModal.value = false
		else if (e.key === "Enter") deleteCurrentPhoto()
		return
	}
	if (e.key === "ArrowLeft" && props.hasPrev) emit("prev")
	if (e.key === "ArrowRight" && props.hasNext) emit("next")
	if (e.key === "Escape") emit("close")
	if ((e.key === "d" || e.key === "D") && photo.value) showDeleteModal.value = true
}

const handleKeyup = async (e: KeyboardEvent) => {
	if (!photo.value || showDeleteModal.value) return
	if (!["1", "2", "3", "4", "5"].includes(e.key)) return
	await ratePhoto(parseInt(e.key))
}

onMounted(() => {
	window.addEventListener("keydown", handleKeydown)
	window.addEventListener("keyup", handleKeyup)
})

onBeforeUnmount(() => {
	window.removeEventListener("keydown", handleKeydown)
	window.removeEventListener("keyup", handleKeyup)
})

watch(
	() => props.photoId,
	async (id) => {
		if (id === null) return

		loading.value = true
		photo.value = null

		try {
			photo.value = await getPhotoById(id)
		} catch (error: unknown) {
			const err = error as { response?: { data?: { message?: string; status?: number } }; message?: string }
			errorStore.set(true, err.response?.data?.message ?? err.message ?? "", err.response?.data?.status ?? 500)
			emit("close")
			loading.value = false
			return
		}

		loading.value = false

		try {
			const detail = await incrementPhotoView(id)
			if (photo.value?.id === id) photo.value = { ...photo.value, viewCount: detail.viewCount }
		} catch {
			// best-effort
		}
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
						<div class="detail-row">
							<span class="detail-label">{{ language.get("Rating") }}</span>
							<span class="detail-stars" @mouseleave="hoverRating = 0">
								<span
									v-for="n in 5"
									:key="n"
									class="detail-star"
									:class="{
										'star-filled': hoverRating === 0 && n <= (photo.rating ?? 0),
										'star-hover': hoverRating > 0 && n <= hoverRating,
									}"
									@mouseenter="hoverRating = n"
									@click="ratePhoto(n)"
									:title="language.get('Rating') + ': ' + n + ' (key ' + n + ')'"
								>★</span>
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

					<div class="sidebar-actions">
						<button class="sidebar-delete-btn" @click="showDeleteModal = true" :title="language.get('Delete') + ' (D)'">
							<svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" fill="currentColor" viewBox="0 0 16 16">
								<path d="M5.5 5.5A.5.5 0 0 1 6 6v6a.5.5 0 0 1-1 0V6a.5.5 0 0 1 .5-.5zm2.5 0a.5.5 0 0 1 .5.5v6a.5.5 0 0 1-1 0V6a.5.5 0 0 1 .5-.5zm3 .5a.5.5 0 0 0-1 0v6a.5.5 0 0 0 1 0V6z"/>
								<path fill-rule="evenodd" d="M14.5 3a1 1 0 0 1-1 1H13v9a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V4h-.5a1 1 0 0 1-1-1V2a1 1 0 0 1 1-1H6a1 1 0 0 1 1-1h2a1 1 0 0 1 1 1h3.5a1 1 0 0 1 1 1v1zM4.118 4 4 4.059V13a1 1 0 0 0 1 1h6a1 1 0 0 0 1-1V4.059L11.882 4H4.118zM2.5 3V2h11v1h-11z"/>
							</svg>
							{{ language.get('Delete') }}
						</button>
					</div>
				</div>
			</Transition>
		</template>

		<!-- Delete confirmation modal -->
		<Transition name="modal">
			<div
				v-if="showDeleteModal"
				class="viewer-modal-overlay"
				@click.self="showDeleteModal = false"
			>
				<div class="viewer-modal">
					<h3 class="viewer-modal__title">{{ language.get('Delete Photo') }}</h3>
					<p class="viewer-modal__message">{{ language.get('Are you sure you want to delete this photo?') }}</p>
					<p class="viewer-modal__filename">{{ photo?.filename }}</p>
					<div class="viewer-modal__actions">
						<button class="viewer-modal__btn viewer-modal__btn--cancel" @click="showDeleteModal = false">
							{{ language.get('Cancel') }}
						</button>
						<button class="viewer-modal__btn viewer-modal__btn--confirm" @click="deleteCurrentPhoto">
							{{ language.get('Delete') }}
						</button>
					</div>
				</div>
			</div>
		</Transition>

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
		overflow-y: auto;
		overflow-x: hidden;
		padding: 16px;
		color: rgba(255, 255, 255, 0.9);
		box-sizing: border-box;
		display: flex;
		flex-direction: column;
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
		flex: 1;
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

	.detail-stars {
		display: flex;
		gap: 2px;
	}

	.detail-star {
		font-size: 1.25rem;
		line-height: 1;
		cursor: pointer;
		color: rgba(255, 255, 255, 0.25);
		transition: color 0.1s;
	}

	.star-filled {
		color: #f0b429;
	}

	.star-hover {
		color: #ffd700;
	}

	.sidebar-actions {
		margin-top: 20px;
		padding-top: 16px;
		border-top: 1px solid rgba(255, 255, 255, 0.08);
	}

	.sidebar-delete-btn {
		display: flex;
		align-items: center;
		justify-content: center;
		gap: 6px;
		width: 100%;
		background: rgba(192, 57, 43, 0.25);
		border: 1px solid rgba(192, 57, 43, 0.4);
		color: rgba(255, 120, 100, 0.9);
		border-radius: 6px;
		padding: 6px 12px;
		font-size: 0.82rem;
		cursor: pointer;
		transition: background 0.15s, border-color 0.15s;
	}

	.sidebar-delete-btn:hover {
		background: rgba(192, 57, 43, 0.5);
		border-color: rgba(231, 76, 60, 0.6);
	}

	.viewer-modal-overlay {
		position: absolute;
		inset: 0;
		background: rgba(0, 0, 0, 0.65);
		display: flex;
		align-items: center;
		justify-content: center;
		z-index: 100;
	}

	.viewer-modal {
		background: #1e1e1e;
		border: 1px solid rgba(255, 255, 255, 0.15);
		border-radius: 8px;
		padding: 1.75rem 2rem;
		min-width: 320px;
		max-width: 480px;
		text-align: center;
	}

	.viewer-modal__title {
		color: #fff;
		font-size: 1.1rem;
		font-weight: 600;
		margin: 0 0 0.75rem;
	}

	.viewer-modal__message {
		color: rgba(255, 255, 255, 0.75);
		font-size: 0.9rem;
		margin: 0 0 0.4rem;
	}

	.viewer-modal__filename {
		color: rgba(255, 255, 255, 0.45);
		font-size: 0.8rem;
		overflow: hidden;
		text-overflow: ellipsis;
		white-space: nowrap;
		margin: 0 0 1.5rem;
	}

	.viewer-modal__actions {
		display: flex;
		gap: 0.75rem;
		justify-content: center;
	}

	.viewer-modal__btn {
		padding: 0.5rem 1.4rem;
		border-radius: 6px;
		font-size: 0.9rem;
		cursor: pointer;
		border: 1px solid transparent;
		transition: background 0.15s, border-color 0.15s;
	}

	.viewer-modal__btn--cancel {
		background: rgba(255, 255, 255, 0.1);
		border-color: rgba(255, 255, 255, 0.2);
		color: rgba(255, 255, 255, 0.85);
	}

	.viewer-modal__btn--cancel:hover {
		background: rgba(255, 255, 255, 0.2);
	}

	.viewer-modal__btn--confirm {
		background: #c0392b;
		border-color: #c0392b;
		color: #fff;
	}

	.viewer-modal__btn--confirm:hover {
		background: #e74c3c;
		border-color: #e74c3c;
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

	.modal-enter-active,
	.modal-leave-active {
		transition: opacity 0.18s ease;
	}

	.modal-enter-from,
	.modal-leave-to {
		opacity: 0;
	}
</style>
