import axios from "axios";
import config from "@/functions/config.ts";

/**
 * Get all settings from database
 */
export const getSettings = async () => {
    try {
        const response = await axios.get(config.defaultServer() + '/api/setting/all')

        if (response.status === 200) {
            return response.data
        }
    } catch(error: any) {
        throw error
    }
}

/**
 * Get a setting from database
 *
 * @param id
 */
export const getSetting = async (id: string) => {
    try {
        const response = await axios.get(config.defaultServer() + `/api/setting/${id}`)

        if (response.status === 200) {
            return response.data
        }
    } catch(error: any) {
        throw error
    }
}

/**
 * Update a setting in database
 *
 * @param data
 */
export const updateSetting = async (data: Object) => {
    try {
        const response = await axios.put(config.defaultServer() + '/api/setting', data, {
            headers: {
                'Content-Type': 'application/json'
            }
        })

        if (response.status === 200) {
            return response
        }
    } catch(error: any) {
        throw error
    }
}
