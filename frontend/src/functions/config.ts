import {ref, Ref} from "vue";
import {Language} from "@/types";

const language: Ref<string> = ref('')

/** List of UI languages supported by the application. */
const languages: Array<Language> = [
        {
            lang: 'el',
            name: 'Ελληνικά'
        },
        {
            lang: 'en',
            name: 'English'
        }
    ]

/**
 * Returns the base URL of the backend API server, using the same host
 * that loaded the frontend page so remote machines resolve correctly.
 *
 * @returns the server origin, e.g. `"http://192.168.1.10:9999"`
 */
const defaultServer = () => {
    return `${window.location.protocol}//${window.location.hostname}:9999`
}

/**
 * Returns the currently active UI language, reading from `localStorage` on
 * first access and falling back to `"el"` if nothing is stored.
 *
 * @returns the ISO language code, e.g. `"en"` or `"el"`
 */
const defaultLanguage = () => {
    if (language.value === '') {
        const defaultLang = localStorage.getItem('defaultLanguage')

        if (defaultLang) {
            language.value = defaultLang
        } else {
            language.value = 'el'
        }
    }

    return language.value
}

/**
 * Sets the active UI language and persists the choice to `localStorage`.
 *
 * @param lang ISO language code to activate, e.g. `"en"` or `"el"`
 */
const setLanguage = (lang: string) => {
    language.value = lang
    localStorage.setItem('defaultLanguage', lang)
}

/**
 * Returns the list of UI languages available in this application.
 *
 * @returns array of {@link Language} options
 */
const getLanguages = () => {
    return languages
}

export default {
    defaultServer,
    defaultLanguage,
    setLanguage,
    getLanguages
}
