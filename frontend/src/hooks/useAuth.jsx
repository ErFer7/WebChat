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
      console.log(data)
      setApplicationConnectInfo(data)
      setIsAuthenticated(true)

      localStorage.setItem('token', data.token)
      localStorage.setItem('applicationHost', data.applicationHost)
      localStorage.setItem('userId', data.userId)
      localStorage.setItem('clientId', clientId)
    },
    [clientId]
  )

  const logout = useCallback(() => {
    localStorage.removeItem('token')
    localStorage.removeItem('applicationHost')
    localStorage.removeItem('userId')
    localStorage.removeItem('clientId')
    setIsAuthenticated(false)
  }, [])

  useEffect(() => {
    const token = localStorage.getItem('token')
    const applicationHost = localStorage.getItem('applicationHost')
    const clientId = localStorage.getItem('clientId')
    const userIdString = localStorage.getItem('userId')

    if (token && applicationHost && clientId && userIdString) {
      setIsAuthenticated(true)
      setApplicationConnectInfo({ token, applicationHost, userId: parseInt(userIdString, 10) })
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
