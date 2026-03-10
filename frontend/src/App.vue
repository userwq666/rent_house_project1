<template>
  <router-view />
</template>

<script>
</script>

<style>
:root {
  /* 南孚风格配色 - 活力橙 + 专业黑 */
  --primary-orange: #FF6600;
  --primary-orange-hover: #E55C00;
  --primary-black: #1A1A1A;
  --secondary-gray: #666666;
  --light-gray: #F5F5F5;
  --border-color: #E0E0E0;
  
  --bg: #FFFFFF;
  --text: #1A1A1A;
  --text-light: #666666;
  --muted: #999999;
  --card: #FFFFFF;
  --border: #E0E0E0;
  --accent: #FF6600; /* 活力橙 */
  --accent-hover: #E55C00;
  --success: #00B894;
  --warning: #FFAB00;
  --danger: #FF7675;
  
  --shadow-sm: 0 2px 8px rgba(0, 0, 0, 0.08);
  --shadow: 0 4px 16px rgba(0, 0, 0, 0.12);
  --shadow-lg: 0 8px 32px rgba(0, 0, 0, 0.16);
  --shadow-glow: 0 0 24px rgba(255, 102, 0, 0.3);
  
  --radius: 8px;
  --transition: all 0.3s cubic-bezier(0.25, 0.8, 0.25, 1);
  --font-main: "PingFang SC", "Microsoft YaHei", "Helvetica Neue", Arial, sans-serif;
}

*,
*::before,
*::after {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

body {
  margin: 0;
  min-height: 100vh;
  font-family: var(--font-main);
  background: var(--bg);
  color: var(--text);
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  line-height: 1.5;
}

#app {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

/* 自定义滚动条 */
::-webkit-scrollbar {
  width: 8px;
  height: 8px;
}

::-webkit-scrollbar-track {
  background: transparent;
}

::-webkit-scrollbar-thumb {
  background: #cbd5e1;
  border-radius: 4px;
}

::-webkit-scrollbar-thumb:hover {
  background: #94a3b8;
}

/* 通用样式类 */
.glass {
  background: var(--card);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border: 1px solid rgba(255, 255, 255, 0.5);
  box-shadow: var(--shadow);
}

.glow {
  background: var(--accent) !important;
  border-color: var(--accent) !important;
  color: #fff !important;
  box-shadow: var(--shadow-glow);
  transition: var(--transition);
}

.glow:hover {
  background: var(--accent-hover) !important;
  border-color: var(--accent-hover) !important;
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(79, 70, 229, 0.4);
}

/* Element Plus 覆盖样式 */
.el-button {
  font-weight: 500;
  border-radius: 8px;
}

.el-input__wrapper {
  border-radius: 8px;
  box-shadow: 0 0 0 1px var(--border) inset;
}

.el-input__wrapper.is-focus {
  box-shadow: 0 0 0 1px var(--accent) inset !important;
}

/* 动画 */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
