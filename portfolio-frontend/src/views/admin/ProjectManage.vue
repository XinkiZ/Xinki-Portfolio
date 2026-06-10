<template>
  <div class="admin-page">
    <div class="container"><div class="admin-layout">
      <AdminSidebar />
      <main class="admin-main">
        <div class="page-header"><h1>作品管理</h1><button class="btn-primary" @click="openCreate">+ 新增作品</button></div>
        <table class="data-table">
          <thead><tr><th>ID</th><th>封面</th><th>标题</th><th>标签</th><th>状态</th><th>操作</th></tr></thead>
          <tbody>
            <tr v-for="item in list" :key="item.id">
              <td>{{ item.id }}</td>
              <td><img v-if="item.coverUrl" :src="item.coverUrl" class="table-thumb" /></td>
              <td>{{ item.title }}</td><td>{{ item.tags }}</td>
              <td>{{ item.isPublished ? '已发布' : '草稿' }}</td>
              <td class="actions">
                <button @click="openEdit(item)">编辑</button>
                <button @click="handleDelete(item.id)" class="btn-danger">删除</button>
              </td>
            </tr>
          </tbody>
        </table>
      </main>
    </div></div>

    <div v-if="showForm" class="modal-overlay" @click.self="showForm = false">
      <div class="modal">
        <h2>{{ editingId ? '编辑' : '新增' }}作品</h2>
        <div class="form-group"><label>标题</label><input v-model="form.title" /></div>
        <div class="form-group"><label>描述</label><textarea v-model="form.description" rows="4"></textarea></div>
        <div class="form-group">
          <label>封面图片</label>
          <div class="cover-upload">
            <div class="cover-preview" v-if="form.coverUrl">
              <img :src="form.coverUrl" />
              <button class="btn-clear" @click="form.coverUrl = ''">×</button>
            </div>
            <div class="cover-input-row">
              <input type="file" accept="image/*" @change="handleCoverUpload" ref="fileInput" />
              <span class="upload-divider">或</span>
              <input v-model="form.coverUrl" placeholder="直接粘贴图片 URL" />
            </div>
            <span v-if="uploading" class="upload-status">上传中...</span>
          </div>
        </div>
        <div class="form-group"><label>演示 URL</label><input v-model="form.demoUrl" /></div>
        <div class="form-group"><label>源码 URL</label><input v-model="form.githubUrl" /></div>
        <div class="form-group"><label>标签（逗号分隔）</label><input v-model="form.tags" /></div>
        <div class="form-group"><label>排序</label><input v-model.number="form.sortOrder" type="number" /></div>
        <div class="form-group"><label><input v-model="form.isPublished" type="checkbox" :true-value="1" :false-value="0" /> 已发布</label></div>
        <div class="form-actions">
          <button class="btn-primary" @click="save" :disabled="uploading">保存</button>
          <button class="btn-ghost" @click="showForm = false">取消</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from "vue";
import { getAdminProjects, createProject, updateProject, deleteProject } from "@/api/admin";
import { uploadFile } from "@/api/upload";
import AdminSidebar from "./AdminSidebar.vue";

const list = ref<any[]>([]);
const showForm = ref(false);
const editingId = ref<number | null>(null);
const uploading = ref(false);
const fileInput = ref<HTMLInputElement | null>(null);
const form = ref({ title: "", description: "", coverUrl: "", demoUrl: "", githubUrl: "", tags: "", sortOrder: 0, isPublished: 1 });

async function load() { try { const r: any = await getAdminProjects({ page: 1, pageSize: 100 }); list.value = r.data.records || []; } catch (e) {} }
function openCreate() { editingId.value = null; form.value = { title: "", description: "", coverUrl: "", demoUrl: "", githubUrl: "", tags: "", sortOrder: 0, isPublished: 1 }; showForm.value = true; }
function openEdit(item: any) { editingId.value = item.id; form.value = { ...item }; showForm.value = true; }

async function handleCoverUpload(e: Event) {
  const target = e.target as HTMLInputElement;
  const file = target.files?.[0];
  if (!file) return;
  uploading.value = true;
  try {
    const res: any = await uploadFile(file);
    form.value.coverUrl = res.data.url;
  } catch (err) {
    console.error("Upload failed:", err);
    alert("上传失败，请重试");
  } finally {
    uploading.value = false;
  }
}

async function save() {
  try {
    if (editingId.value) { await updateProject(editingId.value, form.value); }
    else { await createProject(form.value); }
    showForm.value = false; load();
  } catch (e) {}
}
async function handleDelete(id: number) { if (confirm("确定删除？")) { await deleteProject(id); load(); } }
onMounted(load);
</script>

<style scoped>
.admin-page { padding-top: 80px; }
.admin-layout { display: flex; gap: 32px; }
.admin-main { flex: 1; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px; }
.page-header h1 { font-family: var(--font-serif); }
.btn-primary { padding: 10px 24px; background: var(--ink-black); color: var(--ink-white); border: none; border-radius: 4px; cursor: pointer; font-size: 14px; }
.btn-primary:disabled { opacity: 0.5; cursor: not-allowed; }
.btn-ghost { padding: 10px 24px; border: 1px solid var(--ink-black); background: none; border-radius: 4px; cursor: pointer; }
.btn-danger { color: var(--ink-seal); background: none; border: none; cursor: pointer; }
.btn-clear { position: absolute; top: 4px; right: 4px; background: rgba(0,0,0,0.5); color: #fff; border: none; border-radius: 50%; width: 24px; height: 24px; cursor: pointer; font-size: 16px; line-height: 1; }
.data-table { width: 100%; border-collapse: collapse; background: var(--ink-white); border: 1px solid var(--ink-border); }
.data-table th, .data-table td { padding: 12px 16px; text-align: left; border-bottom: 1px solid var(--ink-border); font-size: 14px; }
.data-table th { background: var(--ink-bg); font-weight: 600; }
.actions { display: flex; gap: 8px; }
.table-thumb { width: 48px; height: 36px; object-fit: cover; border-radius: 2px; border: 1px solid var(--ink-border); }
.modal-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.4); display: flex; align-items: center; justify-content: center; z-index: 2000; }
.modal { background: var(--ink-white); padding: 32px; border-radius: 8px; max-width: 520px; width: 100%; max-height: 80vh; overflow-y: auto; }
.modal h2 { font-family: var(--font-serif); margin-bottom: 24px; }
.form-group { margin-bottom: 16px; }
.form-group label { display: block; font-size: 13px; margin-bottom: 4px; }
.form-group input[type="text"], .form-group input[type="number"], .form-group textarea { width: 100%; padding: 8px 12px; border: 1px solid var(--ink-border); border-radius: 4px; font-size: 14px; }
.form-actions { display: flex; gap: 12px; margin-top: 24px; }
.cover-upload { border: 1px solid var(--ink-border); border-radius: 4px; padding: 12px; }
.cover-preview { position: relative; margin-bottom: 12px; }
.cover-preview img { width: 100%; max-height: 200px; object-fit: cover; border-radius: 4px; }
.cover-input-row { display: flex; align-items: center; gap: 8px; }
.cover-input-row input[type="file"] { flex: 1; font-size: 13px; }
.cover-input-row input[type="text"] { flex: 2; }
.upload-divider { color: var(--ink-light); font-size: 13px; }
.upload-status { display: block; margin-top: 8px; font-size: 13px; color: var(--ink-gray); }
</style>