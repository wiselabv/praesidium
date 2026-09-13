<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { Message } from '@arco-design/web-vue'
import type { FieldRule } from '@arco-design/web-vue'
import { fetchProfile, updateProfile } from '../../../api/profile'
import type { ProfileResponse } from '../../../api/profile'
import {
  bindSms,
  deleteWebauthn,
  fetchSecuritySummary,
  generateRecoveryCodes,
  listWebauthn,
  mfaBind,
  mfaDisable,
  mfaSetup,
  registerWebauthn,
  sendCode,
  toggleEmailMfa,
  unbindSms,
} from '../../../api/security'
import type { SecuritySummary, WebauthnItem } from '../../../api/security'
import { bindThirdParty, listThirdParty, unbindThirdParty } from '../../../api/thirdParty'
import type { ThirdPartyItem } from '../../../api/thirdParty'

/**
 * 个人中心页（L4）。
 *
 * 四个 Tab：基本信息（GET/PUT /api/profile）、登录安全（TOTP 两阶段绑定 / 恢复码 /
 * WebAuthn / 短信 / 邮件验证码，/api/profile/security/*）、第三方登录（/api/profile/third-party）、
 * 修改密码（后端暂无独立改密端点，占位说明）。
 */

/* ==================== 基本信息 ==================== */

const profile = ref<ProfileResponse | null>(null)
const profileLoading = ref(false)
const editVisible = ref(false)
const editSaving = ref(false)
const editForm = reactive({
  displayName: '',
  email: '',
  phone: '',
  department: '',
  title: '',
  bio: '',
})

async function loadProfile() {
  profileLoading.value = true
  try {
    profile.value = await fetchProfile()
  } catch (error) {
    Message.error(error instanceof Error ? error.message : '加载个人信息失败')
  } finally {
    profileLoading.value = false
  }
}

function openEdit() {
  const data = profile.value
  if (!data) return
  Object.assign(editForm, {
    displayName: data.displayName,
    email: data.email ?? '',
    phone: data.phone ?? '',
    department: data.department ?? '',
    title: data.title ?? '',
    bio: data.bio ?? '',
  })
  editVisible.value = true
}

async function handleProfileSave() {
  if (!editForm.displayName.trim()) {
    Message.warning('请填写显示名称')
    return
  }
  editSaving.value = true
  try {
    profile.value = await updateProfile({
      displayName: editForm.displayName.trim(),
      email: editForm.email.trim() || null,
      phone: editForm.phone.trim() || null,
      department: editForm.department.trim() || null,
      title: editForm.title.trim() || null,
      bio: editForm.bio.trim() || null,
    })
    editVisible.value = false
    Message.success('个人信息已更新')
  } catch (error) {
    Message.error(error instanceof Error ? error.message : '保存个人信息失败')
  } finally {
    editSaving.value = false
  }
}

/* ==================== 登录安全 ==================== */

const security = ref<SecuritySummary | null>(null)
const webauthnDevices = ref<WebauthnItem[]>([])
const securityLoading = ref(false)

async function loadSecurity() {
  securityLoading.value = true
  try {
    const [summary, devices] = await Promise.all([fetchSecuritySummary(), listWebauthn()])
    security.value = summary
    webauthnDevices.value = devices
  } catch (error) {
    Message.error(error instanceof Error ? error.message : '加载安全设置失败')
  } finally {
    securityLoading.value = false
  }
}

/* ---- TOTP 两阶段绑定 ---- */

const totpModalVisible = ref(false)
const totpStep = ref<'setup' | 'confirm'>('setup')
const totpSecret = ref('')
const totpUri = ref('')
const totpCode = ref('')
const totpLoading = ref(false)

async function openTotpBind() {
  totpModalVisible.value = true
  totpStep.value = 'setup'
  totpCode.value = ''
  totpLoading.value = true
  try {
    const setup = await mfaSetup()
    totpSecret.value = setup.secret
    totpUri.value = setup.otpAuthUri
  } catch (error) {
    Message.error(error instanceof Error ? error.message : '生成绑定信息失败')
    totpModalVisible.value = false
  } finally {
    totpLoading.value = false
  }
}

async function copyTotpUri() {
  await navigator.clipboard.writeText(totpUri.value)
  Message.success('otpauth 链接已复制，可在认证器 App 中手动添加')
}

async function handleTotpConfirm() {
  if (!/^\d{6}$/.test(totpCode.value)) {
    Message.warning('请输入 6 位确认码')
    return
  }
  totpLoading.value = true
  try {
    await mfaBind(totpSecret.value, totpCode.value)
    totpModalVisible.value = false
    Message.success('认证器绑定成功')
    loadSecurity()
  } catch (error) {
    Message.error(error instanceof Error ? error.message : '绑定认证器失败')
  } finally {
    totpLoading.value = false
  }
}

/** TOTP 解绑（即停用 MFA） */
const unbindVisible = ref(false)
const unbindLoading = ref(false)

async function handleUnbind() {
  unbindLoading.value = true
  try {
    await mfaDisable()
    unbindVisible.value = false
    Message.success('认证器已解绑')
    loadSecurity()
  } catch (error) {
    Message.error(error instanceof Error ? error.message : '解绑认证器失败')
  } finally {
    unbindLoading.value = false
  }
}

/* ---- 恢复码 ---- */

const recoveryVisible = ref(false)
const recoveryCodes = ref<string[]>([])
const recoveryRemaining = ref(0)
const recoveryLoading = ref(false)

async function openRecovery() {
  recoveryVisible.value = true
  recoveryLoading.value = true
  try {
    const result = await generateRecoveryCodes()
    recoveryCodes.value = result.codes
    recoveryRemaining.value = result.remaining
  } catch (error) {
    Message.error(error instanceof Error ? error.message : '生成恢复码失败')
    recoveryVisible.value = false
  } finally {
    recoveryLoading.value = false
  }
}

async function copyRecoveryCodes() {
  await navigator.clipboard.writeText(recoveryCodes.value.join('\n'))
  Message.success('恢复码已复制到剪贴板')
}

/* ---- 短信验证码绑定（两步：发送验证码 → 回填确认） ---- */

const smsBindVisible = ref(false)
const smsPhone = ref('')
const smsCode = ref('')
const smsSending = ref(false)
const smsBinding = ref(false)

function openSmsBind() {
  smsPhone.value = ''
  smsCode.value = ''
  smsBindVisible.value = true
}

async function handleSendSmsCode() {
  if (!/^1\d{10}$/.test(smsPhone.value.trim())) {
    Message.warning('请输入正确的 11 位手机号')
    return
  }
  smsSending.value = true
  try {
    await sendCode('sms', smsPhone.value.trim())
    Message.success('验证码已发送（演示环境固定为 123456）')
  } catch (error) {
    Message.error(error instanceof Error ? error.message : '发送验证码失败')
  } finally {
    smsSending.value = false
  }
}

async function handleSmsBindConfirm() {
  if (!/^1\d{10}$/.test(smsPhone.value.trim())) {
    Message.warning('请输入正确的 11 位手机号')
    return
  }
  if (!/^\d{6}$/.test(smsCode.value.trim())) {
    Message.warning('请输入 6 位验证码')
    return
  }
  smsBinding.value = true
  try {
    await bindSms(smsPhone.value.trim(), smsCode.value.trim())
    smsBindVisible.value = false
    Message.success('短信验证码绑定成功')
    loadSecurity()
  } catch (error) {
    Message.error(error instanceof Error ? error.message : '绑定短信验证码失败')
  } finally {
    smsBinding.value = false
  }
}

async function handleSmsUnbind() {
  try {
    await unbindSms()
    Message.success('短信验证码已解绑')
    loadSecurity()
  } catch (error) {
    Message.error(error instanceof Error ? error.message : '解绑短信验证码失败')
  }
}

/* ---- 邮件验证码开关 ---- */

const emailToggling = ref(false)

async function handleEmailToggle(value: string | number | boolean) {
  const enabled = Boolean(value)
  emailToggling.value = true
  try {
    await toggleEmailMfa(enabled)
    Message.success(enabled ? '邮件验证码已启用' : '邮件验证码已停用')
    loadSecurity()
  } catch (error) {
    Message.error(error instanceof Error ? error.message : '切换邮件验证码失败')
  } finally {
    emailToggling.value = false
  }
}

/* ---- WebAuthn 设备管理 ---- */

const webauthnVisible = ref(false)
const webauthnName = ref('')
const webauthnLoading = ref(false)

function openWebauthnRegister() {
  webauthnName.value = ''
  webauthnVisible.value = true
}

async function handleWebauthnRegister() {
  const name = webauthnName.value.trim() || '安全密钥'
  webauthnLoading.value = true
  try {
    await registerWebauthn(name)
    webauthnVisible.value = false
    Message.success('安全密钥注册成功')
    loadSecurity()
  } catch (error) {
    Message.error(error instanceof Error ? error.message : '注册安全密钥失败')
  } finally {
    webauthnLoading.value = false
  }
}

async function handleWebauthnRemove(device: WebauthnItem) {
  try {
    await deleteWebauthn(device.id)
    Message.success('设备已移除')
    loadSecurity()
  } catch (error) {
    Message.error(error instanceof Error ? error.message : '移除设备失败')
  }
}

/* ==================== 第三方登录 ==================== */

const thirdPartyItems = ref<ThirdPartyItem[]>([])
const thirdPartyLoading = ref(false)

/** 第三方提供方展示配置（与登录页 SSO 区对应） */
const THIRD_PARTY_PROVIDERS: { key: string; name: string; desc: string }[] = [
  { key: 'github', name: 'GitHub', desc: '开源社区账号，OIDC 协议' },
  { key: 'oidc', name: '企业 OIDC', desc: 'Keycloak / Authelia 等自托管身份提供方' },
  { key: 'dingtalk', name: '钉钉', desc: '钉钉 App 扫码登录' },
  { key: 'feishu', name: '飞书', desc: '飞书 App 扫码登录' },
  { key: 'wecom', name: '企业微信', desc: '企业微信 App 扫码登录' },
]

async function loadThirdParty() {
  thirdPartyLoading.value = true
  try {
    thirdPartyItems.value = await listThirdParty()
  } catch (error) {
    Message.error(error instanceof Error ? error.message : '加载第三方绑定失败')
  } finally {
    thirdPartyLoading.value = false
  }
}

function isBound(provider: string): boolean {
  return thirdPartyItems.value.some((item) => item.provider === provider)
}

async function handleThirdPartyBind(provider: string) {
  try {
    await bindThirdParty(provider)
    Message.success(`${provider} 绑定成功`)
    loadThirdParty()
  } catch (error) {
    Message.error(error instanceof Error ? error.message : '绑定失败')
  }
}

async function handleThirdPartyUnbind(provider: string) {
  try {
    await unbindThirdParty(provider)
    Message.success(`${provider} 已解绑`)
    loadThirdParty()
  } catch (error) {
    Message.error(error instanceof Error ? error.message : '解绑失败')
  }
}

/* ==================== 修改密码 ==================== */

const pwdForm = reactive({ old: '', next: '', confirm: '' })
const pwdRules: Record<string, FieldRule[]> = {
  old: [{ required: true, message: '请输入当前密码' }],
  next: [
    { required: true, message: '请输入新密码' },
    { minLength: 8, maxLength: 32, message: '密码长度为 8-32 个字符' },
  ],
  confirm: [
    { required: true, message: '请再次输入新密码' },
    {
      validator: (value: string | undefined, callback: (error?: string) => void) => {
        callback(value === pwdForm.next ? undefined : '两次输入的密码不一致')
      },
    },
  ],
}

/** 后端暂无独立修改密码端点，保留占位 */
function handleChangePassword() {
  Message.info('修改密码接口将在后续里程碑开放，请通过管理员重置')
  pwdForm.old = ''
  pwdForm.next = ''
  pwdForm.confirm = ''
}

function formatTime(iso: string | null): string {
  return iso ? new Date(iso).toLocaleString('zh-CN', { hour12: false }) : '-'
}

onMounted(() => {
  loadProfile()
  loadSecurity()
  loadThirdParty()
})
</script>

<template>
  <div class="profile">
    <div class="page-header">
      <h2 class="page-title">个人中心</h2>
    </div>

    <a-card :bordered="false" class="profile-card">
      <a-tabs tab-position="left" class="profile-tabs">
        <!-- 基本信息 -->
        <a-tab-pane key="basic" title="基本信息">
          <a-spin :loading="profileLoading">
            <template v-if="profile">
              <div class="user-card">
                <!-- 头像尺寸与 --size-avatar-lg token 一致（组件 prop 无法消费 CSS 变量） -->
                <a-avatar :size="48" class="user-avatar">
                  {{ (profile.displayName || profile.username).slice(0, 1).toUpperCase() }}
                </a-avatar>
                <div>
                  <div class="user-name">{{ profile.displayName || profile.username }}</div>
                  <a-tag color="arcoblue" size="small">{{ profile.status === 'active' ? '启用' : profile.status }}</a-tag>
                </div>
                <a-button class="edit-btn" size="small" @click="openEdit">编辑资料</a-button>
              </div>

              <a-descriptions :column="1" class="user-descriptions">
                <a-descriptions-item label="用户 ID">{{ profile.id }}</a-descriptions-item>
                <a-descriptions-item label="用户名">{{ profile.username }}</a-descriptions-item>
                <a-descriptions-item label="邮箱">{{ profile.email ?? '-' }}</a-descriptions-item>
                <a-descriptions-item label="手机号">{{ profile.phone ?? '-' }}</a-descriptions-item>
                <a-descriptions-item label="部门">{{ profile.department ?? '-' }}</a-descriptions-item>
                <a-descriptions-item label="职位">{{ profile.title ?? '-' }}</a-descriptions-item>
                <a-descriptions-item label="个人简介">{{ profile.bio ?? '-' }}</a-descriptions-item>
                <a-descriptions-item label="最近登录">{{ formatTime(profile.lastLoginAt) }}</a-descriptions-item>
                <a-descriptions-item label="最近登录 IP">{{ profile.lastLoginIp ?? '-' }}</a-descriptions-item>
              </a-descriptions>
            </template>
          </a-spin>
        </a-tab-pane>

        <!-- 登录安全 -->
        <a-tab-pane key="security" title="登录安全">
          <a-spin :loading="securityLoading">
            <template v-if="security">
              <!-- TOTP / MFA -->
              <div class="setting-item">
                <div class="setting-info">
                  <div class="setting-title">认证器（TOTP）</div>
                  <div class="setting-desc">Google Authenticator / 1Password 等 App 扫码绑定，绑定后登录需二次验证</div>
                </div>
                <a-tag v-if="security.mfaEnabled" color="green" size="small">已绑定</a-tag>
                <a-tag v-else color="gray" size="small">未绑定</a-tag>
                <a-button v-if="security.mfaEnabled" size="small" @click="unbindVisible = true">
                  解绑
                </a-button>
                <a-button v-else type="primary" size="small" @click="openTotpBind">
                  立即绑定
                </a-button>
              </div>

              <!-- 短信验证码 -->
              <div class="setting-item">
                <div class="setting-info">
                  <div class="setting-title">短信验证码</div>
                  <div class="setting-desc">
                    {{ security.smsEnabled ? `已绑定 ${security.smsPhone}` : '绑定手机号，登录时可选择短信验证' }}
                  </div>
                </div>
                <a-tag v-if="security.smsEnabled" color="green" size="small">已绑定</a-tag>
                <a-tag v-else color="gray" size="small">未绑定</a-tag>
                <a-button v-if="security.smsEnabled" size="small" @click="handleSmsUnbind">
                  解绑
                </a-button>
                <a-button v-else type="primary" size="small" @click="openSmsBind">
                  立即绑定
                </a-button>
              </div>

              <!-- 邮件验证码 -->
              <div class="setting-item">
                <div class="setting-info">
                  <div class="setting-title">邮件验证码</div>
                  <div class="setting-desc">启用后登录时可选择邮件验证（发送至账号邮箱）</div>
                </div>
                <a-switch
                  :model-value="security.emailMfaEnabled"
                  :loading="emailToggling"
                  @change="handleEmailToggle"
                />
              </div>

              <!-- WebAuthn 安全密钥设备 -->
              <div class="setting-item">
                <div class="setting-info">
                  <div class="setting-title">安全密钥（WebAuthn）</div>
                  <div class="setting-desc">指纹 / Windows Hello / 硬件密钥，抗钓鱼最高等级</div>
                </div>
                <a-button type="primary" size="small" @click="openWebauthnRegister">
                  注册设备
                </a-button>
              </div>
              <div v-for="device in webauthnDevices" :key="device.id" class="device-item">
                <icon-safe class="device-icon" />
                <div class="device-info">
                  <div class="device-name">{{ device.name }}</div>
                  <div class="device-time">注册于 {{ formatTime(device.addedAt) }}</div>
                </div>
                <a-button size="small" @click="handleWebauthnRemove(device)">移除</a-button>
              </div>

              <!-- 恢复码 -->
              <div class="setting-item">
                <div class="setting-info">
                  <div class="setting-title">恢复码</div>
                  <div class="setting-desc">
                    丢失验证设备时的一次性备用码，仅生成时完整展示（当前剩余 {{ security.recoveryCodesLeft }} 个）
                  </div>
                </div>
                <a-button size="small" @click="openRecovery">重新生成</a-button>
              </div>
            </template>
          </a-spin>
        </a-tab-pane>

        <!-- 第三方登录 -->
        <a-tab-pane key="thirdParty" title="第三方登录">
          <a-spin :loading="thirdPartyLoading">
            <p class="third-party-hint">绑定后可在登录页使用对应方式快捷登录</p>
            <div v-for="provider in THIRD_PARTY_PROVIDERS" :key="provider.key" class="setting-item">
              <div class="setting-info">
                <div class="setting-title">{{ provider.name }}</div>
                <div class="setting-desc">{{ provider.desc }}</div>
              </div>
              <a-tag v-if="isBound(provider.key)" color="green" size="small">已绑定</a-tag>
              <a-tag v-else color="gray" size="small">未绑定</a-tag>
              <a-button
                v-if="isBound(provider.key)"
                size="small"
                @click="handleThirdPartyUnbind(provider.key)"
              >
                解绑
              </a-button>
              <a-button
                v-else
                type="primary"
                size="small"
                @click="handleThirdPartyBind(provider.key)"
              >
                绑定
              </a-button>
            </div>
          </a-spin>
        </a-tab-pane>

        <!-- 修改密码 -->
        <a-tab-pane key="password" title="修改密码">
          <a-form
            :model="pwdForm"
            :rules="pwdRules"
            layout="vertical"
            class="password-form"
            @submit-success="handleChangePassword"
          >
            <a-form-item label="当前密码" field="old">
              <a-input-password v-model="pwdForm.old" placeholder="请输入当前密码" />
            </a-form-item>
            <a-form-item label="新密码" field="next">
              <a-input-password v-model="pwdForm.next" placeholder="8-32 个字符" />
            </a-form-item>
            <a-form-item label="确认新密码" field="confirm">
              <a-input-password v-model="pwdForm.confirm" placeholder="再次输入新密码" />
            </a-form-item>
            <a-button type="primary" html-type="submit">确认修改</a-button>
          </a-form>
        </a-tab-pane>
      </a-tabs>
    </a-card>

    <!-- 编辑资料抽屉 -->
    <a-drawer v-model:visible="editVisible" title="编辑资料" :width="420" :footer="false">
      <a-form :model="editForm" layout="vertical">
        <a-form-item label="显示名称" required>
          <a-input v-model="editForm.displayName" />
        </a-form-item>
        <a-form-item label="邮箱">
          <a-input v-model="editForm.email" placeholder="选填" />
        </a-form-item>
        <a-form-item label="手机号">
          <a-input v-model="editForm.phone" placeholder="选填" />
        </a-form-item>
        <a-form-item label="部门">
          <a-input v-model="editForm.department" placeholder="选填" />
        </a-form-item>
        <a-form-item label="职位">
          <a-input v-model="editForm.title" placeholder="选填" />
        </a-form-item>
        <a-form-item label="个人简介">
          <a-textarea v-model="editForm.bio" placeholder="选填" :auto-size="{ minRows: 2, maxRows: 4 }" />
        </a-form-item>
        <a-form-item>
          <a-space class="drawer-actions">
            <a-button @click="editVisible = false">取消</a-button>
            <a-button type="primary" :loading="editSaving" @click="handleProfileSave">保存</a-button>
          </a-space>
        </a-form-item>
      </a-form>
    </a-drawer>

    <!-- TOTP 绑定弹窗（两阶段：setup 展示 otpauth → confirm 校验动态码） -->
    <a-modal
      v-model:visible="totpModalVisible"
      title="绑定认证器"
      :footer="false"
      width="400px"
      @cancel="totpStep = 'setup'"
    >
      <div class="totp-modal">
        <template v-if="totpStep === 'setup'">
          <a-spin :loading="totpLoading">
            <p class="totp-hint">
              复制下方 otpauth 链接，在认证器 App（Google Authenticator / 1Password 等）中手动添加，或使用 App 的「输入密钥」功能录入。
            </p>
            <a-input-group class="totp-uri-group">
              <a-input :model-value="totpUri" readonly />
              <a-button @click="copyTotpUri">复制</a-button>
            </a-input-group>
            <a-button type="primary" long @click="totpStep = 'confirm'">已添加，输入确认码</a-button>
          </a-spin>
        </template>
        <template v-else>
          <p class="totp-hint">输入 App 显示的 6 位动态码完成绑定</p>
          <a-input
            v-model="totpCode"
            placeholder="6 位确认码"
            maxlength="6"
            class="totp-code-input"
            @press-enter="handleTotpConfirm"
          />
          <a-space class="totp-actions">
            <a-button @click="totpStep = 'setup'">上一步</a-button>
            <a-button type="primary" :loading="totpLoading" @click="handleTotpConfirm">
              确认绑定
            </a-button>
          </a-space>
        </template>
      </div>
    </a-modal>

    <!-- 解绑确认弹窗 -->
    <a-modal
      v-model:visible="unbindVisible"
      title="解绑认证器"
      width="400px"
      :ok-loading="unbindLoading"
      @ok="handleUnbind"
    >
      <p>解绑后登录将不再要求认证器验证（若 MFA 仍开启，将使用其他可用方式）。</p>
    </a-modal>

    <!-- 短信验证码绑定弹窗 -->
    <a-modal v-model:visible="smsBindVisible" title="绑定短信验证码" :footer="false" width="400px">
      <div class="bind-modal">
        <p class="bind-hint">输入手机号并获取验证码（演示环境验证码固定为 123456）</p>
        <a-input-group>
          <a-input v-model="smsPhone" placeholder="11 位手机号" />
          <a-button :loading="smsSending" @click="handleSendSmsCode">获取验证码</a-button>
        </a-input-group>
        <a-input v-model="smsCode" placeholder="6 位验证码" maxlength="6" />
        <a-button type="primary" long :loading="smsBinding" @click="handleSmsBindConfirm">
          确认绑定
        </a-button>
      </div>
    </a-modal>

    <!-- WebAuthn 注册设备弹窗 -->
    <a-modal v-model:visible="webauthnVisible" title="注册安全密钥" :footer="false" width="400px">
      <div class="bind-modal">
        <p class="bind-hint">为设备命名后注册（演示环境由后端直接生成凭据元数据）</p>
        <a-input v-model="webauthnName" placeholder="设备名称，如 Windows Hello" />
        <a-button type="primary" long :loading="webauthnLoading" @click="handleWebauthnRegister">
          开始注册
        </a-button>
      </div>
    </a-modal>

    <!-- 恢复码弹窗（宽度与 --size-modal-lg token 一致） -->
    <a-modal v-model:visible="recoveryVisible" title="恢复码" :footer="false" width="480px">
      <a-spin :loading="recoveryLoading">
        <p class="recovery-hint">
          以下恢复码仅完整展示一次，每个仅可使用一次。请妥善保存，共 {{ recoveryRemaining }} 个可用。
        </p>
        <div class="recovery-grid">
          <div v-for="code in recoveryCodes" :key="code" class="recovery-code">
            {{ code }}
          </div>
        </div>
        <div class="recovery-actions">
          <a-button @click="copyRecoveryCodes">复制全部</a-button>
        </div>
      </a-spin>
    </a-modal>
  </div>
</template>

<style scoped>
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--spacing-card);
}

.page-title {
  margin: 0;
  font-size: var(--font-size-lg);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-1);
}

.profile-card {
  box-shadow: var(--shadow-card);
}

.profile-tabs {
  min-height: var(--size-profile-tabs);
}

/* ---------- 基本信息 ---------- */
.user-card {
  display: flex;
  align-items: center;
  gap: var(--spacing-card);
  padding-bottom: var(--spacing-xl);
  border-bottom: 1px solid var(--color-border);
}

.user-avatar {
  background: var(--color-primary);
}

.user-name {
  margin-bottom: var(--spacing-xs);
  font-size: var(--font-size-lg);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-1);
}

.edit-btn {
  margin-left: auto;
}

.user-descriptions {
  margin-top: var(--spacing-xl);
}

/* ---------- 登录安全 ---------- */
.setting-item {
  display: flex;
  align-items: center;
  gap: var(--spacing-card);
  padding: var(--spacing-card) 0;
  border-bottom: 1px solid var(--color-border);
}

.setting-info {
  flex: 1;
}

.setting-title {
  font-size: var(--font-size-md);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-1);
}

.setting-desc {
  margin-top: var(--spacing-xs);
  font-size: var(--font-size-xs);
  color: var(--color-text-3);
}

/* WebAuthn 设备条目 */
.device-item {
  display: flex;
  align-items: center;
  gap: var(--spacing-card);
  padding: var(--spacing-card) 0 var(--spacing-card) var(--spacing-page);
  border-bottom: 1px solid var(--color-border);
}

.device-icon {
  font-size: var(--size-icon-md);
  color: var(--color-primary);
}

.device-info {
  flex: 1;
}

.device-name {
  font-size: var(--font-size-sm);
  color: var(--color-text-1);
}

.device-time {
  margin-top: var(--spacing-xs);
  font-size: var(--font-size-xs);
  color: var(--color-text-3);
}

/* ---------- 第三方登录 ---------- */
.third-party-hint {
  margin: 0 0 var(--spacing-card);
  font-size: var(--font-size-xs);
  color: var(--color-text-3);
}

/* ---------- 绑定弹窗 ---------- */
.bind-modal {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-card);
}

.bind-hint {
  margin: 0;
  font-size: var(--font-size-xs);
  color: var(--color-text-3);
}

/* ---------- 修改密码 ---------- */
.password-form {
  max-width: var(--size-password-form);
}

/* ---------- TOTP 绑定弹窗 ---------- */
.totp-modal {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-card);
}

.totp-hint {
  margin: 0;
  font-size: var(--font-size-xs);
  color: var(--color-text-3);
}

.totp-uri-group {
  width: 100%;
}

.totp-code-input {
  width: 100%;
}

.totp-actions {
  justify-content: flex-end;
}

/* ---------- 恢复码弹窗 ---------- */
.recovery-hint {
  margin: 0 0 var(--spacing-card);
  font-size: var(--font-size-xs);
  color: var(--color-text-3);
}

.recovery-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: var(--spacing-row);
  margin-bottom: var(--spacing-card);
}

.recovery-code {
  padding: var(--spacing-row);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-table-header);
  font-family: var(--font-family-mono);
  font-size: var(--font-size-sm);
  text-align: center;
  color: var(--color-text-1);
}

.recovery-actions {
  display: flex;
  justify-content: flex-end;
  gap: var(--spacing-row);
}

.drawer-actions {
  justify-content: flex-end;
  width: 100%;
}
</style>
