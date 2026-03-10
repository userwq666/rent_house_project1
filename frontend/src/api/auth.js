import request from '../utils/axios'

/**
 * 用户注册
 */
export function register(data) {
  return request({
    url: '/api/auth/register',
    method: 'post',
    data
  })
}

/**
 * 用户登录
 */
export function login(data) {
  return request({
    url: '/api/auth/login',
    method: 'post',
    data
  })
}

/**
 * 获取当前用户信息
 */
export function getCurrentUser() {
  return request({
    url: '/api/auth/me',
    method: 'get'
  })
}

/**
 * 获取个人资料
 */
export function getProfile() {
  return request({
    url: '/api/auth/profile',
    method: 'get'
  })
}

/**
 * 更新个人资料
 */
export function updateProfile(data) {
  return request({
    url: '/api/auth/profile',
    method: 'put',
    data
  })
}

/**
 * 根据用户名获取用户信息
 */
export function getUserInfo(username) {
  return request({
    url: `/api/auth/user/${encodeURIComponent(username)}`,
    method: 'get'
  })
}
