"""异常行为检测。

计划：基于审计事件流（命令、会话元数据）构建用户行为基线，
使用 scikit-learn IsolationForest 等算法输出异常分。
"""

# TODO(阶段二): 实现基线建模与 detector.detect(event) -> AnomalyScore
