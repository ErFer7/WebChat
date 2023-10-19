'''
Webchat.
'''

from os.path import join

from source.command_manager import ClientCommandManager


class ClientInterface():

    '''
    App.
    '''

    _client_command_manager: ClientCommandManager

    def __init__(self) -> None:
        self._client_command_manager = ClientCommandManager()

    def print_banner(self) -> None:
        '''
        Exibe o banner.
        '''

        with open(join('assets', 'text', 'banner.txt'), 'r', encoding='utf-8') as banner:
            print(banner.read())

    def print_horizontal_line(self) -> None:
        '''
        Exibe uma linha horizontal.
        '''

        print('-' * 80)

    def run(self) -> None:
        '''
        Executa a aplicação.
        '''

        self.print_horizontal_line()
        self.print_banner()
        self.print_horizontal_line()

        while True:
            command = input('>>> ').split()

            if len(command) == 0:
                continue

            try:
                match command[0]:
                    case 'register' | 'r':
                        self._client_command_manager.register(command[1], command[2])
                    case 'login' | 's':
                        self._client_command_manager.login(command[1], command[2])
                    case 'send' | 's':
                        pass
                    case 'exit' | 'q':
                        print('>>> Exiting...')
                        self._client_command_manager.exit_client()
                        break
                    case _:
                        print('>>> Comando inválido.')
            except IndexError:
                print('>>> Argumentos inválidos.')

            print(self._client_command_manager.receive_packet())
