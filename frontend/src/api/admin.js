import request from '../utils/axios'

export function fetchAllUsers() {
  return request({
    url: '/api/admin/users',
    method: 'get'
  })
}

export function updateUserRestrictions(userId, data) {
  return request({
    url: `/api/admin/users/${userId}/restrictions`,
    method: 'put',
    data
  })
}

export function updateUserStatus(userId, data) {
  return request({
    url: `/api/admin/users/${userId}/status`,
    method: 'put',
    data
  })
}
