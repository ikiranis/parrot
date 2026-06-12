// You have to add at include declaration the "router/**/*.ts", on tsconfig.json
// and install vue-router with npm

import {RouteRecordRaw, createRouter, createWebHashHistory} from "vue-router"
import Home from "@/views/Home.vue"
import Settings from "@/views/setting/Settings.vue"
import Setting from "@/views/setting/Setting.vue"
import Photos from "@/views/Photos.vue"
import LibraryFolders from "@/views/LibraryFolders.vue"
import LibraryFolder from "@/views/libraryFolder/LibraryFolder.vue"
import Slideshow from "@/views/Slideshow.vue"

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
            folderId: route.query.folderId != null ? Number(route.query.folderId) : null
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
