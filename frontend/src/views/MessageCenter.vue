<template>
  <div class="message-center">
    <el-page-header @back="$router.push('/home')">
      <template #content>
        <span class="page-title">消息中心</span>
      </template>
    </el-page-header>

    <div class="chat-container">
      <div class="contact-list">
        <div class="list-header">
          <h3>{{ isOperator ? '待办消息' : '联系人' }}</h3>
          <div class="header-actions">
            <el-button type="primary" link @click="showAddContact = true">
              <el-icon><Plus /></el-icon>
            </el-button>
            <el-button type="primary" link @click="loadContacts">刷新</el-button>
          </div>
        </div>

        <el-scrollbar class="contact-scroll">
          <div v-for="contact in contacts" :key="getContactUnique(contact)" class="contact-item-wrapper">
            <div class="contact-item-container">
              <div
                class="contact-item"
                :class="{ active: activeContactKey === getContactUnique(contact) }"
                @click="selectContact(contact)"
              >
                <div class="contact-avatar">
                  <el-avatar :size="40">{{ getContactName(contact).charAt(0) || '?' }}</el-avatar>
                </div>
                <div class="contact-info">
                  <div class="contact-name">{{ getContactName(contact) }}</div>
                  <div class="contact-last-message">{{ contact.content || '-' }}</div>
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

      <div class="chat-window">
        <div class="chat-header">
          <div class="chat-header-info">
            <div class="chat-contact-name">
              {{ currentContact ? getContactName(currentContact) : '选择一个联系人开始聊天' }}
            </div>
          </div>
        </div>

        <el-scrollbar class="chat-messages" ref="messageScroll">
          <template v-if="currentContact">
            <div v-for="message in chatMessages" :key="message.id" class="message-item">
              <div
                v-if="!message.requireAction || getActionConfig(message).actionMode === 'none'"
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
                :house-id="message.relatedHouseId"
                :require-action="message.requireAction"
                :action-mode="getActionConfig(message).actionMode"
                :primary-text="getActionConfig(message).primaryText"
                :secondary-text="getActionConfig(message).secondaryText"
                :primary-button-type="getActionConfig(message).primaryButtonType"
                :secondary-button-type="getActionConfig(message).secondaryButtonType"
                :accepted-text="getActionConfig(message).acceptedText"
                :rejected-text="getActionConfig(message).rejectedText"
                @action="payload => handleMessageAction(payload, message)"
              />
            </div>
          </template>

          <el-empty v-else description="点击左侧联系人开始聊天" />
        </el-scrollbar>

        <div class="chat-input">
          <div class="input-container">
            <el-input
              v-model="newMessage"
              type="textarea"
              :autosize="{ minRows: 2, maxRows: 6 }"
              placeholder="输入消息..."
              @keydown.enter.exact.prevent="sendMessage"
            />
            <div class="chat-actions">
              <el-button type="primary" @click="sendMessage" :loading="sending" class="send-button">发送</el-button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <el-dialog v-model="showAddContact" title="添加联系人" width="400px">
      <el-form :model="addContactForm" label-width="80px">
        <el-form-item label="用户名">
          <el-input v-model="addContactForm.username" placeholder="请输入对方用户名" clearable />
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
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import {
  getMessageContacts,
  getChatMessages,
  sendMessage as sendApiMessage,
  updateMessageStatus,
  archiveContactMessages
} from '../api/messages'
import { lookupContact } from '../api/auth'
import {
  respondTermination,
  respondTerminationByCounterparty,
  approveContractByAdmin,
  rejectContractByAdmin,
  approveContractByLandlord,
  approveContractByLandlordWithStaff,
  rejectContractByLandlord,
  uploadSignedContract,
  getAvailableStaffOptions
} from '../api/contract'
import { approveHouseByStaff, rejectHouseByStaff } from '../api/house'
import MessageActionCard from '../components/MessageActionCard.vue'

const route = useRoute()
const router = useRouter()

const principalType = sessionStorage.getItem('principalType') || 'USER'
const userType = sessionStorage.getItem('userType') || 'USER'
const isOperator = principalType === 'OPERATOR' || userType === 'ADMIN' || userType === 'STAFF'
const isAdmin = isOperator && userType === 'ADMIN'
const isStaff = isOperator && userType === 'STAFF'
const currentPrincipalId = Number(sessionStorage.getItem(isOperator ? 'operatorId' : 'userId') || '0')

const contacts = ref([])
const chatMessages = ref([])
const currentContact = ref(null)
const loading = ref(false)
const sending = ref(false)
const adding = ref(false)
const newMessage = ref('')
const messageScroll = ref(null)
const showAddContact = ref(false)
const addContactForm = ref({ username: '' })

const SYSTEM_ID = -1
const CANCELLED = '__CANCEL__'

let messageRefreshInterval = null

const activeContactKey = computed(() => (currentContact.value ? getContactUnique(currentContact.value) : ''))

const parseContactKey = (contactKey) => {
  if (!contactKey || !contactKey.includes(':')) return null
  const [type, idPart] = contactKey.split(':', 2)
  const id = Number(idPart)
  if (!Number.isFinite(id)) return null
  return { type, id }
}

const getContactId = (contact) => {
  if (!contact) return null
  if (typeof contact.contactId === 'number') return contact.contactId

  const keyRef = parseContactKey(contact.contactKey)
  if (keyRef) return keyRef.id

  if (typeof contact.id === 'string' && contact.id.startsWith('temp-')) {
    const extracted = Number(contact.id.replace('temp-', '').split('-').pop())
    return Number.isFinite(extracted) ? extracted : null
  }

  if (contact.contactType === 'SYSTEM') return SYSTEM_ID

  if (contact.senderId === currentPrincipalId) return contact.receiverId ?? contact.senderId
  if (contact.senderId && contact.senderId !== currentPrincipalId) return contact.senderId
  if (contact.receiverId && contact.receiverId !== currentPrincipalId) return contact.receiverId
  return null
}

const getContactType = (contact) => {
  if (!contact) return null
  if (contact.contactType) return String(contact.contactType).toUpperCase()

  const keyRef = parseContactKey(contact.contactKey)
  if (keyRef) return keyRef.type

  const contactId = getContactId(contact)
  if (contactId === SYSTEM_ID) return 'SYSTEM'

  if (
    (typeof contact.senderOperatorId === 'number' && contact.senderOperatorId === contactId) ||
    (typeof contact.receiverOperatorId === 'number' && contact.receiverOperatorId === contactId)
  ) {
    return 'OPERATOR'
  }

  return 'USER'
}

const getContactUnique = (contact) => {
  if (!contact) return 'unknown'
  if (contact.contactKey) return contact.contactKey
  const type = getContactType(contact) || 'USER'
  const id = getContactId(contact)
  return `${type}:${id ?? 'unknown'}`
}

const getContactName = (contact) => {
  if (!contact) return '未知联系人'
  if (getContactType(contact) === 'SYSTEM') return '系统消息'

  if (isSentMessage(contact)) {
    return contact.receiverName || contact.receiverOperatorName || contact.senderName || '未知联系人'
  }
  return contact.senderName || contact.senderOperatorName || contact.receiverName || '未知联系人'
}

const isSentMessage = (message) => {
  return message.senderId === currentPrincipalId || message.senderOperatorId === currentPrincipalId
}

const updateUnreadCountCache = (list) => {
  const totalUnread = (list || []).reduce((sum, item) => sum + Number(item.unreadCount || 0), 0)
  sessionStorage.setItem('unreadMessageCount', String(totalUnread))
  window.dispatchEvent(new CustomEvent('unreadcountchange', { detail: totalUnread }))
}

const startMessageRefresh = () => {
  if (messageRefreshInterval) clearInterval(messageRefreshInterval)
  messageRefreshInterval = setInterval(async () => {
    await loadContacts()
    if (currentContact.value) {
      await loadChatMessages(currentContact.value)
    }
  }, 5000)
}

const stopMessageRefresh = () => {
  if (!messageRefreshInterval) return
  clearInterval(messageRefreshInterval)
  messageRefreshInterval = null
}

const loadContacts = async () => {
  try {
    loading.value = true
    const selectedKey = currentContact.value ? getContactUnique(currentContact.value) : null

    const { data } = await getMessageContacts()
    const list = data || []
    contacts.value = list
    updateUnreadCountCache(list)

    if (selectedKey) {
      const matched = list.find((item) => getContactUnique(item) === selectedKey)
      if (matched) currentContact.value = matched
    }
  } catch (error) {
    ElMessage.error(error.response?.data || '获取联系人失败')
  } finally {
    loading.value = false
  }
}

const selectContact = async (contact) => {
  const contactId = getContactId(contact)
  if (!contactId && contactId !== SYSTEM_ID) {
    ElMessage.warning('该会话暂时无法打开')
    return
  }

  currentContact.value = contact
  await loadChatMessages(contact)
  await loadContacts()

  await nextTick()
  if (messageScroll.value?.wrapRef) {
    messageScroll.value.setScrollTop(messageScroll.value.wrapRef.scrollHeight)
  }
}

const loadChatMessages = async (contact) => {
  try {
    const contactId = getContactId(contact)
    if (!contactId && contactId !== SYSTEM_ID) {
      chatMessages.value = []
      return
    }

    const contactType = getContactType(contact)
    const { data } = await getChatMessages(contactId, contactType)
    chatMessages.value = data || []
  } catch (error) {
    ElMessage.error(error.response?.data || '获取聊天记录失败')
  }
}

const normalizeActionType = (message) => {
  if (!message?.requireAction) return null

  if (message.type === 'USER_CHAT' && message.relatedContractId && !isOperator) {
    return 'LANDLORD_CONTRACT_DECISION'
  }

  if (message.type === 'ADMIN_NOTIFICATION' && message.requireAction) {
    if (message.relatedRequestId) return 'FORCE_TERMINATION_NOTICE'
    if (message.relatedContractId && isAdmin) return 'CONTRACT_PENDING_ADMIN_APPROVAL'
  }

  return message.type
}

const defaultActionConfig = {
  actionMode: 'none',
  primaryText: '同意',
  secondaryText: '拒绝',
  primaryButtonType: 'primary',
  secondaryButtonType: 'danger',
  acceptedText: '已同意',
  rejectedText: '已拒绝'
}

const getActionConfig = (message) => {
  const actionType = normalizeActionType(message)
  if (!actionType) return defaultActionConfig

  const map = {
    TERMINATION_REQUEST: {
      actionMode: 'dual',
      primaryText: '同意终止',
      secondaryText: '拒绝终止',
      primaryButtonType: 'warning',
      secondaryButtonType: 'danger',
      acceptedText: '已同意终止',
      rejectedText: '已拒绝终止'
    },
    TERMINATION_PENDING_STAFF_REVIEW: {
      actionMode: isStaff ? 'dual' : 'none',
      primaryText: '审核通过',
      secondaryText: '驳回申请',
      primaryButtonType: 'success',
      secondaryButtonType: 'danger',
      acceptedText: '已通过',
      rejectedText: '已驳回'
    },
    FORCE_TERMINATION_NOTICE: {
      actionMode: isOperator ? 'dual' : 'none',
      primaryText: '提交通过',
      secondaryText: '驳回申请',
      primaryButtonType: 'warning',
      secondaryButtonType: 'danger',
      acceptedText: '已提交通过',
      rejectedText: '已驳回'
    },
    CONTRACT_PENDING_ADMIN_APPROVAL: {
      actionMode: isAdmin ? 'dual' : 'none',
      primaryText: '合同通过',
      secondaryText: '合同驳回',
      primaryButtonType: 'success',
      secondaryButtonType: 'danger',
      acceptedText: '已通过',
      rejectedText: '已驳回'
    },
    CONTRACT_PENDING_STAFF_SIGNING: {
      actionMode: isStaff ? 'single' : 'none',
      primaryText: '上传签约文件',
      secondaryText: '拒绝',
      primaryButtonType: 'primary',
      secondaryButtonType: 'danger',
      acceptedText: '已上传',
      rejectedText: '已拒绝'
    },
    HOUSE_PENDING_STAFF_REVIEW: {
      actionMode: isStaff ? 'dual' : 'none',
      primaryText: '房源通过',
      secondaryText: '驳回房源',
      primaryButtonType: 'success',
      secondaryButtonType: 'danger',
      acceptedText: '已通过',
      rejectedText: '已驳回'
    },
    LANDLORD_CONTRACT_DECISION: {
      actionMode: !isOperator ? 'dual' : 'none',
      primaryText: '同意签约',
      secondaryText: '拒绝签约',
      primaryButtonType: 'primary',
      secondaryButtonType: 'danger',
      acceptedText: '已同意签约',
      rejectedText: '已拒绝签约'
    }
  }

  return map[actionType] || defaultActionConfig
}

const promptText = async ({ title, message, required = false, placeholder = '' }) => {
  try {
    const { value } = await ElMessageBox.prompt(message, title, {
      inputPlaceholder: placeholder,
      confirmButtonText: '确认',
      cancelButtonText: '取消',
      inputPattern: required ? /\S+/ : undefined,
      inputErrorMessage: required ? '内容不能为空' : undefined
    })
    return (value || '').trim()
  } catch (error) {
    if (error === 'cancel' || error === 'close') {
      const cancelled = new Error(CANCELLED)
      cancelled.code = CANCELLED
      throw cancelled
    }
    throw error
  }
}

const pickSignedFile = () => {
  return new Promise((resolve) => {
    const input = document.createElement('input')
    input.type = 'file'
    input.accept = '.pdf,.jpg,.jpeg,.png'
    input.onchange = () => {
      const file = input.files && input.files[0] ? input.files[0] : null
      resolve(file)
    }
    input.click()
  })
}

const selectOptionalStaffId = async () => {
  const { data } = await getAvailableStaffOptions()
  const options = data || []
  if (!options.length) return null

  const listText = options
    .map((item) => `${item.id}: ${item.displayName || item.name || '业务员'} ${item.phone ? `(${item.phone})` : ''}`)
    .join('\n')

  const value = await promptText({
    title: '可选指定业务员',
    message: `可输入业务员ID（留空则自动分配）：\n${listText}`,
    required: false,
    placeholder: '例如：3'
  })

  if (!value) return null

  const staffId = Number(value)
  if (!Number.isInteger(staffId)) throw new Error('业务员ID格式不正确')

  const exists = options.some((item) => Number(item.id) === staffId)
  if (!exists) throw new Error('指定业务员不在可选列表中')

  return staffId
}

const dispatchBusinessAction = async (message, actionKey) => {
  const actionType = normalizeActionType(message)
  const approve = actionKey === 'primary'

  if (!actionType) throw new Error('该消息不支持业务动作')

  if (actionType === 'TERMINATION_REQUEST') {
    if (!message.relatedRequestId) throw new Error('缺少终止申请ID')
    const comment = await promptText({
      title: approve ? '同意终止' : '拒绝终止',
      message: '可填写处理意见（选填）：',
      required: false
    })
    await respondTerminationByCounterparty(message.relatedRequestId, { approve, comment })
    return
  }

  if (actionType === 'TERMINATION_PENDING_STAFF_REVIEW') {
    if (!message.relatedRequestId) throw new Error('缺少终止申请ID')
    const comment = await promptText({
      title: approve ? '终止审核通过' : '终止审核驳回',
      message: '请填写审核意见：',
      required: true
    })
    await respondTermination(message.relatedRequestId, { approve, comment })
    return
  }

  if (actionType === 'FORCE_TERMINATION_NOTICE') {
    if (!message.relatedRequestId) throw new Error('缺少终止申请ID')
    const comment = await promptText({
      title: approve ? '强制终止通过' : '强制终止驳回',
      message: approve ? '通过时必须填写说明：' : '可填写驳回说明（选填）：',
      required: approve
    })
    await respondTermination(message.relatedRequestId, { approve, comment })
    return
  }

  if (actionType === 'CONTRACT_PENDING_ADMIN_APPROVAL') {
    if (!message.relatedContractId) throw new Error('缺少合同ID')
    if (approve) {
      await approveContractByAdmin(message.relatedContractId)
    } else {
      const reason = await promptText({
        title: '驳回合同',
        message: '请填写驳回原因：',
        required: true
      })
      await rejectContractByAdmin(message.relatedContractId, { reason })
    }
    return
  }

  if (actionType === 'CONTRACT_PENDING_STAFF_SIGNING') {
    if (!message.relatedContractId) throw new Error('缺少合同ID')
    const file = await pickSignedFile()
    if (!file) {
      const cancelled = new Error(CANCELLED)
      cancelled.code = CANCELLED
      throw cancelled
    }
    await uploadSignedContract(message.relatedContractId, file)
    return
  }

  if (actionType === 'HOUSE_PENDING_STAFF_REVIEW') {
    if (!message.relatedHouseId) throw new Error('缺少房源ID')
    if (approve) {
      await approveHouseByStaff(message.relatedHouseId)
    } else {
      const reason = await promptText({
        title: '驳回房源',
        message: '请填写驳回原因：',
        required: true
      })
      await rejectHouseByStaff(message.relatedHouseId, reason)
    }
    return
  }

  if (actionType === 'LANDLORD_CONTRACT_DECISION') {
    if (!message.relatedContractId) throw new Error('缺少合同ID')
    if (approve) {
      const staffId = await selectOptionalStaffId()
      if (staffId) {
        await approveContractByLandlordWithStaff(message.relatedContractId, staffId)
      } else {
        await approveContractByLandlord(message.relatedContractId)
      }
    } else {
      const reason = await promptText({
        title: '拒绝签约',
        message: '可填写拒绝原因（选填）：',
        required: false
      })
      await rejectContractByLandlord(message.relatedContractId, { reason })
    }
    return
  }

  throw new Error('未匹配到可执行动作')
}

const resolveStatusByAction = (actionKey) => (actionKey === 'primary' ? 'ACCEPT' : 'REJECT')

const isAlreadyProcessedError = (message) => {
  const text = String(message || '')
  const keywords = ['已处理', '状态异常', '当前不在', '无权', '不存在', '无法']
  return keywords.some((item) => text.includes(item))
}

const handleMessageAction = async ({ messageId, action }, message) => {
  try {
    await dispatchBusinessAction(message, action)

    const status = resolveStatusByAction(action)
    await updateMessageStatus(messageId, status)

    await loadContacts()
    if (currentContact.value) {
      await loadChatMessages(currentContact.value)
    }

    ElMessage.success('处理成功')
  } catch (error) {
    if (error?.code === CANCELLED || error?.message === CANCELLED) return

    const errorMsg = error.response?.data || error.message || '操作失败'
    if (isAlreadyProcessedError(errorMsg)) {
      ElMessage.warning('该事项已被处理，已刷新')
      await loadContacts()
      if (currentContact.value) {
        await loadChatMessages(currentContact.value)
      }
      return
    }

    ElMessage.error(`操作失败：${errorMsg}`)
  }
}

const deleteContact = async (contact) => {
  if (isOperator) {
    ElMessage.warning('业务账号暂不支持删除待办消息')
    return
  }

  try {
    await ElMessageBox.confirm(
      `确定要删除与 ${getContactName(contact)} 的聊天记录吗？删除后会被归档隐藏。`,
      '删除联系人',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    const contactId = getContactId(contact)
    const contactType = getContactType(contact)

    if (contactId === SYSTEM_ID) {
      ElMessage.warning('系统会话不可删除')
      return
    }

    await archiveContactMessages(contactId, contactType)

    if (currentContact.value && getContactUnique(currentContact.value) === getContactUnique(contact)) {
      currentContact.value = null
      chatMessages.value = []
    }

    await loadContacts()
    ElMessage.success('聊天记录已归档')
  } catch (error) {
    if (error === 'cancel' || error === 'close') return
    ElMessage.error(error.response?.data || error.message || '删除失败')
  }
}

const addContact = async () => {
  const username = addContactForm.value.username.trim()
  if (!username) {
    ElMessage.warning('请输入用户名')
    return
  }

  try {
    adding.value = true
    const { data } = await lookupContact(username)

    if (!data) {
      ElMessage.error('联系人不存在')
      return
    }

    const targetType = String(data.principalType || '').toUpperCase() === 'OPERATOR' ? 'OPERATOR' : 'USER'
    const targetId = targetType === 'OPERATOR' ? data.operatorId : data.userId

    if (!targetId) {
      ElMessage.error('无法识别联系人ID')
      return
    }

    const key = `${targetType}:${targetId}`
    const existing = contacts.value.find((item) => getContactUnique(item) === key)
    if (existing) {
      showAddContact.value = false
      await selectContact(existing)
      return
    }

    const displayName = data.displayName || data.username || '未知联系人'
    const tempContact = {
      id: `temp-${targetType}-${targetId}`,
      contactType: targetType,
      contactId: targetId,
      senderId: targetType === 'USER' ? targetId : null,
      senderOperatorId: targetType === 'OPERATOR' ? targetId : null,
      senderName: displayName,
      senderOperatorName: displayName,
      content: '点击开始聊天',
      createdAt: new Date().toISOString(),
      unreadCount: 0
    }

    contacts.value.unshift(tempContact)
    showAddContact.value = false
    addContactForm.value.username = ''
    await selectContact(tempContact)
    ElMessage.success('添加成功')
  } catch (error) {
    ElMessage.error(error.response?.data || error.message || '添加失败')
  } finally {
    adding.value = false
  }
}

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
    const contactType = getContactType(currentContact.value)

    if (!contactId && contactId !== SYSTEM_ID) {
      ElMessage.warning('该会话暂时无法发送消息')
      return
    }

    if (contactId === SYSTEM_ID || contactType === 'SYSTEM') {
      ElMessage.warning('系统会话不可发送消息')
      return
    }

    const payload = {
      title: '聊天消息',
      content: newMessage.value
    }

    if (contactType === 'OPERATOR') {
      payload.receiverOperatorId = contactId
    } else {
      payload.receiverId = contactId
    }

    await sendApiMessage(payload)
    newMessage.value = ''

    await loadChatMessages(currentContact.value)
    await loadContacts()

    await nextTick()
    if (messageScroll.value?.wrapRef) {
      messageScroll.value.setScrollTop(messageScroll.value.wrapRef.scrollHeight)
    }
  } catch (error) {
    ElMessage.error(error.response?.data || '发送消息失败')
  } finally {
    sending.value = false
  }
}

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

const handleUnreadCountChange = () => {
  loadContacts()
}

const handleReceiverIdParam = async () => {
  const receiverId = route.query.receiverId
  const receiverName = route.query.receiverName
  const receiverType = String(route.query.receiverType || 'USER').toUpperCase()
  if (!receiverId) return

  await nextTick()

  const targetKey = `${receiverType}:${Number(receiverId)}`
  const targetContact = contacts.value.find((item) => getContactUnique(item) === targetKey)

  if (targetContact) {
    await selectContact(targetContact)
  } else {
    const tempContact = {
      id: `temp-${receiverType}-${receiverId}`,
      contactType: receiverType,
      contactId: Number(receiverId),
      senderId: receiverType === 'USER' ? Number(receiverId) : null,
      senderOperatorId: receiverType === 'OPERATOR' ? Number(receiverId) : null,
      senderName: receiverName || '对方',
      senderOperatorName: receiverName || '对方',
      content: '点击开始聊天',
      createdAt: new Date().toISOString(),
      unreadCount: 0
    }

    currentContact.value = tempContact
    chatMessages.value = []
  }

  const { receiverId: _rid, receiverName: _rname, receiverType: _rtype, ...restQuery } = route.query
  await router.replace({ path: route.path, query: restQuery })
}

onMounted(async () => {
  await loadContacts()
  window.addEventListener('unreadcountchange', handleUnreadCountChange)
  startMessageRefresh()
  await handleReceiverIdParam()
})

onUnmounted(() => {
  window.removeEventListener('unreadcountchange', handleUnreadCountChange)
  stopMessageRefresh()
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
  animation: pageFadeIn 0.28s ease;
}

.contact-list {
  width: 32%;
  min-width: 320px;
  max-width: 420px;
  border-right: 1px solid #e4e7ed;
  display: flex;
  flex-direction: column;
  background-color: #fff;
}

.list-header {
  padding: 20px 24px;
  border-bottom: 1px solid #e4e7ed;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.list-header h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
}

.header-actions {
  display: flex;
  gap: 8px;
  align-items: center;
}

.contact-scroll {
  flex: 1;
}

.contact-item-container {
  display: flex;
  align-items: center;
  position: relative;
}

.contact-item {
  display: flex;
  padding: 14px 20px;
  cursor: pointer;
  transition: all 0.22s ease;
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
  background-color: #409eff;
  opacity: 0;
  transition: opacity 0.22s ease;
}

.contact-item:hover {
  background-color: #f5f7fa;
  transform: translateX(1px);
}

.contact-item.active {
  background-color: #ecf5ff;
}

.contact-item.active::before {
  opacity: 1;
}

.contact-item-wrapper {
  animation: itemFadeIn 0.22s ease both;
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
  margin-bottom: 6px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.contact-last-message {
  font-size: 13px;
  color: #909399;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.contact-meta {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 4px;
}

.contact-time {
  font-size: 12px;
  color: #909399;
}

.delete-btn {
  margin-right: 12px;
  opacity: 0;
  visibility: hidden;
  transform: translateY(4px);
  transition: all 0.2s ease;
}

.contact-item-container:hover .delete-btn {
  opacity: 1;
  visibility: visible;
  transform: translateY(0);
}

.chat-window {
  flex: 1;
  display: flex;
  flex-direction: column;
  background-color: #f5f7fa;
  min-width: 0;
}

.chat-header {
  padding: 14px 20px;
  border-bottom: 1px solid #e4e7ed;
  background-color: #fff;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.05);
}

.chat-contact-name {
  font-weight: 600;
  font-size: 18px;
  color: var(--text);
}

.chat-messages {
  flex: 1;
  padding: 20px;
}

.message {
  display: flex;
  margin-bottom: 16px;
  animation: itemFadeIn 0.2s ease both;
}

.message.sent {
  justify-content: flex-end;
}

.message.received {
  justify-content: flex-start;
}

.message-content {
  max-width: 60%;
  padding: 10px 14px;
  border-radius: 10px;
  word-break: break-word;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.message-content:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 14px rgba(0, 0, 0, 0.1);
}

.message.sent .message-content {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
  border-bottom-right-radius: 4px;
}

.message.received .message-content {
  background: #fff;
  border: 1px solid #e4e7ed;
  border-bottom-left-radius: 4px;
}

.message-text {
  white-space: pre-wrap;
}

.message-time {
  font-size: 12px;
  margin-top: 6px;
  opacity: 0.8;
  text-align: right;
}

.chat-input {
  padding: 12px 20px 16px;
  border-top: 1px solid #e4e7ed;
  background-color: #fff;
}

.input-container {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.chat-actions {
  display: flex;
  justify-content: flex-end;
}

.send-button {
  min-width: 80px;
  height: 36px;
  border-radius: 8px;
  border: none;
  font-weight: 600;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  transition: all 0.2s ease;
}

.send-button:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.35);
}

.send-button:active {
  transform: translateY(0);
}

@keyframes pageFadeIn {
  from {
    opacity: 0;
    transform: translateY(6px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes itemFadeIn {
  from {
    opacity: 0;
    transform: translateY(4px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@media (max-width: 768px) {
  .message-center {
    padding: 0 10px;
  }

  .chat-container {
    height: calc(100vh - 100px);
    flex-direction: column;
  }

  .contact-list {
    width: 100%;
    max-width: none;
    min-width: 0;
    max-height: 45%;
  }
}
</style>
