'''
Sistema de deploy.
'''


from os.path import join
from source.deployer import Deployer

print('[O DEPLOYER ESTÁ IMCOMPLETO]')

deployer = Deployer(join('config', 'production.json'))
deployer.run()
