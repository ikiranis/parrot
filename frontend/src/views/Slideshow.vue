<script setup lang="ts">
import { ref, Ref, computed, watch, nextTick, onMounted, onUnmounted } from "vue"
import { language } from "@/functions/languageStore.ts"
import { errorStore } from "@/components/error/errorStore.ts"
import { getRandomPhotos, getPhotoImageUrl, getThumbnailUrl, setPhotoRating, incrementPhotoView, deletePhoto } from "@/api/photo.ts"
import { getSettingByName } from "@/api/setting.ts"
import type { MediaFile, PhotoDetail } from "@/types"
import Error from "@/components/error/Error.vue"

const PREFETCH_MAX = 10
const MAX_HISTORY = 20

/**
 * Tracks the off-screen {@link HTMLImageElement} instances created to warm the
 * browser cache for upcoming photos. Holding an explicit reference is what lets
 * us release the decoded bitmap once the photo has been shown or evicted —
 * without this, fire-and-forget images accumulate and inflate memory usage.
 */
const preloadedImages = new Map<number, HTMLImageElement>()

const navigating = ref(false)
const currentPhoto: Ref<MediaFile | null> = ref(null)
const currentDetail: Ref<PhotoDetail | null> = ref(null)
const showDetails = ref(false)
const prefetchQueue: Ref<MediaFile[]> = ref([])
const slideshowEl: Ref<HTMLElement | null> = ref(null)
const isFullscreen = ref(false)
const history: Ref<MediaFile[]> = ref([])
const historyIndex = ref(-1)
const isAutoPlay = ref(false)
const hoverRating = ref(0)
const slideshowTime = ref(3000)
let slideshowTimer: ReturnType<typeof setInterval> | null = null
const showStrip = ref(false)
const stripRef: Ref<HTMLElement | null> = ref(null)
const showDeleteModal = ref(false)

/** All photos visible in the strip: history (oldest→current→future) followed by upcoming prefetch. */
const stripPhotos = computed(() => [...history.value, ...prefetchQueue.value])

watch(currentPhoto, async (photo) => {
	showDetails.value = false
	currentDetail.value = null
	if (!photo) return
	try {
		const detail = await incrementPhotoView(photo.id)
		const existing = currentDetail.value
		if (existing !== null) {
			currentDetail.value = { ...(existing as PhotoDetail), viewCount: detail.viewCount }
		} else {
			currentDetail.value = detail
		}
	} catch {
		// best-effort
	}
})

const stopAutoPlay = () => {
	isAutoPlay.value = false
	if (slideshowTimer !== null) {
		clearInterval(slideshowTimer)
		slideshowTimer = null
	}
}

const startAutoPlay = () => {
	if (slideshowTimer !== null) clearInterval(slideshowTimer)
	isAutoPlay.value = true
	slideshowTimer = setInterval(() => navigateForward(), slideshowTime.value)
}

const toggleAutoPlay = () => {
	if (isAutoPlay.value) {
		stopAutoPlay()
	} else {
		startAutoPlay()
	}
}

onMounted(async () => {
	window.addEventListener('keydown', handleKeydown)
	window.addEventListener('keyup', handleKeyup)
	document.addEventListener('fullscreenchange', handleFullscreenChange)
	try {
		const setting = await getSettingByName('slideshowTime')
		if (setting) {
			const parsed = parseInt(setting.settingValue)
			if (!isNaN(parsed) && parsed > 0) slideshowTime.value = parsed
		}
	} catch {
		// best-effort — use default 3000 ms
	}
	await navigateForward()
})

onUnmounted(() => {
	window.removeEventListener('keydown', handleKeydown)
	window.removeEventListener('keyup', handleKeyup)
	document.removeEventListener('fullscreenchange', handleFullscreenChange)
	stopAutoPlay()
	prefetchQueue.value = []
	history.value = []
	currentPhoto.value = null
	releaseAllImages()
})

const handleFullscreenChange = () => {
	isFullscreen.value = !!document.fullscreenElement
}

/**
 * Warms the browser cache for a single photo, keeping a tracked reference so the
 * decoded image can later be released. No-op if the photo is already preloaded.
 *
 * @param photo the media file to preload
 */
const preloadImage = (photo: MediaFile): void => {
	if (preloadedImages.has(photo.id)) return
	const img = new Image()
	img.src = getPhotoImageUrl(photo.id)
	preloadedImages.set(photo.id, img)
}

/**
 * Releases a previously preloaded image, clearing its source so the browser can
 * reclaim the decoded bitmap, and drops the tracked reference. Safe to call for
 * an id that was never preloaded.
 *
 * @param id the media file id whose preloaded image should be released
 */
const releaseImage = (id: number): void => {
	const img = preloadedImages.get(id)
	if (!img) return
	img.src = ''
	preloadedImages.delete(id)
}

/**
 * Releases every tracked preloaded image. Used on teardown to ensure no decoded
 * bitmaps outlive the component.
 */
const releaseAllImages = (): void => {
	preloadedImages.forEach((img) => {
		img.src = ''
	})
	preloadedImages.clear()
}

const preloadNext = async () => {
	const slots = PREFETCH_MAX - prefetchQueue.value.length
	if (slots <= 0) return

	try {
		const photos = await getRandomPhotos(slots)
		for (const photo of photos) {
			prefetchQueue.value.push(photo)
			preloadImage(photo)
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
				preloadImage(p)
			}
		} catch (error: unknown) {
			const err = error as { response?: { data?: { message?: string; status?: number } }; message?: string }
			errorStore.set(true, err.response?.data?.message ?? err.message ?? '', err.response?.data?.status ?? 500)
		}
	}

	if (photo) {
		// The visible <img> now owns this photo's decode; drop the preload copy
		// so we never hold the same bitmap twice.
		releaseImage(photo.id)
		history.value.push(photo)
		// Cap history so it never pins more than the most recent MAX_HISTORY
		// photos (and their cached images) for the life of the session.
		if (history.value.length > MAX_HISTORY) {
			const evicted = history.value.shift()!
			releaseImage(evicted.id)
		}
		historyIndex.value = history.value.length - 1
		currentPhoto.value = photo
	}

	navigating.value = false
	preloadNext()
}

/**
 * Scrolls the strip container so the current photo thumbnail is centered in view.
 * Uses a nextTick to wait for the DOM to reflect the latest historyIndex before querying.
 */
const scrollStripToCurrent = () => {
	nextTick(() => {
		if (!stripRef.value) return
		const current = stripRef.value.querySelector<HTMLElement>('.slideshow__strip-item--current')
		current?.scrollIntoView({ behavior: 'smooth', block: 'nearest', inline: 'center' })
	})
}

watch(showStrip, (visible) => { if (visible) scrollStripToCurrent() })
watch(historyIndex, () => { if (showStrip.value) scrollStripToCurrent() })

/**
 * Jumps directly to a photo in history by its position in the strip.
 * No-op for prefetch-queue items (index >= history.length) or while navigating.
 *
 * @param index position in stripPhotos
 */
const navigateToHistoryPhoto = (index: number) => {
	if (navigating.value || index === historyIndex.value) return
	if (index >= history.value.length) return
	historyIndex.value = index
	currentPhoto.value = history.value[index]
}

const deleteCurrentPhoto = async () => {
	if (!currentPhoto.value) return
	const photo = currentPhoto.value
	showDeleteModal.value = false

	try {
		await deletePhoto(photo.id)
	} catch (error: unknown) {
		const err = error as { response?: { data?: { message?: string; status?: number } }; message?: string }
		errorStore.set(true, err.response?.data?.message ?? err.message ?? '', err.response?.data?.status ?? 500)
		return
	}

	history.value.splice(historyIndex.value, 1)
	releaseImage(photo.id)
	historyIndex.value = history.value.length - 1
	await navigateForward()
}

const handleKeydown = (event: KeyboardEvent) => {
	if (showDeleteModal.value) {
		if (event.key === 'Escape') showDeleteModal.value = false
		else if (event.key === 'Enter') deleteCurrentPhoto()
		return
	}
	if (event.key === 'ArrowRight') navigateForward()
	else if (event.key === 'ArrowLeft') navigateBack()
	else if (event.key === 'ArrowDown') {
		event.preventDefault()
		showDetails.value = !showDetails.value
	} else if (event.key === 'ArrowUp') {
		event.preventDefault()
		showStrip.value = !showStrip.value
	} else if (event.key === ' ') {
		event.preventDefault()
		toggleAutoPlay()
	} else if (event.key === 'd' || event.key === 'D') {
		if (currentPhoto.value) showDeleteModal.value = true
	}
}

const ratePhoto = async (rating: number) => {
	if (!currentPhoto.value) return
	const photoId = currentPhoto.value.id
	const prev = currentDetail.value
	if (prev) currentDetail.value = { ...prev, rating }
	try {
		const updated = await setPhotoRating(photoId, rating)
		if (currentPhoto.value?.id === photoId) {
			currentDetail.value = updated
		}
	} catch {
		// best-effort — optimistic update stays
	}
}

const handleKeyup = async (event: KeyboardEvent) => {
	if (!currentPhoto.value || !['1', '2', '3', '4', '5'].includes(event.key)) return
	await ratePhoto(parseInt(event.key))
	navigateForward()
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

			<div class="slideshow__rating" @mouseleave="hoverRating = 0">
				<span
					v-for="n in 5"
					:key="n"
					class="slideshow__star"
					:class="{
						'slideshow__star--filled': hoverRating > 0 ? n <= hoverRating : currentDetail?.rating != null && n <= currentDetail.rating,
						'slideshow__star--hover': hoverRating > 0 && n <= hoverRating,
					}"
					@mouseenter="hoverRating = n"
					@click="ratePhoto(n)"
				>★</span>
			</div>

			<div class="slideshow__hud-right">
				<div class="slideshow__views">
					<svg xmlns="http://www.w3.org/2000/svg" width="14" fill="currentColor" viewBox="0 0 16 16">
						<path d="M16 8s-3-5.5-8-5.5S0 8 0 8s3 5.5 8 5.5S16 8 16 8zM1.173 8a13.133 13.133 0 0 1 1.66-2.043C4.12 4.668 5.88 3.5 8 3.5c2.12 0 3.879 1.168 5.168 2.457A13.133 13.133 0 0 1 14.828 8c-.058.087-.122.183-.195.288-.335.48-.83 1.12-1.465 1.755C11.879 11.332 10.119 12.5 8 12.5c-2.12 0-3.879-1.168-5.168-2.457A13.134 13.134 0 0 1 1.172 8z"/>
						<path d="M8 5.5a2.5 2.5 0 1 0 0 5 2.5 2.5 0 0 0 0-5zM4.5 8a3.5 3.5 0 1 1 7 0 3.5 3.5 0 0 1-7 0z"/>
					</svg>
					{{ currentDetail?.viewCount ?? 0 }}
				</div>
				<button
					class="slideshow__autoplay-btn"
					:class="{ 'slideshow__autoplay-btn--active': isAutoPlay }"
					@click="toggleAutoPlay"
					:title="isAutoPlay ? language.get('Pause') : language.get('Play')"
				>
					<svg v-if="!isAutoPlay" xmlns="http://www.w3.org/2000/svg" width="20" fill="currentColor" viewBox="0 0 16 16">
						<path d="m11.596 8.697-6.363 3.692c-.54.313-1.233-.066-1.233-.697V4.308c0-.63.692-1.01 1.233-.696l6.363 3.692a.802.802 0 0 1 0 1.393z"/>
					</svg>
					<svg v-else xmlns="http://www.w3.org/2000/svg" width="20" fill="currentColor" viewBox="0 0 16 16">
						<path d="M5.5 3.5A1.5 1.5 0 0 1 7 5v6a1.5 1.5 0 0 1-3 0V5a1.5 1.5 0 0 1 1.5-1.5zm5 0A1.5 1.5 0 0 1 12 5v6a1.5 1.5 0 0 1-3 0V5a1.5 1.5 0 0 1 1.5-1.5z"/>
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
			</div>

			<div class="slideshow__info" :class="{ 'slideshow__info--visible': showDetails }">
				<div class="slideshow__info-row">
					<span class="slideshow__info-label">{{ language.get('File') }}:</span>
					<span class="slideshow__info-value">{{ currentDetail?.filename ?? currentPhoto.filename }}</span>
				</div>
				<div class="slideshow__info-row" v-if="currentDetail?.album">
					<span class="slideshow__info-label">{{ language.get('Album') }}:</span>
					<span class="slideshow__info-value">{{ currentDetail.album }}</span>
				</div>
				<div class="slideshow__info-row">
					<span class="slideshow__info-label">{{ language.get('Path') }}:</span>
					<span class="slideshow__info-value">{{ currentDetail?.path ?? currentPhoto.path }}</span>
				</div>
			</div>
		</template>

		<div v-else class="slideshow__empty">
			{{ language.get("No photos found. Scan a folder to import photos.") }}
		</div>

		<Transition name="strip">
			<div
				v-if="showStrip && stripPhotos.length > 0"
				class="slideshow__strip"
				ref="stripRef"
			>
				<div
					v-for="(photo, index) in stripPhotos"
					:key="photo.id + '-' + index"
					class="slideshow__strip-item"
					:class="{
						'slideshow__strip-item--current': index === historyIndex,
						'slideshow__strip-item--upcoming': index >= history.length,
					}"
					@click="navigateToHistoryPhoto(index)"
				>
					<img
						v-if="photo.thumbnailId"
						:src="getThumbnailUrl(photo.thumbnailId)"
						:alt="photo.filename"
						class="slideshow__strip-thumb"
					/>
					<div v-else class="slideshow__strip-placeholder" />
				</div>
			</div>
		</Transition>

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

		<Transition name="modal">
			<div
				v-if="showDeleteModal"
				class="slideshow__modal-overlay"
				@click.self="showDeleteModal = false"
			>
				<div class="slideshow__modal">
					<h3 class="slideshow__modal-title">{{ language.get('Delete Photo') }}</h3>
					<p class="slideshow__modal-message">{{ language.get('Are you sure you want to delete this photo?') }}</p>
					<p class="slideshow__modal-filename">{{ currentPhoto?.filename }}</p>
					<div class="slideshow__modal-actions">
						<button class="slideshow__modal-btn slideshow__modal-btn--cancel" @click="showDeleteModal = false">
							{{ language.get('Cancel') }}
						</button>
						<button class="slideshow__modal-btn slideshow__modal-btn--confirm" @click="deleteCurrentPhoto">
							{{ language.get('Delete') }}
						</button>
					</div>
				</div>
			</div>
		</Transition>

		<Error />
	</div>
</template>

<style scoped lang="scss">
.strip-enter-active,
.strip-leave-active {
	transition: transform 0.25s ease, opacity 0.25s ease;
}

.strip-enter-from,
.strip-leave-to {
	transform: translateY(-100%);
	opacity: 0;
}

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

	&__empty {
		color: rgba(255, 255, 255, 0.6);
		font-size: 1.1rem;
	}

	&__rating {
		position: absolute;
		top: 1rem;
		left: 4rem;
		display: flex;
		gap: 0.1rem;
		background: rgba(0, 0, 0, 0.55);
		padding: 0.3rem 0.6rem;
		border-radius: 6px;
	}

	&__star {
		font-size: 2.2rem;
		color: rgba(255, 255, 255, 0.25);
		line-height: 1;
		cursor: pointer;
		transition: color 0.1s;

		&--filled {
			color: #f5c518;
		}

		&--hover {
			color: #ffd700;
		}
	}

	&__hud-right {
		position: absolute;
		top: 1rem;
		right: 1rem;
		display: flex;
		align-items: center;
		gap: 0.5rem;
	}

	&__views {
		display: flex;
		align-items: center;
		gap: 0.35rem;
		background: rgba(0, 0, 0, 0.55);
		color: rgba(255, 255, 255, 0.85);
		font-size: 0.8rem;
		padding: 0.4rem 0.65rem;
		border-radius: 6px;
		border: 1px solid rgba(255, 255, 255, 0.2);
	}

	&__autoplay-btn {
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

		&--active {
			opacity: 1;
			background: rgba(255, 255, 255, 0.2);
		}
	}

	&:hover &__autoplay-btn {
		opacity: 1;
	}

	&__fullscreen-btn {
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

	&:hover &__fullscreen-btn {
		opacity: 1;
	}

	&__info {
		position: absolute;
		bottom: 1.25rem;
		left: 50%;
		transform: translateX(-50%);
		background: rgba(0, 0, 0, 0.7);
		padding: 0.6rem 1rem;
		border-radius: 6px;
		max-width: 80%;
		opacity: 0;
		pointer-events: none;
		transition: opacity 0.2s;

		&--visible {
			opacity: 1;
			pointer-events: auto;
		}
	}

	&__info-row {
		display: flex;
		gap: 0.5rem;
		font-size: 0.82rem;
		color: rgba(255, 255, 255, 0.85);
		overflow: hidden;

		& + & {
			margin-top: 0.25rem;
		}
	}

	&__info-label {
		color: rgba(255, 255, 255, 0.5);
		flex-shrink: 0;
	}

	&__info-value {
		overflow: hidden;
		text-overflow: ellipsis;
		white-space: nowrap;
	}

	&__strip {
		position: absolute;
		top: 0;
		left: 0;
		right: 0;
		height: 104px;
		background: rgba(0, 0, 0, 0.8);
		display: flex;
		align-items: center;
		gap: 6px;
		padding: 0 12px;
		overflow-x: auto;
		overflow-y: hidden;
		z-index: 10;
		scrollbar-width: none;

		&::-webkit-scrollbar {
			display: none;
		}
	}

	&__strip-item {
		flex-shrink: 0;
		width: 80px;
		height: 80px;
		border-radius: 5px;
		overflow: hidden;
		cursor: pointer;
		border: 2px solid transparent;
		transition: border-color 0.15s, transform 0.15s, opacity 0.15s;

		&:hover:not(&--current):not(&--upcoming) {
			border-color: rgba(255, 255, 255, 0.5);
		}

		&--current {
			border-color: #f5c518;
			transform: scale(1.1);
			cursor: default;
		}

		&--upcoming {
			cursor: default;
			opacity: 0.55;
		}
	}

	&__strip-thumb {
		width: 100%;
		height: 100%;
		object-fit: cover;
		display: block;
	}

	&__strip-placeholder {
		width: 100%;
		height: 100%;
		background: rgba(255, 255, 255, 0.08);
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

	&__modal-overlay {
		position: absolute;
		inset: 0;
		background: rgba(0, 0, 0, 0.65);
		display: flex;
		align-items: center;
		justify-content: center;
		z-index: 100;
	}

	&__modal {
		background: #1e1e1e;
		border: 1px solid rgba(255, 255, 255, 0.15);
		border-radius: 8px;
		padding: 1.75rem 2rem;
		min-width: 320px;
		max-width: 480px;
		text-align: center;
	}

	&__modal-title {
		color: #fff;
		font-size: 1.1rem;
		font-weight: 600;
		margin: 0 0 0.75rem;
	}

	&__modal-message {
		color: rgba(255, 255, 255, 0.75);
		font-size: 0.9rem;
		margin: 0 0 0.4rem;
	}

	&__modal-filename {
		color: rgba(255, 255, 255, 0.45);
		font-size: 0.8rem;
		overflow: hidden;
		text-overflow: ellipsis;
		white-space: nowrap;
		margin: 0 0 1.5rem;
	}

	&__modal-actions {
		display: flex;
		gap: 0.75rem;
		justify-content: center;
	}

	&__modal-btn {
		padding: 0.5rem 1.4rem;
		border-radius: 6px;
		font-size: 0.9rem;
		cursor: pointer;
		border: 1px solid transparent;
		transition: background 0.15s, border-color 0.15s;

		&--cancel {
			background: rgba(255, 255, 255, 0.1);
			border-color: rgba(255, 255, 255, 0.2);
			color: rgba(255, 255, 255, 0.85);

			&:hover {
				background: rgba(255, 255, 255, 0.2);
			}
		}

		&--confirm {
			background: #c0392b;
			border-color: #c0392b;
			color: #fff;

			&:hover {
				background: #e74c3c;
				border-color: #e74c3c;
			}
		}
	}
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
