<script setup lang="ts">

import SettingEdit from '@/components/setting/Edit.vue'
import error from "@/components/error/Error.vue"

/** Props for the Setting view. */
interface Props {
    /** The setting identifier passed via the router. */
    id: string;
}

const props = defineProps<Props>()

</script>

<template>
    <setting-edit :id="props.id"/>

    <error />
</template>
