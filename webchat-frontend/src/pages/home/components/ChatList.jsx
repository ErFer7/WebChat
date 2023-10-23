import GroupIcon from '@mui/icons-material/Group'
import PersonIcon from '@mui/icons-material/Person'
import { Box, Divider, List, ListItemButton, ListItemIcon, ListItemText } from '@mui/material'
import PropTypes from 'prop-types'
import { useState } from 'react'
export function ChatList({ chats }) {
  const [selectedIndex, setSelectedIndex] = useState(1)

  const handleListItemClick = (event, index) => {
    setSelectedIndex(index)
  }

  return (
    <Box sx={{ width: '100%', bgcolor: 'background.paper' }}>
      <List component='nav' aria-label='main mailbox folders'>
        {chats.map((chat) => {
          return (
            <div key={chat.id}>
              <ListItemButton
                selected={selectedIndex === chat.id}
                onClick={(event) => handleListItemClick(event, chat.id)}
              >
                <ListItemIcon>{chat.groupChat ? <GroupIcon /> : <PersonIcon />}</ListItemIcon>
                <ListItemText primary={chat.name} />
              </ListItemButton>
              <Divider />
            </div>
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
