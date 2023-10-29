import GroupIcon from '@mui/icons-material/Group'
import MarkChatUnreadIcon from '@mui/icons-material/MarkChatUnread'
import PersonIcon from '@mui/icons-material/Person'
import { Box, Divider, IconButton, List, ListItem, ListItemButton, ListItemIcon, ListItemText } from '@mui/material'
import PropTypes from 'prop-types'

function ChatList({ chats, selectedChatId, setSelectedChat, notifyChatId }) {
  const handleListItemClick = (_, chat) =>
    setSelectedChat({ id: chat.id, name: chat.name, isGroupChat: chat.groupChat })

  return (
    <Box sx={{ width: '100%', bgcolor: 'background.paper' }}>
      <List component='nav' aria-label='main mailbox folders'>
        {chats.map((chat) => {
          return (
            <div key={chat.id}>
              <ListItemButton
                selected={selectedChatId === chat.id}
                onClick={(event) => handleListItemClick(event, chat)}
              >
                <ListItemIcon>{chat.groupChat ? <GroupIcon /> : <PersonIcon />}</ListItemIcon>
                <ListItemText primary={chat.name} />
                {notifyChatId == chat.id && (
                  <ListItem
                    secondaryAction={
                      <IconButton edge='end' aria-label='comments'>
                        <MarkChatUnreadIcon />
                      </IconButton>
                    }
                    disablePadding
                  />
                )}
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
  setSelectedChat: PropTypes.func,
  selectedChatId: PropTypes.number,
  notifyChatId: PropTypes.number,
}

export { ChatList }
