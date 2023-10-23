import AccountCircle from '@mui/icons-material/AccountCircle'
import LogoutIcon from '@mui/icons-material/Logout'
import { TabContext, TabList, TabPanel } from '@mui/lab'

import { AppBar, Box, Button, Grid, IconButton, Tab, Toolbar, Typography } from '@mui/material'
import { useEffect, useMemo, useState } from 'react'
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

  const loggedUsername = useMemo(
    () => userList?.find((user) => user.id == applicationConnectionInfo.userId).username,
    [userList, applicationConnectionInfo]
  )

  const commonRequestPacket = useMemo(() => {
    return {
      hostType: 'CLIENT',
      token: applicationConnectionInfo.token,
      status: null,
      operationType: 'REQUEST',
      payload: {
        userId: applicationConnectionInfo.userId,
      },
    }
  }, [applicationConnectionInfo])

  const commonConnectedRequestPacket = useMemo(() => {
    return {
      ...commonRequestPacket,
      id: appHandshakeInfo.id,
      operationType: 'REQUEST',
    }
  }, [appHandshakeInfo, commonRequestPacket])

  const handleChangeTab = (event, newValue) => {
    setTableValue(newValue)
  }

  useEffect(() => {
    if (lastJsonMessage) {
      const data = lastJsonMessage
      if (data?.operationType == 'INFO' && data?.payloadType == 'HOST' && !handshaked) {
        setAppHandshakeInfo({ host: data?.payload?.host, id: data.id }) // Vai causar 1 retrigger, então vou controlar com handshaked
        if (isAuthenticated) {
          const connectionPacket = {
            ...commonRequestPacket,
            id: data?.id,
            payloadType: 'CONNECTION',
            payload: {
              ...commonRequestPacket.payload,
              host: data?.payload?.host,
            },
          }
          sendJsonMessage(connectionPacket)
          setHandkshaked(true)
        }
      } else if (data?.payloadType == 'CONNECTION' && data?.status == 'OK') {
        // conectado, vou começar a requisitar as coisas da tela...
        sendJsonMessage({ ...commonConnectedRequestPacket, payloadType: 'CHAT_LISTING' })
        sendJsonMessage({ ...commonConnectedRequestPacket, payloadType: 'USER_LISTING' })
      } else if (data?.payloadType == 'USER_LISTING' && data?.status == 'OK') {
        const newUserList = data?.payload?.users
        setUserList(newUserList)
      } else if (data?.payloadType == 'CHAT_LISTING' && data?.status == 'OK') {
        setChatList(data?.payload?.chats)
      }
      console.log(data)
    }
  }, [lastJsonMessage, sendJsonMessage, isAuthenticated, handshaked, commonConnectedRequestPacket, commonRequestPacket])

  return isAuthenticated ? (
    <div>
      <Grid container sx={{ mb: 4 }}>
        <Box sx={{ flexGrow: 1 }}>
          <AppBar position='static'>
            <Toolbar>
              <Typography
                variant='h6'
                component='div'
                sx={{ flexGrow: 1, fontFamily: 'monospace', fontWeight: 700, letterSpacing: '.3rem' }}
              >
                WEBCHAT
              </Typography>
              <Typography>{loggedUsername}</Typography>
              <IconButton size='large' color='inherit' sx={{ mr: 2 }}>
                <AccountCircle />
              </IconButton>
              <Button onClick={logout} variant='outlined' color='inherit' startIcon={<LogoutIcon />}>
                Sair
              </Button>
            </Toolbar>
          </AppBar>
        </Box>
      </Grid>
      <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
        <Grid container sx={{ height: '80vh', maxWidth: '1280px' }}>
          <Grid item xs={3}>
            {chatList && userList && (
              <Box sx={{ width: '100%', typography: 'body1' }}>
                <TabContext value={tabValue}>
                  <Box sx={{ borderBottom: 1, borderColor: 'divider' }}>
                    <TabList onChange={handleChangeTab}>
                      <Tab label='Conversas' value='chatList' />
                      <Tab label='Usuários' value='userList' />
                    </TabList>
                  </Box>
                  <TabPanel value='chatList' sx={{ p: 0 }}>
                    <ChatList chats={chatList} />
                  </TabPanel>
                  <TabPanel value='userList' sx={{ p: 0 }}>
                    <UserList users={userList} />
                  </TabPanel>
                </TabContext>
              </Box>
            )}
          </Grid>
          <Grid item xs={9}>
            listar mensagens
          </Grid>
        </Grid>
      </div>
    </div>
  ) : (
    <Navigate to='/login' />
  )
}

export default HomePage
