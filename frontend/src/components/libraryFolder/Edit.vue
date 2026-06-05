<script setup lang="ts">
import { onMounted, ref } from "vue"
import router from "@/router"
import { getLibraryFolder, createLibraryFolder, updateLibraryFolder } from "@/api/libraryFolder.ts"
import { errorStore } from "@/components/error/errorStore.ts"
import { language } from "@/functions/languageStore.ts"
import Loading from "@/components/utilities/Loading.vue"

/** Props for the LibraryFolder Edit component. */
interface Props {
	/** ID of the library folder to edit, or "new" to create one. */
	id: string
}

const props = defineProps<Props>()

const isNew = props.id === "new"

const path = ref("")
const loading = ref(false)
const loadingFolder = ref(false)

onMounted(async () => {
	if (isNew) return

	loadingFolder.value = true

	await getLibraryFolder(Number(props.id))
		.then((data) => {
			if (data) path.value = data.path
		})
		.catch((error: unknown) => {
			const err = error as { response?: { data?: { message?: string; status?: number } }; message?: string }
			errorStore.set(true, err.response?.data?.message ?? err.message ?? "", err.response?.data?.status ?? 500)
		})

	loadingFolder.value = false
})

/**
 * Saves the library folder (create or update).
 */
const save = async () => {
	if (!path.value.trim()) return

	loading.value = true

	const action = isNew
		? createLibraryFolder(path.value.trim())
		: updateLibraryFolder(Number(props.id), path.value.trim())

	await action
		.then(() => {
			errorStore.set(true, language.get("Library folder saved successfully"), 204)
			router.push({ name: "LibraryFolders" })
		})
		.catch((error: unknown) => {
			const err = error as { response?: { data?: { message?: string; status?: number } }; message?: string }
			errorStore.set(true, err.response?.data?.message ?? err.message ?? "", err.response?.data?.status ?? 500)
		})

	loading.value = false
}
</script>

<template>
	<div v-if="loadingFolder" class="row">
		<Loading />
	</div>

	<div v-if="!loadingFolder" class="container-fluid mt-4">
		<h4 class="mb-3">{{ isNew ? language.get("Add Library Folder") : language.get("Edit Library Folder") }}</h4>

		<div class="row">
			<div class="col-12 my-2">
				<label for="folderPath" class="form-label">{{ language.get("Path") }}</label>
				<input
					id="folderPath"
					type="text"
					v-model="path"
					class="form-control"
					:placeholder="language.get('Enter absolute folder path')"
					@keyup.enter="save"
				/>
			</div>
		</div>

		<div class="row mt-2">
			<button
				type="submit"
				class="btn btn-primary mx-auto my-3 col-12 col-lg-5"
				:disabled="loading || !path.trim()"
				@click="save"
			>
				<span v-if="loading" class="spinner-border spinner-border-sm me-1" role="status"></span>
				{{ language.get("Save") }}
			</button>

			<button
				type="button"
				class="btn btn-outline-dark my-3 mx-auto col-12 col-lg-5"
				@click="router.go(-1)"
			>
				{{ language.get("Cancel") }}
			</button>
		</div>
	</div>
</template>
