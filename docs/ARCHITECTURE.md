# Praesidium 架构与代码风格

> **核心原则：结构即文档。**
> 目录名、包名、文件名直接说业务语言，即使忘了代码，看到结构就能看出系统在做什么。

---

## 1. 总体布局

| 模块 | 语言 | 职责 |
|------|------|------|
| `praesidium-core` | Rust | 核心代理：SSH/RDP 终端代理、命令审计、会话录制、策略执行 |
| `praesidium-admin` | Java | 管理面：用户/资产/授权、RBAC/ABAC、审批流、SSO（DDD 分层） |
| `praesidium-python` | Python | 自动化（Ansible）与 AI 异常检测（automation/ai 双子包） |
| `praesidium-web` | Vue 3 + TS | 前端：套娃组件 + 统一自由设置 |

通信边界（已定，不重议）：
- 管理操作走 Java HTTP（前端 `/api` → 8080）
- 终端流量直连 Rust WebSocket（前端 `/ws` → 8081），不经过 Java
- 审计事件异步走消息队列，Python 消费做 AI
- Java 签发 JWT，Rust 验签

---

## 2. 后端代码风格：DDD 分层（Java）

### 2.1 为什么 DDD 而不是 MVC

- MVC 的退化路径：业务逻辑"无家可归"，逐渐堆进 Service 层；项目变大后 Controller/Service 全挤在一起，结构看不清。
- Praesidium 领域天然复杂：身份、资产、授权策略、审批流、审计，是**多个清晰的限界上下文**，不是简单 CRUD。
- DDD 的包名就是业务语言：忘了代码，看包名就懂系统在做什么。
- 开源项目：贡献者从目录结构就能找到修改点，不需要读完全部代码。

### 2.2 限界上下文（Bounded Context）

```
io.github.wiselabv.praesidium.admin
├── shared/        # 共享内核：通用值对象、领域事件基类、统一响应、分页
├── identity/      # 身份域：用户、角色、认证、SSO、JWT
├── asset/         # 资产域：主机、账号、协议
├── access/        # 访问控制域：授权策略（RBAC/ABAC）、审批流
└── audit/         # 审计域：会话记录、命令回放
```

以后需要新模块（如 `session` 在线会话管理、`system` 系统设置）时，**照搬同一个套娃结构**即可。

### 2.3 每个上下文内部的四层（套娃）

每个上下文内部永远是同样的四层，任何上下文都不例外：

```
identity/
├── domain/           # 领域层：实体、值对象、领域服务、仓储接口
│   ├── model/        #   User（实体）、Role（值对象/实体）
│   └── service/      #   领域服务（跨实体的业务规则）
├── application/      # 应用层：用例编排、DTO、应用服务
├── infrastructure/   # 基础设施层：仓储实现、持久化映射、外部集成
└── api/              # 接口层：Controller、请求/响应 DTO
```

**依赖规则（只向内，不向外）：**

```
api → application → domain ← infrastructure
```

- `domain` 不依赖任何层，也不依赖任何框架（纯业务语言）。
- `application` 只依赖 `domain`，一个方法一个用例。
- `infrastructure` 实现 `domain` 里的仓储接口（依赖倒置）。
- `api` 只依赖 `application`，薄薄一层，只做参数转换和响应封装。
- **跨上下文**：只能通过对方的 `application` 层接口或领域事件通信，禁止直接摸对方表/实体。

### 2.4 命名约定

| 东西 | 规则 | 例子 |
|------|------|------|
| 实体 | 业务名词 | `User`、`Asset` |
| 值对象 | 业务名词 | `HostAccount`、`AssetAddress` |
| 仓储接口 | 放 `domain`，名 `XxxRepository` | `UserRepository` |
| 仓储实现 | 放 `infrastructure`，名 `XxxRepositoryJpa` | `UserRepositoryJpa` |
| 应用服务 | 放 `application`，名 `XxxService`，方法=用例动词 | `UserService.createUser()` |
| Controller | 放 `api`，名 `XxxController` | `UserController` |
| DTO | 放 `api` 或 `application`，名 `XxxRequest`/`XxxResponse` | `UserCreateRequest` |
| 领域事件 | `domain/event`，名 `XxxHappened` 过去式 | `AuthorizationApproved` |

---

## 3. 前端架构：套娃组件 + 统一自由设置（Vue）

### 3.1 套娃组件：四层

外层套内层，层层组合。**每层只能引用自己这层或更内层，禁止反向或跨层跳用。**

```
L4 页面      features/xxx/views/        AssetListView.vue      （最外层：组合 L3）
L3 业务组件  features/xxx/components/   AssetTable.vue         （带业务语义）
L2 组合组件  components/common/         PmTable/PmForm/PmDialog（无业务，通用）
L1 原子组件  components/base/           PmButton/PmInput/PmTag （包一层 Arco，统一风格）
```

规则：
- **L1**：对 Arco 组件的薄封装，统一主题/尺寸/校验风格，业务代码不许直接用 Arco 裸组件（要统一样式就改这一层）。
- **L2**：由 L1 组合出的无业务通用件（表格、表单、弹窗、搜索栏）。
- **L3**：某个 feature 私有的业务组件，由 L2 组合。
- **L4**：页面，由 L3 组合，越薄越好。
- **两个 feature 都要用的组件 → 下沉到 L2/L1**，不许 feature 之间互相引用。

### 3.2 统一自由设置：Schema 驱动

**统一**：所有 CRUD 界面遵循同一套配置规范，表格/表单/搜索栏/操作全用一个配置对象描述。
**自由**：字段、校验、动作自由组合，新页面 = 复制配置改字段，不写重复代码。

目标形态（示例，随功能迭代落地）：

```ts
// features/asset/views/config.ts —— 看完配置就知道这个页面是干嘛的
export const assetPage: CrudPageConfig = {
  title: '资产管理',
  search: [
    { name: 'hostname', label: '主机名', type: 'input' },
    { name: 'protocol', label: '协议', type: 'select', options: ['SSH', 'RDP', 'VNC'] },
  ],
  table: {
    columns: [
      { dataIndex: 'hostname', title: '主机名' },
      { dataIndex: 'address', title: '地址' },
      { dataIndex: 'protocol', title: '协议', render: 'tag' },
    ],
    actions: ['edit', 'delete', 'grant'],   // 授权入口
  },
  form: {
    fields: [
      { name: 'hostname', label: '主机名', type: 'input', rules: ['required'] },
      { name: 'protocol', label: '协议', type: 'select', options: ['SSH', 'RDP', 'VNC'] },
    ],
  },
}
```

好处：忘了代码，打开 `config.ts` 就能看出页面有什么字段、什么动作、什么校验——**配置本身就是文档**。

### 3.3 目录结构：镜像后端限界上下文

```
src/
├── components/            # 套娃 L1/L2（无业务，全局共享）
│   ├── base/              # L1 原子组件（Pm 前缀，包 Arco）
│   └── common/            # L2 组合组件
├── features/              # 业务功能（L3/L4），目录名镜像后端上下文
│   ├── identity/          # 对应 Java identity
│   ├── asset/             # 对应 Java asset
│   ├── access/            # 对应 Java access
│   ├── audit/             # 对应 Java audit
│   └── terminal/          # 终端直连（走 Rust WebSocket，不镜像 Java）
│       ├── views/         # L4 页面
│       ├── components/    # L3 业务组件
│       ├── stores/        # 本 feature 私有状态
│       ├── api/           # 本 feature 的 API 调用
│       └── types.ts       # 本 feature 的类型定义
├── stores/                # 全局共享状态（auth 等，放这；feature 私有的放 feature 里）
├── router/
├── layouts/               # 布局壳（最外层套娃）
└── assets/
```

### 3.4 前端命名约定

| 东西 | 规则 | 例子 |
|------|------|------|
| 目录 | 小写单数 | `views/`、`components/` |
| Vue 文件 | PascalCase | `AssetListView.vue` |
| 基础组件 | `Pm` 前缀 | `PmButton`、`PmTable` |
| 页面文件 | `XxxView.vue` | `AssetListView.vue` |
| 页面配置 | `config.ts`，跟页面同目录 | `features/asset/views/config.ts` |
| Store | `useXxxStore` | `useAuthStore` |
| API 函数 | 动词开头 | `listAssets()`、`createAsset()` |

---

## 4. 跨模块通用约定

- **统一响应格式**（Java 所有接口）：`{ code, message, data }`，`code=0` 表示成功。
- **环境变量前缀**：`PRAESIDIUM_`（如 `PRAESIDIUM_CORE_ADDR`），各模块只读自己的前缀段。
- **注释与文档**：中文；标识符英文。注释解释"为什么"，不解释"是什么"。
- **错误处理**：Rust 用 `anyhow`/`thiserror` 分层，Java 异常统一由全局 handler 转统一响应，前端 axios 拦截器统一弹错。
- **前端 TS 严格模式**：`noUnusedLocals` / `noUnusedParameters` 已开，不许放松。

---

## 5. 如何扩展（忘了代码时的操作手册）

1. **加一个后端业务模块**：复制某个上下文的四层目录，改名，照 2.3 的结构填。
2. **加一个前端页面**：在对应 `features/xxx/views/` 下新建 `XxxView.vue` + `config.ts`，配置驱动，不动框架代码。
3. **改全局样式**：只改 `components/base/`（L1），全局生效。
4. **跨模块复用组件**：下沉到 `components/common/`（L2），禁止 feature 互引。

---

## 6. 当前实施状态

- Java：DDD 包骨架已建（`shared` + 四个上下文，各层以 `package-info.java` 标注职责），详见 §2.2 树。
- Web：`features/terminal/` 已按 §3.3 落地；`identity/asset/access/audit` 待对应功能开发时创建。
- Rust：`praesidium-core` 代码已就绪，**编译验证暂缓**（本机缺 C 工具链，待安装 Visual Studio 后恢复；恢复 russh 时用 ring 后端，见 `Cargo.toml` 内 TODO）。
