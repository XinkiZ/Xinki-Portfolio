<template>
  <div class="admin-page"><div class="container"><div class="admin-layout">
    <AdminSidebar /><main class="admin-main">
      <div class="page-header"><h1>技能管理</h1><button class="btn-primary" @click="openCreate">+ 新增</button></div>
      <table class="data-table">
        <thead><tr><th>ID</th><th>名称</th><th>分类</th><th>熟练度</th><th>操作</th></tr></thead>
        <tbody><tr v-for="item in list" :key="item.id">
          <td>{{ item.id }}</td><td>{{ item.name }}</td><td>{{ item.category }}</td><td>{{ item.level }}</td>
          <td class="actions"><button @click="openEdit(item)">编辑</button><button @click="handleDelete(item.id)" class="btn-danger">删除</button></td>
        </tr></tbody>
      </table>
    </main>
  </div></div>

  <div v-if="showForm" class="modal-overlay" @click.self="showForm = false">
    <div class="modal"><h2>{{ editingId ? '编辑' : '新增' }}技能</h2>
      <div class="form-group"><label>名称</label><input v-model="form.name" /></div>
      <div class="form-group"><label>分类</label><input v-model="form.category" /></div>
      <div class="form-group"><label>熟练度</label><input v-model.number="form.level" type="number" min="0" max="100" /></div>
      <div class="form-group"><label>排序</label><input v-model.number="form.sortOrder" type="number" /></div>
      <div class="form-actions"><button class="btn-primary" @click="save">保存</button><button class="btn-ghost" @click="showForm = false">取消</button></div>
    </div>
  </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from "vue";
import { getAdminSkills, createSkill, updateSkill, deleteSkill } from "@/api/admin";
import AdminSidebar from "./AdminSidebar.vue";
const list = ref<any[]>([]);
const showForm = ref(false); const editingId = ref<number | null>(null);
const form = ref({ name: "", category: "", level: 50, sortOrder: 0 });
async function load() { try { const r: any = await getAdminSkills(); list.value = r.data || []; } catch (e) {} }
function openCreate() { editingId.value = null; form.value = { name: "", category: "", level: 50, sortOrder: 0 }; showForm.value = true; }
function openEdit(item: any) { editingId.value = item.id; form.value = { ...item }; showForm.value = true; }
async function save() { try { if (editingId.value) await updateSkill(editingId.value, form.value); else await createSkill(form.value); showForm.value = false; load(); } catch (e) {} }
async function handleDelete(id: number) { if (confirm("确定？")) { await deleteSkill(id); load(); } }
onMounted(load);
</script>

<style scoped>
.admin-page { padding-top: 80px; } .admin-layout { display: flex; gap: 32px; } .admin-main { flex: 1; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px; }
.page-header h1 { font-family: var(--font-serif); }
.btn-primary { padding: 10px 24px; background: var(--ink-black); color: var(--ink-white); border: none; border-radius: 4px; cursor: pointer; font-size: 14px; }
.btn-ghost { padding: 10px 24px; border: 1px solid var(--ink-black); background: none; border-radius: 4px; cursor: pointer; }
.btn-danger { color: var(--ink-seal); background: none; border: none; cursor: pointer; }
.data-table { width: 100%; border-collapse: collapse; background: var(--ink-white); border: 1px solid var(--ink-border); }
.data-table th, .data-table td { padding: 12px 16px; text-align: left; border-bottom: 1px solid var(--ink-border); font-size: 14px; }
.data-table th { background: var(--ink-bg); font-weight: 600; } .actions { display: flex; gap: 8px; }
.modal-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.4); display: flex; align-items: center; justify-content: center; z-index: 2000; }
.modal { background: var(--ink-white); padding: 32px; border-radius: 8px; max-width: 500px; width: 100%; max-height: 80vh; overflow-y: auto; }
.modal h2 { font-family: var(--font-serif); margin-bottom: 24px; } .form-group { margin-bottom: 16px; }
.form-group label { display: block; font-size: 13px; margin-bottom: 4px; }
.form-group input { width: 100%; padding: 8px 12px; border: 1px solid var(--ink-border); border-radius: 4px; font-size: 14px; }
.form-actions { display: flex; gap: 12px; margin-top: 24px; }
</style>
