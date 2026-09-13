<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Message } from '@arco-design/web-vue'
import type { FieldRule } from '@arco-design/web-vue'
import { useAuthStore, detectIdentity } from '../../../stores/auth'
import type { AuthIdentity, AuthSource, SsoProvider } from '../../../stores/auth'
import QrPlaceholder from '../components/QrPlaceholder.vue'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

/** 登录方式 Tab：密码 / 验证码 / 扫码（扫码页签内直接展示二维码） */
type LoginTab = 'password' | 'code' | 'scan'
const activeTab = ref<LoginTab>('password')

const form = reactive({ account: '', password: '', remember: true, source: 'local' as AuthSource })
/** 验证码登录表单（手机号/邮箱 + 验证码，账号类型自动识别） */
const codeForm = reactive({ account: '', code: '' })
const loading = ref(false)

/** 跳转型 SSO（GitHub/OIDC/SAML/CAS）：真实实现为重定向授权页，等宽图标按钮展示 */
const ssoLinks = [
  { key: 'github', label: 'GitHub', icon: 'icon-github' },
  { key: 'oidc', label: 'OIDC', icon: 'icon-safe' },
  { key: 'saml', label: 'SAML', icon: 'icon-idcard' },
  { key: 'cas', label: 'CAS', icon: 'icon-link' },
] as const

/** 账号输入框图标随识别结果联动（空输入默认用户名/手机号） */
const accountIcon = computed(() => {
  if (form.source === 'ldap') return 'icon-user'
  const iconMap: Record<AuthIdentity, string> = {
    username: 'icon-user',
    phone: 'icon-mobile',
    email: 'icon-email',
  }
  return iconMap[detectIdentity(form.account)]
})
const codeAccountIcon = computed(() =>
  detectIdentity(codeForm.account) === 'email' ? 'icon-email' : 'icon-mobile',
)

/**
 * 密码登录账号校验：不做类型选择，按内容自动识别并套用对应规则
 * （全数字 → 手机号规则；含 @ → 邮箱规则；其他 → 用户名规则）。
 */
const rules = computed<Record<string, FieldRule[]>>(() => ({
  account: [
    {
      required: true,
      validator: (value: string | undefined, callback: (error?: string) => void) => {
        const v = (value ?? '').trim()
        if (!v) return callback('请输入用户名 / 手机号 / 邮箱')
        if (/^\d+$/.test(v) && !/^1\d{10}$/.test(v)) return callback('请输入正确的 11 位手机号')
        if (v.includes('@') && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(v)) {
          return callback('请输入正确的邮箱地址')
        }
        if (!v.includes('@') && !/^\d+$/.test(v) && (v.length < 2 || v.length > 32)) {
          return callback('用户名长度为 2-32 个字符')
        }
        return callback()
      },
    },
  ],
  password: [
    { required: true, message: '请输入密码' },
    { minLength: 6, maxLength: 64, message: '密码长度为 6-64 个字符' },
  ],
}))

/** 验证码登录表单规则：账号仅接受手机号/邮箱（自动识别） */
const codeRules = computed<Record<string, FieldRule[]>>(() => ({
  account: [
    {
      required: true,
      validator: (value: string | undefined, callback: (error?: string) => void) => {
        const v = (value ?? '').trim()
        if (!v) return callback('请输入手机号 / 邮箱')
        if (/^\d+$/.test(v) && !/^1\d{10}$/.test(v)) return callback('请输入正确的 11 位手机号')
        if (v.includes('@') && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(v)) {
          return callback('请输入正确的邮箱地址')
        }
        if (!v.includes('@') && !/^\d+$/.test(v)) return callback('请输入手机号或邮箱')
        return callback()
      },
    },
  ],
  code: [
    { required: true, message: '请输入验证码' },
    { match: /^\d{6}$/, message: '验证码为 6 位数字' },
  ],
}))

const accountPlaceholder = computed(() =>
  form.source === 'ldap' ? '域账号（如 CORP\\zhangsan）' : '用户名 / 手机号 / 邮箱',
)

onMounted(() => {
  // 已登录用户直达系统（避免重复登录）
  if (auth.isLoggedIn) {
    router.replace('/dashboard')
  }
})

// 切换认证源：域账号仅支持密码登录，切换时清空表单避免残留校验提示
watch(
  () => form.source,
  (next) => {
    if (next === 'ldap') {
      activeTab.value = 'password'
    }
    form.account = ''
    form.password = ''
  },
)

/** 进入 MFA 二次验证阶段（密码登录 next=mfa 的公共收尾） */
function goMfa() {
  router.replace({
    name: 'mfa',
    query: {
      redirect: (route.query.redirect as string | undefined) ?? '/dashboard',
    },
  })
}

/**
 * 阶段一：密码登录。后端按账号 MFA 开关返回 next：
 * done = 未启用 MFA，直接完成登录；mfa = 携带 sessionToken 进入二次验证。
 * 域账号（LDAP/AD）走 /login/domain，同样按 MFA 状态分流（当前后端域登录直接签发）。
 */
async function handleLogin() {
  loading.value = true
  try {
    if (form.source === 'ldap') {
      await auth.loginDomain(form.account, form.password, form.remember)
      Message.success('登录成功')
      router.replace((route.query.redirect as string | undefined) ?? '/dashboard')
      return
    }
    const next = await auth.login(form.account, form.password, form.remember)
    if (next === 'mfa') {
      goMfa()
    } else {
      Message.success('登录成功')
      router.replace((route.query.redirect as string | undefined) ?? '/dashboard')
    }
  } catch (error) {
    Message.error(error instanceof Error ? error.message : '登录失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

/** 验证码登录：短信/邮件验证码替代密码完成主认证（演示验证码固定 123456） */
async function handleCodeLogin() {
  loading.value = true
  try {
    await auth.loginCode(codeForm.account, codeForm.code, form.remember)
    Message.success('登录成功')
    router.replace((route.query.redirect as string | undefined) ?? '/dashboard')
  } catch (error) {
    Message.error(error instanceof Error ? error.message : '登录失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

/** 发送登录验证码：演示环境验证码固定 123456，服务端日志可见发送记录 */
function handleSendCode() {
  Message.info('演示环境验证码固定为 123456（发送记录见服务端日志）')
}

/**
 * SSO 登录（演示）：回调按第三方绑定定位用户并直接签发令牌。
 * 真实实现为先 GET /api/auth/sso/{provider}/authorize 跳转 IdP 授权页，
 * 授权后浏览器携带 state 回到系统，再以回调换取令牌。
 */
async function handleSso(provider: SsoProvider) {
  loading.value = true
  try {
    await auth.loginSso(provider, form.remember)
    Message.success('登录成功')
    router.replace((route.query.redirect as string | undefined) ?? '/dashboard')
  } catch (error) {
    Message.error(error instanceof Error ? error.message : 'SSO 登录失败，请确认已绑定该方式')
  } finally {
    loading.value = false
  }
}

/** 恢复码登录：MFA 设备不可用时的一次性登录入口 */
const recoveryVisible = ref(false)
const recoveryForm = reactive({ account: '', code: '' })
const recoveryLoading = ref(false)

function openRecovery() {
  recoveryForm.account = ''
  recoveryForm.code = ''
  recoveryVisible.value = true
}

async function handleRecoveryLogin() {
  if (!recoveryForm.account.trim()) {
    Message.warning('请输入用户名')
    return
  }
  if (!/^[\w-]+$/.test(recoveryForm.code.trim())) {
    Message.warning('请输入正确的恢复码')
    return
  }
  recoveryLoading.value = true
  try {
    await auth.loginRecovery(recoveryForm.account.trim(), recoveryForm.code.trim(), form.remember)
    recoveryVisible.value = false
    Message.success('登录成功')
    router.replace((route.query.redirect as string | undefined) ?? '/dashboard')
  } catch (error) {
    Message.error(error instanceof Error ? error.message : '恢复码登录失败')
  } finally {
    recoveryLoading.value = false
  }
}

/** 扫码登录：后端未实现，入口保留提示即将上线 */
function handleScanConfirm() {
  Message.info('扫码登录即将上线')
}
</script>

<template>
  <div class="login-page">
    <!-- 动态渐变背景：紫/粉/蓝光斑缓慢漂浮 -->
    <div class="bg-orb orb-purple"></div>
    <div class="bg-orb orb-pink"></div>
    <div class="bg-orb orb-blue"></div>

    <!-- 主卡片：1280×693 分屏（左品牌展示区 / 右登录表单区），左右硬分割 -->
    <div class="login-card">
      <!-- 左侧品牌展示区：深海军蓝 + 同色系圆形暗纹 -->
      <div class="brand-side">
        <div class="brand-deco deco-a"></div>
        <div class="brand-deco deco-b"></div>

        <div class="brand-top">
          <div class="brand-mark"><icon-safe /></div>
          <span class="brand-name">Praesidium</span>
        </div>

        <h1 class="brand-title">安全接入，每一台服务器</h1>
        <p class="brand-desc">开源堡垒机 · 运维安全与审计平台</p>

        <ul class="brand-features">
          <li>细粒度 RBAC 权限管理</li>
          <li>SSH / RDP / VNC 全协议接入</li>
          <li>全量操作审计与回放</li>
        </ul>
      </div>

      <!-- 右侧登录表单区：纯白，表单有效宽度 360px 栏内居中 -->
      <div class="form-side">
        <div class="form-inner">
          <h2 class="welcome-title">欢迎回来</h2>
          <p class="welcome-sub">请输入您的账号信息登录系统</p>

          <!-- 登录方式：密码登录 / 验证码登录 / 扫码登录 -->
          <a-tabs v-model:active-key="activeTab" size="small" class="login-tabs">
            <!-- 密码登录 -->
            <a-tab-pane key="password" title="密码登录">
              <!-- 认证源切换：本地账号 / 外部目录（LDAP/AD） -->
              <a-radio-group v-model="form.source" type="button" size="small" class="source-switch">
                <a-radio value="local">本地账号</a-radio>
                <a-radio value="ldap">域账号</a-radio>
              </a-radio-group>

              <a-form
                :model="form"
                :rules="rules"
                layout="vertical"
                @submit-success="handleLogin"
              >
                <a-form-item field="account" hide-asterisk hide-label>
                  <a-input
                    v-model="form.account"
                    :placeholder="accountPlaceholder"
                    allow-clear
                    @press-enter="handleLogin"
                  >
                    <template #prefix><component :is="accountIcon" /></template>
                  </a-input>
                </a-form-item>
                <a-form-item field="password" hide-asterisk hide-label>
                  <a-input-password
                    v-model="form.password"
                    placeholder="密码"
                    allow-clear
                    @press-enter="handleLogin"
                  >
                    <template #prefix><icon-lock /></template>
                  </a-input-password>
                </a-form-item>

                <a-button
                  type="primary"
                  html-type="submit"
                  long
                  :loading="loading"
                  class="login-submit"
                >
                  {{ form.source === 'ldap' ? '域账号登录' : '登 录' }}
                </a-button>

                <div class="login-options">
                  <a-checkbox v-model="form.remember">记住我</a-checkbox>
                  <a-link @click="openRecovery">使用恢复码登录</a-link>
                </div>
              </a-form>
            </a-tab-pane>

            <!-- 验证码登录（域账号仅支持密码，禁用） -->
            <a-tab-pane key="code" title="验证码登录" :disabled="form.source === 'ldap'">
              <a-form
                :model="codeForm"
                :rules="codeRules"
                layout="vertical"
                @submit-success="handleCodeLogin"
              >
                <a-form-item field="account" hide-asterisk hide-label>
                  <a-input
                    v-model="codeForm.account"
                    placeholder="手机号 / 邮箱"
                    allow-clear
                  >
                    <template #prefix><component :is="codeAccountIcon" /></template>
                  </a-input>
                </a-form-item>
                <a-form-item field="code" hide-asterisk hide-label>
                  <a-input
                    v-model="codeForm.code"
                    placeholder="6 位验证码"
                    :max-length="6"
                    @press-enter="handleCodeLogin"
                  >
                    <template #prefix><icon-code /></template>
                    <template #suffix>
                      <a-link @click="handleSendCode">获取验证码</a-link>
                    </template>
                  </a-input>
                </a-form-item>

                <p class="code-hint">
                  验证码仅替代密码完成主认证，登录后仍需二次验证（MFA）
                </p>

                <a-button
                  type="primary"
                  html-type="submit"
                  long
                  :loading="loading"
                  class="login-submit"
                >
                  验证码登录
                </a-button>
              </a-form>
            </a-tab-pane>

            <!-- 扫码登录：页签内直接展示二维码，用对应 App 扫码即可登录 -->
            <a-tab-pane key="scan" title="扫码登录" :disabled="form.source === 'ldap'">
              <div class="scan-panel">
                <QrPlaceholder />

                <p class="scan-tip">
                  使用钉钉 / 飞书 / 企业微信 App 扫描二维码登录（即将上线）
                </p>

                <a-button
                  type="primary"
                  long
                  class="login-submit"
                  @click="handleScanConfirm"
                >
                  扫码登录（即将上线）
                </a-button>
              </div>
            </a-tab-pane>
          </a-tabs>

          <!-- 跳转型 SSO：等宽图标按钮（真实实现为重定向授权页） -->
          <div class="sso-divider"><span>其他登录方式</span></div>
          <div class="sso-buttons">
            <a-button
              v-for="item in ssoLinks"
              :key="item.key"
              class="sso-button"
              @click="handleSso(item.key)"
            >
              <template #icon><component :is="item.icon" /></template>
              {{ item.label }}
            </a-button>
          </div>

          <p class="login-mock">演示账号：admin / Admin@123 · demo / Demo@123（两步验证）</p>
        </div>
      </div>
    </div>

    <!-- 底部版权区：主卡片下方 60px 居中 -->
    <p class="copyright">Copyright © 2026 Praesidium · Apache-2.0</p>

    <!-- 恢复码登录弹窗（MFA 设备不可用时的一次性登录） -->
    <a-modal v-model:visible="recoveryVisible" title="恢复码登录" :footer="false" width="400px">
      <div class="recovery-login">
        <p class="recovery-login-hint">
          使用个人中心生成的恢复码完成一次性登录（每个恢复码仅可使用一次）
        </p>
        <a-input v-model="recoveryForm.account" placeholder="用户名" @press-enter="handleRecoveryLogin" />
        <a-input
          v-model="recoveryForm.code"
          placeholder="恢复码（如 A1B2-C3D4）"
          @press-enter="handleRecoveryLogin"
        />
        <a-button type="primary" long :loading="recoveryLoading" @click="handleRecoveryLogin">
          登录
        </a-button>
      </div>
    </a-modal>
  </div>
</template>

<style scoped>
/* 画布：紫→粉→蓝渐变通底，主卡片与版权区垂直居中 */
.login-page {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 60px;
  min-height: 100vh;
  padding: var(--spacing-xl) 0;
  background: linear-gradient(
    120deg,
    var(--color-auth-grad-purple),
    var(--color-auth-grad-pink) 35%,
    var(--color-auth-grad-blue) 70%,
    var(--color-auth-grad-purple)
  );
  background-size: 400% 400%;
  animation: auth-bg-shift 18s ease-in-out infinite alternate;
  overflow: hidden;
}

/* 渐变基底缓慢漂移，形成流动感 */
@keyframes auth-bg-shift {
  from {
    background-position: 0% 50%;
  }
  to {
    background-position: 100% 50%;
  }
}

/* 漂浮光斑：紫/粉/蓝三色柔光，各自缓慢游移 */
.bg-orb {
  position: absolute;
  border-radius: var(--radius-round);
  filter: blur(90px);
  pointer-events: none;
}

.orb-purple {
  width: 480px;
  height: 480px;
  top: -120px;
  left: -80px;
  background: var(--color-auth-orb-purple);
  animation: orb-drift-a 26s ease-in-out infinite alternate;
}

.orb-pink {
  width: 420px;
  height: 420px;
  right: -60px;
  bottom: -100px;
  background: var(--color-auth-orb-pink);
  animation: orb-drift-b 30s ease-in-out infinite alternate;
}

.orb-blue {
  width: 380px;
  height: 380px;
  top: 40%;
  left: 55%;
  background: var(--color-auth-orb-blue);
  animation: orb-drift-c 24s ease-in-out infinite alternate;
}

@keyframes orb-drift-a {
  from {
    transform: translate(0, 0) scale(1);
  }
  to {
    transform: translate(120px, 80px) scale(1.15);
  }
}

@keyframes orb-drift-b {
  from {
    transform: translate(0, 0) scale(1);
  }
  to {
    transform: translate(-100px, -60px) scale(1.1);
  }
}

@keyframes orb-drift-c {
  from {
    transform: translate(0, 0) scale(1.05);
  }
  to {
    transform: translate(-80px, 100px) scale(0.95);
  }
}

/* 主卡片：1280×693，圆角 16，柔和下沉阴影，左右 1:1 硬分割 */
.login-card {
  position: relative;
  z-index: 1;
  display: flex;
  width: var(--size-auth-card-w);
  height: var(--size-auth-card-h);
  border-radius: var(--radius-auth-card);
  background: var(--color-card-bg);
  box-shadow: var(--shadow-auth-card);
  overflow: hidden;
}

/* ---------- 左侧品牌展示区 ---------- */
.brand-side {
  position: relative;
  display: flex;
  flex-direction: column;
  flex: 1;
  padding: 56px 48px 48px;
  background: var(--color-brand-navy);
  overflow: hidden;
}

/* 同色系柔和圆形暗纹 */
.brand-deco {
  position: absolute;
  border-radius: var(--radius-round);
  background: var(--color-brand-deco);
  pointer-events: none;
}

.deco-a {
  width: 420px;
  height: 420px;
  top: -160px;
  right: -140px;
}

.deco-b {
  width: 300px;
  height: 300px;
  bottom: -120px;
  left: -100px;
}

.brand-top {
  display: flex;
  align-items: center;
  gap: var(--spacing-card);
}

/* 品牌标识：44×44 圆角 8 方块 + 白色盾牌图标 */
.brand-mark {
  display: flex;
  align-items: center;
  justify-content: center;
  width: var(--size-brand-mark);
  height: var(--size-brand-mark);
  border-radius: var(--radius-lg);
  background: var(--color-brand-mark-bg);
  color: var(--color-brand-text);
  font-size: var(--size-brand-icon);
}

.brand-name {
  font-size: var(--font-size-xl);
  font-weight: var(--font-weight-semibold);
  color: var(--color-brand-text);
}

.brand-title {
  margin: var(--spacing-xl) 0 var(--spacing-row);
  font-size: var(--font-size-auth-brand);
  font-weight: var(--font-weight-semibold);
  line-height: var(--line-height-relaxed);
  color: var(--color-brand-text);
}

.brand-desc {
  margin: 0;
  font-size: var(--font-size-md);
  color: var(--color-brand-text-soft);
}

/* 特性列表：底部区域，6px 圆点前置 */
.brand-features {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-xs);
  margin: auto 0 0;
  padding: 0;
  list-style: none;
}

.brand-features li {
  display: flex;
  align-items: center;
  gap: var(--spacing-row);
  font-size: var(--font-size-md);
  line-height: var(--line-height-feature);
  color: var(--color-brand-text-soft);
}

.brand-features li::before {
  content: '';
  width: var(--size-brand-feature-dot);
  height: var(--size-brand-feature-dot);
  border-radius: var(--radius-round);
  background: var(--color-brand-text-soft);
}

/* ---------- 右侧登录表单区 ---------- */
.form-side {
  display: flex;
  flex: 1;
  align-items: center;
  justify-content: center;
  background: var(--color-card-bg);
}

.form-inner {
  width: var(--size-auth-form-w);
}

.welcome-title {
  margin: 0;
  font-size: var(--font-size-xxl);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-1);
}

.welcome-sub {
  margin: var(--spacing-xs) 0 var(--spacing-card);
  font-size: var(--font-size-md);
  color: var(--color-text-3);
}

/* 三个标签均分表单宽度、文字居中，保证左右对称 */
.login-tabs :deep(.arco-tabs-nav) {
  margin-bottom: var(--spacing-card);
}

.login-tabs :deep(.arco-tabs-nav-tab) {
  width: 100%;
}

.login-tabs :deep(.arco-tabs-nav-tab-list) {
  display: flex;
  width: 100%;
}

.login-tabs :deep(.arco-tabs-tab) {
  flex: 1;
  display: flex;
  justify-content: center;
}

.login-tabs :deep(.arco-tabs-content) {
  padding-top: 0;
}

/* 输入框：38px 高、圆角 8、浅灰填充，聚焦切回白底 + 主色描边 */
.form-inner :deep(.arco-input) {
  height: var(--size-auth-input-h);
  background: var(--color-input-bg);
  border: 1px solid transparent;
  border-radius: var(--radius-lg);
}

.form-inner :deep(.arco-input input) {
  height: 100%;
  background: transparent;
}

.form-inner :deep(.arco-input-focus) {
  background: var(--color-card-bg);
  border-color: var(--color-primary);
}

/* 登录按钮：40px 高、圆角 8、通栏深色实心 */
.login-submit {
  height: var(--size-auth-button-h);
  border-radius: var(--radius-lg);
}

/* 认证源切换（本地账号 / 域账号） */
.source-switch {
  display: flex;
  margin-bottom: var(--spacing-card);
}

.source-switch :deep(.arco-radio-button) {
  flex: 1;
  text-align: center;
}

/* 登录按钮下方的辅助行（记住我 / 忘记密码） */
.login-options {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: var(--spacing-row);
  font-size: var(--font-size-sm);
}

/* 验证码登录说明：强调主认证与二次验证的顺序 */
.code-hint {
  margin: var(--spacing-xs) 0 var(--spacing-row);
  font-size: var(--font-size-xs);
  color: var(--color-text-3);
}

/* 扫码登录：二维码 + 提示（二维码尺寸随放大后的卡片局部调大） */
.scan-panel {
  display: flex;
  flex-direction: column;
  align-items: center;
  --size-qr: 200px;
}

.scan-tip {
  margin: var(--spacing-card) 0;
  font-size: var(--font-size-xs);
  color: var(--color-text-3);
  text-align: center;
}

/* SSO 入口：分割线 + 等宽图标按钮行 */
.sso-divider {
  display: flex;
  align-items: center;
  gap: var(--spacing-row);
  margin: var(--spacing-card) 0;
  font-size: var(--font-size-xs);
  color: var(--color-text-4);
}

.sso-divider::before,
.sso-divider::after {
  content: '';
  flex: 1;
  height: 1px;
  background: var(--color-border);
}

.sso-buttons {
  display: flex;
  gap: var(--spacing-row);
}

.sso-button {
  flex: 1 1 0;
  min-width: 0;
  padding: 0 var(--spacing-xs);
  font-size: var(--font-size-sm);
  color: var(--color-text-2);
}

/* 演示账号提示（开发期辅助演示，上线前移除） */
.login-mock {
  margin: var(--spacing-card) 0 0;
  text-align: center;
  font-size: var(--font-size-xs);
  color: var(--color-text-4);
}

/* 恢复码登录弹窗 */
.recovery-login {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-card);
}

.recovery-login-hint {
  margin: 0;
  font-size: var(--font-size-xs);
  color: var(--color-text-3);
}

/* 底部版权区 */
.copyright {
  position: relative;
  z-index: 1;
  margin: 0;
  font-size: var(--font-size-xs);
  color: var(--color-text-2);
}

/* 窄屏降级：卡片宽度自适应，隐藏品牌区，表单单列居中 */
@media (max-width: 980px) {
  .login-card {
    width: calc(100vw - 2 * var(--spacing-page));
    height: auto;
    min-height: var(--size-auth-card-h);
  }

  .brand-side {
    display: none;
  }
}
</style>
