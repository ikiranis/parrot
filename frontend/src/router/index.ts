// You have to add at include declaration the "router/**/*.ts", on tsconfig.json
// and install vue-router with npm

import {LocationQueryValue, RouteRecordRaw, createRouter, createWebHashHistory} from "vue-router"
import type { PhotoQuery } from "@/types"
import Home from "@/views/Home.vue"
import Settings from "@/views/setting/Settings.vue"
import Setting from "@/views/setting/Setting.vue"
import Photos from "@/views/Photos.vue"
import LibraryFolders from "@/views/LibraryFolders.vue"
import LibraryFolder from "@/views/libraryFolder/LibraryFolder.vue"
import Slideshow from "@/views/Slideshow.vue"

/**
 * Parses the slideshow's `q` route query parameter into a {@link PhotoQuery}.
 *
 * The criteria are carried as a JSON string so they can grow new filter fields without
 * changing the route shape. Returns null when the parameter is absent or cannot be parsed,
 * so a malformed link simply falls back to an unscoped slideshow rather than failing.
 *
 * @param raw the raw `q` query value as provided by vue-router
 * @returns the parsed search criteria, or null when absent or invalid
 */
const parseQueryParam = (raw: LocationQueryValue | LocationQueryValue[]): PhotoQuery | null => {
    if (typeof raw !== "string" || raw === "") return null
    try {
        return JSON.parse(raw) as PhotoQuery
    } catch {
        return null
    }
}

const routes : RouteRecordRaw[] = [
    {
        path: '/',
        name: 'Home',
        component: Home
    },
    {
        path: '/settings',
        name: 'Settings',
        component: Settings
    },
    {
        path: '/setting/:id',
        name: 'Setting',
        component: Setting,
        props: true
    },
    {
        path: '/photos',
        name: 'Photos',
        component: Photos,
        props: route => ({
            folderId: route.query.folderId != null ? Number(route.query.folderId) : null
        })
    },
    {
        path: '/library-folders',
        name: 'LibraryFolders',
        component: LibraryFolders
    },
    {
        path: '/library-folder/new',
        name: 'LibraryFolderNew',
        component: LibraryFolder,
        props: { id: 'new' }
    },
    {
        path: '/library-folder/:id',
        name: 'LibraryFolder',
        component: LibraryFolder,
        props: true
    },
    {
        path: '/slideshow',
        name: 'Slideshow',
        component: Slideshow,
        props: route => ({
            folderId: route.query.folderId != null ? Number(route.query.folderId) : null,
            // The search criteria travel as a single JSON string param so they can grow new
            // filter fields without touching the route. Malformed JSON falls back to no query.
            query: parseQueryParam(route.query.q)
        })
    },
]

// Set route path with webHistory, on current path
const router = createRouter({
    // history: createWebHistory(window.location.pathname),
    history: createWebHashHistory(),
    routes
})

export default router
