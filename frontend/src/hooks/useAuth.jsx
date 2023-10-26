import PropTypes from 'prop-types'
import { createContext, useCallback, useContext, useEffect, useState } from 'react'
import { v4 as uuidv4 } from 'uuid'

const AuthContext = createContext()

export function AuthProvider({ children }) {
  const [isAuthenticated, setIsAuthenticated] = useState(false)
  const [applicationConnectionInfo, setApplicationConnectInfo] = useState({})
  const [clientId, setClientId] = useState()

  const login = useCallback(
    (data) => {
      const payload = data?.payload
      setApplicationConnectInfo({
        userId: payload?.userId,
        token: payload?.token,
        applicationHost: payload?.applicationHost,
      })
      setIsAuthenticated(true)

      localStorage.setItem('token', payload?.token)
      localStorage.setItem('applicationHost', payload?.applicationHost)
      localStorage.setItem('clientId', clientId)
      localStorage.setItem('userId', payload?.userId)
    },
    [clientId]
  )

  const logout = useCallback(() => {
    localStorage.removeItem('token')
    localStorage.removeItem('applicationHost')
    localStorage.removeItem('clientId')
    localStorage.removeItem('userId')
    setIsAuthenticated(false)
  }, [])

  useEffect(() => {
    const token = localStorage.getItem('token')
    const applicationHost = localStorage.getItem('applicationHost')
    const clientId = localStorage.getItem('clientId')
    const userId = localStorage.getItem('userId')

    if (token && applicationHost && clientId) {
      setIsAuthenticated(true)
      setApplicationConnectInfo({ token, applicationHost, userId })
      setClientId(clientId)
    } else {
      setClientId(uuidv4())
    }
  }, [])

  return (
    <AuthContext.Provider value={{ isAuthenticated, login, logout, applicationConnectionInfo, clientId }}>
      {children}
    </AuthContext.Provider>
  )
}

AuthProvider.propTypes = {
  children: PropTypes.node,
}

export function useAuth() {
  const { isAuthenticated, login, logout, applicationConnectionInfo, clientId } = useContext(AuthContext)
  return { isAuthenticated, login, logout, applicationConnectionInfo, clientId }
}
