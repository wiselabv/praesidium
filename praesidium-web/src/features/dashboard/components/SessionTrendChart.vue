<script setup lang="ts">
import { onMounted, onUnmounted, ref, watch } from 'vue'
import * as echarts from 'echarts/core'
import { LineChart } from 'echarts/charts'
import { GridComponent, LegendComponent, TooltipComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'

/**
 * 趋势折线图（L3，dashboard 专用，支持多系列 + 双 Y 轴）。
 *
 * 展示近 N 天会话量 / 失败登录次数等趋势（审计链路聚合）。
 * 按需注册 ECharts 模块控制体积；图表配色从 tokens.css 运行时读取，
 * 保持与全站设计语言一致。
 * TODO(总览): 接口就绪后由 /api/dashboard/session-trend 提供数据。
 */
echarts.use([LineChart, GridComponent, LegendComponent, TooltipComponent, CanvasRenderer])

/** 单条趋势线：tone 决定着色与所在 Y 轴（primary 左轴 / danger 右轴） */
interface TrendSeries {
  /** 系列名（图例与 tooltip 展示） */
  name: string
  /** 数据点（与 labels 一一对应） */
  values: number[]
  /** 着色功能色 */
  tone: 'primary' | 'danger'
}

const props = withDefaults(
  defineProps<{
    labels: string[]
    series: TrendSeries[]
  }>(),
  { labels: () => [], series: () => [] },
)

const container = ref<HTMLDivElement | null>(null)
let chart: echarts.ECharts | null = null

/** 系列功能色 → token（图表 option 同样遵守无硬编码规范） */
const TONE_TOKENS: Record<TrendSeries['tone'], { line: string; soft: string }> = {
  primary: { line: '--color-primary', soft: '--primary-3' },
  danger: { line: '--color-danger', soft: 'rgba(255, 153, 148, 0.55)' },
}

/** 读取 CSS 变量 token */
function token(name: string): string {
  return getComputedStyle(document.documentElement).getPropertyValue(name).trim()
}

/** 渲染/更新图表 */
function render() {
  if (!container.value) return
  if (!chart) chart = echarts.init(container.value)
  // 空数据跳过渲染：ECharts 6 在 yAxis 为空数组时轴构建器缺失会抛错，数据到达后 watch 重渲染
  if (props.labels.length === 0 || props.series.length === 0) {
    chart.clear()
    return
  }
  const multiAxis = props.series.length > 1
  chart.setOption({
    grid: { outerBounds: { left: 8, right: 16, top: 40, bottom: 8 } },
    tooltip: {
      trigger: 'axis',
      backgroundColor: token('--color-card-bg'),
      borderColor: token('--color-border'),
      textStyle: { color: token('--color-text-1'), fontSize: 12 },
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: props.labels,
      axisLine: { lineStyle: { color: token('--color-border') } },
      axisTick: { show: false },
      axisLabel: { color: token('--color-text-3'), fontSize: 12 },
    },
    yAxis: props.series.map((_, index) => ({
      type: 'value',
      minInterval: 1,
      position: index === 0 ? 'left' : 'right',
      splitLine: index === 0 ? { lineStyle: { color: token('--color-border') } } : { show: false },
      axisLabel: { color: token('--color-text-3'), fontSize: 12 },
    })),
    series: props.series.map((series, index) => {
      const colors = TONE_TOKENS[series.tone]
      return {
        name: series.name,
        type: 'line',
        yAxisIndex: index,
        data: series.values,
        smooth: true,
        symbol: 'circle',
        symbolSize: 5,
        lineStyle: { width: 3, color: token(colors.line) },
        itemStyle: { color: token(colors.line) },
        areaStyle:
          index === 0
            ? {
                color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                  { offset: 0, color: token(colors.soft) },
                  { offset: 1, color: 'rgba(173, 200, 255, 0)' },
                ]),
              }
            : undefined,
      }
    }),
    // 单系列时隐藏图例（标题已说明含义）
    legend: multiAxis
      ? {
          top: 0,
          icon: 'circle',
          itemWidth: 8,
          itemHeight: 8,
          textStyle: { color: token('--color-text-2'), fontSize: 12 },
        }
      : { show: false },
  })
}

/** 容器尺寸变化自适应 */
let observer: ResizeObserver | null = null

onMounted(() => {
  render()
  observer = new ResizeObserver(() => chart?.resize())
  if (container.value) observer.observe(container.value)
})

watch(() => props.series, render, { deep: true })

onUnmounted(() => {
  observer?.disconnect()
  chart?.dispose()
  chart = null
})
</script>

<template>
  <div ref="container" class="trend-chart" />
</template>

<style scoped>
.trend-chart {
  width: 100%;
  height: 260px;
}
</style>
