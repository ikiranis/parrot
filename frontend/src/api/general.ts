import axios from "axios";
import config from "@/functions/config.ts";

/**
 * Checks whether the backend API is reachable.
 *
 * @returns `true` when the server responds with HTTP 200, `false` otherwise
 */
export const checkAppAlive = async (): Promise<boolean> => {
    try {
        const response = await axios.get(config.defaultServer() + '/api/general/appAlive')

        if (response.status === 200) {
            return true
        }

        return false
    } catch (error: unknown) {
        return false
    }
}

/**
 * Deletes all library data from the database and removes the thumbnails directory from disk.
 * This operation is irreversible.
 *
 * @returns void on success
 */
export const deepClean = async (): Promise<void> => {
    await axios.delete(config.defaultServer() + '/api/general/deep-clean')
}
