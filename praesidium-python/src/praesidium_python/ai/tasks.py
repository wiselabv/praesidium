"""Celery 任务：AI 分析入口。

消费线程把命令事件转交 Celery worker 异步检测，
检测结果写回共享业务库 ``audit_anomalies``。
"""

from __future__ import annotations

from praesidium_python.ai.detector import CommandDetector
from praesidium_python.celery_app import celery_app
from praesidium_python.db import insert_anomaly

# 全局检测器（worker 进程内单例；历史积累用于无监督基线）
_detector = CommandDetector()


@celery_app.task(name="ai.ping")
def ping() -> str:
    """探活任务：验证任务队列链路。"""
    return "pong"


@celery_app.task(name="ai.detect_command")
def detect_command(event: dict) -> dict | None:
    """检测命令事件；异常时落库并返回结果。"""
    # Rust 网关发布的是扁平 snake_case 信封（AuditEnvelope + serde flatten）
    user_id = int(event.get("user_id", 0))
    command = str(event.get("command", "")).strip()
    if not command or user_id <= 0:
        return None

    result = _detector.detect(user_id, command)
    if result.is_anomaly:
        insert_anomaly(
            user_id=user_id,
            session_id=int(event.get("session_id", 0)),
            asset_id=int(event.get("asset_id", 0)),
            account=str(event.get("account", "")),
            command=result.command,
            anomaly_type=result.anomaly_type,
            score=result.score,
            detail=result.detail,
        )
    else:
        # 正常命令进入历史，用于训练基线
        _detector.observe(user_id, command)
    return result.to_dict()
