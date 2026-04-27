import axios from "axios";
import config from "@/functions/config.ts";

/**
 * Trigger a photo scan for the given server-side folder path.
 *
 * @param folderPath absolute path on the server to scan
 */
export const scanFolder = async (folderPath: string) => {
    try {
        const response = await axios.post(config.defaultServer() + '/api/photo/scan', { folderPath }, {
            headers: { 'Content-Type': 'application/json' }
        })

        if (response.status === 200) {
            return response.data
        }
    } catch (error: any) {
        throw error
    }
}

/**
 * Get a paginated list of photos from the database.
 *
 * @param page page index (0-based)
 * @param size page size
 */
export const getPhotos = async (page: number = 0, size: number = 20) => {
    try {
        const response = await axios.get(config.defaultServer() + '/api/photo/all', {
            params: { page, size }
        })

        if (response.status === 200) {
            return response.data
        }
    } catch (error: any) {
        throw error
    }
}
