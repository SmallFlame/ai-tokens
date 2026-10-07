/**
 * 兼容 HTTP / HTTPS 的复制工具。
 * 优先使用 Clipboard API（需 HTTPS），降级到 execCommand（兼容 HTTP）。
 */
export function copyText(text) {
  if (navigator.clipboard && window.isSecureContext) {
    return navigator.clipboard.writeText(text)
  }
  // HTTP 环境降级方案
  return new Promise((resolve, reject) => {
    const el = document.createElement('textarea')
    el.value = text
    el.style.cssText = 'position:fixed;top:-9999px;left:-9999px;opacity:0'
    document.body.appendChild(el)
    el.focus()
    el.select()
    const ok = document.execCommand('copy')
    document.body.removeChild(el)
    ok ? resolve() : reject(new Error('execCommand copy failed'))
  })
}
