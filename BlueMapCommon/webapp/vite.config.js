import {fileURLToPath, URL} from 'node:url'

import {defineConfig} from 'vite'
import vue from '@vitejs/plugin-vue'

// noinspection JSUnusedGlobalSymbols
export default defineConfig({
    plugins: [vue()],
    base: './',
    resolve: {
        alias: {
            '@': fileURLToPath(new URL('./src', import.meta.url))
        }
    },
    define: {
        __VUE_I18N_FULL_INSTALL__: true,
        __VUE_I18N_LEGACY_API__: false,
        __INTLIFY_PROD_DEVTOOLS__: false,
    },
    build: {},
    server: {
        proxy: {
            '/settings.json': {
                target: 'http://localhost:8100',
                //target: 'https://bluecolored.de/bluemap',
                changeOrigin: true,
            },
            '/maps': {
                target: 'http://localhost:8100',
                //target: 'https://bluecolored.de/bluemap',
                changeOrigin: true,
            }
        }
    }
})
