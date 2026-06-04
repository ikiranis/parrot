import axios from "axios"
import config from "@/functions/config.ts"
import type { Folder } from "@/types"

/**
 * Retrieves all scanned folder records from the library.
 *
 * @returns array of {@link Folder} entries
 */
export const getFolders = async (): Promise<Folder[]> => {
	try {
		const response = await axios.get(config.defaultServer() + "/api/folders")
		if (response.status === 200) {
			return response.data
		}
		return []
	} catch (error: unknown) {
		throw error
	}
}
