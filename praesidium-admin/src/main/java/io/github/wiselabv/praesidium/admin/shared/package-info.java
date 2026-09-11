/**
 * 共享内核（shared）：跨上下文复用的通用设施。
 * <ul>
 *   <li>{@code domain}：通用值对象（Id、时间）、领域事件基类</li>
 *   <li>{@code infrastructure}：时钟、序列化、事件总线等通用实现</li>
 *   <li>{@code api}：统一响应（code/message/data）、分页、全局异常处理</li>
 * </ul>
 * 上下文之间复用代码只允许放这里，不允许上下文直接互相依赖。
 */
package io.github.wiselabv.praesidium.admin.shared;
