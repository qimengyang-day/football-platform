import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/',
    redirect: '/login'
  },
  {
    path: '/login',
    component: () => import('@/views/Login.vue')
  },
  {
    path: '/register',
    component: () => import('@/views/Register.vue')
  },
  {
    path: '/admin',
    component: () => import('@/layouts/AdminLayout.vue'),
    meta: { roles: ['ADMIN'] },
    children: [
      { path: 'green-field', component: () => import('@/views/admin/GreenField.vue') },
      { path: 'dashboard', component: () => import('@/views/admin/Dashboard.vue') },
      { path: 'players', component: () => import('@/views/admin/Players.vue') },
      { path: 'fans', component: () => import('@/views/admin/Fans.vue') },
      { path: 'clubs', component: () => import('@/views/admin/Clubs.vue') },
      { path: 'leagues', component: () => import('@/views/admin/Leagues.vue') },
      { path: 'match-manage', component: () => import('@/views/admin/MatchManage.vue') },
      { path: 'player-profile-updates', component: () => import('@/views/admin/PlayerProfileUpdates.vue') },
      { path: 'profile', component: () => import('@/views/common/ProfileCenter.vue') }
    ]
  },
  {
    path: '/player',
    component: () => import('@/layouts/PlayerLayout.vue'),
    meta: { roles: ['PLAYER'] },
    children: [
      { path: 'profile', component: () => import('@/views/player/Profile.vue') },
      { path: 'club', component: () => import('@/views/player/Club.vue') },
      { path: 'account', component: () => import('@/views/common/ProfileCenter.vue') }
    ]
  },
  {
    path: '/fan',
    component: () => import('@/layouts/FanLayout.vue'),
    meta: { roles: ['FAN'] },
    children: [
      { path: 'home', component: () => import('@/views/fan/Home.vue') },
      { path: 'team', component: () => import('@/views/fan/Team.vue') },
      { path: 'matches', component: () => import('@/views/fan/Matches.vue') },
      { path: 'players', component: () => import('@/views/fan/Players.vue') },
      { path: 'comments', component: () => import('@/views/fan/Comments.vue') },
      { path: 'profile', component: () => import('@/views/fan/Profile.vue') },
      { path: 'account', component: () => import('@/views/common/ProfileCenter.vue') },
      { path: 'match/:id', component: () => import('@/views/fan/MatchDetail.vue') }
    ]
  },
  {
    path: '/club',
    component: () => import('@/layouts/ClubLayout.vue'),
    meta: { roles: ['CLUB'] },
    children: [
      { path: 'dashboard', component: () => import('@/views/club/Dashboard.vue') },
      { path: 'profile', component: () => import('@/views/common/ProfileCenter.vue') }
    ]
  },
  {
    path: '/unauthorized',
    component: () => import('@/views/Unauthorized.vue')
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/login'
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  const role = localStorage.getItem('role')

  if (to.path === '/login' || to.path === '/register') return next()
  if (!token) return next('/login')

  if (to.meta.roles && !to.meta.roles.includes(role)) {
    return next('/unauthorized')
  }
  next()
})

export default router