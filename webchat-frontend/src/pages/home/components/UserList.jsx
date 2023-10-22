import InboxIcon from '@mui/icons-material/Inbox'
import { Box, Divider, List, ListItemButton, ListItemIcon, ListItemText } from '@mui/material'
import PropTypes from 'prop-types'
import { useState } from 'react'

export function UserList({ users }) {
  const [selectedIndex, setSelectedIndex] = useState(1)

  const handleListItemClick = (event, index) => {
    setSelectedIndex(index)
  }

  return (
    <Box sx={{ width: '100%', maxWidth: 360, bgcolor: 'background.paper' }}>
      <List component='nav' aria-label='main mailbox folders'>
        {users.map((user) => {
          return (
            <div key={user.id}>
              <ListItemButton
                selected={selectedIndex === user.id}
                onClick={(event) => handleListItemClick(event, user.id)}
              >
                <ListItemIcon>
                  <InboxIcon />
                </ListItemIcon>
                <ListItemText primary={user.username} secondary={`id: ${user.id}`} />
              </ListItemButton>
              <Divider />
            </div>
          )
        })}
      </List>
    </Box>
  )
}

UserList.propTypes = {
  users: PropTypes.arrayOf(PropTypes.object),
}
