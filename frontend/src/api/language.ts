import axios from "axios";
import config from "@/functions/config.ts";

/**
 * Get all languages from database
 */
export const getLanguages = async (language: string) => {
    try {
        const response = await axios.get(config.defaultServer() + '/api/language/all/' + language)

        if (response.status === 200) {
            return response.data
        }
    } catch(error: any) {
        throw error
    }
}

/**
 * Set language
 *
 * @param language
 */
export const setNewLanguage = async (data: Object) => {
    try {
        const response = await axios.post(config.defaultServer() + '/api/language', data, {
            headers: {
                'Content-Type': 'application/json'
            }
        })

        if (response.status === 200) {
            return response.data
        }
    } catch(error: any) {
        throw error
    }
}
