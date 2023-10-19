'''
Pacote.
'''

from json import dumps

from source.host_type import HostType
from source.status import Status
from source.operation_type import OperationType
from source.payload_type import PayloadType


class Packet():

    '''
    Pacote.
    '''

    _host: str
    _host_type: HostType
    _token: str | None
    _status: Status | None
    _operation_type: OperationType
    _payload_type: PayloadType
    _payload: dict | None

    def __init__(self,
                 host: str,
                 host_type: HostType,
                 operation_type: OperationType,
                 payload_type: PayloadType,
                 token: str | None = None,
                 status: Status | None = None,
                 payload: dict | None = None) -> None:
        self._host = host
        self._host_type = host_type
        self._token = token
        self._status = status
        self._operation_type = operation_type
        self._payload_type = payload_type
        self._payload = payload

    def __str__(self) -> str:
        packet = {
            'host': self._host,
            'hostType': self._host_type.value,
            'token': self._token,
            'status': self._status.value if self._status is not None else None,
            'operationType': self._operation_type.value,
            'payloadType': self._payload_type.value,
            'payload': self._payload
        }

        return dumps(packet)
