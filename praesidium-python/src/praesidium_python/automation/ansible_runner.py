"""Ansible 执行封装（ansible-runner）。

支持三种任务形态：
- run_playbook：执行现成 playbook
- batch_command：ad-hoc 命令（内部生成临时 playbook 走 shell 模块）
- gather_facts：采集目标主机事实
"""

from __future__ import annotations

import json
import sys
import tempfile
from dataclasses import dataclass, field
from pathlib import Path

# ansible-runner 依赖 fcntl（Unix-only），Windows 开发机上不可用；
# 生产控制节点为 Linux，平台守卫保证 Windows 上 import 链不崩。
_ANSIBLE_AVAILABLE = sys.platform != "win32"
if _ANSIBLE_AVAILABLE:
    import ansible_runner


@dataclass
class RunResult:
    """Ansible 执行结果（可 JSON 序列化）。"""

    status: str
    rc: int
    stdout: str
    facts: dict = field(default_factory=dict)

    def to_dict(self) -> dict:
        return {
            "status": self.status,
            "rc": self.rc,
            "stdout": self.stdout[-4096:],
            "facts": self.facts,
        }


def _inventory(hosts: list[str]) -> str:
    """生成 INI 格式 inventory 文本。"""
    lines = ["[all]"] + hosts + ["[all:vars]", "ansible_ssh_common_args='-o StrictHostKeyChecking=no'"]
    return "\n".join(lines) + "\n"


def _run_playbook_impl(playbook: str, hosts: list[str], extra_vars: dict | None) -> RunResult:
    """执行 playbook（共享实现）。"""
    if not _ANSIBLE_AVAILABLE:
        return RunResult(
            status="failed",
            rc=127,
            stdout="ansible-runner 仅支持 Linux 控制节点（当前平台不可用）",
        )
    extra_vars = extra_vars or {}
    with tempfile.TemporaryDirectory(prefix="praesidium-ansible-") as tmp:
        tmp_path = Path(tmp)
        result = ansible_runner.run(
            private_data_dir=str(tmp_path),
            playbook=playbook,
            inventory=_inventory(hosts),
            extravars=extra_vars,
            quiet=True,
        )
    return RunResult(
        status=result.status,
        rc=result.rc,
        stdout=result.stdout.read() if result.stdout else "",
    )


def run_playbook(playbook: str, hosts: list[str], extra_vars: dict | None = None) -> RunResult:
    """执行现成 playbook 文件。"""
    if not Path(playbook).exists():
        return RunResult(status="failed", rc=127, stdout=f"playbook 不存在: {playbook}")
    return _run_playbook_impl(playbook, hosts, extra_vars)


def batch_command(hosts: list[str], command: str) -> RunResult:
    """批量执行 shell 命令（内部生成临时 playbook）。"""
    playbook_yaml = f"""
- name: praesidium batch command
  hosts: all
  gather_facts: false
  tasks:
    - name: execute command
      ansible.builtin.shell: {json.dumps(command)}
      register: _cmd_out
    - name: print output
      ansible.builtin.debug:
        var: _cmd_out.stdout_lines
"""
    with tempfile.TemporaryDirectory(prefix="praesidium-ansible-") as tmp:
        playbook_path = Path(tmp) / "batch.yml"
        playbook_path.write_text(playbook_yaml, encoding="utf-8")
        result = _run_playbook_impl(str(playbook_path), hosts, None)
    return result


def gather_facts(hosts: list[str]) -> RunResult:
    """采集主机事实（内部生成临时 playbook）。"""
    playbook_yaml = """
- name: praesidium gather facts
  hosts: all
  gather_facts: true
  tasks: []
"""
    with tempfile.TemporaryDirectory(prefix="praesidium-ansible-") as tmp:
        playbook_path = Path(tmp) / "facts.yml"
        playbook_path.write_text(playbook_yaml, encoding="utf-8")
        return _run_playbook_impl(str(playbook_path), hosts, None)
