import PropTypes from 'prop-types'
import { createContext, useCallback, useContext, useEffect, useState } from 'react'

const AuthContext = createContext()

export function AuthProvider({ children }) {
  const [isAuthenticated, setIsAuthenticated] = useState(false)
  const [applicationConnectionInfo, setApplicationConnectInfo] = useState({})

  const login = useCallback((data) => {
    const payload = data?.payload

    const newAppConnInfo = {
      userId: payload?.userId,
      applicationId: payload?.applicationId,
      token: payload?.token,
      applicationHost: payload?.applicationHost,
    }

    setApplicationConnectInfo(newAppConnInfo)
    setIsAuthenticated(true)
    localStorage.setItem('token', payload?.token)
    localStorage.setItem('applicationConnectionInfo', JSON.stringify(newAppConnInfo))
  }, [])

  const logout = useCallback(() => {
    localStorage.removeItem('token')
    localStorage.removeItem('applicationConnectionInfo')
    setIsAuthenticated(false)
  }, [])

  useEffect(() => {
    const token = localStorage.getItem('token')
    const appConnInfo = localStorage.getItem('applicationConnectionInfo')
    if (token) {
      setIsAuthenticated(true)
    }
    if (appConnInfo) {
      setApplicationConnectInfo(JSON.parse(appConnInfo))
    }
  }, [])

  return (
    <AuthContext.Provider value={{ isAuthenticated, login, logout, applicationConnectionInfo }}>
      {children}
    </AuthContext.Provider>
  )
}

AuthProvider.propTypes = {
  children: PropTypes.node,
}

export function useAuth() {
  const { isAuthenticated, login, logout, applicationConnectionInfo } = useContext(AuthContext)
  return { isAuthenticated, login, logout, applicationConnectionInfo }
}
