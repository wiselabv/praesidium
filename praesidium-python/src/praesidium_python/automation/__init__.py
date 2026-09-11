"""自动化运维：Ansible 批量执行、定时巡检。

对外接口由 ``main.py`` 以 ``/automation`` 前缀挂载。
"""

from fastapi import APIRouter

router = APIRouter()


@router.get("/health")
def health() -> dict[str, str]:
    """模块探活。"""
    return {"status": "ok", "module": "automation"}
