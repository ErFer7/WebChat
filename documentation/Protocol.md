# Protocolo

- [Protocolo](#protocolo)
  - [Formato](#formato)
  - [Fluxo de conexões](#fluxo-de-conexões)
    - [Conexão entre servidor de aplicação e o gateway](#conexão-entre-servidor-de-aplicação-e-o-gateway)
    - [Conexão entre cliente e o gateway](#conexão-entre-cliente-e-o-gateway)
    - [Fechamento de conexão com o cliente](#fechamento-de-conexão-com-o-cliente)
    - [Criação de usuário](#criação-de-usuário)
    - [Obtenção de uma conversa](#obtenção-de-uma-conversa)
    - [Criação de uma conversa em grupo](#criação-de-uma-conversa-em-grupo)
    - [Adição de usuário em uma conversa em grupo](#adição-de-usuário-em-uma-conversa-em-grupo)
    - [Envio de mensagem](#envio-de-mensagem)

## Formato

```json
{
    "hostType": "[type]",
    "token": "[token]" | null,
    "status": "[status]" | null,
    "operationType": "[operationType]",
    "payloadType": "[payloadType]",
    "payload": {...} | null
}
```

- **Host**: Endereço do host que enviou a mensagem.
- **hostType**: Tipo de host que enviou a mensagem.
  - **CLIENT**: Cliente.
  - **GATEWAY**: Servidor de gateway.
  - **APPLICATION**: Servidor de aplicação.
- **Token**: Token de autenticação.
- **Status**: Status da mensagem.
  - **OK**: Mensagem enviada com sucesso.
  - **ERROR**: Erro ao enviar a mensagem.
- **operationType**: Tipo de operação.
  - **REQUEST**: Pedido.
  - **RESPONSE**: Resposta.
  - **INFO**: Informação
- **payloadType**: Tipo de payload.
  - **CONNECTION**: Conexão.
  - **ROUTING**: Roteamento.
  - **USER_CREATION**: Criação do usuário.
  - **USER_LISTING**: Listagem de usuários.
  - **USER_APPLICATION_SERVER**: Host do servidor de aplicação que está conectado com o usuário.
  - **DISCONNECTION**: Fechamento de conexão.
  - **GROUP_CHAT_CREATION**: Criação de conversa em grupo.
  - **CHAT_LISTING**: Listagem de conversas.
  - **GROUP_CHAT_ADDITION**: Adição de usuário na conversa.
  - **MESSAGE**: Mensagem.
  - **MESSAGE_LISTING**: Listagem de mensagens.
- **payload**: Dados da mensagem, pode ser nulo.

---

## Fluxo de conexões

### Conexão entre servidor de aplicação e o gateway

```mermaid
sequenceDiagram
    Application -->> Gateaway: Websocket connection
    Gateaway ->> Application: Host
    Application ->> Gateaway: Connection request
    Gateaway ->> Application: Connection response
```

1. A conexão é estabelecida.

2. O Gateway informa o host do servidor de aplicação

```json
{
    "id": "[id]",
    "hostType": "GATEWAY",
    "token": null,
    "status": "[status]",
    "operationType": "INFO",
    "payloadType": "HOST",
    "payload": {
        "host": "[host]"
    }
}
```

3. O servidor de aplicação envia uma mensagem de conexão para o gateway.

```json
{
    "id": "[id]",
    "hostType": "APPLICATION",
    "token": null,
    "status": null,
    "operationType": "REQUEST",
    "payloadType": "CONNECTION",
    "payload": {
        "identifier": "[id]",
        "password": "[password]",
        "host": "[host]",
        "externalPort": "[externalPort]"
    }
}
```

4. O gateway responde com uma mensagem de status.

```json
{
    "id": "[id]",
    "hostType": "GATEWAY",
    "token": null,
    "status": "[status]",
    "operationType": "RESPONSE",
    "payloadType": "CONNECTION",
    "payload": {
      "token": "[token]"
    }
}
```

---

### Conexão entre cliente e o gateway

```mermaid
sequenceDiagram
    Client -->> Gateway: Websocket connection
    Gateway ->> Client: Host
    Client ->> Gateway: Routing request
    Gateway ->> Application: Routing request
    Application ->> Gateway: Routing response
    Gateway -->> Client: Routing response
    Client -->> Gateway: Websocket disconnection
    Client -->> Application: Websocket connection
    Application ->> Client: Host
    Client ->> Application: Connection request
    Application ->> Client: Connection response
```

1. O cliente se conecta

2. O Gateway informa o cliente sobre seu host (o id e o host retornados deverão ser usados pelo cliente nos próximos pacotes)

    ```json
    {
        "id": "[id]",
        "hostType": "GATEWAY",
        "token": null,
        "status": "[status]",
        "operationType": "INFO",s
        "payloadType": "HOST",
        "payload": {
            "host": "[host]"
        }
    }
    ```

3. O cliente envia uma mensagem de conexão (login) para o gateway.

    ```json
    {
        "id": "[id]",
        "hostType": "CLIENT",
        "token": null,
        "status": null,
        "operationType": "REQUEST",
        "payloadType": "ROUTING",
        "payload": {
            "host": "[host]",
            "username": "[id]",
            "password": "[password]"
        }
    }
    ```

4. O gateway autentica a conexão e envia avisa um servidor de aplicação.

      ```json
      {
          "id": "[id]",
          "hostType": "GATEWAY",
          "token": null,
          "status": null,
          "operationType": "REQUEST",
          "payloadType": "ROUTING",
          "payload": {
              "userId": "[id]",
              "token": "[token]"
          }
      }
    ```

5. O servidor de aplicação confirma que recebeu a mensagem.

    ```json
    {
      "id": "[id]",
      "hostType": "APPLICATION",
      "token": null,
      "status": "[status]",
      "operationType": "RESPONSE",
      "payloadType": "ROUTING",
      "payload": {
        "userId": "[id]",
        "token": "[token]"
      }
    }
    ```

6. O gateway avisa o cliente do roteamento.

    ```json
    {
        "id": "[id]",
        "hostType": "GATEWAY",
        "token": null,
        "status": "[status]",
        "operationType": "RESPONSE",
        "payloadType": "ROUTING",
        "payload": {
            "userId": "[id]",
            "token": "[token]",
            "applicationId": "[id]",
            "applicationHost": "[ip:port]"
        }
    }
    ```

7. O cliente fecha a conexão com o gateway.

8. O cliente se conecta com o servidor de aplicação.

9. O servidor de aplicação envia para o cliente as informações de host (o id e o host retornados deverão ser usados pelo cliente nos próximos pacotes).

    ```json
    {
        "id": "[id]",
        "hostType": "APPLICATION",
        "token": null,
        "status": "[status]",
        "operationType": "INFO",
        "payloadType": "HOST",
        "payload": {
            "host": "[host]"
        }
    }
    ```

10. O cliente tenta a conexão com o servidor direcionado.

    ```json
    {
        "id": "[id]",
        "hostType": "CLIENT",
        "token": "[token]",
        "status": null,
        "operationType": "REQUEST",
        "payloadType": "CONNECTION",
        "payload": {
          "host": "[host]",
          "userId": "[id]"
        }
    }
    ```

11. O servidor responde com o status.

    ```json
    {
        "id": "[ip:port]",
        "hostType": "CLIENT",
        "token": null,
        "status": "[status]",
        "operationType": "REQUEST",
        "payloadType": "CONNECTION",
        "payload": null
    }
    ```

---

### Fechamento de conexão com o cliente

```mermaid
sequenceDiagram
  Client ->> Application: Closing request
  Application ->> Gateway: Closing request
  Gateway ->> Application: Closing response
  Application ->> Client: Closing response
  Client -->> Application: Websocket disconnection
```

1. O cliente envia uma requisição de fechamento.

```json
{
    "id": "[id]",
    "hostType": "CLIENT",
    "token": "[token]",
    "status": null,
    "operationType": "REQUEST",
    "payloadType": "DISCONNECTION",
    "payload": {
      "userId": "[id]"
    }
}
```

2. O servidor envia uma requisição de fechamento para o gateway.

```json
{
    "id": "[id]",
    "hostType": "APPLICATION",
    "token": "[token]",
    "status": null,
    "operationType": "REQUEST",
    "payloadType": "DISCONNECTION",
    "payload": {
      "userId": "[id]"
    }
}
```

3. O gateway responde para o servidor.

```json
{
    "id": "[id]",
    "hostType": "GATEWAY",
    "token": null,
    "status": "[status]",
    "operationType": "RESPONSE",
    "payloadType": "DISCONNECTION",
    "payload": {
      "userId": "[id]"
    }
}
```

4. O servidor responde para o cliente.

```json
{
    "id": "[id]",
    "hostType": "APPLICATION",
    "token": null,
    "status": "[status]",
    "operationType": "RESPONSE",
    "payloadType": "DISCONNECTION",
    "payload": null
}
```

5. O cliente se desconecta.

---

### Criação de usuário

```mermaid
sequenceDiagram
  Client -->> Gateway: Websocket connection
  Gateway ->> Client: Host
  Client ->> Gateway: Creation request
  Gateway ->> Client: Creation response
  Client -->> Gateway: Websocket disconnection
```

1. O cliente se conecta

2. O Gateway informa o cliente sobre seu host

```json
{
    "id": "[id]",
    "hostType": "GATEWAY",
    "token": null,
    "status": "[status]",
    "operationType": "INFO",
    "payloadType": "HOST",
    "payload": {
        "host": "[host]"
    }
}
```

3. O cliente envia uma mensagem de conexão para o gateway.

```json
{
    "id": "[id]",
    "hostType": "CLIENT",
    "token": null,
    "status": null,
    "operationType": "REQUEST",
    "payloadType": "USER_CREATION",
    "payload": {
        "host": "[host]",
        "username": "[username]",
        "password": "[password]"
    }
}
```

4. O gateway responde.

```json
{
    "id": "[id]",
    "hostType": "GATEWAY",
    "token": null,
    "status": "[status]",
    "operationType": "RESPONSE",
    "payloadType": "USER_CREATION",
    "payload": {
        "message": "[message]"
    }
}
```

5. O cliente se desconecta.

---

### Obtenção de uma conversa

```mermaid
sequenceDiagram
  Client ->> Application: Get chat request
  Application ->> Client: Get chat response
```

---

### Criação de uma conversa em grupo

```mermaid
sequenceDiagram
  Client ->> Application: Create group chat request
  Application ->> Client: Create group chat response
```

---

### Adição de usuário em uma conversa em grupo

```mermaid
sequenceDiagram
  Client ->> Application: Add user to group chat request
  Application ->> Client: Add user to group chat response
```

---

### Envio de mensagem

Cenário em que os dois usuários estão conectados em um mesmo servidor.

```mermaid
sequenceDiagram
  Client A ->> Application: Message
  Application ->> Client A: Response
  Application ->> Client B: Message
  Client B ->> Application: Response
```

Cenário em que os dois usuários estão conectados em servidores diferentes, mas os servidores estão conectados entre si.

```mermaid
sequenceDiagram
    Client A ->> Application A: Message
    Application A ->> Client A: Response
    Application A ->> Application B: Message
    Application B ->> Application A: Response
    Application B ->> Client B: Message
    Client B ->> Application B: Response
```

Cenário em que os dois usuários estão conectados em servidores diferentes, mas os servidores não estão conectados entre si.

```mermaid
sequenceDiagram
    Client A ->> Application A: Message
    Application A ->> Client A: Response
    Application A ->> Gateway: Request application server
    Gateway ->> Application A: application server host
    Application A ->> Application B: Request connection with message
    Application B ->> Application A: Connection and message response
    Application B ->> Client B: Message
    Client B ->> Application B: Response
```
