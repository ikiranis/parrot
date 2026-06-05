<script setup lang="ts">
import LibraryFolderEdit from "@/components/libraryFolder/Edit.vue"
import Error from "@/components/error/Error.vue"

/** Props for the LibraryFolder view. */
interface Props {
	/** The library folder identifier, or "new" when creating. */
	id: string
}

const props = defineProps<Props>()
</script>

<template>
	<library-folder-edit :id="props.id" />

	<Error />
</template>
