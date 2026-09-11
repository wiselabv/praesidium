# features/ 目录约定（套娃组件 + 统一自由设置）

完整规范见仓库根目录 `docs/ARCHITECTURE.md`，这里只留速查。

## 套娃四层

```
L4 页面      features/xxx/views/        XxxView.vue        最外层，越薄越好
L3 业务组件  features/xxx/components/   AssetTable.vue     带业务语义
L2 组合组件  components/common/         PmTable/PmForm     无业务，通用
L1 原子组件  components/base/           PmButton/PmInput   包一层 Arco，统一风格
```

- 只准引用自己这层或更内层；L3 之间禁止互相引用。
- 两个 feature 都要用的组件 → 下沉到 `components/common/` 或 `components/base/`。
- 业务代码不许直接用 Arco 裸组件，统一样式只改 L1。

## 统一自由设置

- CRUD 页面用 `config.ts` 描述（搜索栏/表格列/表单字段/动作），页面代码不重复。
- 新页面 = 复制 `config.ts` 改字段；忘了代码看配置就知道页面有什么。

## 每个 feature 的内部结构

```
features/<名称>/            # 目录名镜像后端限界上下文（identity/asset/access/audit）
├── views/                  # L4 页面
├── components/             # L3 业务组件
├── stores/                 # 本 feature 私有状态
├── api/                    # 本 feature 的 API 调用
└── types.ts                # 本 feature 的类型定义
```

跨 feature 共享的状态放 `src/stores/`（如 auth）。
