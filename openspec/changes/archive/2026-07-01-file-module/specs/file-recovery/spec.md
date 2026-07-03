## ADDED Requirements

### Requirement: 软删除（移入回收站）
系统 SHALL 支持文件软删除。删除文件时更新 UserFile 的 deleteStatus 字段（0=正常, 1=已删除），文件移入回收站视图。软删除操作 MUST 在事务中完成。

#### Scenario: 成功删除单个文件
- **WHEN** 用户请求删除一个文件（提供 fileId）
- **THEN** 系统将 UserFile 的 deleteStatus 设为 1，记录删除时间（deleteTime）
- **THEN** 文件从正常文件列表中消失，出现在回收站列表中
- **THEN** 返回操作成功

#### Scenario: 批量删除文件
- **WHEN** 用户请求批量删除多个文件（提供 fileId 列表）
- **THEN** 系统在同一事务中将所有文件的 deleteStatus 设为 1
- **THEN** 返回成功删除的文件数量

#### Scenario: 删除文件夹
- **WHEN** 用户请求删除一个文件夹
- **THEN** 系统将文件夹及其所有子文件的 deleteStatus 设为 1

### Requirement: 回收站列表查询
系统 SHALL 支持查询回收站中的文件列表，显示已删除的文件和文件夹。

#### Scenario: 查询回收站列表
- **WHEN** 用户请求查看回收站
- **THEN** 系统返回该用户所有 deleteStatus=1 的文件列表，包含文件名、原路径、删除时间、剩余恢复天数
- **THEN** 支持分页，默认按删除时间降序排列

### Requirement: 恢复文件
系统 SHALL 支持从回收站恢复文件。恢复时检查原路径是否存在同名文件。

#### Scenario: 成功恢复文件
- **WHEN** 用户请求恢复回收站中的文件（提供 fileId），且原路径不存在同名文件
- **THEN** 系统将 deleteStatus 恢复为 0，清空 deleteTime
- **THEN** 文件重新出现在原目录中，从回收站消失

#### Scenario: 恢复时原路径存在同名文件
- **WHEN** 用户请求恢复文件，但原路径已存在同名文件
- **THEN** 系统返回冲突提示，用户可选择重命名后恢复

#### Scenario: 批量恢复文件
- **WHEN** 用户请求批量恢复多个文件
- **THEN** 系统在同一事务中恢复所有文件，跳过有冲突的文件并返回冲突列表

### Requirement: 永久删除
系统 SHALL 支持永久删除回收站中的文件。永久删除 MUST 异步执行：先更新数据库状态，再通过异步线程池调用 UFOP Deleter 清理存储对象。

#### Scenario: 成功永久删除单个文件
- **WHEN** 用户在回收站中请求永久删除一个文件
- **THEN** 系统立即从数据库中删除 UserFile 记录
- **THEN** 异步检查 FileBean 是否还有其他 UserFile 引用，若无则通过 UFOP Deleter 清理存储对象并删除 FileBean
- **THEN** 释放该文件占用的用户配额空间

#### Scenario: 永久删除共享存储对象
- **WHEN** 永久删除的文件对应的 FileBean 仍被其他 UserFile 引用
- **THEN** 系统仅删除当前 UserFile，不删除 FileBean 和存储对象
- **THEN** 仅释放当前 UserFile 对应的配额

### Requirement: 回收站自动清理
系统 SHALL 通过定时任务自动清理回收站中超过 30 天的文件。

#### Scenario: 自动清理过期文件
- **WHEN** 定时任务执行（每天凌晨）
- **THEN** 系统查找所有 deleteTime 超过 30 天的文件
- **THEN** 对这些文件执行永久删除流程（异步清理存储对象）
- **THEN** 记录清理日志
