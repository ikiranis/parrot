<script setup lang="ts">
import { ref, Ref, onMounted, onUnmounted } from "vue"
import { language } from "@/functions/languageStore.ts"
import { errorStore } from "@/components/error/errorStore.ts"
import { getRandomPhotos, getPhotoImageUrl } from "@/api/photo.ts"
import type { MediaFile } from "@/types"
import Error from "@/components/error/Error.vue"

const PREFETCH_MAX = 10

const navigating = ref(false)
const currentPhoto: Ref<MediaFile | null> = ref(null)
const prefetchQueue: Ref<MediaFile[]> = ref([])
const slideshowEl: Ref<HTMLElement | null> = ref(null)
const isFullscreen = ref(false)
const history: Ref<MediaFile[]> = ref([])
const historyIndex = ref(-1)

onMounted(async () => {
	window.addEventListener('keydown', handleKeydown)
	document.addEventListener('fullscreenchange', handleFullscreenChange)
	await navigateForward()
})

onUnmounted(() => {
	window.removeEventListener('keydown', handleKeydown)
	document.removeEventListener('fullscreenchange', handleFullscreenChange)
})

const handleFullscreenChange = () => {
	isFullscreen.value = !!document.fullscreenElement
}

/** Fills the prefetch queue up to PREFETCH_MAX with a single batch request, adding photos one by one. */
const preloadNext = async () => {
	const slots = PREFETCH_MAX - prefetchQueue.value.length
	if (slots <= 0) return

	try {
		const photos = await getRandomPhotos(slots)
		for (const photo of photos) {
			prefetchQueue.value.push(photo)
			const img = new Image()
			img.src = getPhotoImageUrl(photo.id)
		}
	} catch {
		// best-effort — silently ignore preload errors
	}
}

const navigateBack = () => {
	if (navigating.value || historyIndex.value <= 0) return
	historyIndex.value--
	currentPhoto.value = history.value[historyIndex.value]
}

const navigateForward = async () => {
	if (navigating.value) return
	navigating.value = true

	if (historyIndex.value < history.value.length - 1) {
		historyIndex.value++
		currentPhoto.value = history.value[historyIndex.value]
		navigating.value = false
		return
	}

	let photo: MediaFile | null = null

	if (prefetchQueue.value.length > 0) {
		photo = prefetchQueue.value.shift()!
	} else {
		try {
			const photos = await getRandomPhotos(PREFETCH_MAX)
			photo = photos[0] ?? null
			for (const p of photos.slice(1)) {
				prefetchQueue.value.push(p)
				const img = new Image()
				img.src = getPhotoImageUrl(p.id)
			}
		} catch (error: unknown) {
			const err = error as { response?: { data?: { message?: string; status?: number } }; message?: string }
			errorStore.set(true, err.response?.data?.message ?? err.message ?? '', err.response?.data?.status ?? 500)
		}
	}

	if (photo) {
		history.value.push(photo)
		historyIndex.value = history.value.length - 1
		currentPhoto.value = photo
	}

	navigating.value = false
	preloadNext()
}

const handleKeydown = (event: KeyboardEvent) => {
	if (event.key === 'ArrowRight') navigateForward()
	else if (event.key === 'ArrowLeft') navigateBack()
}

const toggleFullscreen = async () => {
	if (!slideshowEl.value) return

	if (!document.fullscreenElement) {
		await slideshowEl.value.requestFullscreen()
	} else {
		await document.exitFullscreen()
	}
}
</script>

<template>
	<div class="slideshow" ref="slideshowEl">
		<template v-if="currentPhoto">
			<img
				:src="getPhotoImageUrl(currentPhoto.id)"
				:alt="currentPhoto.filename"
				class="slideshow__image"
			/>
			<div class="slideshow__filename">{{ currentPhoto.filename }}</div>
		</template>

		<div v-else class="slideshow__empty">
			{{ language.get("No photos found. Scan a folder to import photos.") }}
		</div>

		<button
			class="slideshow__arrow slideshow__arrow--left"
			@click="navigateBack"
			:title="language.get('Previous')"
			:disabled="navigating || historyIndex <= 0"
		>
			<svg xmlns="http://www.w3.org/2000/svg" width="32" fill="currentColor" class="bi bi-chevron-left" viewBox="0 0 16 16">
				<path fill-rule="evenodd" d="M11.354 1.646a.5.5 0 0 1 0 .708L5.707 8l5.647 5.646a.5.5 0 0 1-.708.708l-6-6a.5.5 0 0 1 0-.708l6-6a.5.5 0 0 1 .708 0z"/>
			</svg>
		</button>

		<button
			class="slideshow__arrow slideshow__arrow--right"
			@click="navigateForward"
			:title="language.get('Next')"
			:disabled="navigating"
		>
			<svg xmlns="http://www.w3.org/2000/svg" width="32" fill="currentColor" class="bi bi-chevron-right" viewBox="0 0 16 16">
				<path fill-rule="evenodd" d="M4.646 1.646a.5.5 0 0 1 .708 0l6 6a.5.5 0 0 1 0 .708l-6 6a.5.5 0 0 1-.708-.708L10.293 8 4.646 2.354a.5.5 0 0 1 0-.708z"/>
			</svg>
		</button>

		<button
			class="slideshow__fullscreen-btn"
			@click="toggleFullscreen"
			:title="isFullscreen ? language.get('Exit Fullscreen') : language.get('Fullscreen')"
		>
			<svg v-if="!isFullscreen" xmlns="http://www.w3.org/2000/svg" width="20" fill="currentColor" class="bi bi-fullscreen" viewBox="0 0 16 16">
				<path d="M1.5 1h4a.5.5 0 0 1 0 1h-4a.5.5 0 0 0-.5.5v4a.5.5 0 0 1-1 0v-4A1.5 1.5 0 0 1 1.5 1zm9 0h4A1.5 1.5 0 0 1 16 2.5v4a.5.5 0 0 1-1 0v-4a.5.5 0 0 0-.5-.5h-4a.5.5 0 0 1 0-1zm-10 10.5v4A1.5 1.5 0 0 0 1.5 15h4a.5.5 0 0 0 0-1h-4a.5.5 0 0 1-.5-.5v-4a.5.5 0 0 0-1 0zm9 0a.5.5 0 0 0-1 0v4a.5.5 0 0 0 .5.5h4a.5.5 0 0 0 0-1h-4V11z"/>
			</svg>
			<svg v-else xmlns="http://www.w3.org/2000/svg" width="20" fill="currentColor" class="bi bi-fullscreen-exit" viewBox="0 0 16 16">
				<path d="M5.5 0a.5.5 0 0 1 .5.5v4A1.5 1.5 0 0 1 4.5 6h-4a.5.5 0 0 1 0-1h4a.5.5 0 0 0 .5-.5v-4a.5.5 0 0 1 .5-.5zm5 0a.5.5 0 0 1 .5.5v4a.5.5 0 0 0 .5.5h4a.5.5 0 0 1 0 1h-4A1.5 1.5 0 0 1 10 4.5v-4a.5.5 0 0 1 .5-.5zM0 10.5a.5.5 0 0 1 .5-.5h4A1.5 1.5 0 0 1 6 11.5v4a.5.5 0 0 1-1 0v-4a.5.5 0 0 0-.5-.5h-4a.5.5 0 0 1-.5-.5zm10 1a1.5 1.5 0 0 1 1.5-1.5h4a.5.5 0 0 1 0 1h-4a.5.5 0 0 0-.5.5v4a.5.5 0 0 1-1 0v-4z"/>
			</svg>
		</button>

		<Error />
	</div>
</template>

<style scoped lang="scss">
.slideshow {
	position: relative;
	background: #111;
	min-height: calc(100vh - 80px);
	display: flex;
	align-items: center;
	justify-content: center;
	overflow: hidden;
	border-radius: 4px;

	&:fullscreen,
	&:-webkit-full-screen {
		min-height: 100vh;
		border-radius: 0;

		.slideshow__image {
			width: 100vw;
			height: 100vh;
			max-width: none;
			max-height: none;
			object-fit: contain;
		}
	}

	&__image {
		max-width: 100%;
		max-height: calc(100vh - 100px);
		object-fit: contain;
		display: block;
	}

	&__loading {
		color: white;
	}

	&__empty {
		color: rgba(255, 255, 255, 0.6);
		font-size: 1.1rem;
	}

	&__filename {
		position: absolute;
		bottom: 1.25rem;
		left: 50%;
		transform: translateX(-50%);
		color: rgba(255, 255, 255, 0.75);
		font-size: 0.85rem;
		background: rgba(0, 0, 0, 0.55);
		padding: 0.25rem 0.75rem;
		border-radius: 4px;
		max-width: 80%;
		white-space: nowrap;
		overflow: hidden;
		text-overflow: ellipsis;
	}

	&__arrow {
		position: absolute;
		top: 50%;
		transform: translateY(-50%);
		background: rgba(255, 255, 255, 0.1);
		border: 1px solid rgba(255, 255, 255, 0.2);
		color: white;
		padding: 0.75rem 1rem;
		cursor: pointer;
		border-radius: 6px;
		opacity: 0;
		transition: opacity 0.2s, background 0.2s;

		&--left {
			left: 1rem;
		}

		&--right {
			right: 1rem;
		}

		&:hover:not(:disabled) {
			background: rgba(255, 255, 255, 0.25);
		}

		&:disabled {
			cursor: not-allowed;
			opacity: 0.3 !important;
		}
	}

	&:hover .slideshow__arrow:not(:disabled) {
		opacity: 1;
	}

	&__fullscreen-btn {
		position: absolute;
		top: 1rem;
		right: 1rem;
		background: rgba(255, 255, 255, 0.1);
		border: 1px solid rgba(255, 255, 255, 0.2);
		color: white;
		padding: 0.4rem 0.6rem;
		cursor: pointer;
		border-radius: 6px;
		opacity: 0;
		transition: opacity 0.2s, background 0.2s;

		&:hover {
			background: rgba(255, 255, 255, 0.25);
		}
	}

	&:hover .slideshow__fullscreen-btn {
		opacity: 1;
	}
}
</style>
