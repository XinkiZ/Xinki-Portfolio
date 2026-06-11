<template>
  <div class="admin-page">
    <div class="container"><div class="admin-layout">
      <AdminSidebar />
      <main class="admin-main">
        <div class="page-header">
          <h1>AI 文档分析助手</h1>
        </div>
        <p class="page-desc">上传 PDF 或 README 文件，AI 将自动提取项目信息和技能列表，一键填入到管理后台。</p>

        <!-- Upload Area -->
        <div v-if="!result" class="upload-section">
          <div
            class="upload-zone"
            :class="{ dragging: isDragging }"
            @dragover.prevent="isDragging = true"
            @dragleave="isDragging = false"
            @drop.prevent="handleDrop"
            @click="triggerFileInput"
          >
            <div class="upload-icon">📄</div>
            <p v-if="!analyzing">拖拽文件到此处，或点击选择文件</p>
            <p v-else class="analyzing-text">AI 正在分析文档中...</p>
            <span class="upload-hint">支持 PDF、Markdown、TXT 格式，限 10MB</span>
          </div>
          <input
            ref="fileInput"
            type="file"
            accept=".pdf,.md,.txt,.markdown,README"
            @change="handleFileSelect"
            style="display: none"
          />
          <div v-if="errorMsg" class="error-msg">{{ errorMsg }}</div>
        </div>

        <!-- Results Area -->
        <div v-if="result" class="results-section">
          <div class="results-actions">
            <button class="btn-back" @click="reset">← 重新上传</button>
          </div>

          <div class="results-grid">
            <!-- Project Panel -->
            <div class="result-panel project-panel">
              <h2 class="panel-title">📁 提取的作品信息</h2>
              <div class="form-group">
                <label>标题</label>
                <input v-model="editableProject.title" />
              </div>
              <div class="form-group">
                <label>简介（卡片展示）</label>
                <input v-model="editableProject.summary" maxlength="500" placeholder="一句话介绍，用于作品卡片展示" />
              </div>
              <div class="form-group">
                <label>详细描述（支持 HTML）</label>
                <textarea v-model="editableProject.description" rows="6"></textarea>
              </div>
              <div class="form-group">
                <label>标签（逗号分隔）</label>
                <input v-model="editableProject.tags" />
              </div>
              <div class="form-group">
                <label>演示 URL</label>
                <input v-model="editableProject.demoUrl" />
              </div>
              <div class="form-group">
                <label>源码 URL</label>
                <input v-model="editableProject.githubUrl" />
              </div>
              <button class="btn-primary btn-save" @click="saveProject" :disabled="savingProject">
                {{ savingProject ? '保存中...' : '保存为作品' }}
              </button>
              <span v-if="projectSaved" class="saved-tip">✓ 已保存</span>
            </div>

            <!-- Skills Panel -->
            <div class="result-panel skills-panel">
              <h2 class="panel-title">🛠 提取的技能列表</h2>
              <div v-if="editableSkills.length === 0" class="empty-skills">
                未检测到技能，请尝试上传更详细的文档。
              </div>
              <div v-for="(skill, index) in editableSkills" :key="index" class="skill-item">
                <div class="skill-row">
                  <input v-model="skill.name" placeholder="技能名称" class="skill-name" />
                  <input v-model="skill.category" placeholder="分类" class="skill-category" />
                  <div class="skill-level-group">
                    <label>熟练度</label>
                    <input v-model.number="skill.level" type="number" min="1" max="100" class="skill-level" />
                  </div>
                  <div class="skill-actions">
                    <button class="btn-save-skill" @click="saveSingleSkill(index)" :disabled="skill._saving">
                      {{ skill._saved ? '✓ 已保存' : skill._saving ? '...' : '保存' }}
                    </button>
                    <button class="btn-remove-skill" @click="removeSkill(index)">×</button>
                  </div>
                </div>
              </div>
              <div class="skills-footer">
                <button class="btn-add-skill" @click="addSkill">+ 手动添加技能</button>
                <button class="btn-primary btn-save-all" @click="saveAllSkills" :disabled="savingAllSkills">
                  {{ savingAllSkills ? '保存中...' : '一键保存全部技能' }}
                </button>
              </div>
            </div>
          </div>
        </div>
      </main>
    </div></div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from "vue";
import { analyzeDocument, createProject, createSkill } from "@/api/admin";
import AdminSidebar from "./AdminSidebar.vue";

const fileInput = ref<HTMLInputElement | null>(null);
const isDragging = ref(false);
const analyzing = ref(false);
const errorMsg = ref("");
const result = ref<any>(null);

const editableProject = reactive({
  title: "",
  summary: "",
  description: "",
  tags: "",
  demoUrl: "",
  githubUrl: "",
});

interface EditableSkill {
  name: string;
  category: string;
  level: number;
  _saving: boolean;
  _saved: boolean;
}
const editableSkills = ref<EditableSkill[]>([]);
const savingProject = ref(false);
const projectSaved = ref(false);
const savingAllSkills = ref(false);

function handleFileSelect(e: Event) {
  const target = e.target as HTMLInputElement;
  const file = target.files?.[0];
  if (file) analyze(file);
}

function handleDrop(e: DragEvent) {
  isDragging.value = false;
  const file = e.dataTransfer?.files?.[0];
  if (file) analyze(file);
}

async function analyze(file: File) {
  const allowedExts = [".pdf", ".md", ".txt", ".markdown"];
  const ext = "." + file.name.split(".").pop()?.toLowerCase();
  const isReadme = file.name.toLowerCase() === "readme";
  if (!isReadme && !allowedExts.includes(ext)) {
    errorMsg.value = "不支持的文件格式，请上传 PDF/Markdown/TXT 文件";
    return;
  }
  errorMsg.value = "";
  analyzing.value = true;
  try {
    const res: any = await analyzeDocument(file);
    const data = res.data || res;
    result.value = data;
    editableProject.title = data.project?.title || "";
    editableProject.description = data.project?.description || "";
    editableProject.tags = data.project?.tags || "";
    editableProject.demoUrl = data.project?.demoUrl || "";
    editableProject.githubUrl = data.project?.githubUrl || "";
    editableSkills.value = (data.skills || []).map((s: any) => ({
      name: s.name || "",
      category: s.category || "",
      level: s.level || 50,
      _saving: false,
      _saved: false,
    }));
    projectSaved.value = false;
  } catch (e: any) {
    errorMsg.value = e?.message || "分析失败，请重试";
  } finally {
    analyzing.value = false;
  }
}

async function saveProject() {
  if (!editableProject.title.trim()) {
    alert("请填写作品标题");
    return;
  }
  savingProject.value = true;
  try {
    await createProject({
      title: editableProject.title,
      summary: editableProject.summary,
      description: editableProject.description,
      tags: editableProject.tags,
      demoUrl: editableProject.demoUrl,
      githubUrl: editableProject.githubUrl,
      coverUrl: "",
      sortOrder: 0,
      isPublished: 1,
    });
    projectSaved.value = true;
  } catch (e: any) {
    alert("保存失败：" + (e?.message || ""));
  } finally {
    savingProject.value = false;
  }
}

async function saveSingleSkill(index: number) {
  const skill = editableSkills.value[index];
  if (!skill.name.trim()) return;
  skill._saving = true;
  try {
    await createSkill({
      name: skill.name,
      category: skill.category || "未分类",
      level: skill.level || 50,
      icon: "",
      sortOrder: 0,
    });
    skill._saved = true;
  } catch (e: any) {
    alert("保存失败：" + (e?.message || ""));
  } finally {
    skill._saving = false;
  }
}

async function saveAllSkills() {
  savingAllSkills.value = true;
  let count = 0;
  for (let i = 0; i < editableSkills.value.length; i++) {
    const skill = editableSkills.value[i];
    if (!skill.name.trim()) continue;
    skill._saving = true;
    try {
      await createSkill({
        name: skill.name,
        category: skill.category || "未分类",
        level: skill.level || 50,
        icon: "",
        sortOrder: 0,
      });
      skill._saved = true;
      count++;
    } catch (e) {
      // continue
    }
    skill._saving = false;
  }
  savingAllSkills.value = false;
  if (count > 0) alert("成功保存 " + count + " 个技能");
}

function addSkill() {
  editableSkills.value.push({
    name: "",
    category: "",
    level: 50,
    _saving: false,
    _saved: false,
  });
}

function removeSkill(index: number) {
  editableSkills.value.splice(index, 1);
}

function triggerFileInput() { fileInput.value?.click(); }

function reset() {
  result.value = null;
  errorMsg.value = "";
  if (fileInput.value) fileInput.value.value = "";
}
</script>

<style scoped>
.admin-page { padding-top: 80px; }
.admin-layout { display: flex; gap: 32px; }
.admin-main { flex: 1; }
.page-header { margin-bottom: 8px; }
.page-header h1 { font-family: var(--font-serif); }
.page-desc { color: var(--ink-light); font-size: 14px; margin-bottom: 32px; }

/* Upload */
.upload-section { max-width: 600px; }
.upload-zone {
  border: 2px dashed var(--ink-border);
  border-radius: 12px;
  padding: 64px 32px;
  text-align: center;
  cursor: pointer;
  transition: all 0.3s ease;
  background: var(--ink-bg);
}
.upload-zone:hover, .upload-zone.dragging {
  border-color: var(--ink-black);
  background: var(--ink-paper);
}
.upload-icon { font-size: 48px; margin-bottom: 16px; }
.upload-zone p { font-size: 15px; color: var(--ink-black); margin-bottom: 8px; }
.analyzing-text { color: var(--ink-seal) !important; font-weight: 500; animation: pulse 1.5s infinite; }
@keyframes pulse { 0%, 100% { opacity: 1; } 50% { opacity: 0.5; } }
.upload-hint { font-size: 13px; color: var(--ink-light); }
.error-msg { margin-top: 12px; color: var(--ink-seal); font-size: 14px; }

/* Results */
.results-section { margin-top: 0; }
.results-actions { margin-bottom: 24px; }
.btn-back {
  background: none; border: 1px solid var(--ink-border); border-radius: 4px;
  padding: 8px 16px; cursor: pointer; font-size: 14px;
  color: var(--ink-light); transition: all 0.2s;
}
.btn-back:hover { border-color: var(--ink-black); color: var(--ink-black); }
.results-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 32px; }
.result-panel {
  background: var(--ink-white);
  border: 1px solid var(--ink-border);
  border-radius: 8px;
  padding: 24px;
}
.panel-title {
  font-family: var(--font-serif);
  font-size: 18px;
  margin-bottom: 20px;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--ink-border);
}
.form-group { margin-bottom: 16px; }
.form-group label { display: block; font-size: 13px; color: var(--ink-light); margin-bottom: 4px; }
.form-group input[type="text"],
.form-group textarea {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid var(--ink-border);
  border-radius: 4px;
  font-size: 14px;
  font-family: inherit;
  background: var(--ink-bg);
  transition: border-color 0.2s;
}
.form-group input:focus,
.form-group textarea:focus {
  outline: none;
  border-color: var(--ink-black);
  background: var(--ink-white);
}

/* Buttons */
.btn-primary {
  padding: 10px 24px;
  background: var(--ink-black);
  color: var(--ink-white);
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  transition: opacity 0.2s;
}
.btn-primary:disabled { opacity: 0.5; cursor: not-allowed; }
.btn-save { width: 100%; margin-top: 8px; }
.saved-tip { display: inline-block; margin-left: 12px; color: #4caf50; font-size: 14px; }

/* Skills */
.empty-skills { color: var(--ink-light); font-size: 14px; text-align: center; padding: 32px 0; }
.skill-item { margin-bottom: 12px; }
.skill-row { display: flex; gap: 8px; align-items: center; flex-wrap: wrap; }
.skill-name { flex: 2; min-width: 100px; }
.skill-category { flex: 1.5; min-width: 80px; }
.skill-level-group { display: flex; align-items: center; gap: 4px; flex: 0.8; }
.skill-level-group label { font-size: 12px; color: var(--ink-light); white-space: nowrap; }
.skill-level { width: 60px; }
.skill-row input {
  padding: 8px 10px;
  border: 1px solid var(--ink-border);
  border-radius: 4px;
  font-size: 13px;
  background: var(--ink-bg);
}
.skill-row input:focus { outline: none; border-color: var(--ink-black); }
.skill-actions { display: flex; gap: 4px; }
.btn-save-skill {
  padding: 8px 12px;
  background: none;
  border: 1px solid var(--ink-border);
  border-radius: 4px;
  cursor: pointer;
  font-size: 13px;
  white-space: nowrap;
  min-width: 64px;
}
.btn-save-skill:disabled { opacity: 0.5; cursor: not-allowed; }
.btn-save-skill:hover:not(:disabled) { border-color: var(--ink-black); }
.btn-remove-skill {
  padding: 8px;
  background: none; border: none;
  cursor: pointer; color: var(--ink-light);
  font-size: 16px;
}
.btn-remove-skill:hover { color: var(--ink-seal); }

.skills-footer { margin-top: 20px; padding-top: 16px; border-top: 1px solid var(--ink-border); display: flex; gap: 12px; align-items: center; }
.btn-add-skill {
  padding: 8px 16px;
  background: none;
  border: 1px dashed var(--ink-border);
  border-radius: 4px;
  cursor: pointer;
  font-size: 13px;
  color: var(--ink-light);
}
.btn-add-skill:hover { border-color: var(--ink-black); color: var(--ink-black); }
.btn-save-all { flex: 1; }

@media (max-width: 900px) {
  .results-grid { grid-template-columns: 1fr; }
}
</style>