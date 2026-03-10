<template>
  <div class="message-action-card" :class="`message-${type}`">
    <div class="message-content">
      <h4 class="message-title">{{ title }}</h4>
      <p class="message-description">{{ content }}</p>

      <div class="message-type-tag">
        <el-tag :type="tagType">{{ tagText }}</el-tag>
      </div>

      <div class="message-meta">
        <span class="message-time">{{ formattedTime }}</span>
        <span v-if="contractId" class="meta-id">合同: {{ contractId }}</span>
        <span v-if="houseId" class="meta-id">房源: {{ houseId }}</span>
      </div>
    </div>

    <div v-if="requireAction && canAct && actionMode === 'dual'" class="action-buttons">
      <el-button
        :type="primaryButtonType"
        size="small"
        @click="handleAction('primary')"
        :loading="loading === 'primary'"
      >
        {{ primaryText }}
      </el-button>
      <el-button
        :type="secondaryButtonType"
        size="small"
        @click="handleAction('secondary')"
        :loading="loading === 'secondary'"
      >
        {{ secondaryText }}
      </el-button>
    </div>

    <div v-else-if="requireAction && canAct && actionMode === 'single'" class="action-buttons">
      <el-button
        :type="primaryButtonType"
        size="small"
        @click="handleAction('primary')"
        :loading="loading === 'primary'"
      >
        {{ primaryText }}
      </el-button>
    </div>

    <div v-else-if="status === 'ACCEPT' || status === 'REJECT'" class="action-result">
      <el-tag :type="status === 'ACCEPT' ? 'success' : 'danger'">
        {{ status === 'ACCEPT' ? acceptedText : rejectedText }}
      </el-tag>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'

const props = defineProps({
  id: { type: Number, required: true },
  title: { type: String, required: true },
  content: { type: String, required: true },
  type: { type: String, default: 'USER_CHAT' },
  status: { type: String, default: 'UNREAD' },
  createdAt: { type: [String, Date], required: true },
  contractId: { type: Number, default: null },
  houseId: { type: Number, default: null },
  requireAction: { type: Boolean, default: false },
  actionMode: { type: String, default: 'dual' }, // dual | single | none
  primaryText: { type: String, default: '同意' },
  secondaryText: { type: String, default: '拒绝' },
  primaryButtonType: { type: String, default: 'success' },
  secondaryButtonType: { type: String, default: 'danger' },
  acceptedText: { type: String, default: '已处理' },
  rejectedText: { type: String, default: '已拒绝' }
})

const emit = defineEmits(['action'])
const loading = ref(null)

const formattedTime = computed(() => {
  const d = new Date(props.createdAt)
  if (Number.isNaN(d.getTime())) return '-'
  return d.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
})

const canAct = computed(() => ['PENDING', 'UNREAD', 'READ'].includes(props.status))

const tagText = computed(() => {
  const map = {
    TERMINATION_REQUEST: '终止申请',
    TERMINATION_RESPONSE: '终止回复',
    FORCE_TERMINATION_NOTICE: '强制终止',
    ADMIN_NOTIFICATION: '管理员通知',
    CONTRACT_APPROVAL_NOTICE: '合同审批通过',
    CONTRACT_REJECTION_NOTICE: '合同审批拒绝',
    CONTRACT_PENDING_ADMIN_APPROVAL: '待管理员审批',
    CONTRACT_PENDING_STAFF_SIGNING: '待业务员签约',
    TERMINATION_PENDING_STAFF_REVIEW: '待终止审核',
    HOUSE_PENDING_STAFF_REVIEW: '房源待审核',
    HOUSE_REVIEW_RESULT: '房源审核结果',
    USER_CHAT: '消息'
  }
  return map[props.type] || props.type
})

const tagType = computed(() => {
  const map = {
    TERMINATION_REQUEST: 'warning',
    TERMINATION_RESPONSE: 'info',
    FORCE_TERMINATION_NOTICE: 'danger',
    ADMIN_NOTIFICATION: 'info',
    CONTRACT_APPROVAL_NOTICE: 'success',
    CONTRACT_REJECTION_NOTICE: 'danger',
    CONTRACT_PENDING_ADMIN_APPROVAL: 'warning',
    CONTRACT_PENDING_STAFF_SIGNING: 'warning',
    TERMINATION_PENDING_STAFF_REVIEW: 'warning',
    HOUSE_PENDING_STAFF_REVIEW: 'warning',
    HOUSE_REVIEW_RESULT: 'info',
    USER_CHAT: ''
  }
  return map[props.type] || ''
})

const handleAction = (action) => {
  if (loading.value) return
  loading.value = action
  emit('action', { messageId: props.id, action })
  loading.value = null
}
</script>

<style scoped>
.message-action-card {
  border: 1px solid var(--border);
  border-radius: 8px;
  padding: 16px;
  background-color: #fff;
  margin-bottom: 16px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
  transition: box-shadow 0.3s ease;
}

.message-action-card:hover {
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1);
}

.message-TERMINATION_REQUEST {
  border-left: 4px solid #409eff;
}

.message-TERMINATION_RESPONSE {
  border-left: 4px solid #67c23a;
}

.message-FORCE_TERMINATION_NOTICE {
  border-left: 4px solid #e6a23c;
}

.message-ADMIN_NOTIFICATION {
  border-left: 4px solid #909399;
}

.message-CONTRACT_APPROVAL_NOTICE {
  border-left: 4px solid #67c23a;
}

.message-CONTRACT_REJECTION_NOTICE {
  border-left: 4px solid #f56c6c;
}

.message-content {
  margin-bottom: 12px;
}

.message-type-tag {
  margin-top: 8px;
}

.message-title {
  margin: 0 0 8px 0;
  font-size: 16px;
  font-weight: 600;
  color: var(--text);
}

.message-description {
  margin: 0 0 12px 0;
  font-size: 14px;
  color: var(--text-light);
  line-height: 1.5;
}

.message-meta {
  display: flex;
  gap: 16px;
  font-size: 12px;
  color: var(--text-light);
  flex-wrap: wrap;
}

.meta-id {
  color: #606266;
}

.action-buttons {
  display: flex;
  gap: 12px;
  justify-content: flex-end;
}
</style>

