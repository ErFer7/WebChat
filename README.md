# WebChat

Um sistema simples de chat em tempo real.

---

## Stack

* **Gerenciador de projetos**: Maven
* **Frameworks de backend**: [SNF4J](https://github.com/snf4j/snf4j) and [Spring Boot](https://spring.io/projects/spring-boot)
* **Linguagem de backend**: Java
* **Biblioteca de frontend**: React
* **Linguagem de frontend**: JavaScript
* **Banco de dados**: PostgresSQL

---

## Como executar localmente

### Requisitos do banco de dados

* **Docker**: [Instalação](https://docs.docker.com/engine/install/)

### Requisitos do backend

* **SDK Man**: [Instalação](https://sdkman.io/install)
* **Java 21**: `sdk install java 21-zulu`
* **Maven**: `sdk install maven`

### Requisitos do frontend

* **NVM**: `curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.39.1/install.sh | bash`
* **Node**: `nvm install node`
* **Yarn**: `npm install --global yarn`

---

### Banco de dados

**Requisitos**:

* Docker

1. Navegue até a pasta database com `cd database`
2. Execute o comando `docker-compose up -d db`

---

### Preparação do backend

1. Na raíz do repositório, execute o comando `mvn clean install`
2. Gere os pacotes com `mvn package`

---

### Servidor de Gateway

1. Navegue até a pasta gateway com `cd gateway/target`
2. Execute o comando `java -jar gateway-1.0.jar`

---

### Servidor de Application

1. Navegue até a pasta application com `cd application/target`
2. Execute o comando `java -jar application-1.0.jar` para cada instância desejada

---

### Frontend

1. Navegue até a pasta frontend com `cd frontend`
2. Execute o comando `yarn install`
3. Execute o comando `yarn start`

---

**OBS**: Execute sempre na ordem:

1. Banco de dados
2. Servidor de Gateway
3. Servidores de Aplicação
4. Frontend

---

### Pastas de configurações de URL's

API Gateway no Frontend: `frontend/src/pages/login/ClientService.js`
Gateway no Backend: `common/src/main/resources/config/network.properties`
Banco de dados no Backend: `common/src/main/resources/config/application.properties`

[Changelog](Changelog.md)

---

[Documentação](Documentation.md)
