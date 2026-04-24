<script setup lang="ts">
import {SortField} from "@/types";

defineProps({
    sortField: {
        type: Object as () => SortField,
        required: true
    }
})
</script>

<template>
    <span v-if="sortField.enable">
        <span v-if="sortField.order === 'asc'">
            <svg xmlns="http://www.w3.org/2000/svg" width="16" class="tw-ml-1 tw-w-3 tw-h-3"
                 aria-hidden="true" fill="currentColor" viewBox="0 0 16 16">
                <path
                    d="m7.247 4.86-4.796 5.481c-.566.647-.106 1.659.753 1.659h9.592a1 1 0 0 0 .753-1.659l-4.796-5.48a1 1 0 0 0-1.506 0z"/>
            </svg>
        </span>

        <span v-if="sortField.order === 'desc'">
            <svg xmlns="http://www.w3.org/2000/svg" width="16" class="tw-ml-1 tw-w-3 tw-h-3"
                 aria-hidden="true" fill="currentColor" viewBox="0 0 16 16">
                <path
                    d="M7.247 11.14 2.451 5.658C1.885 5.013 2.345 4 3.204 4h9.592a1 1 0 0 1 .753 1.659l-4.796 5.48a1 1 0 0 1-1.506 0z"/>
            </svg>
        </span>
    </span>

    <span v-else>
        <span>
            <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="currentColor"
                 class="bi bi-caret-right-fill" viewBox="0 0 16 16">
                <path
                    d="m12.14 8.753-5.482 4.796c-.646.566-1.658.106-1.658-.753V3.204a1 1 0 0 1 1.659-.753l5.48 4.796a1 1 0 0 1 0 1.506z"/>
            </svg>
        </span>
    </span>
</template>
