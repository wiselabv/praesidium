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

    # 与 Java 管理服务共享的业务库
    database_url: str = (
        "postgresql+psycopg2://praesidium:praesidium@127.0.0.1:5432/praesidium"
    )


@lru_cache
def get_settings() -> Settings:
    """获取全局唯一配置实例。"""
    return Settings()
