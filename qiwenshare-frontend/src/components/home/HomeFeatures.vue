<script setup lang="ts">
/** 功能介绍数据 */
const features = [
  { icon: 'UploadFilled', title: '多种存储方式', desc: '支持本地、MinIO、阿里云OSS、七牛云、FastDFS 五种存储后端' },
  { icon: 'Connection', title: '分片断点续传', desc: '大文件分片上传，支持断点续传，上传进度实时显示' },
  { icon: 'Edit', title: '在线文档编辑', desc: '集成 OnlyOffice，支持 Word/Excel/PPT 在线编辑' },
  { icon: 'Delete', title: '回收站', desc: '文件删除后进入回收站，支持恢复和彻底删除' },
  { icon: 'Files', title: '文件分类与视图', desc: '按文件类型分类，支持列表/网格/时间线三种视图' },
  { icon: 'Search', title: '全局搜索', desc: '基于 Elasticsearch 实现全文检索，毫秒级响应' },
  { icon: 'Share', title: '文件分享', desc: '生成分享链接，支持提取码和有效期设置' },
  { icon: 'Operation', title: '基础操作', desc: '新建/复制/移动/删除/重命名，文件操作一应俱全' },
  { icon: 'View', title: '在线预览', desc: '图片/视频/音频/代码/Markdown/PDF 在线预览' },
]
</script>

<template>
  <section class="home-features">
    <h2 class="features-title">功能介绍</h2>
    <div class="features-grid">
      <div v-for="item in features" :key="item.title" class="feature-card">
        <div class="feature-icon">
          <el-icon :size="36"><component :is="item.icon" /></el-icon>
        </div>
        <h3 class="feature-title">{{ item.title }}</h3>
        <p class="feature-desc">{{ item.desc }}</p>
      </div>
    </div>
  </section>
</template>

<style lang="scss" scoped>
.home-features {
  background: $primary-hover;
  padding: 60px 0;
}

.features-title {
  text-align: center;
  font-size: 28px;
  color: $primary-text;
  margin-bottom: 40px;
}

.features-grid {
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 20px;
}

.feature-card {
  width: 32%;
  text-align: center;
  padding: 30px 20px;
  margin-bottom: 20px;
  border-radius: 8px;
  transition: all 0.3s;
  cursor: default;

  &:hover {
    background: linear-gradient(to right, $primary, #79bbff);
    color: #fff;

    .feature-icon {
      background: rgba(255, 255, 255, 0.2);
      color: #fff;
    }

    .feature-title,
    .feature-desc {
      color: #fff;
    }
  }
}

.feature-icon {
  width: 100px;
  height: 100px;
  border-radius: 50%;
  background: $primary-hover;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 20px;
  color: $primary;
  transition: all 0.3s;
}

.feature-title {
  font-size: 18px;
  margin-bottom: 12px;
  color: $primary-text;
}

.feature-desc {
  font-size: 14px;
  color: $regular-text;
  line-height: 1.6;
}
</style>
