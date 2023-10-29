import AddIcon from '@mui/icons-material/Add'
import { TabContext, TabList, TabPanel } from '@mui/lab'
import { Box, CircularProgress, Grid, Tab } from '@mui/material'
import { useCallback, useMemo, useState } from 'react'
import { Navigate } from 'react-router-dom'
import useWebSocket from 'react-use-websocket'
import { useAuth } from '../../hooks/useAuth'
import { ChatList, CreateGroupForm, Header, MessageSection, UserList } from './components'

function HomePage() {
  const { isAuthenticated, applicationConnectionInfo, clientId, logout } = useAuth()

  const [chatList, setChatList] = useState()
  const [userList, setUserList] = useState()
  const [tabValue, setTabValue] = useState('chatList')
  const [groupForm, setGroupForm] = useState({ groupName: '', usernames: [] })
  const [createGroupAlert, setCreateGroupAlert] = useState()
  const [selectedChat, setSelectedChat] = useState({ id: null, name: null, isGroupChat: null, usernames: [] })
  const [notifyChatId, setNotifyChatId] = useState(null)
  const [messages, setMessages] = useState([])
  const [loading, setLoading] = useState({ websocket: true, chats: false, users: false, messages: false })

  const handleNewWebSocketMessage = (message) => {
    const data = JSON.parse(message.data)
    if (isAuthenticated) {
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

        case 'CHAT_LISTING':
          data?.status == 'OK' && setChatList(data?.payload?.chats)
          setLoading((prevState) => ({ ...prevState, chats: false }))
          break

        case 'USER_LISTING':
          data?.status == 'OK' && setUserList(data?.payload?.users)
          setLoading((prevState) => ({ ...prevState, users: false }))
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
          data?.payload?.chatId &&
            handleSelectChat({ id: data?.payload?.chatId, name: data?.payload?.targetUsername, isGroupChat: false })
          data?.status == 'CREATED' && fetchChats()
          break
        case 'CHAT_USERS_LISTING':
          data?.status == 'OK' &&
            setSelectedChat((prevState) => ({ ...prevState, usernames: data?.payload?.usernames }))
          break
        case 'MESSAGE_LISTING':
          data?.status == 'OK' && setMessages(data?.payload?.messages)
          setLoading((prevState) => ({ ...prevState, messages: false }))
          break

        case 'MESSAGE':
          if (data?.status == 'OK') {
            data?.payload?.chatId == selectedChat.id
              ? setMessages((prevState) =>
                  prevState.concat({
                    senderId: data?.payload?.userId,
                    senderUsername: data?.payload?.senderUsername,
                    message: data?.payload?.message,
                    sentAt: data?.payload?.sentAt,
                  })
                )
              : setNotifyChatId(data?.payload?.chatId)
          }
          break

        default:
          break
      }
      console.log(data)
    }
  }

  const { sendJsonMessage } = useWebSocket(
    `ws:/${applicationConnectionInfo?.applicationHost}`,
    {
      onOpen: () => {
        console.log(`Connected to App WS`)
        setLoading((prevState) => ({ ...prevState, websocket: false }))
      },
      shouldReconnect: () => true,
      reconnectAttempts: 5,
      reconnectInterval: 1000,
      onReconnectStop: () => logout(),
      onMessage: handleNewWebSocketMessage,
    },
    isAuthenticated
  )

  // Essas informação poderia vir do backend diretamente
  const loggedUsername = useMemo(
    () => userList?.find((user) => user.id == applicationConnectionInfo.userId).username,
    [userList, applicationConnectionInfo]
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

  const handleChangeTab = (_, newValue) => setTabValue(newValue)

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
        chatId: selectedChat.id,
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

  const handleSelectChat = useCallback(
    (chat) => {
      setSelectedChat(chat)
      setNotifyChatId((prevState) => (prevState == chat.id ? null : prevState))
      setLoading((prevState) => ({ ...prevState, messages: true }))

      const requestPacket = {
        ...commonRequestPacket,
        payload: {
          ...commonRequestPacket.payload,
          chatId: chat.id,
        },
      }
      sendJsonMessage({ ...requestPacket, payloadType: 'MESSAGE_LISTING' })
      chat.isGroupChat && sendJsonMessage({ ...requestPacket, payloadType: 'CHAT_USERS_LISTING' })
    },
    [sendJsonMessage, commonRequestPacket]
  )

  const fetchChats = useCallback(() => {
    setLoading((prevState) => ({ ...prevState, chats: true }))
    sendJsonMessage({ ...commonRequestPacket, payloadType: 'CHAT_LISTING' })
  }, [sendJsonMessage, commonRequestPacket])

  const fetchUsers = useCallback(() => {
    setLoading((prevState) => ({ ...prevState, users: true }))
    sendJsonMessage({ ...commonRequestPacket, payloadType: 'USER_LISTING' })
  }, [sendJsonMessage, commonRequestPacket])

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

  return isAuthenticated ? (
    <div>
      <Header username={loggedUsername} />
      <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
        <Grid container sx={{ height: '80vh', maxWidth: '1280px' }}>
          <Grid item xs={3}>
            {(loading.websocket || loading.chats || loading.users) && <CircularProgress />}
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
                  <TabPanel value='chatList' sx={{ p: 0 }} style={{ maxHeight: '80vh', overflowY: 'auto' }}>
                    <ChatList
                      chats={chatList}
                      selectedChat={selectedChat}
                      setSelectedChat={handleSelectChat}
                      notifyChatId={notifyChatId}
                    />
                  </TabPanel>
                  <TabPanel value='userList' sx={{ p: 0 }} style={{ maxHeight: '80vh', overflowY: 'auto' }}>
                    <UserList users={userList} handleClickUser={handleClickUser} />
                  </TabPanel>
                  <TabPanel value='createGroup' sx={{ p: 0 }} style={{ maxHeight: '80vh', overflowY: 'auto' }}>
                    <CreateGroupForm
                      groupForm={groupForm}
                      setGroupForm={setGroupForm}
                      handleCreateGroup={handleCreateGroup}
                      alert={createGroupAlert}
                      setAlert={setCreateGroupAlert}
                      userList={userList}
                    />
                  </TabPanel>
                </TabContext>
              </Box>
            )}
          </Grid>
          <Grid item xs={9}>
            {selectedChat.id && (
              <MessageSection messages={messages} selectedChat={selectedChat} handleSendMessage={handleSendMessage} />
            )}
            {loading.messages && <CircularProgress />}
          </Grid>
        </Grid>
      </div>
    </div>
  ) : (
    <Navigate to='/login' />
  )
}

export default HomePage
