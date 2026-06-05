<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from "vue"
import { language } from "@/functions/languageStore.ts"
import { startScan, getScanStatus } from "@/api/scan.ts"
import type { ScanJobResponse } from "@/types"

const POLL_INTERVAL_MS = 10_000

const scanState = ref<ScanJobResponse | null>(null)
const starting = ref(false)
const errorMessage = ref("")
const showErrorLogs = ref(false)
let pollTimer: ReturnType<typeof setInterval> | null = null

onMounted(() => {
	fetchStatus()
})

onUnmounted(() => {
	stopPolling()
})

const fetchStatus = async () => {
	try {
		scanState.value = await getScanStatus()
		if (scanState.value.status === "RUNNING") {
			startPolling()
		}
	} catch {
		// silently ignore status fetch errors on mount
	}
}

const startPolling = () => {
	if (pollTimer !== null) return
	pollTimer = setInterval(async () => {
		try {
			scanState.value = await getScanStatus()
			if (scanState.value.status !== "RUNNING") {
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
}

const onStartScan = async () => {
	errorMessage.value = ""
	showErrorLogs.value = false
	starting.value = true
	try {
		scanState.value = await startScan()
		startPolling()
	} catch (error: unknown) {
		const err = error as { response?: { data?: { message?: string } }; message?: string }
		errorMessage.value = err.response?.data?.message ?? err.message ?? language.get("Failed to start scan")
	} finally {
		starting.value = false
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
}))

/**
 * Returns the Bootstrap progress-bar modifier classes for the current scan status.
 */
const progressBarClass = computed(() => ({
	"progress-bar-striped": scanState.value?.status === "RUNNING",
	"progress-bar-animated": scanState.value?.status === "RUNNING",
	"bg-success": scanState.value?.status === "COMPLETED",
	"bg-danger": scanState.value?.status === "FAILED",
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

			<!-- Phase detail: folders or files count -->
			<p v-if="phaseDetail" class="small text-muted mb-3">{{ phaseDetail }}</p>
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
								/ {{ (scanState.totalFiles || scanState.added).toLocaleString() }}
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
