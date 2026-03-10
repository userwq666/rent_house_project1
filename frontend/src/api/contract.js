import request from '../utils/axios'

/**
 * 创建合同（签约）
 */
export function createContract(data) {
  return request({
    url: '/api/contracts',
    method: 'post',
    data
  })
}

/**
 * 获取我的合同
 */
export function getMyContracts() {
  return request({
    url: '/api/contracts/my',
    method: 'get'
  })
}

/**
 * 获取作为房主的合同
 */
export function getLandlordContracts() {
  return request({
    url: '/api/contracts/as-landlord',
    method: 'get'
  })
}

/**
 * 获取作为租客的合同
 */
export function getTenantContracts() {
  return request({
    url: '/api/contracts/as-tenant',
    method: 'get'
  })
}

/**
 * 根据ID获取合同详情
 */
export function getContractById(id) {
  return request({
    url: `/api/contracts/${id}`,
    method: 'get'
  })
}

/**
 * 终止合同
 */
export function terminateContract(id, data) {
  return request({
    url: `/api/contracts/${id}/terminate`,
    method: 'put',
    data
 })
}

export function respondTermination(requestId, data) {
  return request({
    url: `/api/contracts/termination/${requestId}/decision`,
    method: 'put',
    data
  })
}

export function respondTerminationByCounterparty(requestId, data) {
  return request({
    url: `/api/contracts/termination/${requestId}/counterparty-decision`,
    method: 'put',
    data
  })
}

export function adminTerminateContract(id) {
  return request({
    url: `/api/contracts/${id}/admin/terminate`,
    method: 'put'
  })
}

export function approveContractByLandlord(id) {
  return request({
    url: `/api/contracts/${id}/landlord/approve`,
    method: 'put'
  })
}

export function rejectContractByLandlord(id, data) {
  return request({
    url: `/api/contracts/${id}/landlord/reject`,
    method: 'put',
    data
  })
}

export function getAvailableStaffOptions() {
  return request({
    url: '/api/contracts/staff/options',
    method: 'get'
  })
}

export function approveContractByLandlordWithStaff(id, staffId) {
  return request({
    url: `/api/contracts/${id}/landlord/approve`,
    method: 'put',
    data: { staffId }
  })
}

export function approveContractByAdmin(id) {
  return request({
    url: `/api/contracts/${id}/admin/approve`,
    method: 'put'
  })
}

/**
 * 管理员拒绝合同
 */
export function rejectContractByAdmin(id, data) {
  return request({
    url: `/api/contracts/${id}/admin/reject`,
    method: 'put',
    data
  })
}

/**
 * 获取所有合同（仅管理员）
 */
export function getAllContracts() {
  return request({
    url: '/api/contracts/all',
    method: 'get'
  })
}

export function getMyStaffContracts() {
  return request({
    url: '/api/contracts/staff/my',
    method: 'get'
  })
}

export function uploadSignedContract(id, file) {
  const formData = new FormData()
  formData.append('file', file)
  return request({
    url: `/api/contracts/${id}/signed-file`,
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}
