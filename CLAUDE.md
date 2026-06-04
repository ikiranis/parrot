# ParrotApp

A full-stack media library manager. Backend: Java Spring Boot. Frontend: Vue 3 + TypeScript.

## Overview

ParrotApp manages a personal library of photos, videos, music, etc. It exposes a REST API (port 9999) and a Vue 3 frontend (dev port 5173). The backend uses Spring Boot with Apache Derby (embedded); the frontend uses Vite and SCSS.

## Functional Requirements

### Backend

- RESTful API: controllers, services, repositories, models
- CORS configuration
- Internationalization via `multiLanguage.xml`
- Settings management API
- Custom exception handling

### Frontend

- Vue 3 + TypeScript + Vite
- Client-side routing via Vue Router
- Sidebar navigation, Home, Dashboard views
- Language selector component
- Error pages (404, general error)

## Technical Stack

| Layer    | Technology                    |
| -------- | ----------------------------- |
| Backend  | Java 21+, Spring Boot         |
| Database | Apache Derby (embedded)       |
| Frontend | Vue 3, TypeScript, Vite, SCSS |
| API      | REST / JSON, Axios            |

## Coding Standards

### General

- **Indentation:** Tabs everywhere (Java, TypeScript, Vue, HTML, SCSS); tab width = 4 spaces
- **Line endings:** LF (Unix-style)
- **Max line length:** 120 characters
- **Encoding:** UTF-8

### Java

- **Javadoc:** Required on all public classes, interfaces, methods, and fields (`@param`, `@return`, `@throws`); class-level Javadoc must describe purpose and responsibilities
- **Naming:** `PascalCase` classes, `camelCase` methods/variables, `UPPER_SNAKE_CASE` constants, `lowercase.dot.separated` packages
- **Annotations:** Each on its own line above the declaration
- **Braces:** Opening brace on same line (K&R style)
- **Access modifiers:** Always explicit; most restrictive possible
- **Null handling:** Avoid returning `null`; use `Optional<T>` where appropriate
- **Exceptions:** Use custom exception classes; never swallow silently

### TypeScript / Vue

- **TSDoc:** Required on all exported functions, composables, types, and Vue components (props, emits, return values)
- **Naming:** `PascalCase` components (filename must match), `camelCase` functions, `use` prefix for composables, `PascalCase` types/interfaces, `UPPER_SNAKE_CASE` constants
- **Vue SFC order:** `<script setup>` → `<template>` → `<style scoped>`
- **TypeScript:** Strict mode enabled; no `any`
- **Props:** Always typed with `defineProps<T>()` and explicit interface
- **Emits:** Always typed with `defineEmits<T>()`

### SCSS

- **Naming:** BEM methodology (`block__element--modifier`)
- **Scoped styles:** Use `<style scoped>` in Vue SFCs unless global styles are intentionally needed

### API Design

- **Resources:** Plural nouns (`/photos`, `/settings`)
- **HTTP methods:** `GET` (read), `POST` (create), `PUT`/`PATCH` (update), `DELETE` (remove)
- **Responses:** Consistent JSON; errors include `status`, `message`, and optionally `details`

## Project Structure

```text
ParrotApp/
├── frontend/               # Vue 3 frontend
│   ├── src/
│   │   ├── api/            # API client modules
│   │   ├── components/     # Reusable UI components
│   │   ├── functions/      # Utility functions and stores
│   │   ├── router/         # Vue Router configuration
│   │   ├── types/          # TypeScript type definitions
│   │   └── views/          # Page-level view components
│   └── public/
├── src/main/java/          # Spring Boot backend
│   └── eu/apps4net/parrotApp/
│       ├── configurations/ # CORS, language, settings config
│       ├── controllers/    # REST controllers
│       ├── exceptions/     # Custom exception handling
│       ├── models/         # Entity/DTO models
│       ├── repositories/   # Data access layer
│       ├── services/       # Business logic layer
│       └── utilities/      # Shared utilities
├── src/main/resources/     # Application properties and language files
├── db/                     # Embedded Derby database files
└── pom.xml                 # Maven build descriptor
```
