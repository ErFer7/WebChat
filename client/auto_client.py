'''
Fake client.
'''

from json import dumps

import websocket


def create_login_packet():
    packet = {}
    packet['host'] = f'/127.0.0.1:{socket.sock.getsockname()[1]}'
    packet['hostType'] = 'CLIENT'
    packet['token'] = None
    packet['status'] = None
    packet['operationType'] = 'REQUEST'
    packet['payloadType'] = 'ROUTING'
    packet['payload'] = {}
    packet['payload']['identifier'] = 'juniorteste'
    packet['payload']['password'] = 'aaa'
    packet = dumps(packet)
    return packet


def create_register_packet():
    packet = {}
    packet['host'] = f'/127.0.0.1:{socket.sock.getsockname()[1]}'
    packet['hostType'] = 'CLIENT'
    packet['token'] = None
    packet['status'] = None
    packet['operationType'] = 'REQUEST'
    packet['payloadType'] = 'USER_CREATION'
    packet['payload'] = {}
    packet['payload']['identifier'] = 'juniorteste'
    packet['payload']['password'] = 'aaa'
    packet = dumps(packet)
    return packet


connection_token = 'ynxV_tNBVDZFKHmHEWIe3vywVLUWSyI9'


def create_application_connection_packet():
    packet = {}
    packet['host'] = f'/127.0.0.1:{socket.sock.getsockname()[1]}'
    packet['hostType'] = 'CLIENT'
    packet['token'] = connection_token
    packet['status'] = None
    packet['operationType'] = 'REQUEST'
    packet['payloadType'] = 'CONNECTION'
    packet['payload'] = {}
    packet['payload']['userId'] = 4
    packet = dumps(packet)
    return packet


def create_group_chat():
    packet = {}
    packet['host'] = f'/127.0.0.1:{socket.sock.getsockname()[1]}'
    packet['hostType'] = 'CLIENT'
    packet['token'] = connection_token
    packet['status'] = None
    packet['operationType'] = 'REQUEST'
    packet['payloadType'] = 'GROUP_CHAT_CREATION'
    packet['payload'] = {}
    packet['payload']['userId'] = 4
    packet['payload']['membersUsernames'] = ['joaoteste', 'MARIATESTE']
    packet['payload']['groupName'] = 'familiateste2'

    packet = dumps(packet)
    return packet


socket = websocket.create_connection('ws://127.0.0.1:8081')


socket.send(create_group_chat())
print(socket.recv())

socket.close()
