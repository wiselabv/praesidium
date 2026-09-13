<script setup lang="ts">
import { ref, watch } from 'vue'

/**
 * 6 格验证码输入组件（L3：auth 特性内共用）。
 *
 * 交互细节：自动跳格 / 退格回跳 / 整串粘贴 / 聚焦全选。
 * 通过 v-model 以 string[]（长度 6）与父组件双向同步。
 */
const props = defineProps<{
  modelValue: string[]
}>()

const emit = defineEmits<{
  'update:modelValue': [value: string[]]
  /** 6 格全部填满时触发 */
  complete: [value: string]
}>()

const inputs = ref<(HTMLInputElement | null)[]>([])

watch(
  () => props.modelValue,
  (value) => {
    for (let i = 0; i < 6; i += 1) {
      const input = inputs.value[i]
      if (input) input.value = value[i] ?? ''
    }
  },
)

/** 输入一格：只保留数字，自动跳到下一格 */
function handleInput(index: number, event: Event) {
  const el = event.target as HTMLInputElement
  const value = el.value.replace(/\D/g, '').slice(-1)
  const next = [...props.modelValue]
  next[index] = value
  el.value = value
  emit('update:modelValue', next)
  if (value && index < 5) {
    inputs.value[index + 1]?.focus()
  } else if (!value) {
    return
  }
  if (next.every((code) => code !== '')) {
    emit('complete', next.join(''))
  }
}

/** 退格：当前格为空时退回上一格 */
function handleKeydown(index: number, event: KeyboardEvent) {
  if (event.key === 'Backspace' && !props.modelValue[index] && index > 0) {
    inputs.value[index - 1]?.focus()
  }
}

/** 整串粘贴：一次性填充 6 格 */
function handlePaste(event: ClipboardEvent) {
  const text = event.clipboardData?.getData('text').replace(/\D/g, '').slice(0, 6) ?? ''
  if (!text) return
  event.preventDefault()
  const next = Array.from({ length: 6 }, (_, i) => text[i] ?? '')
  emit('update:modelValue', next)
  inputs.value[Math.min(text.length, 5)]?.focus()
  if (next.every((code) => code !== '')) {
    emit('complete', next.join(''))
  }
}

/** 聚焦时全选当前格，方便覆盖输入 */
function handleFocus(index: number) {
  inputs.value[index]?.select()
}

/** 供父组件清空重输 */
function clear() {
  emit('update:modelValue', ['', '', '', '', '', ''])
  inputs.value[0]?.focus()
}

defineExpose({ clear, focus: () => inputs.value[0]?.focus() })
</script>

<template>
  <div class="otp-row" @paste="handlePaste">
    <input
      v-for="(_, index) in modelValue"
      :key="index"
      :ref="(el) => (inputs[index] = el as HTMLInputElement | null)"
      class="otp-cell"
      inputmode="numeric"
      maxlength="1"
      :value="modelValue[index]"
      @input="handleInput(index, $event)"
      @keydown="handleKeydown(index, $event)"
      @focus="handleFocus(index)"
    />
  </div>
</template>

<style scoped>
.otp-row {
  display: flex;
  gap: var(--spacing-row);
}

.otp-cell {
  width: var(--size-otp-cell);
  height: var(--size-otp-cell);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-card-bg);
  color: var(--color-text-1);
  font-size: var(--font-size-lg);
  text-align: center;
  outline: none;
  transition: border-color var(--duration-fast) ease;
}

.otp-cell:focus {
  border-color: var(--color-primary);
}
</style>
