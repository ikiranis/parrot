<script setup lang="ts">
import { ref, Ref, onMounted } from "vue"
import { language } from "@/functions/languageStore.ts"
import { errorStore } from "@/components/error/errorStore.ts"
import { scanFolder, getPhotos, clearLibrary } from "@/api/photo.ts"
import { ScanResult, MediaFile } from "@/types"
import Error from "@/components/error/Error.vue"
import Loading from "@/components/utilities/Loading.vue"
import PhotoDetail from "@/components/PhotoDetail.vue"

const folderPath = ref("")
const scanning = ref(false)
const loadingPhotos = ref(false)
const clearing = ref(false)
const scanResult: Ref<ScanResult | null> = ref(null)
const photos: Ref<MediaFile[]> = ref([])
const totalElements = ref(0)
const currentPage = ref(0)
const PAGE_SIZE = 20
const selectedPhotoId: Ref<number | null> = ref(null)

onMounted(() => {
    loadPhotos()
})

const onScan = async () => {
    if (!folderPath.value.trim()) return

    scanResult.value = null
    scanning.value = true

    await scanFolder(folderPath.value.trim())
        .then((result: ScanResult | undefined) => {
            if (result) scanResult.value = result
            loadPhotos()
        })
        .catch((error: unknown) => {
            const err = error as { response?: { data?: { message?: string; status?: number }; }; message?: string }
            errorStore.set(true, err.response?.data?.message ?? err.message ?? '', err.response?.data?.status ?? 500)
        })

    scanning.value = false
}

const loadPhotos = async (page: number = 0) => {
    loadingPhotos.value = true
    currentPage.value = page

    await getPhotos(page, PAGE_SIZE)
        .then((response: { content: MediaFile[]; totalElements: number }) => {
            photos.value = response.content
            totalElements.value = response.totalElements
        })
        .catch((error: unknown) => {
            const err = error as { response?: { data?: { message?: string; status?: number }; }; message?: string }
            errorStore.set(true, err.response?.data?.message ?? err.message ?? '', err.response?.data?.status ?? 500)
        })

    loadingPhotos.value = false
}

const totalPages = () => Math.ceil(totalElements.value / PAGE_SIZE)

const onClearLibrary = async () => {
    if (!confirm(language.get("Are you sure you want to delete all photos from the library?"))) return

    clearing.value = true

    await clearLibrary()
        .then(() => {
            photos.value = []
            totalElements.value = 0
            currentPage.value = 0
            scanResult.value = null
        })
        .catch((error: unknown) => {
            const err = error as { response?: { data?: { message?: string; status?: number }; }; message?: string }
            errorStore.set(true, err.response?.data?.message ?? err.message ?? '', err.response?.data?.status ?? 500)
        })

    clearing.value = false
}
</script>

<template>
    <div class="container-fluid mt-4">
        <div class="d-flex justify-content-between align-items-center mb-3">
            <h4 class="mb-0">{{ language.get("Photos") }}</h4>
            <button
                class="btn btn-outline-danger btn-sm"
                :disabled="clearing"
                @click="onClearLibrary"
            >
                <span v-if="clearing" class="spinner-border spinner-border-sm me-1" role="status"></span>
                {{ language.get("Clean Library") }}
            </button>
        </div>

        <!-- Folder scan form -->
        <div class="card mb-4">
            <div class="card-header">{{ language.get("Scan Folder") }}</div>
            <div class="card-body">
                <div class="input-group">
                    <input
                        type="text"
                        class="form-control"
                        v-model="folderPath"
                        :placeholder="language.get('Enter server folder path')"
                        @keyup.enter="onScan"
                    />
                    <button
                        class="btn btn-primary"
                        :disabled="scanning || !folderPath.trim()"
                        @click="onScan"
                    >
                        <span v-if="scanning" class="spinner-border spinner-border-sm me-1" role="status"></span>
                        {{ scanning ? language.get("Scanning...") : language.get("Scan") }}
                    </button>
                </div>
            </div>
        </div>

        <!-- Scan result -->
        <div v-if="scanResult" class="alert" :class="scanResult.errors > 0 ? 'alert-warning' : 'alert-success'">
            <strong>{{ scanResult.message }}</strong>
            <ul class="mb-0 mt-1">
                <li>{{ language.get("Added") }}: {{ scanResult.added }}</li>
                <li>{{ language.get("Skipped") }}: {{ scanResult.skipped }}</li>
                <li>{{ language.get("Errors") }}: {{ scanResult.errors }}</li>
                <li>{{ language.get("Folders scanned") }}: {{ scanResult.foldersScanned }}</li>
                <li>{{ language.get("Folders skipped") }}: {{ scanResult.foldersSkipped }}</li>
            </ul>
        </div>

        <!-- Photos list -->
        <div v-if="loadingPhotos" class="row">
            <Loading />
        </div>

        <div v-if="!loadingPhotos && photos.length > 0">
            <div class="d-flex justify-content-between align-items-center mb-2">
                <span class="text-muted small">{{ totalElements }} {{ language.get("photos") }}</span>
            </div>

            <table class="table table-striped table-hover">
                <thead class="text-bg-dark">
                    <tr>
                        <th>#</th>
                        <th>{{ language.get("Filename") }}</th>
                        <th>{{ language.get("Path") }}</th>
                    </tr>
                </thead>
                <tbody>
                    <tr v-for="photo in photos" :key="photo.id">
                        <td class="align-middle">{{ photo.id }}</td>
                        <td class="align-middle">
                            <button
                                class="btn btn-link p-0 text-start text-decoration-none"
                                @click="selectedPhotoId = photo.id"
                            >{{ photo.filename }}</button>
                        </td>
                        <td class="align-middle text-muted small">{{ photo.path }}</td>
                    </tr>
                </tbody>
            </table>

            <!-- Pagination -->
            <nav v-if="totalPages() > 1">
                <ul class="pagination">
                    <li class="page-item" :class="{ disabled: currentPage === 0 }">
                        <a class="page-link" href="#" @click.prevent="loadPhotos(currentPage - 1)">
                            &laquo;
                        </a>
                    </li>
                    <li
                        v-for="p in totalPages()"
                        :key="p"
                        class="page-item"
                        :class="{ active: currentPage === p - 1 }"
                    >
                        <a class="page-link" href="#" @click.prevent="loadPhotos(p - 1)">{{ p }}</a>
                    </li>
                    <li class="page-item" :class="{ disabled: currentPage === totalPages() - 1 }">
                        <a class="page-link" href="#" @click.prevent="loadPhotos(currentPage + 1)">
                            &raquo;
                        </a>
                    </li>
                </ul>
            </nav>
        </div>

        <div v-if="!loadingPhotos && photos.length === 0" class="text-muted mt-3">
            {{ language.get("No photos found. Scan a folder to import photos.") }}
        </div>

        <Error />

        <PhotoDetail :photoId="selectedPhotoId" @close="selectedPhotoId = null" />
    </div>
</template>
