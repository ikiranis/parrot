import axios from "axios";
import config from "@/functions/config.ts";

/** A translation key-value pair returned by the multi-language endpoint. */
export interface TranslationEntry {
	text: string;
	translation: string;
}

/**
 * Retrieves all UI translation strings for the given locale code.
 *
 * @param language the ISO locale code (e.g. `"en"`, `"el"`)
 * @returns array of {@link TranslationEntry} key-value pairs, or `undefined` on error
 */
export const getLanguages = async (language: string): Promise<TranslationEntry[] | undefined> => {
    try {
        const response = await axios.get(config.defaultServer() + '/api/languages/all/' + language)

        if (response.status === 200) {
            return response.data
        }
    } catch (error: unknown) {
        throw error
    }
}

/**
 * Persists a new language selection to the backend.
 *
 * @param data object containing the `language` field to save
 * @returns the saved language data
 */
export const setNewLanguage = async (data: { language: string }) => {
    try {
        const response = await axios.post(config.defaultServer() + '/api/languages', data, {
            headers: {
                'Content-Type': 'application/json'
            }
        })

        if (response.status === 200) {
            return response.data
        }
    } catch (error: unknown) {
        throw error
    }
}
