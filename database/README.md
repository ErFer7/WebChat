## Subindo o banco de dados

Na pasta database, execute o comando:

```bash
docker-compose up -d
```

Isso criará o banco de acordo com as configurações escritas no arquivo docker-compose, já executando os scripts SQL da pasta `database/scripts`.

Para correta execução do codigo, execute o comando:

```bash
mvn clean install
```

E as classes de entidade que referenciam as tabelas no banco, utilizadas pelo queryDSL, serão geradas.

Para parar e remover o container:

```bash
docker stop webchat_db
docker rm webchat_db
```