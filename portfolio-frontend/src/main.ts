import { createApp } from "vue";
import { createPinia } from "pinia";
import App from "./App.vue";
import router from "./router";
import "./styles/global.css";
import "./styles/ink-effects.css";

const app = createApp(App);
app.use(createPinia());
app.use(router);
app.mount("#app");
