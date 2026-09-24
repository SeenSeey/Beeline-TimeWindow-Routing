import "./assets/styles/global.css";

import { createApp } from 'vue'
import { createPinia } from 'pinia'

import App from './App.vue'
import router from './router'
import { applyStoredTheme } from './composables/useTheme'

// before mounting, so the page never flashes the other theme
applyStoredTheme()

const app = createApp(App)

app.use(createPinia())
app.use(router)

app.mount('#app')
