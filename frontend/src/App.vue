<script setup lang="ts">
import {language} from "./functions/languageStore.ts";
import {onMounted, ref} from "vue";
import {checkAppAlive} from "@/api/general.ts";
import Sidebar from "@/components/Sidebar.vue";
import Page404 from "@/components/error/Page404.vue";

const collapsed = ref(false)
const isAppAlive = ref(true)

onMounted(() => {
    // Initial call
    updateAppStatus();

    // Run the update every 10 seconds
    setInterval(updateAppStatus, 60000);
})

/**
 * Check if the app is alive
 */
const updateAppStatus = async () => {
    try {
        const response = await checkAppAlive();

        if (response) {
            isAppAlive.value = true; // Response is a boolean

            return
        }

        isAppAlive.value = false; // Response is not a boolean
    } catch (error) {
        isAppAlive.value = false; // Handle any errors and return false
    }
};

/**
 * Toggle collapse for sidebar
 */
const collapse = () => {
    const sidebar = document.querySelector('.sidebar')
    const content = document.querySelector('.content')

    if (sidebar && content) {
        sidebar.classList.toggle('col-lg-1')
        sidebar.classList.toggle('col-lg-2')
        content.classList.toggle('col-lg-10')
        content.classList.toggle('col-lg-11')

        collapsed.value = !collapsed.value
    }
}

</script>

<template>
    <div v-if="isAppAlive" class="row vh-100">
        <sidebar class="sidebar d-flex flex-column col-lg-2 col-12" :collapsed="collapsed"/>

        <div class="content col-lg-10 col-12 px-5">
            <div class="d-none d-lg-block">
                <span @click="collapse" class="collapseButton btn" :title="collapsed ? language.get('Expand') : language.get('Collapse')">
                    <span v-if="collapsed">
                        <svg xmlns="http://www.w3.org/2000/svg" width="32" height="32" fill="currentColor" class="bi bi-arrow-right-circle" viewBox="0 0 16 16">
                            <path fill-rule="evenodd" d="M1 8a7 7 0 1 0 14 0A7 7 0 0 0 1 8zm15 0A8 8 0 1 1 0 8a8 8 0 0 1 16 0zM4.5 7.5a.5.5 0 0 0 0 1h5.793l-2.147 2.146a.5.5 0 0 0 .708.708l3-3a.5.5 0 0 0 0-.708l-3-3a.5.5 0 1 0-.708.708L10.293 7.5H4.5z"/>
                        </svg>
                    </span>
                    <span v-else>
                        <svg xmlns="http://www.w3.org/2000/svg" width="32" height="32" fill="currentColor" class="bi bi-arrow-left-circle" viewBox="0 0 16 16">
                            <path fill-rule="evenodd" d="M1 8a7 7 0 1 0 14 0A7 7 0 0 0 1 8zm15 0A8 8 0 1 1 0 8a8 8 0 0 1 16 0zm-4.5-.5a.5.5 0 0 1 0 1H5.707l2.147 2.146a.5.5 0 0 1-.708.708l-3-3a.5.5 0 0 1 0-.708l3-3a.5.5 0 1 1 .708.708L5.707 7.5H11.5z"/>
                         </svg>
                    </span>
                </span>
            </div>

            <router-view/>
        </div>
    </div>

    <Page404 v-else
             title="Server Is Offline"
             text="Can't connect to the API service" />
</template>

<style scoped lang="scss">
.collapseButton {
    position: relative;
    left: -3em;
    //opacity: 0.3; /* Set the default opacity, where 1.0 is fully opaque and 0.0 is fully transparent */
    transition: color 0.3s; /* Add a smooth transition effect for opacity changes */
    color: lightgrey;

    &:hover {
        //opacity: 1.0; /* Set opacity to fully opaque on hover */
        color: black;
    }
}
</style>
