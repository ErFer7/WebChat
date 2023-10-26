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
  const { isAuthenticated, applicationConnectionInfo, logout, clientId } = useAuth() // pegar infos dessa hook
  const { sendJsonMessage, lastJsonMessage } = useWebSocket(
    `ws:/${applicationConnectionInfo?.applicationHost}`,
    {
      onOpen: () => console.log(`Connected to App WS`),
      onClose: () => logout(),
    },
    isAuthenticated
  )

  const [chatList, setChatList] = useState()
  const [userList, setUserList] = useState()
  const [tabValue, setTableValue] = useState('chatList')
  const [groupForm, setGroupForm] = useState({ groupName: '', usernames: [] })
  const [createGroupAlert, setCreateGroupAlert] = useState()
  const [selectedChatId, setSelectedChatId] = useState()
  const [messages, setMessages] = useState([])

  const chatName = chatList?.find((chat) => chat.id == selectedChatId)?.name // tentar pegar do back

  const loggedUsername = useMemo(
    () => userList?.find((user) => user.id == applicationConnectionInfo.userId).username,
    [userList, applicationConnectionInfo] // tentar pegar do back
  )

  const commonRequestPacket = useMemo(() => {
    return {
      id: clientId,
      hostType: 'CLIENT',
      token: applicationConnectionInfo.token,
      status: null,
      operationType: 'REQUEST',
      payload: {
        userId: applicationConnectionInfo.userId,
      },
    }
  }, [applicationConnectionInfo, clientId])

  const handleChangeTab = (event, newValue) => {
    setTableValue(newValue)
  }

  const handleCreateGroup = (model) =>
    sendJsonMessage({
      ...commonRequestPacket,
      payloadType: 'GROUP_CHAT_CREATION',
      payload: {
        ...commonRequestPacket.payload,
        groupName: model.groupName,
        membersUsernames: model.usernames,
      },
    })

  const handleClickUser = (username) =>
    sendJsonMessage({
      ...commonRequestPacket,
      payloadType: 'GET_USER_CHAT_ID',
      payload: { ...commonRequestPacket.payload, targetUsername: username },
    })

  const handleSendMessage = (message) => {
    sendJsonMessage({
      ...commonRequestPacket,
      payloadType: 'MESSAGE',
      payload: {
        ...commonRequestPacket.payload,
        chatId: selectedChatId,
        message: message,
      },
    })
    setMessages(
      messages.concat({
        senderId: applicationConnectionInfo.userId,
        message: message,
        sentAt: new Date().toISOString(),
        senderUsername: loggedUsername,
      })
    )
  }

  const handleSetSelectChatId = useCallback(
    (chatId) => {
      setSelectedChatId(chatId)
      sendJsonMessage({
        ...commonRequestPacket,
        payloadType: 'MESSAGE_LISTING',
        payload: {
          ...commonRequestPacket.payload,
          chatId: chatId,
        },
      })
    },
    [sendJsonMessage, commonRequestPacket]
  )

  const fetchChats = useCallback(
    () => sendJsonMessage({ ...commonRequestPacket, payloadType: 'CHAT_LISTING' }),
    [sendJsonMessage, commonRequestPacket]
  )
  const fetchUsers = useCallback(
    () => sendJsonMessage({ ...commonRequestPacket, payloadType: 'USER_LISTING' }),
    [sendJsonMessage, commonRequestPacket]
  )
  const connect = useCallback(
    (host) =>
      sendJsonMessage({
        ...commonRequestPacket,
        payloadType: 'CLIENT_CONNECTION',
        payload: {
          ...commonRequestPacket.payload,
          host: host,
        },
      }),
    [sendJsonMessage, commonRequestPacket]
  )

  useEffect(() => {
    const data = lastJsonMessage
    if (lastJsonMessage && isAuthenticated) {
      switch (data?.payloadType) {
        case 'HOST':
          data?.operationType == 'INFO' && connect(data?.payload?.host)

          break
        case 'CLIENT_CONNECTION':
          if (data?.status == 'OK') {
            fetchChats()
            fetchUsers()
          }

          break
        case 'USER_LISTING':
          data?.status == 'OK' && setUserList(data?.payload?.users)

          break
        case 'CHAT_LISTING':
          data?.status == 'OK' && setChatList(data?.payload?.chats)

          break
        case 'GROUP_CHAT_CREATION':
          if (data?.status == 'CREATED') {
            setCreateGroupAlert({ severity: 'success', message: 'Grupo criado com sucesso!' })
            setGroupForm({ groupName: '', usernames: [] })
            fetchChats()
          } else if (data?.status == 'ERROR' || data?.status == 'VALIDATION_ERROR') {
            setCreateGroupAlert({ severity: 'error', message: data?.payload?.message })
          }

          break
        case 'GET_USER_CHAT_ID':
          data?.payload?.chatId && handleSetSelectChatId(data?.payload?.chatId)
          data?.status == 'CREATED' && fetchChats()

          break
        case 'MESSAGE_LISTING':
          data?.status == 'OK' && setMessages(data?.payload?.messages)

          break
        case 'MESSAGE':
          // PRECISO DO CHATID TAMBÉM PRA SÓ CONCATENAR SE ESTIVER NO CHAT CERTO. se não notifiy
          data?.status == 'OK' &&
            setMessages((prevState) =>
              prevState.concat({
                senderId: data?.payload?.userId,
                senderUsername: data?.payload?.senderUsername,
                message: data?.payload?.message,
                sentAt: data?.payload?.sentAt,
              })
            )

          break
        default:
          break
      }
      console.log(data)
    }
  }, [lastJsonMessage, isAuthenticated, connect, handleSetSelectChatId, fetchChats, fetchUsers, clientId])

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
              <MessageSection
                messages={messages}
                chatName={chatName}
                handleSendMessage={handleSendMessage}
                selfUserId={applicationConnectionInfo.userId}
              />
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
