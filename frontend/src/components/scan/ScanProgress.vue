<script setup lang="ts">
import { ref, onMounted, onUnmounted } from "vue"
import { language } from "@/functions/languageStore.ts"
import { startScan, getScanStatus } from "@/api/scan.ts"
import type { ScanJobResponse } from "@/types"

const POLL_INTERVAL_MS = 10_000

const scanState = ref<ScanJobResponse | null>(null)
const starting = ref(false)
const errorMessage = ref("")
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
			<div class="d-flex align-items-center gap-2 mb-3">
				<span
					class="badge"
					:class="{
						'text-bg-warning': scanState.status === 'RUNNING',
						'text-bg-success': scanState.status === 'COMPLETED',
						'text-bg-danger': scanState.status === 'FAILED',
					}"
				>
					{{ language.get(scanState.status) }}
				</span>
				<small v-if="scanState.startedAt" class="text-muted">
					{{ language.get("Started") }}: {{ formatDate(scanState.startedAt) }}
				</small>
				<small v-if="scanState.completedAt" class="text-muted">
					&mdash; {{ language.get("Finished") }}: {{ formatDate(scanState.completedAt) }}
				</small>
			</div>

			<div class="row g-2 mb-2">
				<div class="col-6 col-md-4">
					<div class="border rounded p-2 text-center">
						<div class="fs-5 fw-bold text-success">{{ scanState.added }}</div>
						<div class="small text-muted">{{ language.get("Added") }}</div>
					</div>
				</div>
				<div class="col-6 col-md-4">
					<div class="border rounded p-2 text-center">
						<div class="fs-5 fw-bold text-secondary">{{ scanState.skipped }}</div>
						<div class="small text-muted">{{ language.get("Skipped") }}</div>
					</div>
				</div>
				<div class="col-6 col-md-4">
					<div class="border rounded p-2 text-center">
						<div class="fs-5 fw-bold" :class="scanState.errors > 0 ? 'text-danger' : 'text-secondary'">
							{{ scanState.errors }}
						</div>
						<div class="small text-muted">{{ language.get("Errors") }}</div>
					</div>
				</div>
				<div class="col-6 col-md-4">
					<div class="border rounded p-2 text-center">
						<div class="fs-5 fw-bold text-info">{{ scanState.foldersScanned }}</div>
						<div class="small text-muted">{{ language.get("Folders Scanned") }}</div>
					</div>
				</div>
				<div class="col-6 col-md-4">
					<div class="border rounded p-2 text-center">
						<div class="fs-5 fw-bold text-secondary">{{ scanState.foldersSkipped }}</div>
						<div class="small text-muted">{{ language.get("Folders Skipped") }}</div>
					</div>
				</div>
			</div>

			<p v-if="scanState.message" class="small text-muted mb-0">{{ scanState.message }}</p>
		</div>

		<div v-else-if="!scanState || scanState.status === 'IDLE'" class="card-body text-muted small">
			{{ language.get("No scan has been run yet. Click \"Scan Library\" to start.") }}
		</div>
	</div>
</template>
