# Product Requirements Document (PRD)

## Project: ParrotApp

---

## 1. Overview

**Product Name:** ParrotApp  
**Version:** 1.0  
**Date:** 2026-05-26  
**Status:** Draft  

### 1.1 Summary

ParrotApp is a full-stack to manage my library of photos, videos, music etc. It provides a reusable scaffold for Java Spring Boot backend and Vue 3 frontend applications, with built-in support for internationalization, settings management.
The project will provide an API to access my media library, and a frontend interface to view and manage the content. The backend will be structured with controllers, services, repositories, and models, while the frontend will utilize Vue 3 with TypeScript and Vite for a modern development experience.

---

## 2. Functional Requirements

### 2.1 Backend (Spring Boot)

- [ ] RESTful API structure with controllers, services, repositories, and models
- [ ] CORS configuration for cross-origin requests
- [ ] Language / internationalization support via `multiLanguage.xml`
- [ ] Settings management API
- [ ] Exception handling with custom exception classes

### 2.2 Frontend (Vue 3)

- [ ] Vue 3 with TypeScript and Vite build tooling
- [ ] Client-side routing via Vue Router
- [ ] Sidebar navigation component
- [ ] Home public view
- [ ] Dashboard view
- [ ] Language selector component
- [ ] Error handling pages (404, general error)

### 2.3 Internationalization

- [ ] Multi-language support on the backend (`multiLanguage.xml`)
- [ ] Language store on the frontend
- [ ] Language selection exposed in the UI

---

## 3. Non-Functional Requirements

| Requirement | Details |
|-------------|---------|
| **Backend Runtime** | Java 21+ |
| **Build Tool** | Maven (mvnw wrapper included) |
| **Frontend Runtime** | Node.js v16+ |
| **Frontend Build** | Vite |
| **API Style** | REST / JSON |
| **Backend Port** | 9999 (default) |
| **Frontend Dev Port** | 5173 (default) |
| **Database** | Embedded (Apache Derby, configured via `db/`) |

---

## 4. Technical Stack

| Layer | Technology |
|-------|------------|
| Backend Language | Java 21+ |
| Backend Framework | Spring Boot |
| Frontend Language | TypeScript |
| Frontend Framework | Vue 3 |
| Frontend Build | Vite |
| Styling | SCSS |
| Database | Apache Derby (embedded) |
| API Communication | REST / Axios (via `api/` modules) |

---

## 5. Coding Standards

### 5.1 General

- **Indentation:** 4 spaces (no tabs) for all files (Java, TypeScript, Vue, HTML, CSS/SCSS)
- **Line endings:** LF (Unix-style)
- **Max line length:** 120 characters
- **Encoding:** UTF-8

### 5.2 Java (Backend)

- **Javadoc:** Required on all public classes, interfaces, methods, and fields
  - Every `@param`, `@return`, and `@throws` must be documented
  - Class-level Javadoc must describe purpose and responsibilities
- **Naming conventions:**
  - Classes: `PascalCase`
  - Methods and variables: `camelCase`
  - Constants: `UPPER_SNAKE_CASE`
  - Packages: `lowercase.dot.separated`
- **Annotations:** Place each annotation on its own line above the declaration
- **Braces:** Opening brace on the same line (K&R style)
- **Access modifiers:** Always explicit; prefer the most restrictive modifier possible
- **Null handling:** Avoid returning `null`; use `Optional<T>` where appropriate
- **Exception handling:** Use custom exception classes; never swallow exceptions silently

### 5.3 TypeScript / Vue (Frontend)

- **JSDoc / TSDoc:** Required on all exported functions, composables, types, and Vue components
  - Describe props, emits, and return values
- **Naming conventions:**
  - Components: `PascalCase` (file and component name must match)
  - Composables / functions: `camelCase`, prefix composables with `use`
  - Types and interfaces: `PascalCase`
  - Constants: `UPPER_SNAKE_CASE`
- **Vue SFC structure order:** `<script setup>`, `<template>`, `<style>`
- **TypeScript:** Strict mode enabled; no use of `any`
- **Props:** Always typed; use `defineProps<T>()` with explicit interface
- **Emits:** Always typed with `defineEmits<T>()`

### 5.4 Styling (SCSS)

- **Indentation:** 4 spaces
- **Naming:** BEM methodology for class names (`block__element--modifier`)
- **Scoped styles:** Use `<style scoped>` in Vue SFCs unless global styles are intentionally needed

### 5.5 API Design

- **REST conventions:** Plural nouns for resources (`/photos`, `/settings`)
- **HTTP methods:** `GET` (read), `POST` (create), `PUT`/`PATCH` (update), `DELETE` (remove)
- **Response format:** Consistent JSON structure for all responses
- **Error responses:** Include `status`, `message`, and optionally `details` fields

---

## 6. Project Structure

```
ParrotApp/
├── frontend/               # Vue 3 frontend app
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

---

