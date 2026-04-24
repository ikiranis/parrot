<script setup lang="ts">

import SettingEdit from '@/components/setting/Edit.vue'
import error from "@/components/error/Error.vue"

const props = defineProps({
    id: {
        type: String,
        required: true
    }
})

</script>

<template>
    <setting-edit :id="props.id"/>

    <error />
</template>
