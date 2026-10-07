import Vue from 'vue'
import Vuex from 'vuex'

Vue.use(Vuex)

const TOKEN_KEY = 'ai_token'
const USER_KEY  = 'ai_user'

export default new Vuex.Store({
  state: {
    token:    localStorage.getItem(TOKEN_KEY) || '',
    userInfo: JSON.parse(localStorage.getItem(USER_KEY) || 'null') || {}
  },
  mutations: {
    SET_LOGIN_INFO(state, { token, userInfo }) {
      state.token = token
      state.userInfo = userInfo
      localStorage.setItem(TOKEN_KEY, token)
      localStorage.setItem(USER_KEY, JSON.stringify(userInfo))
    },
    CLEAR_AUTH(state) {
      state.token    = ''
      state.userInfo = {}
      localStorage.removeItem(TOKEN_KEY)
      localStorage.removeItem(USER_KEY)
    },
    UPDATE_USER_INFO(state, patch) {
      state.userInfo = { ...state.userInfo, ...patch }
      localStorage.setItem(USER_KEY, JSON.stringify(state.userInfo))
    }
  },
  getters: {
    isLoggedIn:  state => !!state.token,
    isSuperAdmin: state => state.userInfo && state.userInfo.roleCode === 'super_admin',
    isAdmin:    state => state.userInfo && ['super_admin', 'advisor'].includes(state.userInfo.roleCode),
    isLeader:   state => state.userInfo && ['super_admin', 'advisor', 'leader'].includes(state.userInfo.roleCode),
    isMember:   state => state.userInfo && state.userInfo.roleCode === 'member',
    username:   state => (state.userInfo && state.userInfo.username) || '',
    nickname:   state => (state.userInfo && state.userInfo.nickname) || '',
    roleName:   state => (state.userInfo && state.userInfo.roleName) || '',
    userRole:   state => (state.userInfo && state.userInfo.roleCode) || 'member',
    // 普通用户缺真实姓名或邮箱时需先完善（管理员豁免，与后端 checkRealNameRegistration 一致）
    needsProfile: state => {
      const u = state.userInfo || {}
      if (['super_admin', 'advisor'].includes(u.roleCode)) return false
      return !u.nickname || !u.nickname.trim() || !u.email || !u.email.trim()
    }
  }
})
