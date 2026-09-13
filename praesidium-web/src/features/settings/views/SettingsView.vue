<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { Message } from '@arco-design/web-vue'
import { fetchSettings, saveSetting } from '../../../api/settings'

/**
 * 系统设置页（L4）。
 *
 * 系统管理：基本 / 安全策略 / 通知 / 认证 / 存储 五组配置，按 Tab 组织。
 * 数据源 GET /api/settings（全量读取）+ PUT /api/settings/{section}（按分组整体保存）。
 * 后端配置为 JSONB 动态存储，本页按已知分组键渲染表单。
 */

/** 基本设置 */
const basic = reactive({
  siteName: 'Praesidium 堡垒机',
  adminEmail: 'admin@example.com',
  sessionTimeout: 30,
  timezone: 'Asia/Shanghai',
  loginBanner: '本系统仅限授权人员使用，所有操作将被审计记录。',
})

/** 安全策略 */
const security = reactive({
  passwordMinLength: 12,
  requireComplexity: true,
  passwordExpireDays: 90,
  /** 失败锁定阈值（次） */
  lockThreshold: 5,
  lockMinutes: 15,
  /** 会话空闲超时（分钟） */
  idleTimeout: 15,
  ipWhitelist: '',
})

/** 通知配置 */
const notify = reactive({
  emailEnabled: true,
  smtpHost: 'smtp.example.com',
  smtpPort: 465,
  smtpUser: 'alert@example.com',
  smtpSsl: true,
  webhookEnabled: false,
  webhookUrl: '',
})

/** 认证配置 */
const auth = reactive({
  mfaRequired: true,
  ssoGithub: true,
  ssoLdap: false,
  ssoOidc: false,
  captchaOnFailed: true,
})

/** 存储配置 */
const storage = reactive({
  recordingRetentionDays: 180,
  logRetentionDays: 365,
  recordingPath: '/data/praesidium/recordings',
  alertRetentionDays: 90,
})

const activeTab = ref('basic')
const loading = ref(false)

/** 分组名 → 表单模型映射（键与后端 JSONB 种子一致） */
const SECTIONS: Record<string, Record<string, unknown>> = {
  basic,
  security,
  notify,
  auth,
  storage,
}

async function loadConfig() {
  loading.value = true
  try {
    const all = await fetchSettings()
    for (const [section, config] of Object.entries(all)) {
      if (SECTIONS[section]) Object.assign(SECTIONS[section], config)
    }
  } catch (error) {
    Message.error(error instanceof Error ? error.message : '加载系统配置失败')
  } finally {
    loading.value = false
  }
}

onMounted(loadConfig)

/** 保存（PUT /api/settings/{section}，后端整体替换该分组配置） */
async function handleSave(section: string) {
  try {
    await saveSetting(section, SECTIONS[section])
    Message.success(`「${section}」配置已保存`)
  } catch (error) {
    Message.error(error instanceof Error ? error.message : '保存配置失败')
  }
}
</script>

<template>
  <div class="settings-page">
    <!-- 页标题区 -->
    <div class="page-header">
      <h2 class="page-title">系统设置</h2>
    </div>

    <!-- 配置分组 Tab -->
    <a-card :bordered="false" class="settings-card">
      <a-spin :loading="loading">
        <a-tabs v-model:active-key="activeTab" type="rounded">
        <!-- 基本设置 -->
        <a-tab-pane key="basic" title="基本设置">
          <a-form :model="basic" layout="vertical" class="settings-form">
            <a-form-item label="站点名称">
              <a-input v-model="basic.siteName" />
            </a-form-item>
            <a-form-item label="管理员邮箱">
              <a-input v-model="basic.adminEmail" placeholder="用于接收系统通知" />
            </a-form-item>
            <a-form-item label="会话超时（分钟）">
              <a-input-number v-model="basic.sessionTimeout" :min="5" :max="1440" />
            </a-form-item>
            <a-form-item label="时区">
              <a-select v-model="basic.timezone">
                <a-option value="Asia/Shanghai">Asia/Shanghai（UTC+8）</a-option>
                <a-option value="UTC">UTC</a-option>
              </a-select>
            </a-form-item>
            <a-form-item label="登录提示语">
              <a-textarea v-model="basic.loginBanner" :auto-size="{ minRows: 2, maxRows: 4 }" />
            </a-form-item>
            <a-form-item>
              <a-button type="primary" @click="handleSave('basic')">保存</a-button>
            </a-form-item>
          </a-form>
        </a-tab-pane>

        <!-- 安全策略 -->
        <a-tab-pane key="security" title="安全策略">
          <a-form :model="security" layout="vertical" class="settings-form">
            <a-form-item label="密码最小长度">
              <a-input-number v-model="security.passwordMinLength" :min="8" :max="32" />
            </a-form-item>
            <a-form-item label="密码复杂度要求">
              <a-switch v-model="security.requireComplexity" />
              <span class="switch-tip">大小写字母 + 数字 + 特殊字符</span>
            </a-form-item>
            <a-form-item label="密码有效期（天）">
              <a-input-number v-model="security.passwordExpireDays" :min="0" :max="365" />
            </a-form-item>
            <a-form-item label="登录失败锁定阈值（次）">
              <a-input-number v-model="security.lockThreshold" :min="1" :max="10" />
            </a-form-item>
            <a-form-item label="锁定时长（分钟）">
              <a-input-number v-model="security.lockMinutes" :min="1" :max="1440" />
            </a-form-item>
            <a-form-item label="会话空闲超时（分钟）">
              <a-input-number v-model="security.idleTimeout" :min="5" :max="240" />
            </a-form-item>
            <a-form-item label="管理入口 IP 白名单">
              <a-input v-model="security.ipWhitelist" placeholder="选填，多个 IP 用逗号分隔" />
            </a-form-item>
            <a-form-item>
              <a-button type="primary" @click="handleSave('security')">保存</a-button>
            </a-form-item>
          </a-form>
        </a-tab-pane>

        <!-- 通知配置 -->
        <a-tab-pane key="notify" title="通知配置">
          <a-form :model="notify" layout="vertical" class="settings-form">
            <a-form-item label="邮件告警">
              <a-switch v-model="notify.emailEnabled" />
            </a-form-item>
            <template v-if="notify.emailEnabled">
              <a-form-item label="SMTP 服务器">
                <a-input v-model="notify.smtpHost" />
              </a-form-item>
              <a-form-item label="SMTP 端口">
                <a-input-number v-model="notify.smtpPort" :min="1" :max="65535" />
              </a-form-item>
              <a-form-item label="发件账号">
                <a-input v-model="notify.smtpUser" />
              </a-form-item>
              <a-form-item label="SSL 加密">
                <a-switch v-model="notify.smtpSsl" />
              </a-form-item>
            </template>
            <a-form-item label="Webhook 告警">
              <a-switch v-model="notify.webhookEnabled" />
            </a-form-item>
            <a-form-item v-if="notify.webhookEnabled" label="Webhook 地址">
              <a-input v-model="notify.webhookUrl" placeholder="如钉钉 / 飞书机器人地址" />
            </a-form-item>
            <a-form-item>
              <a-button type="primary" @click="handleSave('notify')">保存</a-button>
            </a-form-item>
          </a-form>
        </a-tab-pane>

        <!-- 认证配置 -->
        <a-tab-pane key="auth" title="认证配置">
          <a-form :model="auth" layout="vertical" class="settings-form">
            <a-form-item label="强制 MFA 双因素认证">
              <a-switch v-model="auth.mfaRequired" />
              <span class="switch-tip">关闭后用户可跳过 MFA（不建议）</span>
            </a-form-item>
            <a-form-item label="SSO - GitHub">
              <a-switch v-model="auth.ssoGithub" />
            </a-form-item>
            <a-form-item label="SSO - LDAP">
              <a-switch v-model="auth.ssoLdap" />
            </a-form-item>
            <a-form-item label="SSO - OIDC">
              <a-switch v-model="auth.ssoOidc" />
            </a-form-item>
            <a-form-item label="失败后启用图形验证码">
              <a-switch v-model="auth.captchaOnFailed" />
            </a-form-item>
            <a-form-item>
              <a-button type="primary" @click="handleSave('auth')">保存</a-button>
            </a-form-item>
          </a-form>
        </a-tab-pane>

        <!-- 存储配置 -->
        <a-tab-pane key="storage" title="存储配置">
          <a-form :model="storage" layout="vertical" class="settings-form">
            <a-form-item label="录像保留天数">
              <a-input-number v-model="storage.recordingRetentionDays" :min="7" :max="3650" />
            </a-form-item>
            <a-form-item label="审计日志保留天数">
              <a-input-number v-model="storage.logRetentionDays" :min="30" :max="3650" />
            </a-form-item>
            <a-form-item label="告警保留天数">
              <a-input-number v-model="storage.alertRetentionDays" :min="7" :max="3650" />
            </a-form-item>
            <a-form-item label="录像存储路径">
              <a-input v-model="storage.recordingPath" />
            </a-form-item>
            <a-form-item>
              <a-button type="primary" @click="handleSave('storage')">保存</a-button>
            </a-form-item>
          </a-form>
        </a-tab-pane>
      </a-tabs>
      </a-spin>
    </a-card>
  </div>
</template>

<style scoped>
/* 页标题区（全站标准页面结构第 1 层） */
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

.settings-card {
  box-shadow: var(--shadow-card);
}

/* 表单限宽，避免大屏拉伸过长 */
.settings-form {
  max-width: 520px;
  padding-top: var(--spacing-card);
}

.switch-tip {
  margin-left: var(--spacing-row);
  font-size: var(--font-size-xs);
  color: var(--color-text-4);
}
</style>
