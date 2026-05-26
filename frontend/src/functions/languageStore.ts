import {ref, Ref} from 'vue';
import {getLanguages, TranslationEntry} from "../api/language.ts";
import config from "./config.ts";


const state: Ref<Array<TranslationEntry>> = ref([]);

/**
 * Loads the translation strings for the current locale from the backend.
 * Populates the internal store used by {@link get}.
 */
const init = async (): Promise<void> => {
    await getLanguages(config.defaultLanguage())
        .then((response: TranslationEntry[] | undefined) => {
            if (response) state.value = response;
        })
        .catch((error: unknown) => {
            console.log(error);
        })
};

/**
 * Returns the translation for the given key, or an empty string when not found.
 * Triggers a lazy {@link init} call when the store is not yet populated.
 *
 * @param text the translation key to look up
 * @returns the translated string, or `""` if unavailable
 */
const get = (text: string) : string => {
    if (state.value.length === 0) {
        init();
    }

    return state.value.find((item: TranslationEntry) => item.text === text)?.translation ?? '';
};

export const language =  {
    get,
    init
};
