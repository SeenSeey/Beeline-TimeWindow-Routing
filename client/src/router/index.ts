import { createRouter, createWebHistory } from 'vue-router'
import AppLayout from '@/layouts/AppLayout.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      component: AppLayout,
      children: [
        { path: '', redirect: { name: 'schedule' } },
        { path: 'map', name: 'map', component: () => import('@/pages/MapPage.vue') },
        { path: 'schedule', name: 'schedule', component: () => import('@/pages/TimelinePage.vue') },
        { path: 'requests', name: 'requests', component: () => import('@/pages/RequestsPage.vue') },
        {
          path: 'requests/:id(\\d+)',
          name: 'request',
          component: () => import('@/pages/RequestDetailedPage.vue'),
          props: (route) => ({ id: Number(route.params.id) }),
        },
        { path: 'crews', name: 'crews', component: () => import('@/pages/CrewsPage.vue') },
        {
          path: 'crews/:id(\\d+)',
          name: 'crew',
          component: () => import('@/pages/CrewDetailedPage.vue'),
          props: (route) => ({ id: Number(route.params.id) }),
        },
        { path: 'system', name: 'system', component: () => import('@/pages/SystemPage.vue') },
      ],
    },
    { path: '/:pathMatch(.*)*', redirect: { name: 'schedule' } },
  ],
})

export default router
