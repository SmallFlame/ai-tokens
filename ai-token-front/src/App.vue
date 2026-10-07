<template>
  <div id="app">
    <router-view />
  </div>
</template>

<style>
* { margin: 0; padding: 0; box-sizing: border-box; }
body {
  font-family: -apple-system, BlinkMacSystemFont, 'PingFang SC', 'Microsoft YaHei', sans-serif;
  background: #f0f2f5;
  color: #303133;
  font-size: 16px;
}
.page-card {
  background: #fff;
  border-radius: 4px;
  padding: 24px;
  box-shadow: 0 1px 4px rgba(0,21,41,.08);
}
.toolbar {
  margin-bottom: 20px;
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}
.danger { color: #f56c6c !important; }
.danger:hover { color: #f56c6c !important; }

/* ── Element UI 全局字体放大覆写 ── */
.el-table,
.el-table th .cell,
.el-table td .cell         { font-size: 15px; }
.el-table th .cell         { font-weight: 600; }

.el-form-item__label       { font-size: 15px; }
.el-input__inner           { font-size: 15px; }
.el-textarea__inner        { font-size: 15px; }

.el-button                 { font-size: 15px; }
.el-button--text           { font-size: 15px; }

.el-select-dropdown__item  { font-size: 15px; }
.el-dropdown-menu__item    { font-size: 15px; }

.el-dialog__title          { font-size: 18px; font-weight: 600; }
.el-dialog__body           { font-size: 15px; }

.el-menu-item              { font-size: 15px; }
.el-submenu__title         { font-size: 15px; }

.el-tag                    { font-size: 13px; }
.el-message                { font-size: 15px; }
.el-message-box__message   { font-size: 15px; }
.el-pagination             { font-size: 14px; }
</style>
