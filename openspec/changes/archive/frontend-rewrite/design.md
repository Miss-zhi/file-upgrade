## Context

奇文网盘前端已完成5个阶段的开发（frontend-base → file-module-frontend → frontend-preview → admin-module-frontend → search-module-frontend），搭建出完整的 Vue 3 + TypeScript + Pinia + Element Plus 应用。但存在三类交付阻塞问题：

1. **API不对齐**：notice和home模块调用后端不存在的端点（`/notice/list`、`/notice/detail`、`/param/grouplist`），用户配额接口 `GET /quota/info` 未接入
2. **UI缺失**：canvas-nest粒子背景、DragVerify滑块验证、iconfont图标、60+文件类型图标、拖拽上传覆盖层未实现
3. **样式偏差**：组件间距/尺寸/颜色与旧项目有细节差异，需逐项对齐

技术栈约束：Vue 3.5 + Vite 8 + TypeScript 6 + Pinia 3 + Element Plus 2.14 + SCSS。代码架构沿用现有Composition API + `<script setup>`体系，旧项目仅参考UI。

## Goals / Non-Goals

**Goals:**
- 前端API层与后端63个端点完全对齐，类型定义与后端DTO/VO字段一致
- 补齐旧项目有但新项目缺失的5项UI功能（粒子背景、滑块验证、iconfont、文件图标、拖拽覆盖层）
- 所有组件样式与旧项目1:1一致，精确到像素级
- 修复前次实现中的已知Bug（列prop名、排序方法、quota校验等）

**Non-Goals:**
- 不新建后端Controller（notice/param端点不存在时前端降级处理）
- 不改变现有代码架构（不引入新的设计模式或重构composable体系）
- 不处理后端业务逻辑（后端已完成）
- 不做性能优化或bundle size优化

## Decisions

### D1: 不存在端点的降级策略

**决定**: notice和home(param)模块API降级为前端硬编码默认值，不新建后端Controller。

**理由**: 后端已完成且不含notice/param模块。新建后端Controller超出本次change范围。前端降级处理保证页面不崩溃，后续如需真实数据再单独建后端change。

**降级方案**:
- `api/home.ts` 的 `getSystemParams()`: 返回硬编码的默认版权信息 `{ copyright: "© 2024 奇文网盘" }`，Footer显示默认文字
- `api/notice.ts` 的 `getNoticeList()`/`getNoticeDetail()`: 返回空数组/空对象，NoticeListView显示"暂无公告"
- HomeView的HomeNotice组件：无数据时不渲染公告区域

**替代方案**: 从 `/admin/config` 读取公开配置项——但admin端点需管理员权限，普通用户无法访问，故不采用。

### D2: canvas-nest.js集成方式

**决定**: 新增 `canvas-nest.js` 依赖，在LoginView和RegisterView中以composable方式初始化和销毁。

**理由**: 旧项目直接使用canvas-nest.js库，效果一致。用composable封装避免组件臃肿，确保页面离开时正确销毁。

**实现**:
```typescript
// composables/useCanvasNest.ts
export function useCanvasNest(el: Ref<HTMLElement | null>, color: string, count: number) {
  onMounted(() => { /* new CanvasNest({ el, color, pointColor, count }) */ })
  onUnmounted(() => { /* destroy */ })
}
```

### D3: DragVerify组件设计

**决定**: 新建 `components/common/DragVerify.vue`，通过 `v-model:verified` 双向绑定验证状态。

**理由**: 旧项目有同名组件，1:1还原。作为通用组件可在登录和注册页复用。

**接口**:
```typescript
// Props
verified: boolean  // v-model
width: number      // 默认375

// Emits
update:verified(value: boolean)
```

### D4: 文件类型图标迁移策略

**决定**: 从旧项目 `assets/images/file/` 复制全部60+图标文件到新项目对应目录，更新 `types/file.ts` 的 `fileImgMap` 映射表。

**理由**: 这些图标是静态PNG/SVG资源，无法自动生成。直接复制最可靠。

**路径映射**: 旧项目 `src/assets/images/file/` → 新项目 `src/assets/images/file/`

### D5: iconfont迁移策略

**决定**: 从旧项目复制iconfont全套文件（css + eot + woff + ttf + svg）到新项目 `src/assets/styles/iconfont/`，在main.ts中import。

**理由**: iconfont是字体文件+CSS，直接复用旧项目资源。新项目已有Element Plus Icons覆盖大部分场景，iconfont仅用于音频控制等Element Plus不提供的图标。

### D6: 样式审计方法

**决定**: 按组件逐个对照旧项目Stylus源码中的具体数值，修正新项目SCSS中的偏差。优先处理布局框架组件（Header/Aside/Footer），再处理业务组件（FileTable/FileGrid/Dialog等），最后处理预览组件。

**理由**: 旧项目用Stylus，新项目用SCSS，变量名已对齐（颜色值一致），但组件级的padding/margin/height等行内样式需逐项核对。

**对照源**: 旧项目各.vue文件的 `<style lang="stylus">` 部分 + `varibles.styl` + `mediaScreenXs.styl`

### D7: 用户配额接口接入

**决定**: sideMenu store 的 `fetchStorage()` 改为优先调用 `GET /api/v1/quota/info`，降级使用 `GET /api/v1/filetransfer/getstorage`。

**理由**: `/quota/info` 是专门的用户配额端点，语义更清晰。`/filetransfer/getstorage` 作为备用保持兼容。

## Risks / Trade-offs

### [R1] notice/param降级后功能缺失
→ 公告列表页显示"暂无公告"，首页不显示公告滚动，Footer使用默认版权文字。功能可用但数据为空。后续如需真实数据需新建后端change。

### [R2] canvas-nest.js与Vue 3生命周期冲突
→ canvas-nest.js直接操作DOM，需确保在onMounted后初始化、onUnmounted前销毁。用composable封装隔离生命周期管理。

### [R3] 旧项目图标资源可能不完整
→ 复制前验证所有图标文件存在且非空。缺失的图标用file_unknown.png降级。

### [R4] 样式像素级对齐工作量大
→ 按优先级分批处理：P0布局框架 → P1文件管理核心 → P2预览组件 → P3管理页面。每个组件对照旧项目Stylus源码修正。

### [R5] 分页参数名不一致
→ 前端notice模块用 `currentPage/pageCount`，后端admin模块用 `page/pageSize`。统一为后端命名规范（`page/pageSize`），前端适配。
