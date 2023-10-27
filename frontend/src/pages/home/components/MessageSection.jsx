import SendIcon from '@mui/icons-material/Send'
import { Box, Button, Paper, TextField, Typography, useTheme } from '@mui/material'
import PropTypes from 'prop-types'
import { useEffect, useRef, useState } from 'react'
function MessageSection({ messages, chatName, handleSendMessage, selfUserId }) {
  const [message, setMessage] = useState('')
  const theme = useTheme()
  const boxRef = useRef(null)

  const onClickSend = () => {
    if (message) {
      handleSendMessage(message)
      setMessage('')
    }
  }

  const handleMessageChange = (event) => setMessage(event.target.value)

  useEffect(() => {
    // Ajuste o scroll para o final quando as mensagens forem carregadas
    // Posso alterar depois pra não jogar pra baixo sempre, e sim avisar, idk
    if (boxRef.current) {
      boxRef.current.scrollTop = boxRef.current.scrollHeight
    }
  }, [messages])

  return (
    <Box sx={{ px: 3 }}>
      <Box sx={{ flexGrow: 1 }}>
        <Box
          sx={{
            display: 'flex',
            alignItems: 'center',
            p: 1,
            mb: 1,
            borderBottom: `1px solid ${theme.palette.primary.light}`,
          }}
        >
          <Typography sx={{ fontWeight: 'bold' }}>{chatName}</Typography>
        </Box>
      </Box>
      <Box ref={boxRef} style={{ maxHeight: '60vh', overflowY: 'auto' }} sx={{ px: 2 }}>
        {messages?.map((message, index) => (
          <ChatMessage key={index} message={message} selfUserId={selfUserId} />
        ))}
      </Box>
      <TextField
        onChange={handleMessageChange}
        value={message}
        multiline
        rows={3}
        placeholder='Digite sua mensagem'
        sx={{ width: '100%', mt: 2 }}
        onKeyDown={(event) => {
          if (event.key === 'Enter') {
            onClickSend()
          }
        }}
      />
      <div style={{ display: 'flex', justifyContent: 'flex-end' }}>
        <Button sx={{ mt: 2 }} onClick={onClickSend} variant='contained' endIcon={<SendIcon />}>
          Enviar
        </Button>
      </div>
    </Box>
  )
}
function ChatMessage({ message, selfUserId }) {
  const theme = useTheme()
  const isSelf = message.senderId === selfUserId

  return (
    <Box style={{ display: 'flex', flexDirection: 'column', marginBottom: 2 }}>
      <Paper
        sx={{
          p: 1,
          mb: 1,
          backgroundColor: isSelf ? theme.palette.grey[300] : theme.palette.grey[100],
          borderRadius: '10px',
          width: 'fit-content',
          alignSelf: isSelf ? 'flex-end' : 'flex-start',
        }}
      >
        {!isSelf && <Typography style={{ fontWeight: 'bold', marginBottom: 1 }}>{message.senderUsername}</Typography>}
        <Typography>{message.message}</Typography>
        <Typography style={{ fontSize: '0.8rem', color: 'gray' }}>
          {new Date(message.sentAt).toLocaleString()}
        </Typography>
      </Paper>
    </Box>
  )
}

export default MessageSection

MessageSection.propTypes = {
  messages: PropTypes.arrayOf(PropTypes.object),
  chatName: PropTypes.string,
  handleSendMessage: PropTypes.func,
  selfUserId: PropTypes.number,
}

ChatMessage.propTypes = {
  message: PropTypes.object,
  selfUserId: PropTypes.string,
}
