## Why

前五次前端change（frontend-base、file-module-frontend、frontend-preview、admin-module-frontend、search-module-frontend）已搭建出完整的 Vue 3 应用骨架并实现了全部交互逻辑，但存在三类问题阻碍交付：①API层与后端实际端点不对齐（notice/param端点后端不存在、quota端点前端未调用）；②旧项目的关键UI特性未复刻（粒子背景、滑块验证、iconfont、60+文件图标）；③组件样式与旧项目存在细节偏差，需要逐项对齐。本次change将前端从"能跑"提升到"可交付"。

## What Changes

- **修复API不对齐**：notice模块（`/notice/list`、`/notice/detail`）和首页参数（`/param/grouplist`）调用的后端端点不存在，需改为从 `SystemConfig`（`/admin/config`）读取或新建对应后端端点；用户端配额接口 `GET /quota/info` 前端未调用，需接入替代 `getStorage()`
- **补齐缺失UI功能**：登录/注册页canvas-nest粒子背景动画、DragVerify滑块验证组件、自定义iconfont（音频控制图标）、60+文件类型图标资源、拖拽上传全屏覆盖层
- **组件样式1:1还原**：逐组件对照旧项目Stylus样式，修正spacing/sizing/color/animation细节，确保视觉一致
- **修复已知Bug**：FileTable高度计算、删除列prop名、sortMethod、batchDownload、quota校验、spark-md5 any类型等前次遗留问题

## Capabilities

### New Capabilities
- `api-alignment`: 修复前端API层与后端63个端点的对齐问题，包括不存在端点的替换、缺失端点的接入、请求/响应类型的校正
- `missing-ui-features`: 实现旧项目有但新项目缺失的UI功能：canvas-nest粒子背景、DragVerify滑块验证、iconfont图标、文件类型图标资源、拖拽上传覆盖层
- `style-fidelity`: 逐组件样式审计与修正，确保所有布局尺寸、间距、颜色、动画与旧项目1:1一致

### Modified Capabilities
- `file-list-display`: 修正FileTable高度计算、列定义、排序逻辑等与spec的偏差
- `file-upload`: 修复上传面板样式、拖拽覆盖层实现、quota校验时序
- `app-shell`: Header/Footer/Aside的精确尺寸和间距对齐

## Impact

- **API层**：`api/file.ts`、`api/home.ts`、`api/notice.ts`、`stores/sideMenu.ts` 需要修改
- **新增组件**：`DragVerify.vue`、`CanvasNest.vue`（或composable）
- **静态资源**：需从旧项目复制60+文件类型图标、iconfont字体文件、logo图片
- **依赖**：可能新增 `canvas-nest.js`
- **样式文件**：`variables.scss`、`element-override.scss`、`responsive.scss` 及多个组件的 `<style>` 块需要调整
- **后端**：notice和param功能若决定保留，需后端新建对应Controller（不在本次change范围，前端先做降级处理）
