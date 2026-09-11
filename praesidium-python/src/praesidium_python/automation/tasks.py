"""Celery 任务：批量运维入口（由管理服务经消息队列触发）。"""

from praesidium_python.celery_app import celery_app


@celery_app.task(name="automation.ping")
def ping() -> str:
    """探活任务：验证任务队列链路。"""
    return "pong"


# TODO(阶段二): run_playbook / batch_command / gather_facts 等 Ansible 任务
