"""服务骨架的冒烟测试。"""

from fastapi.testclient import TestClient

from praesidium_python.main import app

client = TestClient(app)


def test_health() -> None:
    resp = client.get("/health")
    assert resp.status_code == 200
    body = resp.json()
    assert body["status"] == "ok"
    assert body["service"] == "praesidium-python"


def test_module_health() -> None:
    assert client.get("/automation/health").json()["module"] == "automation"
    assert client.get("/ai/health").json()["module"] == "ai"
