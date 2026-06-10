<template>
  <div class="admin-login-page">
    <div class="login-card fade-up">
      <h1>管理后台</h1>
      <form @submit.prevent="login">
        <div class="form-group"><input v-model="form.username" type="text" placeholder="用户名" required /></div>
        <div class="form-group"><input v-model="form.password" type="password" placeholder="密码" required /></div>
        <p v-if="error" class="error">{{ error }}</p>
        <button type="submit" class="btn-primary" :disabled="loading">{{ loading ? '登录中...' : '登录' }}</button>
      </form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from "vue";
import { useRouter } from "vue-router";
import { login as apiLogin } from "@/api/admin";

const router = useRouter();
const form = reactive({ username: "admin", password: "" });
const loading = ref(false);
const error = ref("");

async function login() {
  loading.value = true; error.value = "";
  try {
    const res: any = await apiLogin({ ...form });
    localStorage.setItem("admin_token", res.data.token);
    router.push("/admin");
  } catch (e: any) { error.value = "登录失败"; }
  finally { loading.value = false; }
}
</script>

<style scoped>
.admin-login-page { min-height: 100vh; display: flex; align-items: center; justify-content: center; background: var(--ink-bg); }
.login-card { background: var(--ink-white); padding: 48px; border: 1px solid var(--ink-border); border-radius: 4px; width: 360px; }
.login-card h1 { font-family: var(--font-serif); text-align: center; margin-bottom: 32px; }
.form-group { margin-bottom: 16px; }
.form-group input { width: 100%; padding: 12px; border: 1px solid var(--ink-border); border-radius: 4px; font-size: 15px; }
.error { color: var(--ink-seal); font-size: 14px; margin-bottom: 16px; }
.btn-primary { width: 100%; padding: 14px; font-size: 16px; background: var(--ink-black); color: var(--ink-white); border: none; border-radius: 4px; cursor: pointer; }
</style>
