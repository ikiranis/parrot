<script setup lang="ts">
import { ref, Ref, onMounted } from "vue"
import { language } from "@/functions/languageStore.ts"
import { errorStore } from "@/components/error/errorStore.ts"
import { getLibraryFolders, deleteLibraryFolder } from "@/api/libraryFolder.ts"
import { exportTagData, importTagData } from "@/api/photo.ts"
import type { LibraryFolder, TagExportItem } from "@/types"
import router from "@/router"
import Error from "@/components/error/Error.vue"
import Loading from "@/components/utilities/Loading.vue"
import ScanProgress from "@/components/scan/ScanProgress.vue"

const IMPORT_CHUNK_SIZE = 500

const loading = ref(false)
const folders: Ref<LibraryFolder[]> = ref([])
const importFileInput = ref<HTMLInputElement | null>(null)
const exportLoading = ref(false)
const importLoading = ref(false)
/** Progress percentage (0–100) while an import is running; null otherwise. */
const importProgress = ref<number | null>(null)
/** Running totals shown below the progress bar. */
const importTotals = ref({ processed: 0, total: 0, updated: 0, notFound: 0 })

onMounted(() => {
	loadFolders()
})

const loadFolders = async () => {
	loading.value = true

	await getLibraryFolders()
		.then((data: LibraryFolder[]) => {
			folders.value = data
		})
		.catch((error: unknown) => {
			const err = error as { response?: { data?: { message?: string; status?: number } }; message?: string }
			errorStore.set(true, err.response?.data?.message ?? err.message ?? "", err.response?.data?.status ?? 500)
		})

	loading.value = false
}

const onAdd = () => {
	router.push({ name: "LibraryFolderNew" })
}

const onEdit = (id: number) => {
	router.push({ name: "LibraryFolder", params: { id } })
}

const onDelete = async (id: number) => {
	if (!confirm(language.get("Are you sure you want to delete this library folder?"))) return

	await deleteLibraryFolder(id)
		.then(() => {
			folders.value = folders.value.filter(f => f.id !== id)
		})
		.catch((error: unknown) => {
			const err = error as { response?: { data?: { message?: string; status?: number } }; message?: string }
			errorStore.set(true, err.response?.data?.message ?? err.message ?? "", err.response?.data?.status ?? 500)
		})
}

const onExport = async () => {
	exportLoading.value = true
	try {
		const data = await exportTagData()
		const json = JSON.stringify(data, null, 2)
		const blob = new Blob([json], { type: 'application/json' })
		const url = URL.createObjectURL(blob)
		const a = document.createElement('a')
		a.href = url
		a.download = 'parrot-tags.json'
		a.click()
		URL.revokeObjectURL(url)
	} catch (error: unknown) {
		const err = error as { response?: { data?: { message?: string; status?: number } }; message?: string }
		errorStore.set(true, err.response?.data?.message ?? err.message ?? "", err.response?.data?.status ?? 500)
	} finally {
		exportLoading.value = false
	}
}

const onImport = () => {
	importFileInput.value?.click()
}

const onFileSelected = async (event: Event) => {
	const file = (event.target as HTMLInputElement).files?.[0]
	if (!file) return
	importLoading.value = true
	importProgress.value = 0
	importTotals.value = { processed: 0, total: 0, updated: 0, notFound: 0 }
	try {
		const text = await file.text()
		const items: TagExportItem[] = JSON.parse(text)
		const total = items.length
		importTotals.value.total = total
		let totalUpdated = 0
		let totalNotFound = 0
		for (let i = 0; i < total; i += IMPORT_CHUNK_SIZE) {
			const chunk = items.slice(i, i + IMPORT_CHUNK_SIZE)
			const result = await importTagData(chunk)
			totalUpdated += result.updated
			totalNotFound += result.notFound
			const processed = Math.min(i + IMPORT_CHUNK_SIZE, total)
			importTotals.value = { processed, total, updated: totalUpdated, notFound: totalNotFound }
			importProgress.value = Math.round(processed / total * 100)
		}
		alert(`${language.get("Import complete")}: ${totalUpdated} ${language.get("updated")}, ${totalNotFound} ${language.get("not found")}`)
	} catch (error: unknown) {
		const err = error as { response?: { data?: { message?: string; status?: number } }; message?: string }
		errorStore.set(true, err.response?.data?.message ?? err.message ?? "", err.response?.data?.status ?? 500)
	} finally {
		importLoading.value = false
		importProgress.value = null
		;(event.target as HTMLInputElement).value = ''
	}
}
</script>

<template>
	<div class="container-fluid mt-4">
		<div class="d-flex justify-content-between align-items-center mb-3">
			<h4 class="mb-0">{{ language.get("Library Folders") }}</h4>
			<div class="d-flex gap-2">
				<button class="btn btn-outline-secondary btn-sm" :disabled="loading" @click="loadFolders">
					<span v-if="loading" class="spinner-border spinner-border-sm me-1" role="status"></span>
					{{ language.get("Refresh") }}
				</button>
				<button class="btn btn-outline-secondary btn-sm" :disabled="exportLoading || importLoading" @click="onExport">
					<span v-if="exportLoading" class="spinner-border spinner-border-sm me-1" role="status"></span>
					{{ language.get("Export Tags") }}
				</button>
				<button class="btn btn-outline-secondary btn-sm" :disabled="exportLoading || importLoading" @click="onImport">
					<span v-if="importLoading" class="spinner-border spinner-border-sm me-1" role="status"></span>
					{{ language.get("Import Tags") }}
				</button>
				<input
					ref="importFileInput"
					type="file"
					accept=".json,application/json"
					class="d-none"
					@change="onFileSelected"
				/>
				<button class="btn btn-primary btn-sm" :disabled="loading" @click="onAdd">
					{{ language.get("Add Folder") }}
				</button>
			</div>
		</div>

		<!-- Import progress bar -->
		<div v-if="importProgress !== null" class="mb-3">
			<div class="d-flex align-items-center gap-2 mb-1">
				<div class="progress flex-grow-1" style="height: 8px;">
					<div
						class="progress-bar progress-bar-striped progress-bar-animated"
						role="progressbar"
						:style="{ width: importProgress + '%' }"
						:aria-valuenow="importProgress"
						aria-valuemin="0"
						aria-valuemax="100"
					></div>
				</div>
				<small class="text-muted text-nowrap">{{ importProgress }}%</small>
			</div>
			<small class="text-muted">
				{{ importTotals.processed.toLocaleString() }} / {{ importTotals.total.toLocaleString() }}
				{{ language.get("items") }}
				&mdash; {{ importTotals.updated.toLocaleString() }} {{ language.get("updated") }},
				{{ importTotals.notFound.toLocaleString() }} {{ language.get("not found") }}
			</small>
		</div>

		<div v-if="loading" class="row">
			<Loading />
		</div>

		<div v-if="!loading && folders.length > 0">
			<div class="mb-2">
				<span class="text-muted small">{{ folders.length }} {{ language.get("folders") }}</span>
			</div>

			<table class="table table-striped table-hover">
				<thead class="text-bg-dark">
					<tr>
						<th>#</th>
						<th>{{ language.get("Path") }}</th>
						<th></th>
					</tr>
				</thead>
				<tbody>
					<tr v-for="folder in folders" :key="folder.id">
						<td class="align-middle">{{ folder.id }}</td>
						<td class="align-middle">{{ folder.path }}</td>
						<td class="align-middle text-end">
							<a class="btn btn-sm me-1" @click="onEdit(folder.id)" :title="language.get('Edit')">
								<svg xmlns="http://www.w3.org/2000/svg" width="16" fill="currentColor"
									class="bi bi-pencil-square" viewBox="0 0 16 16">
									<path
										d="M15.502 1.94a.5.5 0 0 1 0 .706L14.459 3.69l-2-2L13.502.646a.5.5 0 0 1 .707 0l1.293 1.293zm-1.75 2.456-2-2L4.939 9.21a.5.5 0 0 0-.121.196l-.805 2.414a.25.25 0 0 0 .316.316l2.414-.805a.5.5 0 0 0 .196-.12l6.813-6.814z" />
									<path fill-rule="evenodd"
										d="M1 13.5A1.5 1.5 0 0 0 2.5 15h11a1.5 1.5 0 0 0 1.5-1.5v-6a.5.5 0 0 0-1 0v6a.5.5 0 0 1-.5.5h-11a.5.5 0 0 1-.5-.5v-11a.5.5 0 0 1 .5-.5H9a.5.5 0 0 0 0-1H2.5A1.5 1.5 0 0 0 1 2.5v11z" />
								</svg>
							</a>
							<a class="btn btn-sm text-danger" @click="onDelete(folder.id)" :title="language.get('Delete')">
								<svg xmlns="http://www.w3.org/2000/svg" width="16" fill="currentColor"
									class="bi bi-trash" viewBox="0 0 16 16">
									<path
										d="M5.5 5.5A.5.5 0 0 1 6 6v6a.5.5 0 0 1-1 0V6a.5.5 0 0 1 .5-.5zm2.5 0a.5.5 0 0 1 .5.5v6a.5.5 0 0 1-1 0V6a.5.5 0 0 1 .5-.5zm3 .5a.5.5 0 0 0-1 0v6a.5.5 0 0 0 1 0V6z" />
									<path fill-rule="evenodd"
										d="M14.5 3a1 1 0 0 1-1 1H13v9a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V4h-.5a1 1 0 0 1-1-1V2a1 1 0 0 1 1-1H6a1 1 0 0 1 1-1h2a1 1 0 0 1 1 1h3.5a1 1 0 0 1 1 1v1zM4.118 4 4 4.059V13a1 1 0 0 0 1 1h6a1 1 0 0 0 1-1V4.059L11.882 4H4.118zM2.5 3V2h11v1h-11z" />
								</svg>
							</a>
						</td>
					</tr>
				</tbody>
			</table>
		</div>

		<div v-if="!loading && folders.length === 0" class="text-muted mt-3">
			{{ language.get("No library folders configured. Add a folder to start scanning for media.") }}
		</div>

		<ScanProgress />

		<Error />
	</div>
</template>
