# 全文搜索

## Purpose
Elasticsearch 文件索引管理与关键词高亮搜索。

## ADDED Requirements

### Requirement: ES 索引管理
Upload SHALL create index; delete SHALL remove from index

### Requirement: 关键词搜索
POST /search SHALL accept keyword and return highlighted results

### Requirement: 前端搜索
SearchBar SHALL be in header; SearchResult SHALL display highlighted matches

### Requirement: CI 兼容
mvn compile + vue-tsc + vite build all pass; test env disables ES health check
