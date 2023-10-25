import SendIcon from '@mui/icons-material/Send'
import { Box, Button, TextField } from '@mui/material'
import PropTypes from 'prop-types'
import { useState } from 'react'

function MessageSection({ messages, chatName, handleSendMessage }) {
  const [message, setMessage] = useState('')

  const onClickSend = () => {
    if (message) {
      handleSendMessage(message)
      setMessage('')
    }
  }

  const handleMessageChange = (event) => setMessage(event.target.value)

  return (
    <Box sx={{ p: 3 }}>
      <h3>{chatName}</h3>
      {messages?.map((message, index) => (
        <div style={{ marginBottom: '10px' }} key={index} className='message'>
          <div>senderId: {message.senderId}</div>
          <div>{message.message}</div>
          <div>sentAt: {message.sentAt}</div>
        </div>
      ))}
      <TextField
        onChange={handleMessageChange}
        value={message}
        multiline
        rows={3}
        placeholder='Digite sua mensagem'
        sx={{ width: '100%' }}
      />
      <Button sx={{ mt: 2 }} onClick={onClickSend} variant='contained' endIcon={<SendIcon />}>
        Enviar
      </Button>
    </Box>
  )
}

export default MessageSection

MessageSection.propTypes = {
  messages: PropTypes.arrayOf(PropTypes.object),
  chatName: PropTypes.string,
  handleSendMessage: PropTypes.func,
}