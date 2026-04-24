<script setup lang="ts">
import config from "@/functions/config.ts"
import {language} from "@/functions/languageStore.ts"
import {setNewLanguage} from "@/api/language.ts";

defineProps({
    collapsed: {
        type: Boolean,
        required: false
    }
})

const setLanguage = async (lang: string) => {
    config.setLanguage(lang)
    await language.init()

    await setNewLanguage({language: lang})
}

/**
 * Create flag path
 *
 * @param language
 */
const getFlagPath = (language: string) => {
    const basePath = '/flags/'

    return basePath + language + '.svg'
}

</script>

<template>
    <div class="d-flex" :class="collapsed ? 'flex-column justify-content-center' : ''">
        <a v-for="language in config.getLanguages()" class="btn">
            <img :src="getFlagPath(language.lang)" :alt="language.name"
                 :title="language.name" width="48"
                 :class="config.defaultLanguage() === language.lang ? 'currentLanguage' : ''"
                 @click="setLanguage(language.lang)">
        </a>
    </div>
</template>

<style scoped lang="scss">
    .currentLanguage {
        padding: 3px;
        border: 2px solid white;
    }
</style>
