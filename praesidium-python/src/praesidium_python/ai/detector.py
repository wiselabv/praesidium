"""异常行为检测：规则引擎（高危命令）+ IsolationForest（命令序列无监督基线）。

冷启动策略：历史命令不足时仅规则引擎生效；积累足够样本后自动训练
IsolationForest（词袋向量），对偏离用户自身习惯的命令打异常分。
"""

from __future__ import annotations

import re
from dataclasses import dataclass, field
from typing import Any

import numpy as np
from sklearn.ensemble import IsolationForest
from sklearn.feature_extraction.text import CountVectorizer

# 高危命令模式（规则引擎第一道闸）
_RISKY_PATTERNS: list[tuple[str, str]] = [
    (r"\brm\s+(-[a-zA-Z]*\s+)*(-[a-zA-Z]*r[a-zA-Z]*f|-f[a-zA-Z]*r)\b", "高危删除"),
    (r"\bshutdown\b", "关机/重启"),
    (r"\breboot\b", "关机/重启"),
    (r"\bhalt\b", "关机/重启"),
    (r"\bpoweroff\b", "关机/重启"),
    (r"\bdrop\s+(table|database)\b", "删除数据库对象"),
    (r"\btruncate\s+table\b", "清空数据表"),
    (r"\bmkfs\b", "格式化文件系统"),
    (r"\bdd\s+if=", "磁盘覆写"),
    (r"\bchmod\s+777\b", "开放全部权限"),
    (r"\buseradd\b", "新建系统用户"),
    (r"\buserdel\b", "删除系统用户"),
    (r"\bpasswd\b", "修改口令"),
    (r"\bcrontab\b.*-e", "修改定时任务"),
    (r"\biptables\b.*(-F|--flush)", "清空防火墙规则"),
    (r">\s*/dev/sda\b", "写块设备"),
    (r"\bcurl\b.*\|\s*(ba)?sh\b", "远程脚本执行"),
    (r"\bwget\b.*\|\s*(ba)?sh\b", "远程脚本执行"),
    (r"\bexport\s+.*PASS", "导出敏感环境变量"),
]

_MAX_COMMAND_LEN = 256

# 无监督模型训练阈值：单用户历史命令条数
_MIN_TRAIN_SAMPLES = 20
_ANOMALY_THRESHOLD = 0.55


@dataclass
class DetectionResult:
    """检测结果。"""

    command: str
    is_anomaly: bool
    anomaly_type: str = ""
    score: float = 0.0
    detail: str = ""

    def to_dict(self) -> dict[str, Any]:
        return {
            "command": self.command,
            "is_anomaly": self.is_anomaly,
            "anomaly_type": self.anomaly_type,
            "score": round(self.score, 4),
            "detail": self.detail,
        }


@dataclass
class CommandDetector:
    """规则 + 无监督混合检测器（每用户独立基线）。"""

    # 用户命令历史（训练集），user_id -> commands
    history: dict[int, list[str]] = field(default_factory=dict)
    # 训练好的模型（词袋向量化器 + IsolationForest）
    _models: dict[int, tuple[CountVectorizer, IsolationForest]] = field(default_factory=dict)

    def observe(self, user_id: int, command: str) -> None:
        """把正常命令喂入历史（消费事件时先检测后观察）。"""
        self.history.setdefault(user_id, []).append(command)
        if len(self.history[user_id]) >= _MIN_TRAIN_SAMPLES and user_id not in self._models:
            self._train(user_id)

    def _train(self, user_id: int) -> None:
        commands = self.history[user_id]
        vectorizer = CountVectorizer(analyzer="char_wb", ngram_range=(2, 4), min_df=1)
        try:
            x = vectorizer.fit_transform(commands).toarray()
        except ValueError:
            return  # 样本单一无法训练
        if x.shape[0] < _MIN_TRAIN_SAMPLES:
            return
        model = IsolationForest(n_estimators=100, contamination=0.1, random_state=42)
        model.fit(x)
        self._models[user_id] = (vectorizer, model)

    def detect(self, user_id: int, command: str) -> DetectionResult:
        """检测单条命令：先规则后模型。"""
        command = command.strip()[:_MAX_COMMAND_LEN]
        if not command:
            return DetectionResult(command=command, is_anomaly=False)

        # 1. 规则引擎
        for pattern, label in _RISKY_PATTERNS:
            if re.search(pattern, command, re.IGNORECASE):
                return DetectionResult(
                    command=command,
                    is_anomaly=True,
                    anomaly_type="rule",
                    score=0.99,
                    detail=f"命中高危规则：{label}",
                )

        # 2. IsolationForest（模型未训练则跳过）
        model_entry = self._models.get(user_id)
        if model_entry is not None:
            vectorizer, model = model_entry
            try:
                vec = vectorizer.transform([command]).toarray()
            except ValueError:
                vec = np.zeros((1, vectorizer.get_feature_names_out().shape[0]))
            decision = model.decision_function(vec)[0]
            # decision_function 越小越异常；归一化到 [0,1] 分数
            score = float(1.0 / (1.0 + np.exp(decision)))
            if score >= _ANOMALY_THRESHOLD:
                return DetectionResult(
                    command=command,
                    is_anomaly=True,
                    anomaly_type="ml",
                    score=score,
                    detail="偏离该用户命令行为基线",
                )

        return DetectionResult(command=command, is_anomaly=False)
