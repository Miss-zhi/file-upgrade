# OnlyOffice 集成

## ADDED Requirements

### Requirement: 编辑器配置
GET /onlyoffice/edit/{fileId} SHALL return editorConfig with doc/token/editorConfig

### Requirement: 回调处理
POST /onlyoffice/callback SHALL handle save (status 2) and close events

### Requirement: 前端编辑器
OnlyOfficeEditor.vue SHALL embed via iframe and load editorConfig from API

### Requirement: CI 兼容
mvn compile + vue-tsc + vite build pass
