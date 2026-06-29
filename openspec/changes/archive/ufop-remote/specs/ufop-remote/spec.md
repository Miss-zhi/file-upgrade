# UFOP 远程存储实现

## ADDED Requirements

### Requirement: AliyunOSS 全部 7 操作
AliyunOSS SHALL have Uploader/Downloader/Deleter/Reader/Writer/Renamer/Copier implementations extending their respective abstract classes, annotated @Component, with getStorageType() returning ALIYUN_OSS

#### Scenario: Spring 注入验证
- **GIVEN** Spring Context
- **WHEN** 从容器中获取 AliyunOSSUploader Bean
- **THEN** Bean 存在且 getStorageType() == ALIYUN_OSS

### Requirement: Minio 全部 7 操作
Minio SHALL have Uploader/Downloader/Deleter/Reader/Writer/Renamer/Copier implementations with getStorageType() returning MINIO

#### Scenario: Bean 注入验证
- **GIVEN** Spring Context
- **WHEN** 获取 MinioUploader Bean
- **THEN** getStorageType() == MINIO

### Requirement: FastDFS 全部 7 操作
FastDFS SHALL have Uploader/Downloader/Deleter/Reader/Writer/Renamer/Copier implementations with getStorageType() returning FAST_DFS

#### Scenario: Bean 注入验证
- **GIVEN** Spring Context
- **WHEN** 获取 FastDFSUploader Bean
- **THEN** getStorageType() == FAST_DFS

### Requirement: Qiniuyun 全部 7 操作
Qiniuyun SHALL have Uploader/Downloader/Deleter/Reader/Writer/Renamer/Copier implementations with getStorageType() returning QINIU

#### Scenario: Bean 注入验证
- **GIVEN** Spring Context
- **WHEN** 获取 QiniuyunKodoUploader Bean
- **THEN** getStorageType() == QINIU

### Requirement: UFOPFactory 路由
UFOPFactory MUST auto-discover all implementations and route to correct one based on configured StorageType

#### Scenario: 存储类型切换
- **GIVEN** 配置 storageType=ALIYUN_OSS
- **WHEN** 调用 factory.getUploader()
- **THEN** 返回 AliyunOSSUploader 实例

### Requirement: CI 兼容
mvn test MUST pass with all @Component beans verified, remote operations skipped

#### Scenario: 测试通过
- **WHEN** mvn test
- **THEN** 42 → 48 测试，0 failures
