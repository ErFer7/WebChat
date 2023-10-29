import SendIcon from '@mui/icons-material/Send'
import { Alert, Box, Button, Paper, TextField, Typography, useTheme } from '@mui/material'
import PropTypes from 'prop-types'
import { useEffect, useRef, useState } from 'react'
import { useAuth } from '../../../hooks/useAuth'
function MessageSection({ messages, chatName, handleSendMessage }) {
  const { applicationConnectionInfo } = useAuth()

  const [message, setMessage] = useState('')
  const [errorMessage, setErrorMessage] = useState('')
  const theme = useTheme()
  const boxRef = useRef(null)

  function isValidMessage(str) {
    if (str === null || str.trim() === '') return { valid: false, error: 'Mensagem vazia' }
    if (str.length > 1000) return { valid: false, error: 'Mensagem com mais de 1000 caracteres' }
    return { valid: true, error: '' }
  }

  const onClickSend = () => {
    const { valid, error } = isValidMessage(message)
    if (!valid) {
      setErrorMessage(error)
      return
    }
    handleSendMessage(message)
    setMessage('')
  }

  const handleMessageChange = (event) => {
    setMessage(event.target.value)
    setErrorMessage('')
  }

  useEffect(() => {
    // Ajuste o scroll para o final quando as mensagens forem carregadas
    // Pode ser alterado pra não ir pra baixo pra sempre com mudança nas mensagens, mas é o comportamento atual
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
      <Box ref={boxRef} style={{ maxHeight: '55vh', overflowY: 'auto' }} sx={{ px: 2 }}>
        {messages?.map((message, index) => (
          <ChatMessage key={index} message={message} selfUserId={applicationConnectionInfo.userId} />
        ))}
      </Box>
      {errorMessage && (
        <Alert sx={{ mt: 2 }} severity='error'>
          {errorMessage}
        </Alert>
      )}
      <TextField
        onChange={handleMessageChange}
        value={message}
        multiline
        rows={3}
        placeholder='Digite sua mensagem'
        sx={{ width: '100%', mt: 2 }}
        onKeyDown={(event) => {
          if (event.key === 'Enter' && !event.shiftKey) {
            event.preventDefault()
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
          alignSelf: isSelf ? 'flex-end' : 'flex-start',
          maxWidth: '90%',
        }}
      >
        {!isSelf && <Typography style={{ fontWeight: 'bold', marginBottom: 1 }}>{message.senderUsername}</Typography>}
        <Typography style={{ overflowWrap: 'break-word' }}>{message.message}</Typography>
        <Typography style={{ fontSize: '0.8rem', color: 'gray' }}>
          {new Date(message.sentAt).toLocaleString()}
        </Typography>
      </Paper>
    </Box>
  )
}

MessageSection.propTypes = {
  messages: PropTypes.arrayOf(PropTypes.object),
  chatName: PropTypes.string,
  handleSendMessage: PropTypes.func,
}

ChatMessage.propTypes = {
  message: PropTypes.object,
  selfUserId: PropTypes.number,
}

export { MessageSection }
