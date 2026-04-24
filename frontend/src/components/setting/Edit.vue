<script setup lang="ts">

import {onMounted, reactive, ref} from "vue";
import router from "@/router";
import {getSetting, updateSetting} from "@/api/setting.ts";
import {errorStore} from "@/components/error/errorStore.ts";
import {language} from "@/functions/languageStore.ts";
import Loading from "@/components/utilities/Loading.vue";

const props = defineProps({
    id: {
        type: String,
        required: true
    }
})

const setting = reactive({
    id: 0,
    settingName: '',
    settingValue: ''
})

const loading = ref(false)

const loadingSetting = ref(false)

onMounted(async () => {
    loadingSetting.value = true

    // Get the selected setting from API
    await getSetting(props.id)
        .then((data) => {
            const newSetting = {
                id: data.id,
                settingName: data.settingName,
                settingValue: data.settingValue
            }

            Object.assign(setting, newSetting);
        })
        .catch((error) => {
            errorStore.set(true, error.response.data.message, error.response.data.status)
        })

    loadingSetting.value = false
})

/**
 * Save setting to database
 */
const saveSetting = async () => {
    loading.value = true

    await updateSetting({
        id: setting.id,
        settingName: setting.settingName,
        settingValue: setting.settingValue
    })
        .then(() => {
            errorStore.set(true, language.get("Setting saved with success"), 204)

            // return to previous page
            router.go(-1)
        })
        .catch((error) => {
            errorStore.set(true, error.response.data.message, error.response.data.status)
        })

    loading.value = false
}
</script>

<template>
    <div v-if="loadingSetting" class="row">
        <Loading />
    </div>

    <div v-if="!loadingSetting" class="container-fluid">
        <div class="row">
            <div class="col-12 my-2">
                <label for="settingValue" class="text-left">{{ setting.settingName }}</label>
                <input type="text" v-model="setting.settingValue" class="form-control input-group-prepend" name="settingValue">
            </div>
        </div>

        <div class="row">
            <button type="submit" class="btn btn-primary mx-auto my-3 col-12 col-lg-5"
                    @click="saveSetting">{{ language.get("Save") }}</button>

            <button type="button" class="btn btn-outline-dark my-3 mx-auto col-12 col-lg-5"
                    @click="router.go(-1)">{{ language.get("Cancel") }}</button>
        </div>

        <div v-if="loading" class="row">
            <Loading />
        </div>

    </div>
</template>
