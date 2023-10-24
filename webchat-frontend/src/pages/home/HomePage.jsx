import AccountCircle from '@mui/icons-material/AccountCircle'
import AddIcon from '@mui/icons-material/Add'
import LogoutIcon from '@mui/icons-material/Logout'
import { TabContext, TabList, TabPanel } from '@mui/lab'

import { AppBar, Box, Button, Grid, IconButton, Tab, Toolbar, Typography } from '@mui/material'
import { useCallback, useEffect, useMemo, useState } from 'react'
import { Navigate } from 'react-router-dom'
import useWebSocket from 'react-use-websocket'
import { useAuth } from '../../hooks/useAuth'
import ChatList from './components/ChatList'
import CreateGroupForm from './components/CreateGroupForm'
import MessageSection from './components/MessageSection'
import { UserList } from './components/UserList'

function HomePage() {
  const { isAuthenticated, applicationConnectionInfo, logout } = useAuth() // pegar infos dessa hook
  const { sendJsonMessage, lastJsonMessage } = useWebSocket(
    `ws:/${applicationConnectionInfo?.applicationHost}`,
    {
      onOpen: () => console.log(`Connected to App WS`),
      onClose: () => logout(),
    },
    isAuthenticated
  )

  const [appHandshakeInfo, setAppHandshakeInfo] = useState({})
  const [handshaked, setHandkshaked] = useState(false)

  const [chatList, setChatList] = useState()
  const [userList, setUserList] = useState()
  const [tabValue, setTableValue] = useState('chatList')
  const [groupForm, setGroupForm] = useState({ groupName: '', usernames: [] })
  const [createGroupAlert, setCreateGroupAlert] = useState()
  const [selectedChatId, setSelectedChatId] = useState()
  const [messages, setMessages] = useState()

  const chatName = chatList?.find((chat) => chat.id == selectedChatId)?.name

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

  const handleCreateGroup = (model) => {
    console.log(model)
    const createGroupPacket = {
      ...commonConnectedRequestPacket,
      payloadType: 'GROUP_CHAT_CREATION',
      payload: {
        userId: applicationConnectionInfo.userId,
        groupName: model.groupName,
        membersUsernames: model.usernames,
      },
    }
    sendJsonMessage(createGroupPacket)
  }

  const handleClickUser = (username) => {
    const getChatIdPacket = {
      ...commonConnectedRequestPacket,
      payloadType: 'GET_USER_CHAT_ID',
      payload: { userId: applicationConnectionInfo.userId, targetUsername: username },
    }
    sendJsonMessage(getChatIdPacket)
  }

  const handleSendMessage = (message) => {
    const sendMessagePacket = {
      ...commonConnectedRequestPacket,
      payloadType: 'MESSAGE',
      payload: {
        chatId: selectedChatId,
        userId: applicationConnectionInfo.userId,
        message: message,
      },
    }
    sendJsonMessage(sendMessagePacket)
  }

  const handleSetSelectChatId = useCallback(
    (chatId) => {
      console.log(chatId, applicationConnectionInfo.userId)
      setSelectedChatId(chatId)
      const sendMessageListPacket = {
        ...commonConnectedRequestPacket,
        payloadType: 'MESSAGE_LISTING',
        payload: {
          chatId: chatId,
          userId: applicationConnectionInfo.userId,
        },
      }
      sendJsonMessage(sendMessageListPacket)
    },
    [commonConnectedRequestPacket, applicationConnectionInfo.userId, sendJsonMessage]
  )

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
      } else if (data?.payloadType == 'GROUP_CHAT_CREATION') {
        if (data?.status == 'CREATED') {
          setCreateGroupAlert({ severity: 'success', message: 'Grupo criado com sucesso!' })
          setGroupForm({ groupName: '', usernames: [] })
          sendJsonMessage({ ...commonConnectedRequestPacket, payloadType: 'CHAT_LISTING' }) // reload chat list
        } else if (data?.status == 'ERROR' || data?.status == 'VALIDATION_ERROR') {
          setCreateGroupAlert({ severity: 'error', message: data?.payload?.message })
        }
      } else if (data?.payloadType == 'GET_USER_CHAT_ID') {
        data?.payload?.chatId && handleSetSelectChatId(data?.payload?.chatId)

        if (data?.status == 'CREATED') {
          sendJsonMessage({ ...commonConnectedRequestPacket, payloadType: 'CHAT_LISTING' }) // reload chat list
        }
      } else if (data?.payloadType == 'MESSAGE_LISTING' && data?.status == 'OK') {
        setMessages(data?.payload?.messages)
      }
      console.log(data)
    }
  }, [
    lastJsonMessage,
    sendJsonMessage,
    isAuthenticated,
    handshaked,
    commonConnectedRequestPacket,
    commonRequestPacket,
    logout,
    handleSetSelectChatId,
  ])

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
                      <Tab label={<AddIcon />} value='createGroup'></Tab>
                    </TabList>
                  </Box>
                  <TabPanel value='chatList' sx={{ p: 0 }}>
                    <ChatList
                      chats={chatList}
                      selectedChatId={selectedChatId}
                      setSelectedChatId={handleSetSelectChatId}
                      chatName={chatName}
                    />
                  </TabPanel>
                  <TabPanel value='userList' sx={{ p: 0 }}>
                    <UserList users={userList} handleClickUser={handleClickUser} />
                  </TabPanel>
                  <TabPanel value='createGroup' sx={{ p: 0 }}>
                    <CreateGroupForm
                      groupForm={groupForm}
                      setGroupForm={setGroupForm}
                      handleCreateGroup={handleCreateGroup}
                      alert={createGroupAlert}
                      setAlert={setCreateGroupAlert}
                    />
                  </TabPanel>
                </TabContext>
              </Box>
            )}
          </Grid>
          <Grid item xs={9}>
            {selectedChatId && (
              <MessageSection messages={messages} chatName={chatName} handleSendMessage={handleSendMessage} />
            )}
          </Grid>
        </Grid>
      </div>
    </div>
  ) : (
    <Navigate to='/login' />
  )
}

export default HomePage
