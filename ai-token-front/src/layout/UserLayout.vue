<template>
  <el-container class="portal-wrap">
    <!-- 顶部导航 -->
    <el-header class="portal-header">
      <div class="header-left">
        <i class="el-icon-magic-stick logo-icon" />
        <span class="logo-text">AI Token 用户中心</span>
      </div>
      <div class="header-nav">
        <router-link
          v-for="item in navItems"
          :key="item.path"
          :to="item.path"
          class="nav-item"
          :class="{ active: $route.path === item.path }"
        >
          <i :class="item.icon" />
          {{ item.title }}
        </router-link>
      </div>
      <div class="header-right">
        <el-dropdown @command="onCommand">
          <span class="user-btn">
            <i class="el-icon-user-solid" /> {{ username }}
            <i class="el-icon-arrow-down el-icon--right" />
          </span>
          <el-dropdown-menu slot="dropdown">
            <el-dropdown-item command="admin" v-if="isAdmin">
              <i class="el-icon-s-tools" /> 管理后台
            </el-dropdown-item>
            <el-dropdown-item command="logout" divided>
              <i class="el-icon-switch-button" /> 退出登录
            </el-dropdown-item>
          </el-dropdown-menu>
        </el-dropdown>
      </div>
    </el-header>

    <el-main class="portal-main">
      <router-view />
    </el-main>
  </el-container>
</template>

<script>
import { mapGetters } from 'vuex'

export default {
  name: 'UserLayout',
  computed: {
    ...mapGetters(['isAdmin', 'isLeader', 'isMember', 'username', 'needsProfile']),
    navItems() {
      // 未完善个人信息时只保留个人中心
      if (this.needsProfile) {
        return [{ path: '/portal/profile', icon: 'el-icon-user', title: '个人中心' }]
      }
      const items = [
        { path: '/portal/overview',  icon: 'el-icon-data-analysis', title: '我的概览' },
        { path: '/portal/mykeys',    icon: 'el-icon-key',           title: '我的密钥' },
        { path: '/portal/mylogs',    icon: 'el-icon-document',      title: '调用记录' },
        { path: '/portal/channels',  icon: 'el-icon-price-tag',     title: '渠道价格' },
        { path: '/portal/guide',     icon: 'el-icon-info',          title: '使用指南' },
        { path: '/portal/profile',   icon: 'el-icon-user',          title: '个人中心' },
      ]
      // 组长及管理员（role=1, 3）显示团队管理入口
      if (this.isLeader) {
        items.splice(4, 0,
          { path: '/portal/team',     icon: 'el-icon-s-custom',  title: '我的团队' },
          { path: '/portal/teamkeys', icon: 'el-icon-s-order',   title: '团队密钥' }
        )
      }
      return items
    }
  },
  methods: {
    onCommand(cmd) {
      if (cmd === 'logout') {
        this.$store.commit('CLEAR_AUTH')
        this.$router.push('/login')
      } else if (cmd === 'admin') {
        this.$router.push('/dashboard')
      }
    }
  }
}
</script>

<style scoped>
.portal-wrap { height: 100vh; flex-direction: column; overflow: hidden; }

.portal-header {
  background: #fff;
  border-bottom: 1px solid #e4e7ed;
  display: flex;
  align-items: center;
  padding: 0 28px;
  gap: 0;
  flex-shrink: 0;
  box-shadow: 0 1px 4px rgba(0,0,0,0.06);
}
.header-left {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 220px;
}
.logo-icon { font-size: 26px; color: #409EFF; }
.logo-text { font-size: 18px; font-weight: 600; color: #303133; }

.header-nav {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 4px;
}
.nav-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  border-radius: 6px;
  font-size: 16px;
  color: #606266;
  text-decoration: none;
  transition: all 0.2s;
}
.nav-item:hover { background: #f0f7ff; color: #409EFF; }
.nav-item.active { background: #ecf5ff; color: #409EFF; font-weight: 500; }
.nav-item i { font-size: 17px; }

.header-right { min-width: 130px; display: flex; justify-content: flex-end; }
.user-btn { cursor: pointer; color: #606266; font-size: 16px; }
.user-btn:hover { color: #409EFF; }

.portal-main {
  background: #f5f7fa;
  overflow: auto;
  padding: 28px;
}
</style>
