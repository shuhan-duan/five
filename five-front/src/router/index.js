import { createRouter, createWebHistory } from 'vue-router'

// Define routes for the application
const routes = [
  {
    path: '/',
    redirect: '/introduction',
    meta: { requireAuth: false } // Indicate that authentication is not required
  },
  {
    path: '/introduction',
    component: () => import('@/views/IntroductionWindow.vue'),
    meta: { requireAuth: false } // Indicate that authentication is not required
  },
  {
    path: '/boardAI',
    component: () => import('@/views/BoardAIWindow.vue')// AI Board view
  },
  {
    path: '/boardFriend',
    component: () => import('@/views/BoardFriendWindow.vue')// Friend Board view
  },
  {
    path: '/main',
    component: () => import('@/views/MainWindow.vue'), // Main window with nested routes
    children: [
      {
        path: '',
        redirect: '/main/game'
      },
      {
        path: 'game',
        component: () => import('@/views/GameWindow.vue') // Game view
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    component: () => import('@/views/NotFound.vue'),
    meta: { requireAuth: false } // Catch-all route for 404 pages
  }
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes
})

// Function to validate JWT tokens
const isJwtValid = (jwt) => {
  if (!jwt) {
    return false
  }
  try {
    const payload = JSON.parse(atob(jwt.split('.')[1])) // Decode the JWT payload
    const now = Date.now().valueOf() / 1000
    if (typeof payload.exp !== 'undefined' && payload.exp < now) {
      return false // Token has expired
    }
    return true
  } catch (error) {
    return false // Token is invalid
  }
}

import { useUserIderStore } from '@/stores/userIder'

// Global beforeEach guard to check for authentication
router.beforeEach((to, from, next) => {
  const userStore = useUserIderStore()
  const userJwt = userStore.userInfo.jwt

  // Check if the target route requires authentication
  const isAuthRequired = to.meta.requireAuth !== false

  if (isAuthRequired) {
    if (!userJwt || !isJwtValid(userJwt)) {
      // Redirect to the introduction page if JWT is not valid or missing
      next('/introduction');
    } else {
      // Proceed to the target route if JWT is valid
      next();
    }
  } else {
    // Proceed if the target route does not require authentication
    next();
  }
});

export default router
