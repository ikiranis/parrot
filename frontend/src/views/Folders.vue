<script setup lang="ts">
import { ref, Ref, onMounted } from "vue"
import { language } from "@/functions/languageStore.ts"
import { errorStore } from "@/components/error/errorStore.ts"
import { getFolders } from "@/api/folder.ts"
import { Folder } from "@/types"
import Error from "@/components/error/Error.vue"
import Loading from "@/components/utilities/Loading.vue"

const loading = ref(false)
const folders: Ref<Folder[]> = ref([])

onMounted(() => {
	loadFolders()
})

const loadFolders = async () => {
	loading.value = true

	await getFolders()
		.then((data: Folder[]) => {
			folders.value = data
		})
		.catch((error: unknown) => {
			const err = error as { response?: { data?: { message?: string; status?: number } }; message?: string }
			errorStore.set(true, err.response?.data?.message ?? err.message ?? "", err.response?.data?.status ?? 500)
		})

	loading.value = false
}
</script>

<template>
	<div class="container-fluid mt-4">
		<div class="d-flex justify-content-between align-items-center mb-3">
			<h4 class="mb-0">{{ language.get("Folders") }}</h4>
			<button class="btn btn-outline-secondary btn-sm" :disabled="loading" @click="loadFolders">
				<span v-if="loading" class="spinner-border spinner-border-sm me-1" role="status"></span>
				{{ language.get("Refresh") }}
			</button>
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
						<th>{{ language.get("Hash") }}</th>
					</tr>
				</thead>
				<tbody>
					<tr v-for="folder in folders" :key="folder.id">
						<td class="align-middle">{{ folder.id }}</td>
						<td class="align-middle">{{ folder.path }}</td>
						<td class="align-middle text-muted small font-monospace">{{ folder.hash ?? "—" }}</td>
					</tr>
				</tbody>
			</table>
		</div>

		<div v-if="!loading && folders.length === 0" class="text-muted mt-3">
			{{ language.get("No folders found. Scan a folder to import media.") }}
		</div>

		<Error />
	</div>
</template>
