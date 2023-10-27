import axios from 'axios'

const api = axios.create({ baseURL: 'http://localhost:8081/api/client' })

export const gatewayLogin = async (data) => await api.get('/login', { params: data })

export const gatewayRegister = async (data) => await api.post('/register', data)
