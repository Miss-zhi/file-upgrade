# 奇文网盘技术升级 — Harness Engineering 工作流

> AI = Model + Harness。Model 是你无法控制的生成能力，Harness 是你能控制的一切。
> 本文件定义了这个项目中 Harness 的完整架构：Guide（前馈）、Sensor（反馈）、反馈闭环、仪表盘。

---

## 一、Harness 架构总览

```
                        ┌──────────────────────────────────────┐
                        │            你（决策者）                │
                        │  审查 Spec · 批准规则 · 合并代码       │
                        └──────────┬──────────────┬────────────┘
                                   │              │
                              Guide 层         Sensor 层
                           （前馈：约束AI       （反馈：检查AI
                            不犯错）             犯没犯错）
                                   │              │
                        ┌──────────▼──────────────▼────────────┐
                        │          AI Agent（执行者）            │
                        │  读 Guide → 生成代码 → 过 Sensor      │
                        └──────────────────┬───────────────────┘
                                           │
                                    反馈闭环
                              （Sensor 发现的错误
                               沉淀为新 Guide）
```

---

## 二、Guide 体系（前馈层）

Guide 的作用：在 AI 动手**之前**就把约束注入上下文，让 AI 不可能生成违反规则的代码。

### Guide 分层

| 层级 | 文件 | 作用域 | 何时读取 | 更新频率 |
|------|------|--------|----------|----------|
| **G0 全局** | `AGENTS.md`（根目录） | 所有代码 | 每次 AI 会话启动时 | 每个 Change 结束后可能追加新规则 |
| **G1 后端** | `qiwen-file/AGENTS.md` | 后端 Java 代码 | 生成后端代码时 | 后端相关约定变化时 |
| **G2 前端** | `qiwen-file-web/AGENTS.md` | 前端 Vue 代码 | 生成前端代码时 | 前端相关约定变化时 |
| **G3 任务** | `openspec instructions` 输出 | 当前 artifact | 生成 proposal/specs/design/tasks 时 | 每个 artifact 生成前重新获取 |
| **G4 积累规则** | `AGENTS.md` → "已积累规则"段 | 所有代码 | 同 G0 | 每次 AI 犯错后立即追加 |

### 各层 Guide 的内容

**G0 全局约束**（`AGENTS.md`）定义了什么能做、什么不能做：技术栈版本（Java 17、Spring Boot 3.2.x、Vue 3.4.x）、架构分层（Controller→Service→Mapper）、统一响应体（`RestResult<T>`）、ID 生成（雪花算法）、日期处理（`LocalDateTime`）、禁止事项清单（不用 `javax.*`、不用 Options API、不加未声明的依赖等）。

**G1 后端约束**（`qiwen-file/AGENTS.md`）补充了包结构（`com.qiwenshare.file.*`）、Spring Boot 3 迁移要点（Lambda DSL 安全配置、SpringDoc 替代 Springfox）、Service/Controller/Entity/DTO 的创建模板。

**G2 前端约束**（`qiwen-file-web/AGENTS.md`）补充了目录结构（`src/` 组织方式）、组件模板（`<script setup>` 完整示例）、Pinia Setup Store 模式、API 调用模式、样式规范（Stylus scoped、`:deep()`）。

**G3 任务级指令**（`openspec instructions <artifact> --change <name>`）是 OpenSpec 生成的结构化 XML 指令，包含：当前任务的 context（前置 artifact 的内容）、rules（从 AGENTS.md 提取的约束）、dependencies（需要读的文件列表）、template（artifact 的 markdown 模板）、output location（写入路径）。这是 Harness 的**精确注入通道**——每次只给 AI 当前任务需要的上下文，而不是把所有信息一股脑塞进去。

**G4 积累规则**是 AI 犯错后沉淀到 `AGENTS.md` 末尾的新约束。初始为空，随使用逐步增长。每条规则格式：

```markdown
- **已犯错误**：[简述]
- **规则**：[新增约束]
- **日期**：[YYYY-MM-DD]
```

### Guide 优先级

当规则冲突时：G4（积累规则）> G1/G2（模块约束）> G0（全局约束）。积累规则排在最前面，因为它们是最近犯过的错，AI 印象最深。

---

## 三、Sensor 体系（反馈层）

Sensor 的作用：在 AI 动手**之后**检查输出是否正确，发现问题立即打回重做。

### Sensor 分类

| 类型 | 触发时机 | 检查什么 | 失败后果 |
|------|----------|----------|----------|
| **S1 计算型** | AI 每次生成代码后 | 代码能不能编译/lint/构建/测试通过 | AI 自修，最多 3 轮 |
| **S2 规范型** | 全部 task 完成后 | 实现是否匹配 Spec 定义的需求 | AI 补漏或修正 |
| **S3 人工型** | 你审查代码时 | 架构合理性、业务逻辑正确性、代码可读性 | 你要求 AI 修改 |

### S1 计算型 Sensor（自动，AI 自己跑）

这是最频繁的 Sensor，每个 Change 的 apply 阶段至少触发一次。具体检查项对应 CI 流水线的 6 个 job：

```
后端 S1:
  ├─ compile: mvn compile -f qiwen-file/pom.xml -B -DskipTests
  ├─ test:    mvn test -f qiwen-file/pom.xml -B        （MySQL 8 + Redis 7 容器）
  └─ package: mvn package -f qiwen-file/pom.xml -B -DskipTests

前端 S1:
  ├─ lint:      npx eslint src/ --ext .vue,.js,.ts
  ├─ typecheck: npx vue-tsc --noEmit
  └─ build:     npx vite build
```

S1 失败时 AI 的行为：读错误输出 → 定位文件和行号 → 修改代码 → 重跑。最多 3 轮。3 轮还失败则停下来向你报告：错误原文、AI 尝试了什么、建议你怎么处理。

### S2 规范型 Sensor（半自动）

```bash
openspec validate --change <name>
```

这个 Sensor 检查：tasks.md 中所有 task 是否已标记 `[x]`、生成的代码文件是否覆盖了 specs 中定义的所有 requirements、design.md 中描述的技术决策是否被正确实施。

S2 在 apply 阶段结束后、你审查之前运行。它不检查代码质量，只检查"AI 有没有漏掉 Spec 里的需求"。

### S3 人工型 Sensor（你来做）

这是最终的质量关卡。S1 和 S2 只能检查"对不对"，S3 检查"好不好"。你关注的是：

- 架构决策是否合理（类放在哪个包、接口粒度是否合适）
- 业务逻辑是否正确（AI 理解的领域模型对不对）
- 代码是否可读（命名、注释、复杂度）
- 是否有 AGENTS.md 没覆盖到的坏味道

S3 发现问题的处理方式：直接告诉 AI 哪里要改，AI 修改后重新过 S1。

### Sensor 执行顺序

```
AI 生成代码
    │
    ▼
  S1（计算型）──── 失败 → AI 自修（≤3轮）→ 重新 S1
    │                           │
    │ 通过                       │ 3轮仍失败 → 停下来报告给你
    ▼
  S2（规范型）──── 不合规 → AI 补漏 → 重新 S1+S2
    │
    │ 合规
    ▼
  S3（人工型）──── 不满意 → 你指示修改 → AI 改 → 重新 S1
    │
    │ 满意
    ▼
  git commit + push → GitHub CI（S1 的远程版本）
```

---

## 四、反馈闭环（Sensor → Guide 的转化）

这是 Harness Engineering 的核心机制：**每一次 AI 犯错，都变成永久性的防护栏。**

### 闭环流程

```
AI 犯错 → S1/S2/S3 发现 → AI 修复代码
                              │
                              ▼
                    AI 分析根因：是 AGENTS.md 没覆盖到？
                              │
                    ┌─────────┴─────────┐
                    │ 是                 │ 否（偶发错误）
                    ▼                    ▼
          在 AGENTS.md 追加规则     只修复代码，不加规则
                    │
                    ▼
          你审查并批准新规则
                    │
                    ▼
          后续所有 Change 的 AI 都读到这条规则
                    │
                    ▼
          同类错误不再发生 ✓
```

### 什么时候追加规则

不是每次 AI 犯错都要加规则。判断标准：

| 情况 | 处理 |
|------|------|
| AI 用了 `javax.*` 而不是 `jakarta.*` | **加规则** — AGENTS.md 明确禁止但 AI 还是犯了，说明需要更强的提醒 |
| AI 用了 `@Autowired` 而不是构造器注入 | **加规则** — 同上 |
| AI 某个变量名起得不好 | **不加** — 偶发问题，不值得变成永久规则 |
| AI 在错误的包下创建了类 | **加规则** — 说明包结构的描述不够清晰，需要补充 |
| AI 引入了 pom.xml 里没有的依赖 | **加规则** — 禁止事项里写了的，需要强化 |

简单说：**如果 AGENTS.md 已经写了但 AI 还是犯，说明写得不够醒目，追加到"已积累规则"段加强提醒；如果 AGENTS.md 没覆盖到，说明有盲区，新增一条规则。**

### 规则的生命周期

```
新规则追加 → 后续 Change 的 AI 读到 → 不再犯同类错误
                                          │
                              经过 3-4 个 Change 没再触发
                                          │
                                          ▼
                              可以考虑合并到 AGENTS.md 正文
                              （从"已积累规则"移到对应的约束段）
```

---

## 五、仪表盘（Harness 有效性监控）

你需要一个地方一眼看到 Harness 是否在有效控制 AI 的行为。

### 每个 Change 结束后更新

在 `WORKFLOW.md` 末尾维护一个状态表（见下方"进度追踪"段），记录：

- 哪些 Change 已完成、哪些进行中、哪些待开始
- AGENTS.md 积累了多少条规则
- 每个 Change 中 S1 失败了几次（反映 AI 生成质量）
- 哪些规则是被反复触发的（说明 Guide 注入不够强）

### 关键指标

| 指标 | 怎么看 | 健康值 |
|------|--------|--------|
| S1 失败率 | 每个 Change 的 CI 失败次数 | ≤1 次/Change（理想情况 0 次） |
| 规则增长速率 | AGENTS.md 积累规则段的增长速度 | 前 3 个 Change 增长快，后面趋于稳定 |
| 规则重复触发率 | 同一条规则被多次违反 | 应该为 0（如果 >0 说明规则写得不够醒目） |
| Spec 覆盖率 | `openspec validate` 的需求覆盖 | 100% |
| AI 自修成功率 | AI 自己修好 CI 错误的比例 | ≥80%（低于此说明 Guide 体系需要加强） |

---

## 六、操作循环（每个 Change 的执行步骤）

12 个 Change 都按同样的流程走。每一步标注了涉及的 Guide 和 Sensor。

```
┌─────────────────────────────────────────────────────────────────────┐
│                                                                     │
│  ① PROPOSE（你触发，AI 执行）                                       │
│     你：/opsx:propose <name>                                        │
│     AI：openspec new change → 逐个生成 artifact                     │
│     AI：每个 artifact 前跑 openspec instructions 获取 [G3 任务指令]  │
│     AI：生成时遵守 [G0 全局] + [G1/G2 模块] + [G4 积累规则]         │
│     你：审查 proposal.md 和 tasks.md  ← [S3 人工 Sensor]            │
│                                                                     │
│  ② APPLY（你触发，AI 执行）                                         │
│     你：/opsx:apply <name>                                          │
│     AI：openspec instructions apply 获取 [G3 实现指令]               │
│     AI：读全部 context（proposal + specs + design + tasks）          │
│     AI：逐个 task 生成代码，标记 [x]                                 │
│     AI：遵守 [G0+G1+G2+G4] 全部 Guide                               │
│                                                                     │
│  ③ VALIDATE（AI 自动）                                              │
│     AI 跑 [S1 计算型 Sensor]：                                      │
│       后端：mvn compile → mvn test → mvn package                    │
│       前端：npm lint → npm typecheck → npm build                    │
│     S1 失败 → AI 自修（≤3轮）→ 重跑 S1                             │
│     3轮仍失败 → AI 停下来报告                                       │
│     AI 跑 [S2 规范型 Sensor]：openspec validate --change <name>     │
│     S2 不合规 → AI 补漏 → 重跑 S1+S2                               │
│                                                                     │
│  ④ 反馈闭环（AI 提议，你批准）                                      │
│     如果 S1/S2 过程中 AI 犯了模式性错误：                            │
│     AI：分析根因 → 提议在 AGENTS.md 追加规则 [→ G4 更新]            │
│     你：审查并批准                                                   │
│     AI：追加到 AGENTS.md "已积累规则"段                              │
│                                                                     │
│  ⑤ COMMIT（你做）                                                   │
│     你：审查代码  ← [S3 人工 Sensor]                                │
│     你：git add + commit + push                                     │
│     GitHub Actions 自动跑 [S1 远程版本]                             │
│     CI 绿 → 继续；CI 红 → 回来找 AI 修                              │
│                                                                     │
│  ⑥ ARCHIVE（你触发，AI 执行）                                       │
│     你：/opsx:archive <name>                                        │
│     AI：确认所有 task [x]、所有 artifact done                       │
│     AI：同步 delta specs → openspec/specs/                          │
│     AI：移入 openspec/changes/archive/                              │
│     你：更新 WORKFLOW.md 进度追踪表                                 │
│                                                                     │
│  ──→ 下一个 Change                                                   │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 七、12 个 Change 分 4 阶段推进

### 阶段 1 — 基础设施（c1-c3）

| Change | 内容 | 任务数 | S1 门控 |
|--------|------|--------|---------|
| **c1: backend-scaffold** | pom.xml + 启动类 + 包结构 + RestResult + 全局异常 + AOP日志 + application.yml | ~8 | mvn compile/test/package |
| **c2: frontend-scaffold** | package.json + Vite配置 + main.js + Axios封装 + Element Plus + Router + ESLint | ~8 | npm lint/typecheck/build |
| **c3: user-auth** | User实体(双ORM) + JWT(jjwt 0.12) + Security 6 Lambda DSL + 登录注册 + 前端登录页 | ~12 | 全CI + 后端测试 |

### 阶段 2 — 核心功能（c4-c6）

| Change | 内容 | 任务数 | S1 门控 |
|--------|------|--------|---------|
| **c4: file-core** | UFOP存储抽象 + Local实现 + File实体 + 上传/下载/删除/列表接口 | ~10 | 全CI + 文件服务测试 |
| **c5: file-ui** | 文件管理页 + 文件列表表格 + 目录树 + 上传组件 + 面包屑导航 | ~10 | 全CI |
| **c6: user-management** | 用户CRUD接口 + 分页 + 前端用户管理页 | ~8 | 全CI + CRUD测试 |

### 阶段 3 — 高级功能（c7-c9）

| Change | 内容 | 任务数 | S1 门控 |
|--------|------|--------|---------|
| **c7: file-share** | 分享实体 + 创建/列表/取消 + 匿名下载 + 前端分享页 | ~8 | 全CI |
| **c8: search** | ES配置 + 索引管理 + 搜索接口 + 前端搜索组件 | ~8 | 全CI(ES测试环境禁用) |
| **c9: admin-panel** | 系统配置接口 + 统计接口 + 前端管理面板 | ~8 | 全CI |

### 阶段 4 — 收尾（c10-c12）

| Change | 内容 | 任务数 | S1 门控 |
|--------|------|--------|---------|
| **c10: onlyoffice** | OnlyOffice配置 + 回调接口 + 前端编辑器组件 | ~6 | 全CI |
| **c11: docker-compose** | docker-compose.yml + 双Dockerfile + Nginx配置 | ~5 | CI + docker-compose config |
| **c12: final-integration** | 其余4种存储实现(OSS/FastDFS/MinIO/Qiniu) + 集成测试 + README | ~8 | 全CI |

### 依赖关系

```
c1(后端骨架) ──→ c3(用户认证) ──→ c4(文件核心) ──→ c7(分享) ──→ c10(OnlyOffice)
c2(前端骨架) ──→ c5(文件UI) ──→ c8(搜索)
c3 ──→ c6(用户管理) ──→ c9(管理面板)
c11(Docker) 在 c10 之后
c12(集成) 最后，依赖全部完成
```

---

## 八、关键文件清单

| 文件 | Harness 角色 | 说明 |
|------|-------------|------|
| `AGENTS.md` | **G0 全局 Guide + G4 规则存储** | 项目级约束 + 错误→规则闭环的追加位置 |
| `qiwen-file/AGENTS.md` | **G1 后端 Guide** | 后端包结构、Spring Boot 3 迁移规则 |
| `qiwen-file-web/AGENTS.md` | **G2 前端 Guide** | 前端目录结构、Vue 3 组件规范 |
| `openspec instructions` 输出 | **G3 任务 Guide** | 每个 artifact 生成时的精确上下文注入 |
| `.github/workflows/ci.yml` | **S1 计算型 Sensor** | CI 流水线定义（编译/测试/lint/构建） |
| `openspec validate` | **S2 规范型 Sensor** | 检查实现是否匹配 Spec |
| 你（用户） | **S3 人工型 Sensor** | 审查代码、批准规则、合并代码 |
| `.claude/skills/openspec-*` | **操作入口** | 5 个 OpenSpec 技能（propose/apply/sync/archive/explore） |
| `openspec/` | **Spec 仓库** | specs/ 存主 Spec，changes/ 存活跃变更，archive/ 存已完成变更 |

---

## 九、命令速查

```bash
# 初始化（已完成）
openspec init

# 每个 Change 的工作流
openspec new change "<name>"                                    # 创建变更
openspec status --change "<name>" --json                        # 查看进度
openspec instructions <artifact> --change "<name>" --json       # 获取 [G3] 指令
openspec instructions apply --change "<name>" --json            # 获取 [G3] 实现指令
openspec validate --change "<name>"                             # [S2] 规范检查
openspec archive                                                # 归档（通过 /opsx:archive）

# 斜杠命令（在 AI 对话中）
/opsx:explore [topic]       # 探索想法（不写代码）
/opsx:propose <name>        # 创建变更 + 全部 artifact
/opsx:apply <name>          # 实现任务
/opsx:sync <name>           # 同步 delta specs
/opsx:archive <name>        # 归档完成的变更

# 本地 S1 Sensor（CI 模拟）
mvn compile -f qiwen-file/pom.xml -B -DskipTests
mvn test -f qiwen-file/pom.xml -B
cd qiwen-file-web && npm run lint && npm run typecheck && npm run build
```

---

## 十、进度追踪

> 每完成一个 Change 后更新此表。

| Change | 状态 | S1 失败次数 | 新增规则数 | 备注 |
|--------|------|------------|-----------|------|
| c1: backend-scaffold | 待开始 | - | - | |
| c2: frontend-scaffold | 待开始 | - | - | |
| c3: user-auth | 待开始 | - | - | |
| c4: file-core | 待开始 | - | - | |
| c5: file-ui | 待开始 | - | - | |
| c6: user-management | 待开始 | - | - | |
| c7: file-share | 待开始 | - | - | |
| c8: search | 待开始 | - | - | |
| c9: admin-panel | 待开始 | - | - | |
| c10: onlyoffice | 待开始 | - | - | |
| c11: docker-compose | 待开始 | - | - | |
| c12: final-integration | 待开始 | - | - | |

**AGENTS.md 已积累规则数：0**

---

## 十一、最终验证（全部 12 个 Change 归档后）

| 检查项 | Sensor 类型 | 方法 | 预期结果 |
|--------|------------|------|----------|
| OpenSpec 全量验证 | S2 | `openspec validate` | 所有 spec 有效，无孤立需求 |
| 后端 CI | S1 | GitHub Actions on main | 3 个后端 job 全绿 |
| 前端 CI | S1 | GitHub Actions on main | 3 个前端 job 全绿 |
| Spec 完整性 | S2 | 检查 `openspec/specs/` | 所有能力域都有 spec |
| Archive 完整性 | S3 | 检查 `openspec/changes/archive/` | 12 个归档目录 |
| AGENTS.md 规则 | S3 | 检查"已积累规则"段 | 规则累积、无矛盾 |
| 冒烟测试 | S3 | 手动或集成测试 | 注册→登录→上传→列表→下载→分享→搜索 全通 |
| Docker 部署 | S1 | `docker-compose up -d` | 前后端 + MySQL + Redis + OnlyOffice 全启动 |

---

## 十二、执行建议

每个 Change 尽量在一个 AI 会话内完成（propose → apply → validate → archive）。如果 Change 太大，可以分两次会话：第一次 propose + 部分 apply，第二次继续 apply + validate + archive（OpenSpec 通过 `[x]` 标记跟踪进度）。

阶段 1 的 c1 和 c2 必须顺序执行（c2 依赖 c1 的后端 API 契约）。阶段 2 起可以在同一个 Change 内先做后端任务再做前端任务。不要同时跑两个 Change，每个都建立在前一个的基础上。

预估总工作量：12 个 Change、约 97 个任务、8-12 小时 AI+人工时间。
