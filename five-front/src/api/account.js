import request from '@/utils/request'

export const login = (data) => {
    return request.post('/user/login', data)
}

export const register = (data) => {
    return request.post('/user/register', data)
}

export const getUserInfo = (id) => {
    return request.get('/user/' + id)
}

export const out = (id) => {
    return request.delete('/user/' + id)
}
