"""FastAPI 入口：统一暴露自动化与 AI 两组接口。"""

import uvicorn
from fastapi import FastAPI

from praesidium_python import __version__
from praesidium_python.ai import router as ai_router
from praesidium_python.automation import router as automation_router
from praesidium_python.config import get_settings

app = FastAPI(title="Praesidium Python Service", version=__version__)

app.include_router(automation_router, prefix="/automation", tags=["automation"])
app.include_router(ai_router, prefix="/ai", tags=["ai"])


@app.get("/health")
def health() -> dict[str, str]:
    """探活接口。"""
    return {"status": "ok", "service": "praesidium-python", "version": __version__}


def run() -> None:
    """命令行入口（``uv run praesidium-python``）。"""
    settings = get_settings()
    uvicorn.run("praesidium_python.main:app", host=settings.host, port=settings.port)
