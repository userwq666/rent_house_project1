import request from '../utils/axios'

export function fetchMessages() {
  return request({
    url: '/api/messages',
    method: 'get'
  })
}

export function getMessageContacts() {
  return request({
    url: '/api/messages/contacts',
    method: 'get'
  })
}

export function getChatMessages(contactId, contactType) {
  return request({
    url: `/api/messages/chat/${contactId}`,
    method: 'get',
    params: contactType ? { contactType } : undefined
  })
}

export function markMessageRead(id) {
  return request({
    url: `/api/messages/${id}/read`,
    method: 'post'
  })
}

export function updateMessageStatus(id, status) {
  return request({
    url: `/api/messages/${id}/status`,
    method: 'post',
    data: { status }
  })
}

export function markAllMessagesRead() {
  return request({
    url: '/api/messages/read-all',
    method: 'post'
  })
}

export function sendMessage(data) {
  return request({
    url: '/api/messages',
    method: 'post',
    data
  })
}

export function fetchAdminMessages() {
  return request({
    url: '/api/messages/admin',
    method: 'get'
  })
}

export function getUnreadMessageCount() {
  return request({
    url: '/api/messages/unread-count',
    method: 'get'
  })
}

/**
 * 归档与特定联系人的所有消息（前端隐藏，数据库保留）
 */
export function archiveContactMessages(contactId, contactType) {
  return request({
    url: `/api/messages/archive/${contactId}`,
    method: 'post',
    params: contactType ? { contactType } : undefined
  })
}
