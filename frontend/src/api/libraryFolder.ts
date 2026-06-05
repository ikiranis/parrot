import axios from "axios"
import config from "@/functions/config.ts"
import type { LibraryFolder } from "@/types"

/**
 * Retrieves all configured library folders.
 *
 * @returns array of {@link LibraryFolder} entries
 */
export const getLibraryFolders = async (): Promise<LibraryFolder[]> => {
	try {
		const response = await axios.get(config.defaultServer() + "/api/library-folders")
		if (response.status === 200) {
			return response.data
		}
		return []
	} catch (error: unknown) {
		throw error
	}
}

/**
 * Retrieves a single library folder by its identifier.
 *
 * @param id the library folder identifier
 * @returns the matching {@link LibraryFolder}
 */
export const getLibraryFolder = async (id: number): Promise<LibraryFolder | undefined> => {
	try {
		const response = await axios.get(config.defaultServer() + `/api/library-folders/${id}`)
		if (response.status === 200) {
			return response.data
		}
	} catch (error: unknown) {
		throw error
	}
}

/**
 * Creates a new library folder.
 *
 * @param path the absolute server path of the folder
 * @returns the created {@link LibraryFolder}
 */
export const createLibraryFolder = async (path: string): Promise<LibraryFolder | undefined> => {
	try {
		const response = await axios.post(
			config.defaultServer() + "/api/library-folders",
			{ path },
			{ headers: { "Content-Type": "application/json" } }
		)
		if (response.status === 201) {
			return response.data
		}
	} catch (error: unknown) {
		throw error
	}
}

/**
 * Updates an existing library folder.
 *
 * @param id   the library folder identifier
 * @param path the new absolute server path
 * @returns the updated {@link LibraryFolder}
 */
export const updateLibraryFolder = async (id: number, path: string): Promise<LibraryFolder | undefined> => {
	try {
		const response = await axios.put(
			config.defaultServer() + `/api/library-folders/${id}`,
			{ path },
			{ headers: { "Content-Type": "application/json" } }
		)
		if (response.status === 200) {
			return response.data
		}
	} catch (error: unknown) {
		throw error
	}
}

/**
 * Deletes a library folder by its identifier.
 *
 * @param id the library folder identifier
 */
export const deleteLibraryFolder = async (id: number): Promise<void> => {
	try {
		await axios.delete(config.defaultServer() + `/api/library-folders/${id}`)
	} catch (error: unknown) {
		throw error
	}
}
