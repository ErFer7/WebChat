'''
Fábrica de pacotes.
'''

from source.host_type import HostType
from source.status import Status
from source.operation_type import OperationType
from source.payload_type import PayloadType
from source.packet import Packet


class PacketFactory():

    '''
    Fábrica de pacotes.
    '''

    _host: str
    _host_type: HostType
    _token: str | None

    def __init__(self, host: str, host_type: HostType):
        self._host = host
        self._host_type = host_type

    @property
    def token(self) -> str | None:
        '''
        Getter do token.
        '''

        return self._token

    @token.setter
    def token(self, token: str) -> None:
        '''
        Setter do token.
        '''

        self._token = token

    def create_packet(self,
                      operation_type: OperationType,
                      payload_type: PayloadType,
                      status: Status | None = None,
                      payload: dict | None = None) -> Packet:
        '''
        Cria um pacote.
        '''

        return Packet(self._host, self._host_type, operation_type, payload_type, self._token, status, payload)

    def create_registration_packet(self, username: str, password: str) -> Packet:
        '''
        Cria um pacote de registro.
        '''

        payload = {
            'identifier': username,
            'password': password
        }

        return self.create_packet(OperationType.REQUEST, PayloadType.USER_CREATION, None, payload)

    def create_login_packet(self, username: str, password: str) -> Packet:
        '''
        Cria um pacote de login.
        '''

        payload = {
            'identifier': username,
            'password': password
        }

        return self.create_packet(OperationType.REQUEST, PayloadType.ROUTING, None, payload)
