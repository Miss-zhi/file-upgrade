# 文件分享 — 创建/提取码/匿名下载/管理

## Purpose
实现文件分享：生成提取码链接、设置过期时间、匿名访问下载、管理分享记录。

## ADDED Requirements

### Requirement: 分享创建
POST /share/create SHALL accept fileId/expireDays/code, generate unique token, and return share link

### Requirement: 分享验证
GET /share/verify?token=x SHALL validate token+code, return file info if valid

### Requirement: 匿名下载
GET /anonymous/download/{token}?code=x SHALL return file stream without authentication

### Requirement: 分享管理
POST /share/list SHALL return current user's shares; POST /share/cancel SHALL revoke a share

### Requirement: 前端分享对话框
ShareDialog.vue SHALL allow setting expiry time and optional extraction code

### Requirement: CI 兼容
mvn test + vue-tsc + vite build all pass
