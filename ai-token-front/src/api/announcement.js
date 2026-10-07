import request from './request'

export const getAnnouncementList  = ()        => request.get('/admin/announcements')
export const createAnnouncement   = data      => request.post('/admin/announcements', data)
export const updateAnnouncement   = (id, data) => request.put(`/admin/announcements/${id}`, data)
export const deleteAnnouncement   = id        => request.delete(`/admin/announcements/${id}`)
export const getHomeAnnouncement  = ()        => request.get('/announcement/home')
