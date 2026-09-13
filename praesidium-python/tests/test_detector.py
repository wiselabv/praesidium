"""AI 异常检测器单元测试：规则引擎 + 冷启动边界。"""

from praesidium_python.ai.detector import CommandDetector

DETECTOR = CommandDetector()


def test_risky_command_hits_rule() -> None:
    result = DETECTOR.detect(1, "rm -rf /var/log/app")
    assert result.is_anomaly
    assert result.anomaly_type == "rule"
    assert result.score == 0.99
    assert "高危删除" in result.detail


def test_shutdown_hits_rule() -> None:
    result = DETECTOR.detect(1, "shutdown -h now")
    assert result.is_anomaly
    assert result.anomaly_type == "rule"


def test_normal_command_passes() -> None:
    result = DETECTOR.detect(1, "ls -la /var/log")
    assert not result.is_anomaly
    assert result.score == 0.0


def test_blank_command_passes() -> None:
    result = DETECTOR.detect(1, "   ")
    assert not result.is_anomaly


def test_command_truncated_to_max_len() -> None:
    long_cmd = "echo " + "a" * 300
    result = DETECTOR.detect(1, long_cmd)
    assert len(result.command) == 256


def test_cold_start_no_model_no_ml_false_positive() -> None:
    """冷启动（无训练模型）时普通命令不触发 ML 分支。"""
    detector = CommandDetector()
    result = detector.detect(42, "systemctl status nginx")
    assert not result.is_anomaly


def test_observe_accumulates_history() -> None:
    detector = CommandDetector()
    for i in range(25):
        detector.observe(7, f"echo hello {i}")
    assert len(detector.history[7]) == 25
    # 样本充足后模型已训练，检测仍应通过（同类命令）
    result = detector.detect(7, "echo hello 999")
    assert not result.is_anomaly
