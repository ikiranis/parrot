import axios from "axios"
import config from "@/functions/config.ts"
import type { Folder, MediaFile, PageResponse } from "@/types"

/**
 * Searches folders whose relative path contains the given query, case-insensitively.
 * A blank query returns an empty array.
 *
 * @param query the free-text query matched against folder paths
 * @returns array of matching {@link Folder} entries
 */
export const searchFolders = async (query: string): Promise<Folder[]> => {
	const response = await axios.get(config.defaultServer() + "/api/search/folders", {
		params: { query }
	})
	return response.status === 200 ? response.data : []
}

/**
 * Searches a paginated set of image files whose path or filename contains the given query,
 * optionally constrained to a single rating.
 *
 * @param query  the free-text query matched against photo paths and filenames
 * @param rating exact rating filter (1–5), or null to match all ratings
 * @param page   zero-based page index (default 0)
 * @param size   number of records per page (default 50)
 * @returns a {@link PageResponse} of matching {@link MediaFile} entries
 */
export const searchPhotosPage = async (
	query: string,
	rating: number | null = null,
	page: number = 0,
	size: number = 50
): Promise<PageResponse<MediaFile>> => {
	const response = await axios.get(config.defaultServer() + "/api/search/photos", {
		params: { query, rating, page, size }
	})
	if (response.status === 200) {
		return response.data
	}
	return { content: [], totalElements: 0, totalPages: 0, number: 0, last: true, first: true, size }
}
