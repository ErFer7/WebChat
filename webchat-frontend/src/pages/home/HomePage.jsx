import { TabContext, TabList, TabPanel } from '@mui/lab'
import { Box, Tab } from '@mui/material'
import { useEffect, useState } from 'react'
import { Navigate } from 'react-router-dom'
import useWebSocket from 'react-use-websocket'
import { useAuth } from '../../hooks/useAuth'
import ChatList from './components/ChatList'
import { UserList } from './components/UserList'

function HomePage() {
  const { isAuthenticated, applicationConnectionInfo, logout } = useAuth() // pegar infos dessa hook
  const { sendJsonMessage, lastJsonMessage } = useWebSocket(
    `ws:/${applicationConnectionInfo?.applicationHost}`,
    {
      onOpen: () => console.log(`Connected to App WS`),
      onClose: () => console.log('Connection Closed'),
    },
    isAuthenticated
  )

  const [appHandshakeInfo, setAppHandshakeInfo] = useState({})
  const [handshaked, setHandkshaked] = useState(false)
  const [chatList, setChatList] = useState()
  const [userList, setUserList] = useState()
  const [tabValue, setTableValue] = useState('chatList')
  const [loggedUsername, setLoggedUsername] = useState()

  const handleChangeTab = (event, newValue) => {
    setTableValue(newValue)
  }

  useEffect(() => {
    if (lastJsonMessage) {
      const data = lastJsonMessage
      if (data?.operationType == 'INFO' && data?.payloadType == 'HOST' && !handshaked) {
        const newAppHandshakeInfo = {
          host: data?.payload?.host,
          id: data.id,
        }
        setAppHandshakeInfo(newAppHandshakeInfo) // Vai causar 1 retrigger, então vou controlar com handshaked
        if (isAuthenticated) {
          const connectionPacket = {
            id: newAppHandshakeInfo.id,
            hostType: 'CLIENT',
            token: applicationConnectionInfo.token,
            status: null,
            operationType: 'REQUEST',
            payloadType: 'CONNECTION',
            payload: {
              userId: applicationConnectionInfo.userId,
              host: newAppHandshakeInfo.host,
            },
          }
          sendJsonMessage(connectionPacket)
          setHandkshaked(true)
        }
      } else if (data?.payloadType == 'CONNECTION' && data?.status == 'OK') {
        // conectado, vou começar a requisitar as coisas da tela...
        const chatListingPacket = {
          id: appHandshakeInfo.id,
          hostType: 'CLIENT',
          token: applicationConnectionInfo.token,
          status: null,
          operationType: 'REQUEST',
          payloadType: 'CHAT_LISTING',
          payload: {
            userId: applicationConnectionInfo.userId,
          },
        }
        const userListingPacket = {
          ...chatListingPacket,
          payloadType: 'USER_LISTING',
        }
        sendJsonMessage(chatListingPacket)
        sendJsonMessage(userListingPacket)
      } else if (data?.payloadType == 'USER_LISTING' && data?.status == 'OK') {
        const newUserList = data?.payload?.users
        setLoggedUsername(newUserList.find((user) => user.id == applicationConnectionInfo.userId).username)
        setUserList(newUserList)
      } else if (data?.payloadType == 'CHAT_LISTING' && data?.status == 'OK') {
        setChatList(data?.payload?.chats)
      }
      console.log(data)
    }
  }, [lastJsonMessage, sendJsonMessage, applicationConnectionInfo, isAuthenticated, appHandshakeInfo, handshaked])

  return isAuthenticated ? (
    <>
      {loggedUsername && <h1>Usuário logado: {loggedUsername}</h1>}
      <button onClick={logout}>deslogar</button>
      {chatList && userList && (
        <Box sx={{ width: '100%', typography: 'body1' }}>
          <TabContext value={tabValue}>
            <Box sx={{ borderBottom: 1, borderColor: 'divider' }}>
              <TabList onChange={handleChangeTab}>
                <Tab label='Conversas' value='chatList' />
                <Tab label='Usuários' value='userList' />
              </TabList>
            </Box>
            <TabPanel value='chatList'>
              <ChatList chats={chatList} />
            </TabPanel>
            <TabPanel value='userList'>
              <UserList users={userList} />
            </TabPanel>
          </TabContext>
        </Box>
      )}
    </>
  ) : (
    <Navigate to='/login' />
  )
}

export default HomePage
