import axios from "axios";
import config from "@/functions/config.ts";
import type { Setting } from "@/types";

/**
 * Retrieves all settings from the database.
 *
 * @returns array of {@link Setting} records
 */
export const getSettings = async (): Promise<Setting[] | undefined> => {
    try {
        const response = await axios.get(config.defaultServer() + '/api/settings/all')

        if (response.status === 200) {
            return response.data
        }
    } catch (error: unknown) {
        throw error
    }
}

/**
 * Retrieves a single setting by its identifier.
 *
 * @param id the setting identifier
 * @returns the matching {@link Setting}
 */
export const getSetting = async (id: string): Promise<Setting | undefined> => {
    try {
        const response = await axios.get(config.defaultServer() + `/api/settings/${id}`)

        if (response.status === 200) {
            return response.data
        }
    } catch (error: unknown) {
        throw error
    }
}

/**
 * Updates an existing setting in the database.
 *
 * @param data object containing the setting id, name, and new value
 * @returns the Axios response
 */
export const updateSetting = async (data: Record<string, unknown>) => {
    try {
        const response = await axios.put(config.defaultServer() + '/api/settings', data, {
            headers: {
                'Content-Type': 'application/json'
            }
        })

        if (response.status === 200) {
            return response
        }
    } catch (error: unknown) {
        throw error
    }
}
