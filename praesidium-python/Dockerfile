FROM python:3.13-slim

# 使用 uv 管理依赖（直接拷贝官方静态构建的 uv 二进制）
COPY --from=ghcr.io/astral-sh/uv:latest /uv /uvx /bin/

WORKDIR /app
ENV UV_COMPILE_BYTECODE=1 \
    UV_LINK_MODE=copy

# 先拷贝依赖清单，最大化利用构建缓存
COPY pyproject.toml uv.lock ./
RUN uv sync --frozen --no-dev

COPY . .

EXPOSE 8000
CMD ["uv", "run", "--no-dev", "uvicorn", "praesidium_python.main:app", "--host", "0.0.0.0", "--port", "8000"]
