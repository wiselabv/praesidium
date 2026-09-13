"""Celery 任务：批量运维入口（由管理服务经消息队列/HTTP 触发）。"""

from __future__ import annotations

from praesidium_python.automation.ansible_runner import batch_command, gather_facts, run_playbook
from praesidium_python.celery_app import celery_app


@celery_app.task(name="automation.ping")
def ping() -> str:
    """探活任务：验证任务队列链路。"""
    return "pong"


@celery_app.task(name="automation.run_playbook")
def run_playbook_task(playbook: str, hosts: list[str], extra_vars: dict | None = None) -> dict:
    """执行现成 playbook。"""
    return run_playbook(playbook, hosts, extra_vars).to_dict()


@celery_app.task(name="automation.batch_command")
def batch_command_task(hosts: list[str], command: str) -> dict:
    """批量执行 shell 命令。"""
    return batch_command(hosts, command).to_dict()


@celery_app.task(name="automation.gather_facts")
def gather_facts_task(hosts: list[str]) -> dict:
    """采集目标主机事实。"""
    return gather_facts(hosts).to_dict()
