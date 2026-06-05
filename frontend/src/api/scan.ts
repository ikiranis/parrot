import axios from "axios"
import config from "@/functions/config.ts"
import type { ScanJobResponse } from "@/types"

/**
 * Starts a background library scan across all configured library folders.
 * Returns immediately once the job is queued; the caller should poll
 * {@link getScanStatus} to follow progress.
 *
 * @returns the initial {@link ScanJobResponse} for the newly started job
 * @throws the Axios error when the server returns 409 (scan already running) or any other error
 */
export const startScan = async (): Promise<ScanJobResponse> => {
	try {
		const response = await axios.post(config.defaultServer() + "/api/scan/start")
		return response.data
	} catch (error: unknown) {
		throw error
	}
}

/**
 * Returns the current status of the most recent background scan job.
 * Safe to call at any time; returns an idle placeholder when no scan has run.
 *
 * @returns a {@link ScanJobResponse} with live counter values and current status
 */
export const getScanStatus = async (): Promise<ScanJobResponse> => {
	try {
		const response = await axios.get(config.defaultServer() + "/api/scan/status")
		return response.data
	} catch (error: unknown) {
		throw error
	}
}
