'''
Sistema de deploy.
'''


from os.path import join
from source.deployer import Deployer

deployer = Deployer(join('config', 'production.json'))
deployer.run()

