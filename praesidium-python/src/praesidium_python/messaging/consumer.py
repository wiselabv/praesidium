"""消息消费抽象层：Broker 可替换（默认 RabbitMQ/pika）。

消费 Rust 核心经交换机 ``praesidium.audit`` 发布的审计事件：
队列 ``praesidium.python.commands`` 绑定 ``session.command``（命令事件，AI 检测入口）。
"""

from __future__ import annotations

import json
import threading
from abc import ABC, abstractmethod
from typing import Any, Callable


class EventConsumer(ABC):
    """事件消费抽象：替换 Broker 时仅需换实现。"""

    @abstractmethod
    def start(self) -> None:
        """启动消费循环（阻塞线程内运行）。"""

    @abstractmethod
    def stop(self) -> None:
        """优雅停止。"""


class RabbitConsumer(EventConsumer):
    """RabbitMQ 实现（pika 阻塞连接，独立线程消费）。"""

    def __init__(
        self,
        amqp_url: str,
        exchange: str,
        queue: str,
        routing_key: str,
        handler: Callable[[dict[str, Any]], None],
    ) -> None:
        self._amqp_url = amqp_url
        self._exchange = exchange
        self._queue = queue
        self._routing_key = routing_key
        self._handler = handler
        self._thread: threading.Thread | None = None
        self._stopping = threading.Event()

    def start(self) -> None:
        self._thread = threading.Thread(target=self._run, name="rabbit-consumer", daemon=True)
        self._thread.start()

    def stop(self) -> None:
        self._stopping.set()

    def _run(self) -> None:
        import pika

        while not self._stopping.is_set():
            try:
                params = pika.URLParameters(self._amqp_url)
                params.heartbeat = 60
                connection = pika.BlockingConnection(params)
                channel = connection.channel()
                channel.exchange_declare(exchange=self._exchange, exchange_type="topic", durable=True)
                channel.queue_declare(queue=self._queue, durable=True)
                channel.queue_bind(
                    queue=self._queue, exchange=self._exchange, routing_key=self._routing_key
                )
                channel.basic_qos(prefetch_count=10)

                def on_message(
                    ch: pika.adapters.blocking_connection.BlockingChannel,
                    method: pika.spec.Basic.Deliver,
                    _properties: Any,
                    body: bytes,
                ) -> None:
                    try:
                        payload = json.loads(body.decode("utf-8"))
                        self._handler(payload)
                    except Exception:
                        # 单条事件失败不中断消费循环
                        pass
                    finally:
                        ch.basic_ack(delivery_tag=method.delivery_tag)

                channel.basic_consume(queue=self._queue, on_message_callback=on_message)
                channel.start_consuming()
            except Exception:
                if self._stopping.is_set():
                    break
                # 连接失败 5s 后重试
                self._stopping.wait(timeout=5)
