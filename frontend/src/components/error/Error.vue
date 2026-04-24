<script setup lang="ts">

import { errorStore } from "./errorStore.ts"
import {computed, watch} from "vue";

const errorData = computed(() => errorStore.get()); // Using a ref for reactivity

let interval: any = null; // Declare interval variable

const closeAlert = () => {
    errorStore.set(false, '', 0)
}

// Clear the interval when the component is unmounted
// onBeforeUnmount(() => {
//     if (interval) {
//         clearInterval(interval);
//         errorStore.set(false, '', 0);
//     }
// });

// Watch for change on data and add timeout to close alert
watch(() => errorData.value.status, (newStatus) => {
    let count = 1;
    errorStore.setProgress(0);

    if(newStatus < 200 || newStatus >= 300) {
        clearInterval(interval);
        return;
    }

    interval = setInterval(() => {
        errorStore.setProgress(errorData.value.progress + 10);
        count++;

        if (errorData.value.progress > 100) {
            clearInterval(interval);
            errorStore.set(false, '', 0);
        }
    }, 500);
});

/**
 * Get success class color by error status
 *
 * @param status
 */
const getSuccessClassByErrorStatus = (status: number) => {
    if(status >= 200 && status < 300) {
        return 'success'
    }

    return 'danger'
}

</script>

<template>
    <div v-if="errorData.enable" :class="'mx-auto alert alert-dismissible show fade ' + ' alert-' + getSuccessClassByErrorStatus(errorData.status)" role="alert">
        <span>{{ errorData.message }}</span>

        <button type="button" class="btn-close" aria-label="Close" @click="closeAlert"></button>

        <div class="progress mb-0">
            <div :class="'progress-bar progress-bar-animated progress-bar-striped' + ' bg-' + getSuccessClassByErrorStatus(errorData.status)" role="progressbar" aria-valuenow="100"
                 aria-valuemin="0" aria-valuemax="100" :style="'width: ' + errorData.progress + '%'">
            </div>
        </div>
    </div>
</template>

<style scoped lang="scss">
    .progress {
        height: 1px;
        background: transparent;
    }
</style>
