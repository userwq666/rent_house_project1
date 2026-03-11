import { createRouter, createWebHistory } from 'vue-router'
import { ElMessage } from 'element-plus'

const routes = [
  {
    path: '/',
    redirect: '/home'
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/Login.vue')
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('../views/Register.vue')
  },
  {
    path: '/home',
    name: 'Home',
    component: () => import('../views/Home.vue')
  },
  {
    path: '/house/:id',
    name: 'HouseDetail',
    component: () => import('../views/HouseDetail.vue')
  },
  {
    path: '/house/publish',
    name: 'PublishHouse',
    component: () => import('../views/PublishHouse.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/house/edit/:id',
    name: 'EditHouse',
    component: () => import('../views/EditHouse.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/my-houses',
    name: 'MyHouses',
    component: () => import('../views/MyHouses.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/my-contracts',
    name: 'MyContracts',
    component: () => import('../views/MyContracts.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/messages',
    name: 'Messages',
    component: () => import('../views/MessageCenter.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/profile',
    name: 'Profile',
    component: () => import('../views/Profile.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/admin',
    name: 'Admin',
    component: () => import('../views/Admin.vue'),
    meta: { requiresAuth: true, requiresAdmin: true }
  },
  {
    path: '/staff',
    name: 'Staff',
    component: () => import('../views/Staff.vue'),
    meta: { requiresAuth: true, requiresStaff: true }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫
router.beforeEach((to, from, next) => {
  const token = sessionStorage.getItem('token')
  const userType = sessionStorage.getItem('userType')
  
  console.log('路由守卫:', {
    to: to.path,
    hasToken: !!token,
    userType: userType
  })
  
  if (to.meta.requiresAuth && !token) {
    console.log('未登录，跳转至登录页')
    next('/login')
  } else if (to.meta.requiresAdmin && userType !== 'ADMIN') {
    console.log('非管理员尝试访问管理页面，拦截')
    ElMessage.warning('权限不足')
    next('/home')
  } else if (to.meta.requiresStaff && userType !== 'STAFF') {
    ElMessage.warning('仅业务员可访问')
    next('/home')
  } else if (to.path === '/my-houses' && userType === 'ADMIN') {
    // 管理员不能访问我的房源页面
    console.log('管理员访问房源管理页面，拦截')
    ElMessage.warning('管理员不能访问房源管理页面')
    next('/home')
  } else if (to.path === '/my-houses' && userType === 'STAFF') {
    ElMessage.warning('业务员不能访问我的房源页面')
    next('/staff')
  } else if (to.path === '/register' && to.query.mode === 'staff') {
    if (!token) {
      ElMessage.warning('请先登录管理员账号')
      next('/login')
    } else if (userType !== 'ADMIN') {
      ElMessage.warning('仅管理员可创建业务员账号')
      next('/home')
    } else {
      next()
    }
  } else if (to.path === '/login' && token) {
    console.log('已登录用户访问登录页，重定向')
    if (userType === 'ADMIN') next('/admin')
    else if (userType === 'STAFF') next('/staff')
    else next('/home')
  } else {
    console.log('允许访问')
    next()
  }
})

export default router
