import InboxIcon from '@mui/icons-material/Inbox'
import { Box, Divider, List, ListItemButton, ListItemIcon, ListItemText } from '@mui/material'
import PropTypes from 'prop-types'
import { useState } from 'react'

export function ChatList({ chats }) {
  const [selectedIndex, setSelectedIndex] = useState(1)

  const handleListItemClick = (event, index) => {
    setSelectedIndex(index)
  }

  return (
    <Box sx={{ width: '100%', maxWidth: 360, bgcolor: 'background.paper' }}>
      <List component='nav' aria-label='main mailbox folders'>
        {chats.map((chat) => {
          return (
            <>
              <ListItemButton
                selected={selectedIndex === chat.id}
                onClick={(event) => handleListItemClick(event, chat.id)}
              >
                <ListItemIcon>
                  <InboxIcon />
                </ListItemIcon>
                <ListItemText primary={chat.name} secondary={`id: ${chat.id} e groupchat: ${chat.groupChat}`} />
              </ListItemButton>
              <Divider />
            </>
          )
        })}
      </List>
    </Box>
  )
}

ChatList.propTypes = {
  chats: PropTypes.arrayOf(PropTypes.object),
}

export default ChatList
