import axios from "axios";
import config from "@/functions/config.ts";
import type { ScanResult, PhotoDetail, TagExportItem, TagImportResult } from "@/types";

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
 * Triggers a scan of all configured library folders for photo files.
 *
 * @returns a {@link ScanResult} aggregating counts across all library folders
 */
export const scanLibraryFolders = async (): Promise<ScanResult | undefined> => {
    try {
        const response = await axios.post(config.defaultServer() + '/api/photos/scan-library', {}, {
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

/**
 * Retrieves the full detail of a single photo, including its metadata tags.
 *
 * @param id the primary key of the media file
 * @returns a {@link PhotoDetail} combining MediaFile and PhotoTag fields
 */
export const getPhotoById = async (id: number): Promise<PhotoDetail> => {
    try {
        const response = await axios.get(config.defaultServer() + `/api/photos/${id}`)
        return response.data
    } catch (error: unknown) {
        throw error
    }
}

/**
 * Returns up to `count` randomly selected photos in a single request.
 * Returns an empty array when the library is empty (HTTP 204).
 *
 * @param count number of photos to fetch (default 10)
 * @returns array of {@link MediaFile} records
 */
export const getRandomPhotos = async (count: number = 10): Promise<import("@/types").MediaFile[]> => {
    const response = await axios.get(config.defaultServer() + '/api/photos/random', {
        params: { count },
        validateStatus: (s) => s === 200 || s === 204
    })
    return response.status === 200 ? response.data : []
}

/**
 * Returns the URL that serves the raw image bytes for the given photo id.
 *
 * @param id the primary key of the media file
 * @returns the image endpoint URL string
 */
export const getPhotoImageUrl = (id: number): string =>
    config.defaultServer() + `/api/photos/${id}/image`

/**
 * Sets the rating for the specified photo (1–5).
 *
 * @param id     the primary key of the media file
 * @param rating integer between 1 and 5
 * @returns the updated {@link PhotoDetail}
 */
export const setPhotoRating = async (id: number, rating: number): Promise<import("@/types").PhotoDetail> => {
    const response = await axios.patch(config.defaultServer() + `/api/photos/${id}/rating`, { rating })
    return response.data
}

/**
 * Increments the view counter for the specified photo by one.
 *
 * @param id the primary key of the media file
 * @returns the updated {@link PhotoDetail}
 */
export const incrementPhotoView = async (id: number): Promise<import("@/types").PhotoDetail> => {
    const response = await axios.post(config.defaultServer() + `/api/photos/${id}/view`)
    return response.data
}

/**
 * Fetches all tag entries that have at least one view or a rating set.
 *
 * @returns array of {@link TagExportItem} records
 */
export const exportTagData = async (): Promise<TagExportItem[]> => {
    const response = await axios.get(config.defaultServer() + '/api/photos/tags/export')
    return response.data
}

/**
 * Sends a list of tag entries to the server and updates matching records.
 *
 * @param items array of {@link TagExportItem} entries parsed from an import file
 * @returns a {@link TagImportResult} with counts of updated and not-found entries
 */
export const importTagData = async (items: TagExportItem[]): Promise<TagImportResult> => {
    const response = await axios.post(config.defaultServer() + '/api/photos/tags/import', items)
    return response.data
}
