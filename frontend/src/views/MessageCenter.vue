<template>
  <div class="message-center">
    <el-page-header @back="$router.push('/home')">
      <template #content>
        <span class="page-title">消息中心</span>
      </template>
    </el-page-header>

    <div class="chat-container">
      <!-- 联系人列表 -->
      <div class="contact-list">
        <div class="list-header">
          <h3>{{ isOperator ? '待办消息' : '联系人' }}</h3>
          <div class="header-actions">
            <el-button v-if="!isOperator" type="primary" link @click="showAddContact = true">
              <el-icon><Plus /></el-icon>
            </el-button>
            <el-button type="primary" link @click="loadContacts">刷新</el-button>
          </div>
        </div>
        <el-scrollbar class="contact-scroll">
          <div
            v-for="contact in contacts"
            :key="contact.id"
            class="contact-item-wrapper"
          >
            <div class="contact-item-container">
              <div
                class="contact-item"
                :class="{ active: activeContact && activeContact.id === contact.id }"
                @click="selectContact(contact)"
              >
                <div class="contact-avatar">
                  <el-avatar :size="40">{{ getContactName(contact).charAt(0) }}</el-avatar>
                </div>
                <div class="contact-info">
                  <div class="contact-name">{{ getContactName(contact) }}</div>
                  <div class="contact-last-message">{{ contact.content }}</div>
                </div>
                <div class="contact-meta">
                  <div class="contact-time">{{ formatTime(contact.createdAt) }}</div>
                  <el-badge v-if="contact.unreadCount > 0" :value="contact.unreadCount" class="contact-unread" />
                </div>
              </div>
              <el-button
                v-if="!isOperator"
                class="delete-btn"
                type="danger"
                size="small"
                @click.stop="deleteContact(contact)"
              >
                删除
              </el-button>
            </div>
          </div>
          <el-empty v-if="contacts.length === 0 && !loading" description="暂无联系人" />
        </el-scrollbar>
      </div>

      <!-- 聊天窗口 -->
      <div class="chat-window">
        <div class="chat-header">
          <div class="chat-header-info">
            <div class="chat-contact-name">{{ currentContact ? getContactName(currentContact) : '选择一个联系人开始聊天' }}</div>
          </div>
        </div>
        <el-scrollbar class="chat-messages" ref="messageScroll">
          <template v-if="currentContact">
            <div
              v-for="message in chatMessages"
              :key="message.id"
              class="message-item"
            >

              <div
                v-if="!message.requireAction"
                class="message"
                :class="{ sent: isSentMessage(message), received: !isSentMessage(message) }"
              >

                <div class="message-content">

                  <div class="message-text">{{ message.content }}</div>

                  <div class="message-time">{{ formatTime(message.createdAt) }}</div>

                </div>

              </div>

              <MessageActionCard
                v-else
                :id="message.id"
                :title="message.title"
                :content="message.content"
                :type="message.type"
                :status="message.status"
                :created-at="message.createdAt"
                :contract-id="message.relatedContractId"
                :require-action="message.requireAction"
                @action="handleMessageAction"
              />

            </div>
          </template>
          <el-empty v-else description="点击左侧联系人开始聊天" />
        </el-scrollbar>
        <div class="chat-input">
          <div class="input-container">
            <el-input
              v-if="!isOperator"
              v-model="newMessage"
              type="textarea"
              :autosize="{ minRows: 2, maxRows: 6 }"
              placeholder="输入消息..."
              @keydown.enter.exact.prevent="sendMessage"
            />
            <div class="chat-actions">
              <el-button v-if="!isOperator" type="primary" @click="sendMessage" :loading="sending" class="send-button">发送</el-button>
              <div v-else class="operator-tip">业务员账号仅支持查看系统待办消息</div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 添加联系人对话框 -->
    <el-dialog v-model="showAddContact" title="添加联系人" width="400px">
      <el-form :model="addContactForm" label-width="80px">
        <el-form-item label="用户名">
          <el-input
            v-model="addContactForm.username"
            placeholder="请输入对方用户名"
            clearable
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAddContact = false">取消</el-button>
        <el-button type="primary" @click="addContact" :loading="adding">添加</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { getMessageContacts, getChatMessages, sendMessage as sendApiMessage, updateMessageStatus, archiveContactMessages } from '../api/messages'
import { getUserInfo } from '../api/auth'
import MessageActionCard from '../components/MessageActionCard.vue'

const route = useRoute()
const router = useRouter()
const principalType = sessionStorage.getItem('principalType') || 'USER'
const isOperator = principalType === 'OPERATOR'
const currentPrincipalId = Number(sessionStorage.getItem(isOperator ? 'operatorId' : 'userId') || '0')
const contacts = ref([])
const chatMessages = ref([])
const currentContact = ref(null)
const activeContact = ref(null)
const loading = ref(false)
const sending = ref(false)
const adding = ref(false)
const newMessage = ref('')
const messageScroll = ref(null)
const showAddContact = ref(false)
const addContactForm = ref({
  username: ''
})

// 定时刷新消息状态（每 5 秒）
let messageRefreshInterval = null

// 启动消息状态定时刷新
const startMessageRefresh = () => {
  if (messageRefreshInterval) {
    clearInterval(messageRefreshInterval)
  }
  messageRefreshInterval = setInterval(() => {
    console.log('定时刷新消息列表...')
    loadContacts()
    if (currentContact.value) {
      console.log('定时刷新聊天记录...')
      loadChatMessages(currentContact.value)
    }
  }, 5000) // 每 5 秒刷新一次
}

// 停止消息状态定时刷新
const stopMessageRefresh = () => {
  if (messageRefreshInterval) {
    clearInterval(messageRefreshInterval)
    messageRefreshInterval = null
  }
}

// 获取联系人列表
const loadContacts = async () => {
  try {
    loading.value = true
    const { data } = await getMessageContacts()
    contacts.value = data || []

    // 更新未读消息总数
    const totalUnread = (data || []).reduce((sum, contact) => sum + (contact.unreadCount || 0), 0)
    sessionStorage.setItem('unreadMessageCount', totalUnread.toString())
    window.dispatchEvent(new CustomEvent('unreadcountchange', { detail: totalUnread }))
  } catch (error) {
    ElMessage.error(error.response?.data || '获取联系人失败')
  } finally {
    loading.value = false
  }
}

// 标记与特定联系人的消息为已读
const markContactMessagesAsRead = async (contactId) => {
  try {
    const contactIndex = contacts.value.findIndex(contact =>
      (contact.senderId === contactId && contact.receiverId === currentPrincipalId) ||
      (contact.receiverId === contactId && contact.senderId === currentPrincipalId)
    )

    if (contactIndex !== -1 && contacts.value[contactIndex].unreadCount > 0) {
      const unreadCount = contacts.value[contactIndex].unreadCount
      contacts.value[contactIndex].unreadCount = 0

      const currentTotal = Number(sessionStorage.getItem('unreadMessageCount') || '0')
      const newTotal = Math.max(0, currentTotal - unreadCount)
      sessionStorage.setItem('unreadMessageCount', newTotal.toString())
      window.dispatchEvent(new CustomEvent('unreadcountchange', { detail: newTotal }))
    }
  } catch (error) {
    console.error('标记消息为已读失败', error)
  }
}

// 选择联系人
const selectContact = async (contact) => {
  const contactId = getContactId(contact)
  if (!contactId) {
    ElMessage.warning('该会话暂无法打开')
    return
  }

  currentContact.value = contact
  activeContact.value = contact
  await loadChatMessages(contact)
  await markContactMessagesAsRead(contactId)

  await nextTick()
  if (messageScroll.value) {
    messageScroll.value.setScrollTop(messageScroll.value.wrapRef.scrollHeight)
  }
}
const loadChatMessages = async (contact) => {
  try {
    const contactId = getContactId(contact)
    if (!contactId) {
      chatMessages.value = []
      return
    }

    const { data } = await getChatMessages(contactId)
    chatMessages.value = data || []
  } catch (error) {
    ElMessage.error(error.response?.data || '获取聊天记录失败')
  }
}

// 获取联系人 ID（对方的用户 ID）
const getContactId = (contact) => {
  const currentUserId = currentPrincipalId
  if (!contact) return null

  console.log('getContactId - contact:', contact)
  console.log('getContactId - currentUserId:', currentUserId)

  // 系统/管理员通知：sender 为空且接收者是当前用户，使用 -1 作为系统会话标识
  if ((!contact.senderId || contact.senderId === undefined) && contact.receiverId === currentUserId) {
    console.log('getContactId - 系统消息，返回 -1')
    return -1
  }

  // 如果消息是当前用户发送的，返回 receiverId（对方 ID）
  if (contact.senderId === currentUserId) {
    console.log('getContactId - 当前用户发送，返回 receiverId:', contact.receiverId)
    return contact.receiverId || contact.senderId
  }

  // 如果消息是对方发送的，返回 senderId（对方 ID）
  if (contact.senderId && contact.senderId !== currentUserId) {
    console.log('getContactId - 对方发送，返回 senderId:', contact.senderId)
    return contact.senderId
  }

  // 如果 receiverId 不是当前用户，返回 receiverId（对方 ID）
  if (contact.receiverId && contact.receiverId !== currentUserId) {
    console.log('getContactId - 返回 receiverId:', contact.receiverId)
    return contact.receiverId
  }

  // 尝试从 id 中提取（临时联系人格式：temp-xxx）
  if (contact.id && contact.id.startsWith('temp-')) {
    const extractedId = contact.id.replace('temp-', '')
    console.log('getContactId - 从 id 提取，返回:', extractedId)
    return Number(extractedId)
  }

  console.log('getContactId - 无法确定，返回 null')
  return null
}

// 获取联系人名称
const getContactName = (contact) => {
  const currentUserId = currentPrincipalId
  if (contact.senderId === currentUserId) {
    return contact.receiverName || '未知用户'
  }
  return contact.senderName || '系统消息'
}

// 是否是自己发送的消息
const isSentMessage = (message) => {
  const currentUserId = currentPrincipalId
  return message.senderId === currentUserId
}

// 处理消息操作（同意/拒绝）
const handleMessageAction = async ({ messageId, action }) => {
  try {
    await updateMessageStatus(messageId, action)
    if (currentContact.value) {
      await loadChatMessages(currentContact.value)
    }
  } catch (error) {
    ElMessage.error('操作失败：' + (error.response?.data || error.message))
  }
}

// 删除联系人（归档消息，数据库保留记录）
const deleteContact = async (contact) => {
  if (isOperator) {
    ElMessage.warning('业务员账号暂不支持删除待办消息')
    return
  }
  try {
    await ElMessageBox.confirm(
      `确定要删除与 ${getContactName(contact)} 的聊天记录吗？删除后消息将被归档隐藏，但数据库仍会保留以便后期核验。`,
      '删除联系人',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    const contactId = getContactId(contact)
    console.log('删除联系人，contactId:', contactId, 'contact:', contact)

    if (!contactId || contactId === -1) {
      ElMessage.warning('系统消息无法删除')
      return
    }

    // 调用后端归档 API
    await archiveContactMessages(contactId)

    // 如果是当前选中的联系人，先清空
    if (currentContact.value && currentContact.value.id === contact.id) {
      currentContact.value = null
      activeContact.value = null
      chatMessages.value = []
    }

    // 从联系人列表中移除
    const index = contacts.value.findIndex(c => c.id === contact.id)
    if (index !== -1) {
      contacts.value.splice(index, 1)
    }

    ElMessage.success('聊天记录已归档')
  } catch (error) {
    console.error('删除失败，错误详情:', error)
    if (error !== 'cancel') {
      const errorMsg = error.response?.data || error.message || '删除失败'
      ElMessage.error(errorMsg)
    }
  }
}

// 添加联系人
const addContact = async () => {
  if (!addContactForm.value.username.trim()) {
    ElMessage.warning('请输入用户名')
    return
  }

  try {
    adding.value = true
    // 根据用户名获取用户信息
    const { data: userInfo } = await getUserInfo(addContactForm.value.username)

    console.log('添加联系人 - userInfo:', userInfo)

    if (!userInfo) {
      ElMessage.error('用户不存在')
      return
    }

    // 使用 userId 作为联系人 ID
    const contactId = userInfo.userId || userInfo.id

    if (!contactId) {
      ElMessage.error('无法获取用户 ID')
      return
    }

    // 检查是否已存在该联系人
    const existingContact = contacts.value.find(contact => {
      const existingContactId = getContactId(contact)
      return existingContactId === contactId
    })

    if (existingContact) {
      ElMessage.warning('该用户已在联系人列表中')
      await selectContact(existingContact)
      showAddContact.value = false
      return
    }

    // 创建临时联系人对象
    const newContact = {
      id: `temp-${contactId}`,
      senderId: contactId,
      receiverId: null,
      senderName: userInfo.username || '未知用户',
      receiverName: null,
      content: '点击开始聊天',
      createdAt: new Date(),
      unreadCount: 0
    }

    console.log('添加联系人 - newContact:', newContact)

    // 添加到联系人列表
    contacts.value.unshift(newContact)

    // 选中新添加的联系人
    await selectContact(newContact)

    ElMessage.success('添加成功')
    showAddContact.value = false
    addContactForm.value.username = ''
  } catch (error) {
    console.error('添加联系人失败:', error)
    if (error.response?.status === 404) {
      ElMessage.error('用户不存在')
    } else {
      ElMessage.error(error.response?.data || '添加失败')
    }
  } finally {
    adding.value = false
  }
}

// 发送消息
const sendMessage = async () => {
  if (!newMessage.value.trim()) {
    ElMessage.warning('请输入消息内容')
    return
  }

  if (!currentContact.value) {
    ElMessage.warning('请选择联系人')
    return
  }

  try {
    sending.value = true
    const contactId = getContactId(currentContact.value)
    if (!contactId) {
      ElMessage.warning('该会话暂无法发送消息')
      return
    }
    await sendApiMessage({
      receiverId: contactId,
      title: '聊天消息',
      content: newMessage.value
    })

    newMessage.value = ''

    await loadChatMessages(currentContact.value)
    await loadContacts()

    await nextTick()
    if (messageScroll.value) {
      messageScroll.value.setScrollTop(messageScroll.value.wrapRef.scrollHeight)
    }
  } catch (error) {
    ElMessage.error(error.response?.data || '发送消息失败')
  } finally {
    sending.value = false
  }
}

// 格式化时间
const formatTime = (dateString) => {
  if (!dateString) return ''
  const date = new Date(dateString)
  const now = new Date()

  if (date.toDateString() === now.toDateString()) {
    return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
  }

  if (date.getFullYear() === now.getFullYear()) {
    return date.toLocaleDateString('zh-CN', { month: 'short', day: 'numeric' })
  }

  return date.toLocaleDateString('zh-CN', { year: 'numeric', month: 'short', day: 'numeric' })
}

// 处理未读消息数变化事件
const handleUnreadCountChange = () => {
  loadContacts()
}

// 处理路由参数中的 receiverId（用于"联系房主"功能）
const handleReceiverIdParam = async () => {
  if (isOperator) return
  const receiverId = route.query.receiverId
  if (receiverId) {
    // 等待联系人加载完成
    await nextTick()

    // 查找对应的联系人
    const targetContact = contacts.value.find(contact => {
      const contactId = getContactId(contact)
      return contactId === Number(receiverId)
    })

    if (targetContact) {
      // 联系人已存在，直接选中
      await selectContact(targetContact)
    } else {
      // 联系人不存在，创建临时联系人对象（第一次联系房主）
      const tempContact = {
        id: `temp-${receiverId}`,
        senderId: Number(receiverId),
        receiverId: null,
        senderName: '房主',
        receiverName: null,
        content: '点击开始聊天',
        createdAt: new Date(),
        unreadCount: 0
      }

      // 选中临时联系人
      currentContact.value = tempContact
      activeContact.value = tempContact
      chatMessages.value = []

      ElMessage.success('已打开与房主的聊天')
    }

    // 清除 URL 参数，避免刷新后重复处理
    const { receiverId: _, ...restQuery } = route.query
    await router.replace({ path: route.path, query: restQuery })
  }
}

onMounted(async () => {
  await loadContacts()
  window.addEventListener('unreadcountchange', handleUnreadCountChange)
  startMessageRefresh() // 启动定时刷新
  // 处理路由参数
  await handleReceiverIdParam()
})

onUnmounted(() => {
  window.removeEventListener('unreadcountchange', handleUnreadCountChange)
  stopMessageRefresh() // 清理定时器
})
</script>

<style scoped>
.message-center {
  width: 100%;
  max-width: 1600px;
  margin: 20px auto;
  padding: 0 20px;
  box-sizing: border-box;
}

.page-title {
  font-size: 22px;
  font-weight: 600;
  color: var(--text);
}

.chat-container {
  display: flex;
  height: calc(100vh - 130px);
  min-height: 600px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  overflow: hidden;
  margin-top: 12px;
  border: 1px solid #e4e7ed;
  min-width: 1000px;
}

.contact-list {
  width: 30%;
  min-width: 320px;
  max-width: 400px;
  border-right: 1px solid #e4e7ed;
  display: flex;
  flex-direction: column;
  background-color: #fff;
  flex-shrink: 0;
}

.contact-list.hidden {
  display: none;
}

.list-header {
  padding: 20px 24px;
  border-bottom: 1px solid #e4e7ed;
  display: flex;
  justify-content: space-between;
  align-items: center;
  background-color: #fff;
}

.list-header h3 {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
}

.header-actions {
  display: flex;
  gap: 8px;
  align-items: center;
}

.contact-scroll {
  flex: 1;
  height: calc(100% - 69px);
}

.contact-item-wrapper {
  margin-bottom: 0;
}

.contact-item-container {
  display: flex;
  align-items: center;
  position: relative;
}

.contact-item {
  display: flex;
  padding: 16px 24px;
  cursor: pointer;
  transition: background-color 0.2s;
  border-bottom: 1px solid #f5f5f5;
  background-color: #fff;
  flex: 1;
  position: relative;
}

.contact-item::before {
  content: '';
  position: absolute;
  right: 0;
  top: 0;
  bottom: 0;
  width: 3px;
  background-color: #409EFF;
  opacity: 0;
  transition: opacity 0.2s;
}

.contact-item:hover {
  background-color: #f5f7fa;
}

.contact-item.active {
  background-color: #ecf5ff;
}

.contact-item.active::before {
  opacity: 1;
}

.delete-btn {
  margin-left: 12px;
  flex-shrink: 0;
  background-color: #f56c6c;
  color: #fff;
  border: none;
  padding: 6px 14px;
  border-radius: 6px;
  font-size: 13px;
  font-weight: 500;
  transition: all 0.2s;
  box-shadow: 0 2px 6px rgba(245, 108, 108, 0.3);
  opacity: 0;
  visibility: hidden;
  position: absolute;
  right: 16px;
  top: 55%;
  transform: translateY(-50%);
  align-self: center;
}

.contact-item-container:hover .delete-btn {
  opacity: 1;
  visibility: visible;
}

.delete-btn:hover {
  background-color: #f78989;
  box-shadow: 0 3px 8px rgba(245, 108, 108, 0.4);
}

.delete-btn:active {
  background-color: #e74c3c;
  transform: translateY(-50%) scale(0.98);
}

.contact-avatar {
  margin-right: 12px;
  flex-shrink: 0;
}

.contact-info {
  flex: 1;
  min-width: 0;
}

.contact-name {
  font-weight: 600;
  color: var(--text);
  margin-bottom: 6px;
  font-size: 16px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.contact-last-message {
  font-size: 14px;
  color: #909399;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 220px;
}

.contact-meta {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  justify-content: space-between;
  min-width: 60px;
}

.contact-time {
  font-size: 12px;
  color: #909399;
}

.contact-unread {
  margin-top: 4px;
}

.chat-window {
  flex: 1;
  display: flex;
  flex-direction: column;
  background-color: #f5f7fa;
  min-width: 0;
  overflow: hidden;
}

.chat-header {
  display: flex;
  align-items: center;
  padding: 16px 24px;
  border-bottom: 1px solid #e4e7ed;
  background-color: #fff;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.05);
}

.chat-header-info {
  margin-left: 12px;
}

.chat-contact-name {
  font-weight: 600;
  font-size: 18px;
  color: var(--text);
}

.chat-messages {
  flex: 1;
  padding: 24px;
  background-color: #f5f7fa;
  overflow-y: auto;
  min-height: 400px;
}

.message {
  display: flex;
  margin-bottom: 20px;
  max-width: 100%;
}

.message.sent {
  justify-content: flex-end;
}

.message.received {
  justify-content: flex-start;
}

.message-content {
  max-width: 60%;
  padding: 14px 18px;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  word-break: break-word;
  font-size: 15px;
  line-height: 1.6;
}

.message.sent .message-content {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border-bottom-right-radius: 4px;
}

.message.received .message-content {
  background-color: #fff;
  color: var(--text);
  border-bottom-left-radius: 4px;
  border: 1px solid #e4e7ed;
}

.message-text {
  word-wrap: break-word;
  line-height: 1.6;
  white-space: pre-wrap;
}

.message-time {
  font-size: 12px;
  margin-top: 8px;
  text-align: right;
  opacity: 0.8;
}

.message.sent .message-time {
  color: rgba(255, 255, 255, 0.9);
}

.message.received .message-time {
  color: #909399;
}

.chat-input {
  padding: 12px 24px 16px;
  border-top: 1px solid #e4e7ed;
  background-color: #fff;
  box-shadow: 0 -2px 8px rgba(0, 0, 0, 0.05);
}

.input-container {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.chat-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: -4px;
}

.send-button {
  width: 80px;
  height: 36px;
  font-size: 14px;
  font-weight: 600;
  border-radius: 8px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
  transition: all 0.3s ease;
}

.operator-tip {
  color: #909399;
  font-size: 13px;
}

.send-button:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);
}

.send-button:active {
  transform: translateY(0);
}

/* 滚动条美化 */
.chat-messages::-webkit-scrollbar {
  width: 6px;
}

.chat-messages::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 3px;
}

.chat-messages::-webkit-scrollbar-thumb {
  background: #c1c1c1;
  border-radius: 3px;
}

.chat-messages::-webkit-scrollbar-thumb:hover {
  background: #a8a8a8;
}

.contact-scroll::-webkit-scrollbar {
  width: 6px;
}

.contact-scroll::-webkit-scrollbar-track {
  background: #f1f1f1;
}

.contact-scroll::-webkit-scrollbar-thumb {
  background: #c1c1c1;
  border-radius: 3px;
}

/* 输入框美化 */
:deep(.el-textarea__inner) {
  border-radius: 8px;
  border: 1px solid #dcdfe6;
  padding: 12px 16px;
  font-size: 15px;
  resize: none;
  transition: all 0.3s;
}

:deep(.el-textarea__inner:hover) {
  border-color: #409EFF;
}

:deep(.el-textarea__inner:focus) {
  border-color: #667eea;
  box-shadow: 0 0 0 2px rgba(102, 126, 234, 0.2);
}

@media (max-width: 1024px) {
  .chat-container {
    min-width: 800px;
  }

  .contact-list {
    width: 35%;
  }
}

@media (max-width: 768px) {
  .message-center {
    padding: 0 10px;
  }

  .chat-container {
    min-width: 100%;
    height: calc(100vh - 100px);
  }

  .contact-list {
    width: 100%;
    max-width: none;
  }

  .chat-window {
    display: none;
  }
}
</style>
