<script setup lang="ts">
import { ref, watch, onMounted, onBeforeUnmount, computed } from "vue"
import { Modal } from "bootstrap"
import { getPhotoById, getPhotoImageUrl } from "@/api/photo.ts"
import { language } from "@/functions/languageStore.ts"
import { errorStore } from "@/components/error/errorStore.ts"
import type { PhotoDetail } from "@/types"

/**
 * Props accepted by the PhotoDetail modal component.
 *
 * @property photoId - the id of the photo to display, or null when nothing is selected
 */
const props = defineProps<{
	photoId: number | null
}>()

/**
 * Emitted when the modal is closed so the parent can clear its selection.
 */
const emit = defineEmits<{
	(e: "close"): void
}>()

const modalRef = ref<HTMLElement | null>(null)
let bsModal: Modal | null = null

const photo = ref<PhotoDetail | null>(null)
const loading = ref(false)

/** Constructs the image src URL for the currently loaded photo. */
const imageUrl = computed(() =>
	photo.value ? getPhotoImageUrl(photo.value.id) : ""
)

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

onMounted(() => {
	if (modalRef.value) {
		bsModal = new Modal(modalRef.value, { keyboard: true })
		modalRef.value.addEventListener("hidden.bs.modal", () => {
			photo.value = null
			emit("close")
		})
	}
})

onBeforeUnmount(() => {
	bsModal?.dispose()
})

watch(
	() => props.photoId,
	async (id) => {
		if (id === null) return

		loading.value = true
		photo.value = null
		bsModal?.show()

		await getPhotoById(id)
			.then((result: PhotoDetail) => {
				photo.value = result
			})
			.catch((error: unknown) => {
				const err = error as { response?: { data?: { message?: string; status?: number } }; message?: string }
				errorStore.set(true, err.response?.data?.message ?? err.message ?? "", err.response?.data?.status ?? 500)
				bsModal?.hide()
			})

		loading.value = false
	}
)
</script>

<template>
	<div class="modal fade" ref="modalRef" tabindex="-1" aria-modal="true" role="dialog">
		<div class="modal-dialog modal-xl modal-dialog-scrollable modal-dialog-centered">
			<div class="modal-content">

				<div class="modal-header">
					<h5 class="modal-title text-truncate">
						{{ photo?.filename ?? language.get("Loading...") }}
					</h5>
					<button type="button" class="btn-close" data-bs-dismiss="modal" :aria-label="language.get('Close')"></button>
				</div>

				<div class="modal-body">
					<!-- Loading spinner -->
					<div v-if="loading" class="d-flex justify-content-center py-5">
						<div class="spinner-border" role="status">
							<span class="visually-hidden">{{ language.get("Loading...") }}</span>
						</div>
					</div>

					<!-- Content -->
					<div v-else-if="photo" class="row g-4">
						<!-- Photo image -->
						<div class="col-lg-7 text-center">
							<img
								:src="imageUrl"
								:alt="photo.filename"
								class="img-fluid rounded shadow-sm"
								style="max-height: 70vh; object-fit: contain;"
							/>
						</div>

						<!-- Metadata -->
						<div class="col-lg-5">
							<table class="table table-sm table-bordered align-middle">
								<tbody>
									<tr>
										<th class="text-muted w-40">{{ language.get("ID") }}</th>
										<td>{{ photo.id }}</td>
									</tr>
									<tr>
										<th class="text-muted">{{ language.get("Filename") }}</th>
										<td class="text-break">{{ photo.filename }}</td>
									</tr>
									<tr>
										<th class="text-muted">{{ language.get("Path") }}</th>
										<td class="text-break small text-muted">{{ photo.path }}</td>
									</tr>
									<tr v-if="photo.name">
										<th class="text-muted">{{ language.get("Name") }}</th>
										<td>{{ photo.name }}</td>
									</tr>
									<tr v-if="photo.description">
										<th class="text-muted">{{ language.get("Description") }}</th>
										<td>{{ photo.description }}</td>
									</tr>
									<tr v-if="photo.album">
										<th class="text-muted">{{ language.get("Album") }}</th>
										<td>{{ photo.album }}</td>
									</tr>
									<tr v-if="photo.width || photo.height">
										<th class="text-muted">{{ language.get("Dimensions") }}</th>
										<td>{{ photo.width }} × {{ photo.height }} px</td>
									</tr>
									<tr>
										<th class="text-muted">{{ language.get("File Size") }}</th>
										<td>{{ formatFilesize(photo.filesize) }}</td>
									</tr>
									<tr v-if="photo.mimeType">
										<th class="text-muted">{{ language.get("MIME Type") }}</th>
										<td>{{ photo.mimeType }}</td>
									</tr>
									<tr v-if="photo.dateTaken">
										<th class="text-muted">{{ language.get("Date Taken") }}</th>
										<td>{{ formatDate(photo.dateTaken) }}</td>
									</tr>
									<tr v-if="photo.cameraMake || photo.cameraModel">
										<th class="text-muted">{{ language.get("Camera") }}</th>
										<td>{{ [photo.cameraMake, photo.cameraModel].filter(Boolean).join(" ") }}</td>
									</tr>
									<tr v-if="photo.latitude !== null && photo.longitude !== null">
										<th class="text-muted">{{ language.get("GPS") }}</th>
										<td>{{ photo.latitude }}, {{ photo.longitude }}</td>
									</tr>
									<tr v-if="photo.rating">
										<th class="text-muted">{{ language.get("Rating") }}</th>
										<td>
											<span
												v-for="n in 5"
												:key="n"
												:class="n <= (photo.rating ?? 0) ? 'text-warning' : 'text-secondary'"
											>★</span>
										</td>
									</tr>
									<tr v-if="photo.viewCount !== null">
										<th class="text-muted">{{ language.get("Views") }}</th>
										<td>{{ photo.viewCount }}</td>
									</tr>
									<tr v-if="photo.hash">
										<th class="text-muted">{{ language.get("Hash") }}</th>
										<td class="small text-muted text-break">{{ photo.hash }}</td>
									</tr>
									<tr v-if="photo.dateCreated">
										<th class="text-muted">{{ language.get("Added") }}</th>
										<td>{{ formatDate(photo.dateCreated) }}</td>
									</tr>
								</tbody>
							</table>
						</div>
					</div>
				</div>

			</div>
		</div>
	</div>
</template>

<style scoped>
	.w-40 {
		width: 40%;
	}
</style>
