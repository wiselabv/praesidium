<script setup lang="ts">
import { computed } from 'vue'

/**
 * 二维码占位组件（L3）。
 *
 * 渲染 21×21 模块的伪二维码（含三处定位角），用于 TOTP 绑定界面占位。
 * TODO(认证): 接口就绪后替换为真实 otpauth:// URI 渲染（qrcode 库），
 * 组件对外契约不变，仅内部数据源切换。
 */

/** 确定性伪随机（同一 seed 渲染结果稳定，避免刷新跳变） */
function mulberry32(seed: number) {
  return () => {
    seed |= 0
    seed = (seed + 0x6d2b79f5) | 0
    let t = Math.imul(seed ^ (seed >>> 15), 1 | seed)
    t = (t + Math.imul(t ^ (t >>> 7), 61 | t)) ^ t
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296
  }
}

const SIZE = 21
/** 三处定位角（左上 / 右上 / 左下）的起始坐标 */
const FINDERS = [
  [0, 0],
  [0, 14],
  [14, 0],
] as const

const rand = mulberry32(20260912)

const cells = computed(() => {
  const matrix: boolean[][] = []
  for (let row = 0; row < SIZE; row += 1) {
    const line: boolean[] = []
    for (let col = 0; col < SIZE; col += 1) {
      line.push(isDark(row, col))
    }
    matrix.push(line)
  }
  return matrix
})

/** 单格明暗：定位角按二维码标准图案，其余区域伪随机 */
function isDark(row: number, col: number): boolean {
  for (const [fr, fc] of FINDERS) {
    if (row >= fr && row < fr + 7 && col >= fc && col < fc + 7) {
      const lr = row - fr
      const lc = col - fc
      const border = lr === 0 || lr === 6 || lc === 0 || lc === 6
      const center = lr >= 2 && lr <= 4 && lc >= 2 && lc <= 4
      return border || center
    }
  }
  return rand() > 0.5
}
</script>

<template>
  <div class="qr">
    <template v-for="(line, row) in cells" :key="row">
      <div v-for="(dark, col) in line" :key="col" class="qr-cell" :class="{ dark }" />
    </template>
  </div>
</template>

<style scoped>
.qr {
  display: grid;
  grid-template-columns: repeat(21, 1fr);
  grid-template-rows: repeat(21, 1fr);
  width: var(--size-qr);
  height: var(--size-qr);
  padding: var(--spacing-row);
  background: var(--color-card-bg);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
}

.qr-cell {
  background: transparent;
}

.qr-cell.dark {
  background: var(--color-text-1);
}
</style>
