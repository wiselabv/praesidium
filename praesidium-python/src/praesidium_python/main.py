"""FastAPI 入口：统一暴露自动化与 AI 两组接口，并启动审计事件消费线程。"""

from __future__ import annotations

from contextlib import asynccontextmanager

import uvicorn
from fastapi import FastAPI

from praesidium_python import __version__
from praesidium_python.ai import router as ai_router
from praesidium_python.ai.tasks import detect_command
from praesidium_python.automation import router as automation_router
from praesidium_python.config import get_settings
from praesidium_python.messaging import RabbitConsumer


def _start_consumers() -> RabbitConsumer | None:
    """启动命令事件消费者（RabbitMQ 不可用时不阻断服务启动）。"""
    settings = get_settings()
    try:
        consumer = RabbitConsumer(
            amqp_url=settings.amqp_url,
            exchange=settings.audit_exchange,
            queue=settings.ai_queue,
            routing_key=settings.ai_routing_key,
            handler=lambda event: detect_command.delay(event),
        )
        consumer.start()
        return consumer
    except Exception:
        return None


@asynccontextmanager
async def lifespan(_app: FastAPI):
    consumer = _start_consumers()
    yield
    if consumer is not None:
        consumer.stop()


app = FastAPI(title="Praesidium Python Service", version=__version__, lifespan=lifespan)

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
