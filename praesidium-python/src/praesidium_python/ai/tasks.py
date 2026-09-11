"""Celery 任务：AI 分析入口。"""

from praesidium_python.celery_app import celery_app


@celery_app.task(name="ai.ping")
def ping() -> str:
    """探活任务：验证任务队列链路。"""
    return "pong"


# TODO(阶段二): detect_anomaly / rebuild_baseline 等分析任务
