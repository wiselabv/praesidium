"""AI 分析：会话行为异常检测、风险评分。

对外接口由 ``main.py`` 以 ``/ai`` 前缀挂载。
"""

from __future__ import annotations

from fastapi import APIRouter, Query

from praesidium_python.ai.analyzer import list_anomalies

router = APIRouter()


@router.get("/health")
def health() -> dict[str, str]:
    """模块探活。"""
    return {"status": "ok", "module": "ai"}


@router.get("/anomalies")
def anomalies(
    limit: int = Query(default=50, ge=1, le=200),
    status: str | None = Query(default=None, pattern="^(new|acked|ignored)$"),
) -> dict:
    """查询异常记录（异常检测结果，写库方为 Celery worker）。"""
    return {"code": 0, "message": "ok", "data": list_anomalies(limit=limit, status=status)}
