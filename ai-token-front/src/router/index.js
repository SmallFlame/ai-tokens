import Vue from 'vue'
import VueRouter from 'vue-router'
import store from '@/store'

Vue.use(VueRouter)

/**
 * 角色定义（code 字符串）：
 * super_admin - 超级管理员
 * advisor     - 导师
 * leader      - 组长
 * member      - 组员
 */

const routes = [
  {
    path: '/login',
    component: () => import('@/views/login/index.vue'),
    meta: { public: true }
  },
  {
    path: '/register',
    component: () => import('@/views/register/index.vue'),
    meta: { public: true }
  },
  // ── 管理后台（admin layout）──────────────────────────
  {
    path: '/',
    component: () => import('@/layout/index.vue'),
    redirect: '/dashboard',
    children: [
      { path: 'dashboard',    component: () => import('@/views/dashboard/index.vue'),    meta: { title: '数据看板' } },
      { path: 'channel',      component: () => import('@/views/channel/index.vue'),       meta: { title: '渠道管理', superAdmin: true } },
      { path: 'channelstat',  component: () => import('@/views/channelstat/index.vue'),   meta: { title: '渠道统计', superAdmin: true } },
      { path: 'model',        component: () => import('@/views/model/index.vue'),         meta: { title: '模型管理', superAdmin: true } },
      { path: 'apikey',       component: () => import('@/views/apikey/index.vue'),        meta: { title: 'API Key 管理', superAdmin: true } },
      { path: 'log',          component: () => import('@/views/log/index.vue'),           meta: { title: '调用日志', superAdmin: true } },
      { path: 'user',         component: () => import('@/views/user/index.vue'),          meta: { title: '用户管理', superAdmin: true } },
      { path: 'userstat',     component: () => import('@/views/userstat/index.vue'),      meta: { title: '用户统计', superAdmin: true } },
      { path: 'announcement', component: () => import('@/views/announcement/index.vue'),  meta: { title: '公告管理', superAdmin: true } },
      { path: 'admin-users',  component: () => import('@/views/admin-user/index.vue'),    meta: { title: '用户管理', adminOnly: true } },
      { path: 'duplicates',   component: () => import('@/views/duplicate-user/index.vue'), meta: { title: '重复账号排查', admin: true } },
      { path: 'group',        component: () => import('@/views/group/index.vue'),         meta: { title: '小组管理', admin: true } },
      { path: 'recharge-user',  component: () => import('@/views/recharge-user/index.vue'),  meta: { title: '个人充值日志', admin: true } },
      { path: 'recharge-group', component: () => import('@/views/recharge-group/index.vue'), meta: { title: '小组充值日志', admin: true } }
    ]
  },
  // ── 用户门户（user layout）──────────────────────────
  {
    path: '/portal',
    component: () => import('@/layout/UserLayout.vue'),
    redirect: '/portal/overview',
    children: [
      { path: 'overview', component: () => import('@/views/portal/overview/index.vue'), meta: { title: '我的概览' } },
      { path: 'mykeys',   component: () => import('@/views/portal/mykeys/index.vue'),   meta: { title: '我的密钥' } },
      { path: 'mylogs',   component: () => import('@/views/portal/mylogs/index.vue'),   meta: { title: '调用记录' } },
      { path: 'channels', component: () => import('@/views/portal/channels/index.vue'), meta: { title: '渠道价格' } },
      { path: 'team',     component: () => import('@/views/portal/team/index.vue'),     meta: { title: '我的团队', leader: true } },
      { path: 'teamkeys', component: () => import('@/views/portal/teamkeys/index.vue'), meta: { title: '团队密钥', leader: true } },
      { path: 'guide',    component: () => import('@/views/portal/guide/index.vue'),    meta: { title: '使用指南' } },
      { path: 'profile',  component: () => import('@/views/portal/profile/index.vue'),  meta: { title: '个人中心' } }
    ]
  },
  { path: '*', redirect: '/login' }
]

const router = new VueRouter({
  mode: 'history',
  base: process.env.BASE_URL,
  routes
})

router.beforeEach((to, from, next) => {
  const token    = store.state.token
  const roleCode = store.state.userInfo && store.state.userInfo.roleCode

  if (to.meta.public) {
    return token && to.path === '/login'
      ? next((roleCode === 'super_admin' || roleCode === 'advisor') ? '/dashboard' : '/portal/overview')
      : next()
  }
  if (!token) return next('/login')

  // 未完善个人信息的普通用户，强制锁定在个人中心
  if (to.path !== '/portal/profile' && store.getters.needsProfile) {
    return next('/portal/profile')
  }

  // superAdmin: 仅超管
  if (to.meta.superAdmin && roleCode !== 'super_admin') return next('/portal/overview')

  // admin: 超管和导师
  if (to.meta.admin && roleCode !== 'super_admin' && roleCode !== 'advisor') return next('/portal/overview')

  // adminOnly: 仅导师，超管不走此路由
  if (to.meta.adminOnly && roleCode !== 'advisor') return next('/portal/overview')

  // leader: 组长、导师和超管
  if (to.meta.leader && roleCode !== 'super_admin' && roleCode !== 'advisor' && roleCode !== 'leader') return next('/portal/overview')

  next()
})

export default router
