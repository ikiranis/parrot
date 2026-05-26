# Copilot Instructions for ParrotApp

This is a full-stack Java Spring Boot + Vue 3 project. Always follow the specs defined in [PRD.md](../PRD.md).

## Key Rules (from PRD Section 5 — Coding Standards)

- **Indentation:** Tabs everywhere (Java, TypeScript, Vue, SCSS); tab width = 4 spaces
- **Javadoc:** Required on every public Java class, interface, method, and field (`@param`, `@return`, `@throws`)
- **TSDoc:** Required on all exported TypeScript functions, composables, types, and Vue components
- **Java naming:** `PascalCase` classes, `camelCase` methods/variables, `UPPER_SNAKE_CASE` constants
- **TypeScript naming:** `PascalCase` components, `camelCase` functions, `use` prefix for composables
- **Vue SFC order:** `<script setup>` → `<template>` → `<style scoped>`
- **TypeScript strict mode:** enabled; no `any`
- **No null returns:** use `Optional<T>` in Java where appropriate
- **REST conventions:** plural noun resources, standard HTTP verbs, consistent JSON responses

## Stack

- Backend: Java 21+, Spring Boot, Apache Derby (embedded)
- Frontend: Vue 3, TypeScript, Vite, SCSS
- API: REST/JSON on port 9999; frontend dev on port 5173
