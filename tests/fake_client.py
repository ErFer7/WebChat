'''
Fake client.
'''

from json import dumps

import websocket

socket = websocket.create_connection('ws://127.0.0.1:8080')

packet = {}

packet['host'] = f'/127.0.0.1:{socket.sock.getsockname()[1]}'
packet['hostType'] = 'CLIENT'
packet['token'] = None
packet['status'] = None
packet['operationType'] = 'REQUEST'
packet['payloadType'] = 'ROUTING'
packet['payload'] = {}
packet['payload']['identifier'] = 'abc'
packet['payload']['password'] = 'aaa'

packet = dumps(packet)

print(str(packet))

socket.send(str(packet))
print(socket.recv())

socket.close()
