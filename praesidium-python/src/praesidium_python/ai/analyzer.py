"""日志分析：高危命令统计、异常记录查询。

消费线程聚合实时事件；查询侧从共享业务库读取异常记录。
"""

from __future__ import annotations

from typing import Any

from sqlalchemy import text

from praesidium_python.db import get_engine


def list_anomalies(limit: int = 50, status: str | None = None) -> list[dict[str, Any]]:
    """查询异常记录（供管理服务/前端展示）。"""
    sql = "SELECT * FROM audit_anomalies"
    params: dict[str, Any] = {}
    if status:
        sql += " WHERE status = :status"
        params["status"] = status
    sql += " ORDER BY created_at DESC LIMIT :limit"
    params["limit"] = limit
    with get_engine().connect() as conn:
        rows = conn.execute(text(sql), params).mappings().all()
    return [dict(row) for row in rows]
