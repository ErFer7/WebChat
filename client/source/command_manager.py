'''
Comandos.
'''

import websocket

from source.host_type import HostType
from source.packet_factory import PacketFactory


class ClientCommandManager():

    '''
    Comandos.
    '''

    _websocket: websocket.WebSocket
    _packet_factory: PacketFactory

    def __init__(self) -> None:
        self._websocket = websocket.create_connection('ws://127.0.0.1:8080')
        self._packet_factory = PacketFactory(f'/127.0.0.1:{self._websocket.sock.getsockname()[1]}', HostType.CLIENT)

    def register(self, username: str, password: str) -> None:
        '''
        Registra um usuário.
        '''

        self._websocket.send(str(self._packet_factory.create_registration_packet(username, password)))
        response = self._websocket.recv()

        print(f'>>> Received: {response}')

    def login(self, username: str, password: str) -> None:
        '''
        Loga um usuário.
        '''

        self._websocket.send(str(self._packet_factory.create_login_packet(username, password)))
        response = self._websocket.recv()

        print(f'>>> Received: {response}')

    def exit_client(self) -> None:
        '''
        Fecha a conexão.
        '''

        self._websocket.close()
