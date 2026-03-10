import request from '../utils/axios'

/**
 * 获取所有可租房源
 */
export function getAvailableHouses() {
  return request({
    url: '/api/houses/available',
    method: 'get'
  })
}

/**
 * 搜索房源
 */
export function searchHouses(params) {
  return request({
    url: '/api/houses/search',
    method: 'get',
    params
  })
}

/**
 * 根据ID获取房源详情
 */
export function getHouseById(id) {
  return request({
    url: `/api/houses/${id}`,
    method: 'get'
  })
}

/**
 * 获取我发布的房源
 */
export function getMyHouses() {
  return request({
    url: '/api/houses/my',
    method: 'get'
  })
}

/**
 * 发布房源
 */
export function createHouse(data) {
  return request({
    url: '/api/houses',
    method: 'post',
    data
  })
}

/**
 * 更新房源
 */
export function updateHouse(id, data) {
  return request({
    url: `/api/houses/${id}`,
    method: 'put',
    data
  })
}

// 管理员获取全部房源
export function getAllHousesAdmin() {
  return request({
    url: '/api/houses/all',
    method: 'get'
  })
}

export function getStaffPendingHouses() {
  return request({
    url: '/api/houses/staff/pending',
    method: 'get'
  })
}

export function approveHouseByStaff(id) {
  return request({
    url: `/api/houses/${id}/staff/approve`,
    method: 'put'
  })
}

export function rejectHouseByStaff(id, reason) {
  return request({
    url: `/api/houses/${id}/staff/reject`,
    method: 'put',
    data: { reason }
  })
}

/**
 * 下架房源
 */
export function offlineHouse(id) {
  return request({
    url: `/api/houses/${id}/offline`,
    method: 'put'
  })
}

/**
 * 上架房源
 */
export function onlineHouse(id) {
  return request({
    url: `/api/houses/${id}/online`,
    method: 'put'
  })
}

/**
 * 删除房源
 */
export function deleteHouse(id) {
  return request({
    url: `/api/houses/${id}`,
    method: 'delete'
  })
}
