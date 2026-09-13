"""Ansible 执行封装单元测试：参数校验与 inventory 生成。"""

from praesidium_python.automation.ansible_runner import _inventory, run_playbook


def test_inventory_format() -> None:
    text = _inventory(["192.168.1.10", "192.168.1.11"])
    assert "[all]" in text
    assert "192.168.1.10" in text
    assert "StrictHostKeyChecking=no" in text


def test_missing_playbook_returns_failed() -> None:
    result = run_playbook("D:/nonexistent/playbook.yml", ["localhost"])
    assert result.status == "failed"
    assert result.rc == 127
    assert "playbook 不存在" in result.stdout


def test_result_to_dict_truncates_stdout() -> None:
    from praesidium_python.automation.ansible_runner import RunResult

    result = RunResult(status="success", rc=0, stdout="x" * 5000)
    data = result.to_dict()
    assert len(data["stdout"]) == 4096
    assert data["rc"] == 0
