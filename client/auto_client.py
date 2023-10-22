'''
Fake client.
'''

from json import dumps, loads

import websocket


def create_login_packet(id, host):
    packet = {}
    packet['id'] = id
    packet['hostType'] = 'CLIENT'
    packet['token'] = None
    packet['status'] = None
    packet['operationType'] = 'REQUEST'
    packet['payloadType'] = 'ROUTING'
    packet['payload'] = {}
    packet['payload']['username'] = 'juniorteste'
    packet['payload']['password'] = 'aaa'
    packet['payload']['host'] = host

    packet = dumps(packet)
    return packet


def create_register_packet(id, host):
    packet = {}
    packet['id'] = id
    packet['hostType'] = 'CLIENT'
    packet['token'] = None
    packet['status'] = None
    packet['operationType'] = 'REQUEST'
    packet['payloadType'] = 'USER_CREATION'
    packet['payload'] = {}
    packet['payload']['username'] = 'juniorteste'
    packet['payload']['password'] = 'aaa'
    packet['payload']['host'] = host
    packet = dumps(packet)
    return packet


def create_application_connection_packet(id, host, token, user_id):
    packet = {}
    packet['id'] = id
    packet['hostType'] = 'CLIENT'
    packet['token'] = token
    packet['status'] = None
    packet['operationType'] = 'REQUEST'
    packet['payloadType'] = 'CONNECTION'
    packet['payload'] = {}
    packet['payload']['host'] = host
    packet['payload']['userId'] = user_id
    packet = dumps(packet)
    return packet


def create_group_chat(id, token, user_id):
    packet = {}
    packet['id'] = id
    packet['hostType'] = 'CLIENT'
    packet['token'] = token
    packet['status'] = None
    packet['operationType'] = 'REQUEST'
    packet['payloadType'] = 'GROUP_CHAT_CREATION'
    packet['payload'] = {}
    packet['payload']['userId'] = user_id
    packet['payload']['membersUsernames'] = ['MARIATESTE']
    packet['payload']['groupName'] = 'teste_sem_joao'

    packet = dumps(packet)
    return packet


def user_listing(id, token, user_id):
    packet = {}
    packet['id'] = id
    packet['hostType'] = 'CLIENT'
    packet['token'] = token
    packet['status'] = None
    packet['operationType'] = 'REQUEST'
    packet['payloadType'] = 'USER_LISTING'
    packet['payload'] = {}
    packet['payload']['userId'] = user_id
    packet = dumps(packet)
    return packet


def chat_listing(id, token, user_id):
    packet = {}
    packet['id'] = id
    packet['hostType'] = 'CLIENT'
    packet['token'] = token
    packet['status'] = None
    packet['operationType'] = 'REQUEST'
    packet['payloadType'] = 'CHAT_LISTING'
    packet['payload'] = {}
    packet['payload']['userId'] = user_id
    packet = dumps(packet)
    return packet


def group_chat_addition(id, token, user_id):
    packet = {}
    packet['id'] = id
    packet['hostType'] = 'CLIENT'
    packet['token'] = token
    packet['status'] = None
    packet['operationType'] = 'REQUEST'
    packet['payloadType'] = 'GROUP_CHAT_ADDITION'
    packet['payload'] = {}
    packet['payload']['userId'] = user_id
    packet['payload']['addedUserName'] = 'joaoteste'
    packet['payload']['chatId'] = 4
    packet = dumps(packet)
    return packet


def get_user_chat_id(id, token, user_id):
    packet = {}
    packet['id'] = id
    packet['hostType'] = 'CLIENT'
    packet['token'] = token
    packet['status'] = None
    packet['operationType'] = 'REQUEST'
    packet['payloadType'] = 'GET_USER_CHAT_ID'
    packet['payload'] = {}
    packet['payload']['userId'] = user_id
    packet['payload']['targetUsername'] = 'joaoteste'
    packet = dumps(packet)
    return packet


# LOGIN
socket = websocket.create_connection('ws://127.0.0.1:8080')
handshake = loads(socket.recv())
id = handshake['id']
host = handshake['payload']['host']
print(id, host)
socket.send(create_login_packet(id, host))
login_response = loads(socket.recv())
user_id = login_response['payload']['userId']
socket.close()

# CONECTAR COM APLICACAO
socket = websocket.create_connection(
    f"ws:/{login_response['payload']['applicationHost']}")
handshake = loads(socket.recv())
id = handshake['id']
host = handshake['payload']['host']
token = login_response['payload']['token']

socket.send(create_application_connection_packet(id, host, token, user_id))
socket.recv()

# socket.send(user_listing(id, token, user_id))
# socket.send(chat_listing(id, token, user_id))
# socket.send(create_group_chat(id, token, user_id))
# socket.send(group_chat_addition(id, token, user_id))
socket.send(get_user_chat_id(id, token, user_id))

print(socket.recv())
