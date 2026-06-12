<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from "vue"
import { language } from "@/functions/languageStore.ts"
import { startScan, getScanStatus, cancelScan } from "@/api/scan.ts"
import type { ScanJobResponse } from "@/types"

const POLL_INTERVAL_MS = 2_000

/** Weight of the newest sample when smoothing throughput rates (0..1; higher = more reactive). */
const RATE_SMOOTHING = 0.4

const scanState = ref<ScanJobResponse | null>(null)
const starting = ref(false)
const cancelling = ref(false)
const errorMessage = ref("")
const showErrorLogs = ref(false)
let pollTimer: ReturnType<typeof setInterval> | null = null

/** Current wall-clock time, ticked every second while a scan runs so elapsed time updates live. */
const nowMs = ref(Date.now())
let tickTimer: ReturnType<typeof setInterval> | null = null

/** Previous poll sample, used to derive throughput from the delta to the current sample. */
const prevSample = ref<{ t: number; folders: number; added: number; tagged: number } | null>(null)

/** Exponentially-smoothed throughput rates (per second) for the active phase. */
const rates = ref<{ foldersPerSec: number; filesPerSec: number; tagsPerSec: number } | null>(null)

onMounted(() => {
	fetchStatus()
})

onUnmounted(() => {
	stopPolling()
})

/**
 * Stores the latest scan status and refreshes the smoothed throughput rates from the delta
 * since the previous poll. Sampling state is cleared whenever the scan is not actively running.
 *
 * @param s the freshly fetched scan status
 */
const applyState = (s: ScanJobResponse): void => {
	if (s.status === "RUNNING") {
		const now = Date.now()
		const folders = s.foldersScanned + s.foldersSkipped
		const prev = prevSample.value
		if (prev) {
			const dt = (now - prev.t) / 1000
			if (dt > 0) {
				const ema = (previous: number | undefined, instant: number): number =>
					previous === undefined ? instant : previous * (1 - RATE_SMOOTHING) + instant * RATE_SMOOTHING
				rates.value = {
					foldersPerSec: ema(rates.value?.foldersPerSec, Math.max(0, folders - prev.folders) / dt),
					filesPerSec: ema(rates.value?.filesPerSec, Math.max(0, s.added - prev.added) / dt),
					tagsPerSec: ema(rates.value?.tagsPerSec, Math.max(0, s.tagged - prev.tagged) / dt),
				}
			}
		}
		prevSample.value = { t: now, folders, added: s.added, tagged: s.tagged }
	} else {
		prevSample.value = null
		rates.value = null
		cancelling.value = false
	}
	scanState.value = s
}

const fetchStatus = async () => {
	try {
		applyState(await getScanStatus())
		if (scanState.value?.status === "RUNNING") {
			startPolling()
		}
	} catch {
		// silently ignore status fetch errors on mount
	}
}

const startPolling = () => {
	if (tickTimer === null) {
		tickTimer = setInterval(() => {
			nowMs.value = Date.now()
		}, 1_000)
	}
	if (pollTimer !== null) return
	pollTimer = setInterval(async () => {
		try {
			applyState(await getScanStatus())
			if (scanState.value?.status !== "RUNNING") {
				stopPolling()
			}
		} catch {
			// keep polling despite transient errors
		}
	}, POLL_INTERVAL_MS)
}

const stopPolling = () => {
	if (pollTimer !== null) {
		clearInterval(pollTimer)
		pollTimer = null
	}
	if (tickTimer !== null) {
		clearInterval(tickTimer)
		tickTimer = null
	}
}

const onStartScan = async () => {
	errorMessage.value = ""
	showErrorLogs.value = false
	starting.value = true
	prevSample.value = null
	rates.value = null
	try {
		applyState(await startScan())
		startPolling()
	} catch (error: unknown) {
		const err = error as { response?: { data?: { message?: string } }; message?: string }
		errorMessage.value = err.response?.data?.message ?? err.message ?? language.get("Failed to start scan")
	} finally {
		starting.value = false
	}
}

/**
 * Requests cancellation of the running scan. The backend stops at the next safe point and the
 * next status poll reflects the CANCELLED state; the button is disabled while the request is in
 * flight and stays reflecting "cancelling" until the scan leaves the RUNNING state.
 */
const onCancelScan = async () => {
	cancelling.value = true
	try {
		applyState(await cancelScan())
	} catch (error: unknown) {
		const err = error as { response?: { data?: { message?: string } }; message?: string }
		errorMessage.value = err.response?.data?.message ?? err.message ?? language.get("Failed to cancel scan")
		cancelling.value = false
	}
}

const toggleErrorLogs = () => {
	showErrorLogs.value = !showErrorLogs.value
}

/**
 * Returns the Bootstrap badge class for the current scan status.
 */
const statusBadgeClass = computed(() => ({
	"text-bg-warning": scanState.value?.status === "RUNNING",
	"text-bg-success": scanState.value?.status === "COMPLETED",
	"text-bg-danger": scanState.value?.status === "FAILED",
	"text-bg-secondary": scanState.value?.status === "CANCELLED",
}))

/**
 * Returns the Bootstrap progress-bar modifier classes for the current scan status.
 */
const progressBarClass = computed(() => ({
	"progress-bar-striped": scanState.value?.status === "RUNNING",
	"progress-bar-animated": scanState.value?.status === "RUNNING",
	"bg-success": scanState.value?.status === "COMPLETED",
	"bg-danger": scanState.value?.status === "FAILED",
	"bg-secondary": scanState.value?.status === "CANCELLED",
}))

/**
 * Returns a human-readable label for the active scanning phase.
 * Falls back to the raw phase enum name when the translation key is missing.
 */
const phaseLabel = computed((): string => {
	const phase = scanState.value?.phase
	if (!phase) return ""
	const labels: Record<string, string> = {
		COLLECTING: language.get("Collecting directories"),
		SCANNING: language.get("Scanning files"),
		TAGGING: language.get("Reading tags"),
	}
	return labels[phase] || phase
})

/**
 * Returns a detailed progress string (e.g. "1,234 / 5,678 folders") during an active scan.
 */
const phaseDetail = computed((): string => {
	const s = scanState.value
	if (!s || s.status !== "RUNNING" || !s.phase) return ""
	if (s.phase === "SCANNING" && s.totalFolders > 0) {
		const done = s.foldersScanned + s.foldersSkipped
		return `${done.toLocaleString()} / ${s.totalFolders.toLocaleString()} ${language.get("folders")}`
	}
	if (s.phase === "TAGGING" && s.totalFiles > 0) {
		return `${s.tagged.toLocaleString()} / ${s.totalFiles.toLocaleString()} ${language.get("files")}`
	}
	return ""
})

/**
 * Returns the current throughput as a human-readable string (e.g. "1,250 folders/s · 3,400 files/s"),
 * or an empty string until at least two polls have been observed for the active phase.
 */
const rateLabel = computed((): string => {
	const s = scanState.value
	const r = rates.value
	if (!s || s.status !== "RUNNING" || !r) return ""
	const parts: string[] = []
	if (s.phase === "SCANNING") {
		parts.push(`${formatRate(r.foldersPerSec)} ${language.get("folders/s")}`)
		if (r.filesPerSec > 0) parts.push(`${formatRate(r.filesPerSec)} ${language.get("files/s")}`)
	} else if (s.phase === "TAGGING") {
		parts.push(`${formatRate(r.tagsPerSec)} ${language.get("files/s")}`)
	}
	return parts.join(" · ")
})

/**
 * Returns the estimated number of seconds until the active phase finishes, based on the smoothed
 * throughput and the remaining work, or null while a meaningful estimate cannot yet be made.
 */
const etaSeconds = computed((): number | null => {
	const s = scanState.value
	const r = rates.value
	if (!s || s.status !== "RUNNING" || !r) return null
	if (s.phase === "SCANNING" && s.totalFolders > 0 && r.foldersPerSec > 0) {
		const done = s.foldersScanned + s.foldersSkipped
		return Math.max(0, (s.totalFolders - done) / r.foldersPerSec)
	}
	if (s.phase === "TAGGING" && s.totalFiles > 0 && r.tagsPerSec > 0) {
		return Math.max(0, (s.totalFiles - s.tagged) / r.tagsPerSec)
	}
	return null
})

/**
 * Returns the number of seconds elapsed since the scan started, counting up to the present while
 * the scan runs and frozen at the finish time once it completes, or null when no scan is active.
 */
const elapsedSeconds = computed((): number | null => {
	const s = scanState.value
	if (!s || !s.startedAt) return null
	const end = s.completedAt ? new Date(s.completedAt).getTime() : nowMs.value
	return Math.max(0, (end - new Date(s.startedAt).getTime()) / 1000)
})

/**
 * Formats a per-second rate, using one decimal place for small rates and grouped integers above ten.
 *
 * @param perSec the rate in units per second
 * @returns a compact human-readable rate string
 */
const formatRate = (perSec: number): string => {
	if (perSec >= 10) return Math.round(perSec).toLocaleString()
	return perSec.toFixed(1)
}

/**
 * Formats a duration in seconds as a compact "Hh Mm" / "Mm Ss" / "Ss" string.
 *
 * @param totalSeconds the duration in seconds
 * @returns a human-readable duration string
 */
const formatDuration = (totalSeconds: number): string => {
	const secs = Math.max(0, Math.round(totalSeconds))
	const h = Math.floor(secs / 3600)
	const m = Math.floor((secs % 3600) / 60)
	const s = secs % 60
	if (h > 0) return `${h}h ${m}m`
	if (m > 0) return `${m}m ${s}s`
	return `${s}s`
}

/**
 * Formats an ISO 8601 timestamp string into a locale date-time string.
 *
 * @param iso the ISO 8601 string to format
 * @returns a human-readable date-time string
 */
const formatDate = (iso: string): string => {
	return new Date(iso).toLocaleString()
}
</script>

<template>
	<div class="card mb-4">
		<div class="card-header d-flex justify-content-between align-items-center">
			<span class="fw-semibold">{{ language.get("Library Scan") }}</span>
			<div class="d-flex gap-2">
				<button
					v-if="scanState?.status === 'RUNNING'"
					class="btn btn-sm btn-outline-danger"
					:disabled="cancelling"
					@click="onCancelScan"
				>
					<span v-if="cancelling" class="spinner-border spinner-border-sm me-1" role="status"></span>
					{{ cancelling ? language.get("Cancelling...") : language.get("Cancel") }}
				</button>
				<button
					class="btn btn-sm btn-primary"
					:disabled="starting || scanState?.status === 'RUNNING'"
					@click="onStartScan"
				>
					<span v-if="starting || scanState?.status === 'RUNNING'"
						class="spinner-border spinner-border-sm me-1" role="status"></span>
					{{ scanState?.status === "RUNNING" ? language.get("Scanning...") : language.get("Scan Library") }}
				</button>
			</div>
		</div>

		<div v-if="errorMessage" class="alert alert-danger mb-0 rounded-0 border-0 border-bottom">
			{{ errorMessage }}
		</div>

		<div v-if="scanState && scanState.status !== 'IDLE'" class="card-body">

			<!-- Status row -->
			<div class="d-flex align-items-center gap-2 mb-2 flex-wrap">
				<span class="badge" :class="statusBadgeClass">
					{{ language.get(scanState.status) }}
				</span>
				<span v-if="scanState.status === 'RUNNING' && phaseLabel" class="badge text-bg-secondary">
					{{ phaseLabel }}
				</span>
				<small v-if="scanState.startedAt" class="text-muted">
					{{ language.get("Started") }}: {{ formatDate(scanState.startedAt) }}
				</small>
				<small v-if="scanState.completedAt" class="text-muted">
					&mdash; {{ language.get("Finished") }}: {{ formatDate(scanState.completedAt) }}
				</small>
			</div>

			<!-- Progress bar -->
			<div class="d-flex align-items-center gap-2 mb-1">
				<div class="progress flex-grow-1" style="height: 8px;">
					<div
						class="progress-bar"
						:class="progressBarClass"
						role="progressbar"
						:style="{ width: scanState.progressPercent + '%' }"
						:aria-valuenow="scanState.progressPercent"
						aria-valuemin="0"
						aria-valuemax="100"
					></div>
				</div>
				<small class="text-muted text-nowrap">{{ scanState.progressPercent }}%</small>
			</div>

			<!-- Phase detail: counts on the left, live throughput, elapsed time and ETA on the right -->
			<div
				v-if="scanState.status === 'RUNNING'"
				class="d-flex justify-content-between align-items-center gap-2 flex-wrap small text-muted mb-3"
			>
				<span>
					<span v-if="phaseDetail">{{ phaseDetail }}</span>
					<span v-if="rateLabel" :class="{ 'ms-2': phaseDetail }">
						<span v-if="phaseDetail">· </span>{{ rateLabel }}
					</span>
				</span>
				<span class="text-nowrap">
					<span v-if="elapsedSeconds !== null">
						{{ language.get("Elapsed") }}: {{ formatDuration(elapsedSeconds) }}
					</span>
					<span v-if="etaSeconds !== null" :class="{ 'ms-2': elapsedSeconds !== null }">
						<span v-if="elapsedSeconds !== null">· </span>{{ language.get("ETA") }}: {{ formatDuration(etaSeconds) }}
					</span>
				</span>
			</div>
			<p v-else-if="phaseDetail" class="small text-muted mb-3">{{ phaseDetail }}</p>
			<div v-else class="mb-3"></div>

			<!-- Metrics grid -->
			<div class="row g-2 mb-2">
				<div class="col-6 col-md-4">
					<div class="border rounded p-2 text-center">
						<div class="fs-5 fw-bold text-success">
							{{ (scanState.initialFilesCount + scanState.added).toLocaleString() }}
							<small v-if="scanState.totalMediaFilesInLibrary > 0" class="text-muted fs-6">
								/ {{ scanState.totalMediaFilesInLibrary.toLocaleString() }}
							</small>
						</div>
						<div class="small text-muted">{{ language.get("Added") }}</div>
					</div>
				</div>
				<div class="col-6 col-md-4">
					<div class="border rounded p-2 text-center">
						<div class="fs-5 fw-bold text-secondary">{{ scanState.skipped.toLocaleString() }}</div>
						<div class="small text-muted">{{ language.get("Skipped") }}</div>
					</div>
				</div>

				<!-- Errors card: clickable when errors exist -->
				<div
					class="col-6 col-md-4"
					:style="scanState.errors > 0 ? 'cursor: pointer' : ''"
					@click="scanState.errors > 0 && toggleErrorLogs()"
				>
					<div
						class="border rounded p-2 text-center"
						:class="showErrorLogs && scanState.errors > 0 ? 'border-danger' : ''"
					>
						<div class="fs-5 fw-bold" :class="scanState.errors > 0 ? 'text-danger' : 'text-secondary'">
							{{ scanState.errors.toLocaleString() }}
							<small v-if="scanState.errors > 0" class="fs-6 ms-1">{{ showErrorLogs ? "▲" : "▼" }}</small>
						</div>
						<div class="small text-muted">{{ language.get("Errors") }}</div>
					</div>
				</div>

				<div class="col-6 col-md-4">
					<div class="border rounded p-2 text-center">
						<div class="fs-5 fw-bold text-info">
							{{ (scanState.foldersScanned + scanState.foldersSkipped).toLocaleString() }}
							<small v-if="scanState.totalFolders > 0" class="text-muted fs-6">
								/ {{ scanState.totalFolders.toLocaleString() }}
							</small>
						</div>
						<div class="small text-muted">{{ language.get("Folders Scanned") }}</div>
					</div>
				</div>
				<div class="col-6 col-md-4">
					<div class="border rounded p-2 text-center">
						<div class="fs-5 fw-bold text-secondary">{{ scanState.foldersSkipped.toLocaleString() }}</div>
						<div class="small text-muted">{{ language.get("Folders Skipped") }}</div>
					</div>
				</div>
				<div class="col-6 col-md-4">
					<div class="border rounded p-2 text-center">
						<div class="fs-5 fw-bold text-primary">
							{{ scanState.tagged.toLocaleString() }}
							<small class="text-muted fs-6">
								/ {{ Math.max(scanState.tagged, scanState.totalFiles || scanState.added).toLocaleString() }}
							</small>
						</div>
						<div class="small text-muted">{{ language.get("Tags Read") }}</div>
					</div>
				</div>
			</div>

			<!-- Error logs panel (toggled by clicking the Errors card) -->
			<div
				v-if="showErrorLogs && scanState.errorLogs && scanState.errorLogs.length > 0"
				class="border border-danger-subtle rounded bg-body-tertiary p-2 mb-2 scan-progress__error-logs"
			>
				<div
					v-for="(log, idx) in scanState.errorLogs"
					:key="idx"
					class="small font-monospace text-danger"
				>
					{{ log }}
				</div>
			</div>

			<p v-if="scanState.status !== 'RUNNING'" class="small text-muted mb-0">
				{{ scanState.message }}
			</p>
		</div>

		<div v-else-if="!scanState || scanState.status === 'IDLE'" class="card-body text-muted small">
			{{ language.get("No scan has been run yet. Click \"Scan Library\" to start.") }}
		</div>
	</div>
</template>

<style scoped>
.scan-progress__error-logs {
	max-height: 200px;
	overflow-y: auto;
}
</style>
