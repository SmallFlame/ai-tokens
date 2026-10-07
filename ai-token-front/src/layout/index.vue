<template>
  <el-container class="layout-wrap">
    <!-- 侧边栏 -->
    <el-aside :width="collapse ? '64px' : '200px'" class="aside">
      <div class="logo">
        <i class="el-icon-magic-stick"></i>
        <span v-show="!collapse" class="logo-text">AI-Token</span>
      </div>

      <el-menu
        :router="true"
        :collapse="collapse"
        :default-active="$route.path"
        background-color="#304156"
        text-color="#bfcbd9"
        active-text-color="#409EFF"
        class="side-menu"
      >
        <el-menu-item index="/dashboard">
          <i class="el-icon-data-analysis"></i>
          <span slot="title">数据看板</span>
        </el-menu-item>

        <template v-if="isSuperAdmin">
          <el-menu-item index="/channel">
            <i class="el-icon-connection"></i>
            <span slot="title">渠道管理</span>
          </el-menu-item>
          <el-menu-item index="/model">
            <i class="el-icon-cpu"></i>
            <span slot="title">模型管理</span>
          </el-menu-item>
          <el-menu-item index="/apikey">
            <i class="el-icon-key"></i>
            <span slot="title">API Key</span>
          </el-menu-item>
          <el-menu-item index="/log">
            <i class="el-icon-document"></i>
            <span slot="title">调用日志</span>
          </el-menu-item>
          <el-menu-item index="/announcement">
            <i class="el-icon-bell"></i>
            <span slot="title">公告管理</span>
          </el-menu-item>
        </template>

        <el-menu-item v-if="isSuperAdmin" index="/channelstat">
          <i class="el-icon-pie-chart"></i>
          <span slot="title">渠道统计</span>
        </el-menu-item>

        <el-menu-item v-if="isSuperAdmin" index="/user">
          <i class="el-icon-user"></i>
          <span slot="title">用户管理</span>
        </el-menu-item>
        <el-menu-item v-if="isSuperAdmin" index="/userstat">
          <i class="el-icon-s-data"></i>
          <span slot="title">用户统计</span>
        </el-menu-item>
        <el-menu-item v-if="!isSuperAdmin && isAdmin" index="/admin-users">
          <i class="el-icon-user"></i>
          <span slot="title">用户管理</span>
        </el-menu-item>
        <el-menu-item v-if="isAdmin" index="/group">
          <i class="el-icon-s-custom"></i>
          <span slot="title">小组管理</span>
        </el-menu-item>
        <el-menu-item v-if="isAdmin" index="/duplicates">
          <i class="el-icon-warning-outline"></i>
          <span slot="title">重复账号排查</span>
        </el-menu-item>
        <el-menu-item v-if="isAdmin" index="/recharge-user">
          <i class="el-icon-money"></i>
          <span slot="title">个人充值日志</span>
        </el-menu-item>
        <el-menu-item v-if="isAdmin" index="/recharge-group">
          <i class="el-icon-bank-card"></i>
          <span slot="title">小组充值日志</span>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <!-- 右侧主区域 -->
    <el-container class="right-container">
      <el-header class="header">
        <div class="header-left">
          <i
            :class="collapse ? 'el-icon-s-unfold' : 'el-icon-s-fold'"
            class="collapse-btn"
            @click="collapse = !collapse"
          />
          <span class="page-title">{{ $route.meta.title }}</span>
        </div>
        <div class="header-right">
          <el-dropdown @command="onCommand">
            <span class="user-btn">
              <i class="el-icon-user-solid" /> {{ username }}
              <i class="el-icon-arrow-down el-icon--right" />
            </span>
            <el-dropdown-menu slot="dropdown">
              <el-dropdown-item command="portal">
                <i class="el-icon-user" /> 用户中心
              </el-dropdown-item>
              <el-dropdown-item command="logout" divided>
                <i class="el-icon-switch-button" /> 退出登录
              </el-dropdown-item>
            </el-dropdown-menu>
          </el-dropdown>
        </div>
      </el-header>

      <el-main class="main-wrap">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script>
import { mapGetters } from 'vuex'

export default {
  name: 'Layout',
  data() {
    return { collapse: false }
  },
  computed: {
    ...mapGetters(['isAdmin', 'isSuperAdmin', 'username'])
  },
  methods: {
    onCommand(cmd) {
      if (cmd === 'logout') {
        this.$store.commit('CLEAR_AUTH')
        this.$router.push('/login')
      } else if (cmd === 'portal') {
        this.$router.push('/portal/overview')
      }
    }
  }
}
</script>

<style scoped>
.layout-wrap { height: 100vh; overflow: hidden; }

/* 侧边栏 */
.aside {
  background: #304156;
  transition: width 0.25s;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}
.logo {
  height: 68px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 20px;
  font-weight: bold;
  gap: 8px;
  flex-shrink: 0;
  overflow: hidden;
  white-space: nowrap;
}
.logo .el-icon-magic-stick { font-size: 26px; color: #409EFF; }
.logo-text { font-size: 18px; }
.side-menu { border-right: none; flex: 1; }

/* 头部 */
.right-container { overflow: hidden; }
.header {
  background: #fff;
  border-bottom: 1px solid #e6e6e6;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  flex-shrink: 0;
}
.header-left { display: flex; align-items: center; gap: 14px; }
.collapse-btn { font-size: 24px; cursor: pointer; color: #606266; }
.collapse-btn:hover { color: #409EFF; }
.page-title { font-size: 18px; font-weight: 600; color: #303133; }

.user-btn { cursor: pointer; color: #606266; font-size: 16px; }
.user-btn:hover { color: #409EFF; }

/* 主内容 */
.main-wrap { background: #f0f2f5; overflow: auto; padding: 24px; }
</style>
