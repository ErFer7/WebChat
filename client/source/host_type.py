'''
Tipo de host.
'''


from enum import Enum


class HostType(Enum):

    '''
    Tipo de host.
    '''

    CLIENT = 'CLIENT'
    GATEWAY = 'GATEWAY'
    APPLICATION = 'APPLICATION'
