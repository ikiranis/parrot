import axios from "axios";
import config from "@/functions/config.ts";
import type { ScanResult } from "@/types";

/**
 * Triggers a photo scan for the given server-side folder path.
 *
 * @param folderPath absolute path on the server to scan
 * @returns a {@link ScanResult} describing what was added, skipped, or errored
 */
export const scanFolder = async (folderPath: string): Promise<ScanResult | undefined> => {
    try {
        const response = await axios.post(config.defaultServer() + '/api/photos/scan', { folderPath }, {
            headers: { 'Content-Type': 'application/json' }
        })

        if (response.status === 200) {
            return response.data
        }
    } catch (error: unknown) {
        throw error
    }
}

/**
 * Retrieves a paginated list of photos from the database.
 *
 * @param page page index (0-based)
 * @param size number of results per page
 * @returns a Spring {@code Page} wrapper containing the photo entries
 */
export const getPhotos = async (page: number = 0, size: number = 20) => {
    try {
        const response = await axios.get(config.defaultServer() + '/api/photos/all', {
            params: { page, size }
        })

        if (response.status === 200) {
            return response.data
        }
    } catch (error: unknown) {
        throw error
    }
}

/**
 * Deletes all photo records from the database. The physical files are not affected.
 *
 * @returns void on success
 */
export const clearLibrary = async (): Promise<void> => {
    try {
        await axios.delete(config.defaultServer() + '/api/photos/all')
    } catch (error: unknown) {
        throw error
    }
}
