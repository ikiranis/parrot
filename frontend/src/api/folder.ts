import axios from "axios"
import config from "@/functions/config.ts"
import type { Folder, MediaFile } from "@/types"

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
 * Retrieves all image files directly inside the specified folder.
 *
 * @param id the primary key of the folder
 * @returns array of {@link MediaFile} entries of kind IMAGE within the folder
 */
export const getFolderPhotos = async (id: number): Promise<MediaFile[]> => {
	try {
		const response = await axios.get(config.defaultServer() + `/api/folders/${id}/photos`)
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
 * Deletes all folder records from the database.
 *
 * @returns void on success
 */
export const clearFolders = async (): Promise<void> => {
	try {
		await axios.delete(config.defaultServer() + "/api/folders")
	} catch (error: unknown) {
		throw error
	}
}
