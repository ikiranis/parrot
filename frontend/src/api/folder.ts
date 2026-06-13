import axios from "axios"
import config from "@/functions/config.ts"
import type { Folder, MediaFile, PageResponse } from "@/types"

/**
 * Retrieves all folders at the specified nesting level.
 * Level 1 contains direct children of the library root.
 *
 * @param level the nesting level to filter by
 * @returns array of {@link Folder} entries at that level
 */
export const getFoldersByLevel = async (level: number): Promise<Folder[]> => {
	try {
		const response = await axios.get(config.defaultServer() + `/api/folders/level/${level}`)
		if (response.status === 200) {
			return response.data
		}
		return []
	} catch (error: unknown) {
		throw error
	}
}

/**
 * Retrieves the direct child folders of the given folder.
 *
 * @param id the primary key of the parent folder
 * @returns array of direct child {@link Folder} entries
 */
export const getFolderChildren = async (id: number): Promise<Folder[]> => {
	try {
		const response = await axios.get(config.defaultServer() + `/api/folders/${id}/children`)
		if (response.status === 200) {
			return response.data
		}
		return []
	} catch (error: unknown) {
		throw error
	}
}

/**
 * Retrieves a paginated list of image files directly inside the specified folder.
 *
 * @param id        the primary key of the folder
 * @param page      zero-based page index (default 0)
 * @param size      number of records per page (default 50)
 * @param sortBy    field to sort by; a MediaFile or PhotoTag field name (default "filename")
 * @param direction sort direction, "asc" or "desc" (default "asc")
 * @returns a {@link PageResponse} of {@link MediaFile} entries of kind IMAGE within the folder
 */
export const getFolderPhotosPage = async (
	id: number,
	page: number = 0,
	size: number = 50,
	sortBy: string = "filename",
	direction: string = "asc"
): Promise<PageResponse<MediaFile>> => {
	try {
		const response = await axios.get(config.defaultServer() + `/api/folders/${id}/photos`, {
			params: { page, size, sortBy, direction }
		})
		if (response.status === 200) {
			return response.data
		}
		return { content: [], totalElements: 0, totalPages: 0, number: 0, last: true, first: true, size }
	} catch (error: unknown) {
		throw error
	}
}

/**
 * Retrieves the ancestor-to-target folder chain for the folder that directly
 * contains the given photo, ordered from the top-level ancestor down to the
 * photo's folder. The array is empty when the photo sits directly in the
 * library root.
 *
 * @param photoId the primary key of the photo whose folder chain is requested
 * @returns array of {@link Folder} entries from top-level ancestor to the photo's folder
 */
export const getFolderChainByPhoto = async (photoId: number): Promise<Folder[]> => {
	try {
		const response = await axios.get(config.defaultServer() + `/api/folders/by-photo/${photoId}`)
		if (response.status === 200) {
			return response.data
		}
		return []
	} catch (error: unknown) {
		throw error
	}
}

/**
 * Retrieves the ancestor-to-target folder chain for the given folder, ordered
 * from the top-level ancestor down to and including the folder itself. Used to
 * rebuild the breadcrumb when the grid opens directly on a deep folder.
 *
 * @param id the primary key of the folder whose chain is requested
 * @returns array of {@link Folder} entries from top-level ancestor to the folder itself
 */
export const getFolderChain = async (id: number): Promise<Folder[]> => {
	try {
		const response = await axios.get(config.defaultServer() + `/api/folders/${id}/chain`)
		if (response.status === 200) {
			return response.data
		}
		return []
	} catch (error: unknown) {
		throw error
	}
}

/**
 * Returns the URL that serves the raw thumbnail bytes for the given thumbnail id.
 *
 * @param thumbnailId the primary key of the thumbnail record
 * @returns the thumbnail endpoint URL string
 */
export const getThumbnailUrl = (thumbnailId: number): string =>
	config.defaultServer() + `/api/thumbnails/${thumbnailId}`

/**
 * Generates a thumbnail for the specified folder and returns its id.
 * If a thumbnail already exists for the folder, returns the existing id.
 *
 * @param id the primary key of the folder
 * @returns the thumbnail id
 */
export const createFolderThumbnail = async (id: number): Promise<number> => {
	const response = await axios.post(config.defaultServer() + `/api/folders/${id}/thumbnail`)
	return response.data.thumbnailId
}
