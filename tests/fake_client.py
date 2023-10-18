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
    packet['payload']['identifier'] = 'joaoteste'
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
    packet['payloadType'] = 'REGISTER_USER'
    packet['payload'] = {}
    packet['payload']['identifier'] = 'joaoteste'
    packet['payload']['password'] = 'aaa'
    packet = dumps(packet)
    return packet


socket = websocket.create_connection('ws://127.0.0.1:8080')


socket.send(create_register_packet())
print(socket.recv())

socket.close()
