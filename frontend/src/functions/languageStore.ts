import {ref, Ref} from 'vue';
import {getLanguages} from "../api/language.ts";
import config from "./config.ts";

interface Language {
    text: string;
    translation: string;
}

const state: Ref<Array<Language>> = ref([]);

const init = async (): Promise<void> => {
    await getLanguages(config.defaultLanguage())
        .then((response: any) => {
            state.value = response;
        })
        .catch((error: any) => {
            console.log(error);
        })
};

const get = (text: string) : string => {
    if (state.value.length === 0) {
        init();
    }

    return state.value.find((item: Language) => item.text === text)?.translation ?? '';
};

export const language =  {
    get,
    init
};
