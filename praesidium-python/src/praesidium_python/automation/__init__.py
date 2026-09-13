"""自动化运维：Ansible 批量执行、定时巡检。

对外接口由 ``main.py`` 以 ``/automation`` 前缀挂载。
"""

from __future__ import annotations

from typing import Literal

from celery.result import AsyncResult
from fastapi import APIRouter
from pydantic import BaseModel, Field

from praesidium_python.automation.tasks import (
    batch_command_task,
    gather_facts_task,
    run_playbook_task,
)
from praesidium_python.celery_app import celery_app

router = APIRouter()

JobType = Literal["playbook", "batch_command", "gather_facts"]


class JobRequest(BaseModel):
    """任务提交请求。"""

    type: JobType
    hosts: list[str] = Field(min_length=1, max_length=200)
    playbook: str | None = None
    command: str | None = None
    extra_vars: dict | None = None


@router.get("/health")
def health() -> dict[str, str]:
    """模块探活。"""
    return {"status": "ok", "module": "automation"}


@router.post("/jobs")
def submit_job(request: JobRequest) -> dict:
    """提交异步任务，返回 Celery task_id。"""
    if request.type == "playbook":
        if not request.playbook:
            return {"code": 400, "message": "playbook 任务必须提供 playbook 路径", "data": None}
        task = run_playbook_task.delay(request.playbook, request.hosts, request.extra_vars)
    elif request.type == "batch_command":
        if not request.command:
            return {"code": 400, "message": "batch_command 任务必须提供 command", "data": None}
        task = batch_command_task.delay(request.hosts, request.command)
    else:
        task = gather_facts_task.delay(request.hosts)
    return {"code": 0, "message": "ok", "data": {"taskId": task.id}}


@router.get("/jobs/{task_id}")
def job_status(task_id: str) -> dict:
    """查询任务状态与结果（Celery 结果后端）。"""
    result = AsyncResult(task_id, app=celery_app)
    payload: dict = {"taskId": task_id, "status": result.state}
    if result.ready():
        payload["result"] = result.result if result.successful() else str(result.result)
    return {"code": 0, "message": "ok", "data": payload}
