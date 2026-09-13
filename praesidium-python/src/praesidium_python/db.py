"""共享业务库访问：AI 异常检测结果落库（与 Java 管理服务共用 PostgreSQL）。"""

from __future__ import annotations

from functools import lru_cache

from sqlalchemy import create_engine
from sqlalchemy.engine import Engine

from praesidium_python.config import get_settings


@lru_cache
def get_engine() -> Engine:
    """全局唯一 SQLAlchemy engine（连接池）。"""
    return create_engine(get_settings().database_url, pool_pre_ping=True, pool_size=2, max_overflow=3)


def insert_anomaly(
    *,
    user_id: int,
    session_id: int,
    asset_id: int,
    account: str,
    command: str,
    anomaly_type: str,
    score: float,
    detail: str,
) -> None:
    """写入一条异常记录（audit_anomalies 表由 Java 侧 Flyway V6 创建）。"""
    from sqlalchemy import text

    with get_engine().begin() as conn:
        conn.execute(
            text(
                """
                INSERT INTO audit_anomalies
                    (user_id, session_id, asset_id, account, command, anomaly_type, score, detail, status, created_at)
                VALUES
                    (:user_id, :session_id, :asset_id, :account, :command, :anomaly_type, :score, :detail, 'new', now())
                """
            ),
            {
                "user_id": user_id,
                "session_id": session_id,
                "asset_id": asset_id,
                "account": account,
                "command": command,
                "anomaly_type": anomaly_type,
                "score": score,
                "detail": detail,
            },
        )
