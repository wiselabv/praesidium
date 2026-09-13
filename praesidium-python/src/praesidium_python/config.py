"""服务配置：统一从环境变量读取，前缀 ``PRAESIDIUM_``。"""

from functools import lru_cache

from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    """运行配置（默认值面向本地开发）。"""

    model_config = SettingsConfigDict(
        env_prefix="PRAESIDIUM_",
        env_file=".env",
        extra="ignore",
    )

    host: str = "127.0.0.1"
    port: int = 8000

    # Celery broker 与结果存储
    redis_url: str = "redis://127.0.0.1:6379/0"
    celery_result_backend: str = "redis://127.0.0.1:6379/1"

    # 与 Java 管理服务共享的业务库（凭据经环境变量 / .env 注入）
    database_url: str = "postgresql+psycopg2://praesidium@127.0.0.1:5432/praesidium"

    # 审计事件队列（RabbitMQ，AMQP 协议；凭据经环境变量 / .env 注入）
    amqp_url: str = "amqp://127.0.0.1:5672/%2F"
    audit_exchange: str = "praesidium.audit"
    # AI 命令事件消费队列
    ai_queue: str = "praesidium.python.commands"
    ai_routing_key: str = "session.command"

    # 会话录像对象存储（MinIO，S3 兼容；密钥为真实凭据，经环境变量 / .env 注入）
    minio_endpoint: str = "192.168.10.4:9000"
    minio_access_key: str = "minio"
    minio_secret_key: str = ""
    minio_bucket: str = "praesidium-recordings"


@lru_cache
def get_settings() -> Settings:
    """获取全局唯一配置实例。"""
    return Settings()
