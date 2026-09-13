<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Message } from '@arco-design/web-vue'
import { useAuthStore } from '../../../stores/auth'
import type { MfaMethod } from '../../../stores/auth'
import { ApiError } from '../../../api/http'
import OtpInput from '../components/OtpInput.vue'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

/** 验证方式选项（仅认证器 TOTP 已接入，其余提示即将上线） */
const methods: { key: MfaMethod | 'webauthn'; label: string; icon: string; hint: string }[] = [
  { key: 'totp', label: '认证器', icon: 'icon-qrcode', hint: '认证器 App（Google Authenticator / 1Password）' },
  { key: 'webauthn', label: '安全密钥', icon: 'icon-safe', hint: '硬件密钥 / Windows Hello / 指纹' },
  { key: 'sms', label: '短信', icon: 'icon-mobile', hint: '手机短信验证码' },
  { key: 'email', label: '邮件', icon: 'icon-email', hint: '邮箱验证码' },
  { key: 'recovery', label: '恢复码', icon: 'icon-code', hint: '一次性恢复码（丢失设备时使用）' },
]

const method = ref<'totp' | 'webauthn' | 'sms' | 'email' | 'recovery'>('totp')
const codes = ref<string[]>(['', '', '', '', '', ''])
const loading = ref(false)
const otpRef = ref<InstanceType<typeof OtpInput> | null>(null)

const redirect = computed(() => (route.query.redirect as string | undefined) ?? '/dashboard')

const canVerify = computed(() => codes.value.every((code) => code !== ''))

/** 选择验证方式：仅认证器（TOTP）已接入，其余提示即将上线 */
function selectMethod(key: MfaMethod | 'webauthn') {
  if (key !== 'totp') {
    Message.info('该验证方式即将上线')
    return
  }
  method.value = key
}

async function handleVerify() {
  if (!canVerify.value) {
    Message.warning('请完成当前方式的验证信息')
    return
  }
  loading.value = true
  try {
    await auth.verifyMfa(codes.value.join(''))
    Message.success('登录成功')
    router.replace(redirect.value)
  } catch (error) {
    // 登录会话（sessionToken）过期：清待验证态回登录页重来
    if (error instanceof ApiError && error.code === 1003) {
      router.replace('/login')
      Message.error('登录会话已过期，请重新登录')
      return
    }
    otpRef.value?.clear()
    Message.error(error instanceof Error ? error.message : '验证失败，请重试')
  } finally {
    loading.value = false
  }
}

/** 放弃本次登录：清空待验证态回登录页 */
function handleBack() {
  auth.clearSession()
  router.replace('/login')
}
</script>

<template>
  <div class="mfa-page">
    <div class="mfa-card">
      <div class="mfa-icon"><icon-safe /></div>
      <h2 class="mfa-title">两步验证</h2>
      <p class="mfa-sub">验证账号 <strong>{{ auth.pendingName }}</strong>，请选择验证方式</p>

      <!-- 验证方式选择（仅认证器已接入） -->
      <div class="method-row">
        <div
          v-for="item in methods"
          :key="item.key"
          class="method-card"
          :class="{ active: method === item.key }"
          :title="item.hint"
          @click="selectMethod(item.key)"
        >
          <component :is="item.icon" class="method-icon" />
          <span class="method-label">{{ item.label }}</span>
        </div>
      </div>

      <!-- 方式面板 -->
      <div class="panel">
        <!-- TOTP：6 格验证码 -->
        <template v-if="method === 'totp'">
          <p class="panel-hint">输入认证器 App 中显示的 6 位动态验证码</p>
          <OtpInput ref="otpRef" v-model="codes" />
        </template>
        <!-- 其余方式：提示（后端当前仅接入 TOTP 动态码） -->
        <template v-else>
          <p class="panel-hint">该验证方式暂未接入，请先使用认证器 App 完成验证</p>
          <div class="webauthn-hint">
            <icon-safe class="webauthn-icon" />
            <span>
              {{ method === 'recovery' ? '恢复码可在登录页「使用恢复码登录」入口直接登录' : '短信 / 邮件 / 安全密钥验证将在后续里程碑接入' }}
            </span>
          </div>
        </template>
      </div>

      <a-button
        type="primary"
        size="large"
        long
        :loading="loading"
        :disabled="!canVerify"
        class="mfa-submit"
        @click="handleVerify"
      >
        验 证
      </a-button>

      <div class="mfa-links">
        <span />
        <a-link @click="handleBack">返回登录</a-link>
      </div>
    </div>
  </div>
</template>

<style scoped>
.mfa-page {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100vh;
  background: var(--color-page-bg);
}

.mfa-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  width: var(--size-login-card);
  padding: var(--login-card-padding);
  background: var(--color-card-bg);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-card);
}

.mfa-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: var(--size-brand-icon);
  height: var(--size-brand-icon);
  border-radius: var(--radius-round);
  background: color-mix(in srgb, var(--color-primary) 12%, transparent);
  color: var(--color-primary);
  font-size: var(--size-logo-icon);
}

.mfa-title {
  margin: var(--spacing-card) 0 0;
  font-size: var(--font-size-lg);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-1);
}

.mfa-sub {
  margin: var(--spacing-row) 0 0;
  font-size: var(--font-size-xs);
  color: var(--color-text-3);
  text-align: center;
}

/* 方式选择卡片 */
.method-row {
  display: flex;
  gap: var(--spacing-row);
  width: 100%;
  margin: var(--spacing-xl) 0 var(--spacing-card);
}

.method-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--spacing-xs);
  flex: 1;
  padding: var(--spacing-card) 0;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  color: var(--color-text-2);
  cursor: pointer;
  transition: border-color var(--duration-fast) ease, color var(--duration-fast) ease,
    background var(--duration-fast) ease;
}

.method-card:hover {
  border-color: var(--color-primary-hover);
}

.method-card.active {
  border-color: var(--color-primary);
  background: color-mix(in srgb, var(--color-primary) 6%, transparent);
  color: var(--color-primary);
}

.method-icon {
  font-size: var(--size-icon-md);
}

.method-label {
  font-size: var(--font-size-xs);
}

/* 方式面板 */
.panel {
  width: 100%;
  min-height: var(--size-mfa-panel);
  margin-bottom: var(--spacing-card);
}

.panel-hint {
  margin: 0 0 var(--spacing-card);
  font-size: var(--font-size-xs);
  color: var(--color-text-3);
  text-align: center;
}

.webauthn-hint {
  display: flex;
  align-items: center;
  gap: var(--spacing-row);
  padding: var(--spacing-card);
  border-radius: var(--radius-md);
  background: var(--color-table-header);
  font-size: var(--font-size-xs);
  color: var(--color-text-2);
}

.webauthn-icon {
  font-size: var(--size-icon-md);
  color: var(--color-primary);
  flex-shrink: 0;
}

.mfa-submit {
  margin-bottom: var(--spacing-card);
}

.mfa-links {
  display: flex;
  justify-content: space-between;
  width: 100%;
}
</style>
