"""Celery 应用：承载自动化任务与 AI 异步分析任务。

启动 worker（Windows 下需 solo 池）：

    uv run celery -A praesidium_python.celery_app:celery_app worker -l info -P solo
"""

from celery import Celery

from praesidium_python.config import get_settings

celery_app = Celery(
    "praesidium",
    broker=get_settings().redis_url,
    backend=get_settings().celery_result_backend,
)

celery_app.autodiscover_tasks(
    [
        "praesidium_python.automation",
        "praesidium_python.ai",
    ]
)
