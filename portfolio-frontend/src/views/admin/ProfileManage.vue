<template>
  <div class="admin-page">
    <div class="container"><div class="admin-layout">
      <AdminSidebar />
      <main class="admin-main">
        <h1>个人设置</h1>

        <!-- 基本信息 -->
        <div class="profile-section">
          <h2>基本信息</h2>

          <div class="avatar-section">
            <div class="avatar-preview">
              <img v-if="profile.avatar" :src="profile.avatar" alt="avatar" />
              <span v-else class="avatar-placeholder">墨</span>
            </div>
            <div class="avatar-input-row">
              <input type="file" accept="image/*" @change="handleAvatarUpload" ref="avatarInput" />
              <span class="upload-divider">或</span>
              <input v-model="profile.avatar" placeholder="直接粘贴图片 URL" />
            </div>
            <span v-if="uploading" class="upload-status">上传中...</span>
          </div>

          <div class="form-group">
            <label>用户名</label>
            <input v-model="profile.username" />
          </div>

          <div class="form-group">
            <label>个人简介</label>
            <textarea v-model="profile.intro" rows="4" placeholder="简短介绍自己，会展示在首页和关于页面"></textarea>
          </div>

          <div class="form-actions">
            <button class="btn-primary" @click="saveProfile" :disabled="saving">
              {{ saving ? '保存中...' : '保存信息' }}
            </button>
          </div>
          <p v-if="profileMsg" :class="profileMsgType">{{ profileMsg }}</p>
        </div>

        <!-- 修改密码 -->
        <div class="profile-section">
          <h2>修改密码</h2>
          <div class="form-group">
            <label>旧密码</label>
            <input v-model="pw.oldPassword" type="password" />
          </div>
          <div class="form-group">
            <label>新密码</label>
            <input v-model="pw.newPassword" type="password" />
          </div>
          <div class="form-actions">
            <button class="btn-primary" @click="savePassword" :disabled="pwSaving">
              {{ pwSaving ? '修改中...' : '修改密码' }}
            </button>
          </div>
          <p v-if="pwMsg" :class="pwMsgType">{{ pwMsg }}</p>
        </div>

      </main>
    </div></div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from "vue";
import { getProfile, updateProfile, updatePassword } from "@/api/admin";
import { uploadFile } from "@/api/upload";
import AdminSidebar from "./AdminSidebar.vue";

const profile = reactive({ username: "", avatar: "", intro: "" });
const pw = reactive({ oldPassword: "", newPassword: "" });

const uploading = ref(false);
const saving = ref(false);
const profileMsg = ref("");
const profileMsgType = ref("");

const pwSaving = ref(false);
const pwMsg = ref("");
const pwMsgType = ref("");

const avatarInput = ref<HTMLInputElement | null>(null);

onMounted(async () => {
  try {
    const res: any = await getProfile();
    if (res) {
      profile.username = res.data.username || "";
      profile.avatar = res.data.avatar || "";
      profile.intro = res.data.intro || "";
    }
  } catch (e) {
    console.error("Failed to load profile:", e);
  }
});

async function handleAvatarUpload(e: Event) {
  const target = e.target as HTMLInputElement;
  const file = target.files?.[0];
  if (!file) return;
  uploading.value = true;
  try {
    const res: any = await uploadFile(file);
    profile.avatar = res.data.url;
  } catch (err) {
    console.error("Upload failed:", err);
    alert("上传失败，请重试");
  } finally {
    uploading.value = false;
  }
}

async function saveProfile() {
  saving.value = true;
  profileMsg.value = "";
  try {
    await updateProfile({ username: profile.username, avatar: profile.avatar, intro: profile.intro });
    profileMsg.value = "保存成功";
    profileMsgType.value = "msg-success";
  } catch (e: any) {
    profileMsg.value = e?.response?.data?.message || "保存失败";
    profileMsgType.value = "msg-error";
  } finally {
    saving.value = false;
  }
}

async function savePassword() {
  if (!pw.oldPassword || !pw.newPassword) {
    pwMsg.value = "请填写旧密码和新密码";
    pwMsgType.value = "msg-error";
    return;
  }
  pwSaving.value = true;
  pwMsg.value = "";
  try {
    await updatePassword({ oldPassword: pw.oldPassword, newPassword: pw.newPassword });
    pwMsg.value = "密码修改成功";
    pwMsgType.value = "msg-success";
    pw.oldPassword = "";
    pw.newPassword = "";
  } catch (e: any) {
    pwMsg.value = e?.response?.data?.message || "修改失败";
    pwMsgType.value = "msg-error";
  } finally {
    pwSaving.value = false;
  }
}
</script>

<style scoped>
.admin-page { padding-top: 80px; }
.admin-layout { display: flex; gap: 32px; }
.admin-main { flex: 1; }
.admin-main h1 { font-family: var(--font-serif); margin-bottom: 24px; }

.profile-section {
  background: var(--ink-white);
  border: 1px solid var(--ink-border);
  border-radius: 8px;
  padding: 24px;
  margin-bottom: 24px;
}
.profile-section h2 {
  font-family: var(--font-serif);
  font-size: 18px;
  margin-bottom: 20px;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--ink-border);
}

.avatar-section { margin-bottom: 20px; }
.avatar-preview {
  width: 100px; height: 100px; border-radius: 50%;
  margin-bottom: 12px; border: 2px solid var(--ink-border);
  display: flex; align-items: center; justify-content: center; overflow: hidden;
}
.avatar-preview img { width: 100%; height: 100%; object-fit: cover; }
.avatar-placeholder { font-family: var(--font-serif); font-size: 40px; color: var(--ink-light); }
.avatar-input-row { display: flex; align-items: center; gap: 8px; max-width: 500px; }
.avatar-input-row input[type="file"] { flex: 1; font-size: 13px; }
.avatar-input-row input[type="text"] { flex: 2; }
.upload-divider { color: var(--ink-light); font-size: 13px; }
.upload-status { display: block; margin-top: 6px; font-size: 13px; color: var(--ink-gray); }

.form-group { margin-bottom: 16px; }
.form-group label { display: block; font-size: 13px; margin-bottom: 4px; }
.form-group input[type="text"],
.form-group input[type="password"],
.form-group textarea {
  width: 100%; padding: 8px 12px;
  border: 1px solid var(--ink-border); border-radius: 4px;
  font-size: 14px; font-family: inherit;
}
.form-group textarea { resize: vertical; }

.form-actions { margin-top: 16px; }
.btn-primary {
  padding: 10px 24px; background: var(--ink-black); color: var(--ink-white);
  border: none; border-radius: 4px; cursor: pointer; font-size: 14px;
}
.btn-primary:disabled { opacity: 0.5; cursor: not-allowed; }

.msg-success { color: #2e7d32; font-size: 13px; margin-top: 8px; }
.msg-error { color: var(--ink-seal); font-size: 13px; margin-top: 8px; }
</style>