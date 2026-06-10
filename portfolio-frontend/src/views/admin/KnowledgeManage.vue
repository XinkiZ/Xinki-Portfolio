<template>
  <div class="admin-page"><div class="container"><div class="admin-layout">
    <AdminSidebar /><main class="admin-main">
      <div class="page-header">
        <h1>知识库管理</h1>
        <div class="header-actions">
          <button class="btn-primary" @click="triggerUpload">+ 导入文件</button>
          <input ref="fileInput" type="file" accept=".pdf,.md,.txt,.markdown" style="display:none" @change="handleFileChange" />
        </div>
      </div>

      <!-- Import progress -->
      <div v-if="importing" class="import-status">正在导入并生成向量索引，请稍候...</div>

      <!-- Import result -->
      <div v-if="importResult" class="import-result">
        <div class="result-header">
          <strong>{{ importResult.fileName }}</strong>
          <span class="result-ok">成功 {{ importResult.successCount }} 条</span>
          <span v-if="importResult.failedCount > 0" class="result-err">跳过 {{ importResult.failedCount }} 条</span>
        </div>
        <div v-if="importResult.errors && importResult.errors.length" class="result-errors">
          <div v-for="(e, i) in importResult.errors" :key="i">片段 #{{ e.chunkIndex + 1 }}: {{ e.reason }}</div>
        </div>
      </div>

      <!-- File grouping -->
      <div v-if="fileGroups.length === 0 && !importing" class="empty-state">暂无知识库文件，请导入 PDF / MD / TXT 文件</div>

      <div v-for="group in fileGroups" :key="group.sourceHash" class="file-group">
        <div class="file-group-header" @click="group.expanded = !group.expanded">
          <span class="file-icon">📄</span>
          <span class="file-name">{{ group.sourceFile }}</span>
          <span class="file-count">{{ group.chunks.length }} 片段</span>
          <span class="file-arrow">{{ group.expanded ? '▾' : '▸' }}</span>
        </div>
        <div v-if="group.expanded" class="file-group-body">
          <div class="file-group-actions">
            <button class="btn-danger-sm" @click="handleDeleteFile(group.sourceHash)">删除此文件全部片段</button>
          </div>
          <table class="chunk-table">
            <thead><tr><th>#</th><th>内容预览</th><th>操作</th></tr></thead>
            <tbody>
              <tr v-for="chunk in group.chunks" :key="chunk.id">
                <td>{{ chunk.chunkIndex + 1 }}</td>
                <td class="chunk-preview">{{ truncate(chunk.content, 80) }}</td>
                <td class="actions">
                  <button @click="openEdit(chunk)">编辑</button>
                  <button @click="handleDelete(chunk.id)" class="btn-danger">删除</button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </main>
  </div></div>

  <!-- Edit modal -->
  <div v-if="showForm" class="modal-overlay" @click.self="showForm = false">
    <div class="modal">
      <h2>编辑知识片段</h2>
      <div class="form-group"><label>文本内容</label><textarea v-model="form.content" rows="6"></textarea></div>
      <div class="form-group"><label>分类</label><input v-model="form.category" /></div>
      <div class="form-actions">
        <button class="btn-primary" @click="save">保存</button>
        <button class="btn-ghost" @click="showForm = false">取消</button>
      </div>
    </div>
  </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from "vue";
import { getAdminKnowledge, createKnowledge, updateKnowledge, deleteKnowledge, importKnowledge, deleteKnowledgeFile } from "@/api/admin";
import AdminSidebar from "./AdminSidebar.vue";

const list = ref<any[]>([]);
const fileInput = ref<HTMLInputElement>();
const importing = ref(false);
const importResult = ref<any>(null);
const showForm = ref(false);
const editingId = ref<number | null>(null);
const form = ref({ content: "", category: "" });

function truncate(s: string, n: number) { return s && s.length > n ? s.slice(0, n) + "..." : s; }

async function load() {
  try { const r: any = await getAdminKnowledge({ page: 1, pageSize: 500 }); list.value = r.data.records || []; } catch (e) {}
}

// Group chunks by source_hash
const fileGroups = computed(() => {
  const map = new Map<string, { sourceHash: string; sourceFile: string; chunks: any[]; expanded: boolean }>();
  for (const item of list.value) {
    const hash = item.sourceHash || "__orphan__";
    if (!map.has(hash)) {
      map.set(hash, { sourceHash: hash, sourceFile: item.sourceFile || "手动添加", chunks: [], expanded: false });
    }
    map.get(hash)!.chunks.push(item);
  }
  // Sort by created_at desc within each group
  for (const [, g] of map) { g.chunks.sort((a: any, b: any) => (a.chunkIndex || 0) - (b.chunkIndex || 0)); }
  return [...map.values()];
});

function triggerUpload() { fileInput.value?.click(); }

async function handleFileChange(e: Event) {
  const input = e.target as HTMLInputElement;
  const file = input.files?.[0];
  if (!file) return;
  importing.value = true;
  importResult.value = null;
  try {
    const res: any = await importKnowledge(file);
    importResult.value = res.data;
    await load();
  } catch (e: any) {
    alert("导入失败: " + (e?.message || "未知错误"));
  } finally {
    importing.value = false;
    if (input) input.value = "";
  }
}

function openEdit(item: any) { editingId.value = item.id; form.value = { content: item.content, category: item.category || "" }; showForm.value = true; }

async function save() {
  try {
    if (editingId.value) await updateKnowledge(editingId.value, form.value);
    showForm.value = false;
    load();
  } catch (e) {}
}

async function handleDelete(id: number) { if (confirm("确定删除此片段？")) { await deleteKnowledge(id); load(); } }

async function handleDeleteFile(hash: string) {
  if (confirm("确定删除此文件的所有知识片段？此操作不可撤销。")) {
    try {
      const r: any = await deleteKnowledgeFile(hash);
      alert(r.data || "已删除");
      load();
    } catch (e) {}
  }
}

onMounted(load);
</script>

<style scoped>
.admin-page { padding-top: 80px; } .admin-layout { display: flex; gap: 32px; }
.admin-main { flex: 1; } .page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px; }
.page-header h1 { font-family: var(--font-serif); }
.header-actions { display: flex; gap: 12px; }
.btn-primary { padding: 10px 24px; background: var(--ink-black); color: var(--ink-white); border: none; border-radius: 4px; cursor: pointer; font-size: 14px; }
.btn-ghost { padding: 10px 24px; border: 1px solid var(--ink-black); background: none; border-radius: 4px; cursor: pointer; }
.btn-danger { color: var(--ink-seal); background: none; border: none; cursor: pointer; }
.btn-danger-sm { padding: 4px 12px; background: var(--ink-seal); color: #fff; border: none; border-radius: 4px; cursor: pointer; font-size: 12px; }

.import-status { padding: 16px 24px; background: var(--ink-bg); border: 1px solid var(--ink-border); border-radius: 8px; margin-bottom: 20px; text-align: center; font-size: 14px; }
.import-result { padding: 16px 20px; border: 1px solid var(--ink-border); border-radius: 8px; margin-bottom: 20px; background: #f9fafb; }
.result-header { display: flex; align-items: center; gap: 12px; font-size: 14px; margin-bottom: 8px; }
.result-ok { color: #2e7d32; } .result-err { color: var(--ink-seal); }
.result-errors { font-size: 12px; color: #666; margin-top: 4px; padding-left: 12px; border-left: 2px solid #eee; }
.result-errors div { margin: 2px 0; }

.empty-state { text-align: center; padding: 60px 0; color: #999; font-size: 15px; }

.file-group { border: 1px solid var(--ink-border); border-radius: 8px; margin-bottom: 12px; overflow: hidden; }
.file-group-header { display: flex; align-items: center; gap: 10px; padding: 14px 20px; background: var(--ink-bg); cursor: pointer; user-select: none; }
.file-group-header:hover { background: #eef0f2; }
.file-icon { font-size: 18px; } .file-name { font-weight: 600; font-size: 14px; flex: 1; }
.file-count { font-size: 12px; color: #888; } .file-arrow { font-size: 14px; color: #666; }
.file-group-body { padding: 0 20px 20px; }
.file-group-actions { padding: 12px 0 8px; }

.chunk-table { width: 100%; border-collapse: collapse; }
.chunk-table th, .chunk-table td { padding: 8px 12px; text-align: left; border-bottom: 1px solid var(--ink-border); font-size: 13px; }
.chunk-table th { font-weight: 600; color: #666; }
.chunk-preview { max-width: 400px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.actions { display: flex; gap: 8px; }

.modal-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.4); display: flex; align-items: center; justify-content: center; z-index: 2000; }
.modal { background: var(--ink-white); padding: 32px; border-radius: 8px; max-width: 600px; width: 100%; max-height: 80vh; overflow-y: auto; }
.modal h2 { font-family: var(--font-serif); margin-bottom: 24px; }
.form-group { margin-bottom: 16px; } .form-group label { display: block; font-size: 13px; margin-bottom: 4px; }
.form-group input, .form-group textarea { width: 100%; padding: 8px 12px; border: 1px solid var(--ink-border); border-radius: 4px; font-size: 14px; }
.form-actions { display: flex; gap: 12px; margin-top: 24px; }
</style>