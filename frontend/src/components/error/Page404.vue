<script setup lang="ts">
/** Props for the Page404 component. */
interface Props {
    /** Heading text displayed in the 404 card. */
    title: string;
    /** Body text displayed below the heading. */
    text: string;
}

defineProps<Props>()
</script>

<template>
    <div class="container">
        <div class="row justify-content-center align-self-center">
                <div class="text-center">
                    <h1 class="display-4">{{ title }}</h1>
                    <p class="lead">{{ text }}</p>
                </div>
        </div>
    </div>
</template>
