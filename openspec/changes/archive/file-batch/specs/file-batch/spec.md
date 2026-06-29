# 文件批量操作

## ADDED Requirements

### Requirement: 批量删除
POST /file/batch-delete SHALL accept {"ids":[...]} and soft-delete each file belonging to the user

#### Scenario: 批量删除成功
- **GIVEN** 3 个文件已存在，userId = "user001"
- **WHEN** 提交 POST /file/batch-delete {"ids":["id1","id2","id3"]}
- **THEN** 3 个文件标记 deleted=1，正常列表不再出现

### Requirement: 批量移动
POST /file/batch-move SHALL accept {"ids":[...], "targetPath":"/docs"} and update filePath for each file

#### Scenario: 批量移动到文件夹
- **GIVEN** 2 个文件在根目录
- **WHEN** 提交 {"ids":[...], "targetPath":"/docs"}
- **THEN** 文件 filePath 更新为 /docs/filename，parentPath 更新为 /docs/

### Requirement: 前端多选
FileTable.vue SHALL render an el-table selection column and emit selection-change event with selected FileItem array

#### Scenario: 用户勾选文件
- **GIVEN** 文件列表显示 5 个文件
- **WHEN** 用户点击 2 个 checkbox
- **THEN** BatchToolbar 显示"已选 2 项"

### Requirement: CI 兼容
Build MUST pass mvn test (0 failures) and vue-tsc + vite build

#### Scenario: 测试通过
- **WHEN** 执行 mvn test + vue-tsc + vite build
- **THEN** 全部成功
