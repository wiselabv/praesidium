<script setup lang="ts">
import { onMounted, onUnmounted, ref, watch } from 'vue'
import * as echarts from 'echarts/core'
import { PieChart } from 'echarts/charts'
import { LegendComponent, TooltipComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'

/**
 * 资产协议分布环形图（L3，dashboard 专用）。
 *
 * 展示资产按接入协议（SSH/RDP/VNC/其他）的占比（管理链路聚合）。
 * 图表配色从 tokens.css 运行时读取，保持与全站设计语言一致。
 * TODO(总览): 接口就绪后由 /api/dashboard/protocol-dist 提供数据。
 */
echarts.use([PieChart, LegendComponent, TooltipComponent, CanvasRenderer])

interface ProtocolSlice {
  /** 协议名（SSH / RDP / VNC / 其他） */
  name: string
  /** 资产数量 */
  value: number
}

const props = withDefaults(
  defineProps<{
    slices: ProtocolSlice[]
  }>(),
  { slices: () => [] },
)

const container = ref<HTMLDivElement | null>(null)
let chart: echarts.ECharts | null = null

/** 读取 CSS 变量 token（图表 option 同样遵守无硬编码规范） */
function token(name: string): string {
  return getComputedStyle(document.documentElement).getPropertyValue(name).trim()
}

/** 协议切片着色：品牌蓝 / 成功绿 / 告警橙 / 信息青 */
const SLICE_COLORS = ['--color-primary', '--color-success', '--color-warning', '--color-info']

/** 渲染/更新图表 */
function render() {
  if (!container.value) return
  if (!chart) chart = echarts.init(container.value)
  // 空数据防护：无切片时清空画布，避免空 series 触发渲染异常
  if (props.slices.length === 0) {
    chart.clear()
    return
  }
  chart.setOption({
    tooltip: {
      trigger: 'item',
      formatter: '{b}：{c} 台（{d}%）',
      backgroundColor: token('--color-card-bg'),
      borderColor: token('--color-border'),
      textStyle: { color: token('--color-text-1'), fontSize: 12 },
    },
    legend: {
      bottom: 0,
      icon: 'circle',
      itemWidth: 8,
      itemHeight: 8,
      itemGap: 16,
      textStyle: { color: token('--color-text-2'), fontSize: 12 },
    },
    series: [
      {
        type: 'pie',
        radius: ['55%', '78%'],
        center: ['50%', '42%'],
        avoidLabelOverlap: true,
        label: { show: false },
        emphasis: {
          label: {
            show: true,
            fontSize: 14,
            fontWeight: 600,
            color: token('--color-text-1'),
          },
        },
        itemStyle: { borderColor: token('--color-card-bg'), borderWidth: 2 },
        data: props.slices.map((slice, index) => ({
          ...slice,
          itemStyle: { color: token(SLICE_COLORS[index] ?? '--color-primary') },
        })),
      },
    ],
  })
}

/** 容器尺寸变化自适应 */
let observer: ResizeObserver | null = null

onMounted(() => {
  render()
  observer = new ResizeObserver(() => chart?.resize())
  if (container.value) observer.observe(container.value)
})

watch(() => props.slices, render, { deep: true })

onUnmounted(() => {
  observer?.disconnect()
  chart?.dispose()
  chart = null
})
</script>

<template>
  <div ref="container" class="protocol-chart" />
</template>

<style scoped>
.protocol-chart {
  width: 100%;
  height: 260px;
}
</style>
