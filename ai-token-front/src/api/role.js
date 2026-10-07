import request from './request'

/** 查询所有角色 */
export const getRoleList = () => request.get('/admin/roles')

/** 新增角色 */
export const createRole = data => request.post('/admin/roles', data)

/** 修改角色 */
export const updateRole = (id, data) => request.put(`/admin/roles/${id}`, data)

/** 删除角色 */
export const deleteRole = id => request.delete(`/admin/roles/${id}`)
