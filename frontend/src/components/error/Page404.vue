<script setup lang="ts">
defineProps({
    title: {
        type: String,
        required: true
    },
    text: {
        type: String,
        required: true
    }
})
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
