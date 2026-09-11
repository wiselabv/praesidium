"""Ansible 执行封装。

计划：包装 ansible-runner，支持 playbook / ad-hoc 命令两种模式，
执行输出序列化为审计事件写入消息队列，供管理服务落库。
"""

# TODO(阶段二): 实现 run_playbook(playbook, inventory, extra_vars) -> RunResult
