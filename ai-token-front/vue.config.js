const { defineConfig } = require('@vue/cli-service')

module.exports = defineConfig({
  transpileDependencies: true,
  lintOnSave: false,
  publicPath: process.env.NODE_ENV === 'production' ? '/ai-token/' : '/',
  devServer: {
    port: 3000,
    proxy: {
      '/api': {
        target: process.env.VUE_APP_API_TARGET || 'http://localhost:8082',
        changeOrigin: true
      }
    }
  }
})
