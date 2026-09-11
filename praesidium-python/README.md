# praesidium-python

Praesidium 的自动化运维与 AI 分析服务（FastAPI + Celery）。

- `praesidium_python.automation`：Ansible 批量运维、定时巡检
- `praesidium_python.ai`：异常行为检测、风险评分、告警

## 开发

```bash
uv sync                       # 安装依赖
uv run praesidium-python      # 启动 API（http://127.0.0.1:8000，接口文档 /docs）
uv run celery -A praesidium_python.celery_app:celery_app worker -l info -P solo   # 任务 worker
uv run pytest                 # 测试
```

配置通过环境变量注入，前缀 `PRAESIDIUM_`（如 `PRAESIDIUM_REDIS_URL`、`PRAESIDIUM_DATABASE_URL`）。
