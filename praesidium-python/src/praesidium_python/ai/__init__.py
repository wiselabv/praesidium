"""AI 分析：会话行为异常检测、风险评分。

对外接口由 ``main.py`` 以 ``/ai`` 前缀挂载。
"""

from fastapi import APIRouter

router = APIRouter()


@router.get("/health")
def health() -> dict[str, str]:
    """模块探活。"""
    return {"status": "ok", "module": "ai"}
