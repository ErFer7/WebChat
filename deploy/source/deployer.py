'''
Deployer.
'''

import paramiko
from os.path import join
from json import load
from subprocess import call

class Deployer():

    '''
    Deployer.
    '''

    _production_config: dict

    def __init__(self, production_config_path: str) -> None:

        with open(production_config_path, 'r', encoding='utf-8') as production_config_file:
            self._production_config = load(production_config_file)


    def run(self) -> None:
        '''
        Executa.
        '''

        print('>>> Webchat Deployer')

        while True:
            print('\n>>> Selecione uma opção:')
            print('>>> 1 - Setup')
            print('>>> 2 - Transeferir arquivos')
            print('>>> 3 - Inicializar servidor')
            print('>>> 4 - Finalizar servidor')
            print('>>> 5 - Sair')

            option = input('>>> ')

            match option:
                case '1':
                    self.setup()
                case '2':
                    self.transfer()
                case '3':
                    self.start()
                case '5':
                    print('>>> Saindo...')
                    break
                case _:
                    print('>>> Opção inválida')

    def setup(self) -> None:
        '''
        Setup.
        '''

        print('>>> Setup')

        while True:
            print('\n>>> Selecione uma opção:')
            print('>>> 1 - Gerar arquivos de configuração')
            print('>>> 2 - Gerar executáveis')
            print('>>> 3 - Voltar')

            option = input('>>> ')

            match option:
                case '1':
                    self.generate_config_files()
                case '2':
                    self.generate_executables()
                case '3':
                    break
                case _:
                    print('>>> Opção inválida')

    def generate_config_files(self) -> None:
        '''
        Gera arquivos de configuração.
        '''

        print('>>> Gerando arquivos de configuração')

        common_application_properties = f'db.url={self._production_config["database"]["url"]}\n' + \
                                        f'db.user={self._production_config["database"]["user"]}\n' + \
                                        f'db.password={self._production_config["database"]["password"]}\n' + \
                                        f'db.driver={self._production_config["database"]["driver"]}\n'

        gateway_application_properties = f'server.port={self._production_config["gateway"]["restPort"]}\ndebug=true'

        network_properties = f'gatewayHost={self._production_config["gateway"]["host"]}\n' + \
                             f'gatewayPort={self._production_config["gateway"]["websocketPort"]}\n' + \
                             f'gatewayIdentifier={self._production_config["gateway"]["identifier"]}\n' + \
                             f'gatewayPassword={self._production_config["gateway"]["password"]}\n'

        with open(join('config', 'common-application-prod.properties'), 'w', encoding='utf-8') as file:
            file.write(common_application_properties)

        with open(join('config', 'gateway-application-prod.properties'), 'w', encoding='utf-8') as file:
            file.write(gateway_application_properties)

        with open(join('config', 'network-prod.properties'), 'w', encoding='utf-8') as file:
            file.write(network_properties)

        print('>>> Arquivos de configuração gerados')

    def generate_executables(self) -> None:
        '''
        Gera executáveis.
        '''

        print('>>> Gerando executáveis')

        call(['sdk', 'use', 'java', '21-zulu'], cwd='..')
        call(['mvn', 'clean', 'install'], cwd='..')
        call(['mvn', 'package'], cwd='..')

        print('>>> Executáveis gerados')

    def transfer(self) -> None:
        '''
        Transfere os arquivos.
        '''

        print('>>> Transferindo arquivos')

        for server in self._production_config['servers']:
            match server['role']:
                case 'database':
                    ssh = paramiko.SSHClient()
                    ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())

                    username, hostname = server['login'].split('@')
                    ssh.connect(hostname, username=username, password=server['password'])

                    sftp = ssh.open_sftp()

                    sftp.put(join('..', 'database', 'scripts', 'create_tables.sql'), join('webchat-database', 'scripts', 'create_tables.sql'))
                    sftp.put(join('..', 'database', 'docker-compose.yml'), join('webchat-database', 'docker-compose.yml'))

                    sftp.close()
                    ssh.close()
                case 'gateway':
                    ssh = paramiko.SSHClient()
                    ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())

                    username, hostname = server['login'].split('@')
                    ssh.connect(hostname, username=username, password=server['password'])

                    sftp = ssh.open_sftp()

                    sftp.put(join('..', 'gateway', 'target', 'gateway-1.0.jar'), join('webchat-gateway', 'gateway-1.0.jar'))
                    sftp.put(join('config', 'common-application-prod.properties'), join('webchat-gateway', 'application.properties'))
                    sftp.put(join('config', 'gateway-application-prod.properties'), join('webchat-gateway', 'spring-application.properties'))
                    sftp.put(join('config', 'network-prod.properties'), join('webchat-gateway', 'network.properties'))

                    sftp.close()
                    ssh.close()
                case 'application':
                    pass
                case _:
                    print('>>> Papel inválido')

        print('>>> Arquivos transferidos')

    def start(self) -> None:
        '''
        Inicia servidores.
        '''

        while True:
            print('\n>>> Selecione uma opção:')
            print('>>> 1 - Voltar')
            for i, server in enumerate(self._production_config['servers']):
                print(f'>>> {i + 2} - {server["role"]} - {server["login"]}')

            option = input('>>> ')

            match option:
                case '1':
                    break
                case value:
                    self.start_server(int(value) - 2)

    def start_server(self, server: dict) -> None:
        '''
        Inicia servidor.
        ''' 

        print(f'>>> Iniciando servidor {server["role"]} - {server["login"]}')
