<script setup lang="ts">
import {Pageable} from "@/types/index.ts";
import {language} from "@/functions/languageStore.ts";

const props = defineProps({
    pagination: {
        type: Object as () => Pageable,
        required: true
    }
})

const emit = defineEmits(['changePage']);

const prevPage = () => {
    props.pagination.pageNumber--

    emit('changePage', props.pagination)

}

const nextPage = () => {
    props.pagination.pageNumber++

    emit('changePage', props.pagination)
}
</script>

<template>
    <button class="btn btn-sm btn-secondary my-2 mx-2" @click="prevPage" v-if="(pagination.pageNumber+1) > 1">
        {{ language.get("Previous") }}
    </button>
    <span><strong>{{ pagination.pageNumber+1 }}</strong> {{ language.get("of") }} <strong>{{ pagination.totalPages }}</strong> {{ language.get("pages") }}</span>
    <button class="btn btn-sm btn-secondary my-2 mx-2" @click="nextPage"
            v-if="(pagination.pageNumber+1) !== pagination.totalPages">
        {{ language.get("Next") }}
    </button>
</template>
