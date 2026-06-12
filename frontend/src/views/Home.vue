<script setup lang="ts">
import { language } from "@/functions/languageStore.ts";
</script>

<template>
    <div class="container-fluid mt-5 text-center">
        <div class="d-lg-flex flex-lg-wrap justify-content-center">
            <router-link class="btn p-0 m-3" :to="{ name: 'Settings' }">
                <div class="card">
                    <div class="card-img-top">
                        <svg xmlns="http://www.w3.org/2000/svg" width="64" height="64" fill="currentColor"
                            class="bi bi-gear-fill" viewBox="0 0 16 16">
                            <path
                                d="M9.405 1.05c-.413-1.4-2.397-1.4-2.81 0l-.1.34a1.464 1.464 0 0 1-2.105.872l-.31-.17c-1.283-.698-2.686.705-1.987 1.987l.169.311c.446.82.023 1.841-.872 2.105l-.34.1c-1.4.413-1.4 2.397 0 2.81l.34.1a1.464 1.464 0 0 1 .872 2.105l-.17.31c-.698 1.283.705 2.686 1.987 1.987l.311-.169a1.464 1.464 0 0 1 2.105.872l.1.34c.413 1.4 2.397 1.4 2.81 0l.1-.34a1.464 1.464 0 0 1 2.105-.872l.31.17c1.283.698 2.686-.705 1.987-1.987l-.169-.311a1.464 1.464 0 0 1 .872-2.105l.34-.1c1.4-.413 1.4-2.397 0-2.81l-.34-.1a1.464 1.464 0 0 1-.872-2.105l.17-.31c.698-1.283-.705-2.686-1.987-1.987l-.311.169a1.464 1.464 0 0 1-2.105-.872l-.1-.34zM8 10.93a2.929 2.929 0 1 1 0-5.86 2.929 2.929 0 0 1 0 5.858z" />
                        </svg>
                    </div>

                    <div class="card-body">
                        <h5 class="card-title">{{ language.get("Settings") }}</h5>

                        <p class="card-text">{{ language.get("App Settings") }}</p>
                    </div>
                </div>
            </router-link>

            <router-link class="btn p-0 m-3" :to="{ name: 'Photos' }">
                <div class="card">
                    <div class="card-img-top">
                        <svg xmlns="http://www.w3.org/2000/svg" width="64" height="64" fill="currentColor"
                            class="bi bi-images" viewBox="0 0 16 16">
                            <path d="M4.502 9a1.5 1.5 0 1 0 0-3 1.5 1.5 0 0 0 0 3z"/>
                            <path d="M14.002 13a2 2 0 0 1-2 2h-10a2 2 0 0 1-2-2V5A2 2 0 0 1 2 3a2 2 0 0 1 2-2h10a2 2 0 0 1 2 2v8a2 2 0 0 1-1.998 2zM14 2H4a1 1 0 0 0-1 1h9.002a2 2 0 0 1 2 2v7A1 1 0 0 0 15 11V3a1 1 0 0 0-1-1zM2.002 4a1 1 0 0 0-1 1v8l2.646-2.354a.5.5 0 0 1 .63-.062l2.66 1.773 3.71-3.71a.5.5 0 0 1 .577-.094l1.777 1.947V5a1 1 0 0 0-1-1h-10z"/>
                        </svg>
                    </div>

                    <div class="card-body">
                        <h5 class="card-title">{{ language.get("Photos") }}</h5>

                        <p class="card-text">{{ language.get("Browse and scan photos") }}</p>
                    </div>
                </div>
            </router-link>

            <router-link class="btn p-0 m-3" :to="{ name: 'Slideshow' }">
                <div class="card">
                    <div class="card-img-top">
                        <svg xmlns="http://www.w3.org/2000/svg" width="64" height="64" fill="currentColor"
                            class="bi bi-play-btn-fill" viewBox="0 0 16 16">
                            <path d="M0 12V4a2 2 0 0 1 2-2h12a2 2 0 0 1 2 2v8a2 2 0 0 1-2 2H2a2 2 0 0 1-2-2zm6.79-6.907A.5.5 0 0 0 6 5.5v5a.5.5 0 0 0 .79.407l3.5-2.5a.5.5 0 0 0 0-.814l-3.5-2.5z"/>
                        </svg>
                    </div>

                    <div class="card-body">
                        <h5 class="card-title">{{ language.get("Slideshow") }}</h5>

                        <p class="card-text">{{ language.get("Play photos as a slideshow") }}</p>
                    </div>
                </div>
            </router-link>

            <router-link class="btn p-0 m-3" :to="{ name: 'LibraryFolders' }">
                <div class="card">
                    <div class="card-img-top">
                        <svg xmlns="http://www.w3.org/2000/svg" width="64" height="64" fill="currentColor"
                            class="bi bi-folder-plus" viewBox="0 0 16 16">
                            <path d="m.5 3 .04.87a2 2 0 0 0-.342 1.311l.637 7A2 2 0 0 0 2.826 14H9v-1H2.826a1 1 0 0 1-.995-.91l-.637-7A1 1 0 0 1 2.19 4h11.62a1 1 0 0 1 .996 1.09L14.54 8h1.005l.256-2.819A2 2 0 0 0 13.81 3H9.828a2 2 0 0 1-1.414-.586l-.828-.828A2 2 0 0 0 6.172 1H2.5a2 2 0 0 0-2 2m5.672-1a1 1 0 0 1 .707.293L7.586 3H2.19q-.362.002-.683.12L1.5 2.98a1 1 0 0 1 1-.98z"/>
                            <path d="M13.5 9a.5.5 0 0 1 .5.5V11h1.5a.5.5 0 1 1 0 1H14v1.5a.5.5 0 1 1-1 0V12h-1.5a.5.5 0 0 1 0-1H13V9.5a.5.5 0 0 1 .5-.5"/>
                        </svg>
                    </div>

                    <div class="card-body">
                        <h5 class="card-title">{{ language.get("Library Folders") }}</h5>

                        <p class="card-text">{{ language.get("Manage library folders") }}</p>
                    </div>
                </div>
            </router-link>
        </div>
    </div>
</template>

<style scoped lang="scss">
.btn .card {
    width: 180px;
    height: 180px;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: flex-start;
    padding-top: 1.25rem;
}

.btn .card-img-top {
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
}

.btn .card-body {
    padding: 0.75rem 0.5rem 0;
    text-align: center;
}
</style>
