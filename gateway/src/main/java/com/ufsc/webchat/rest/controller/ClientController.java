package com.ufsc.webchat.rest.controller;

import static java.util.Objects.isNull;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ufsc.webchat.model.RoutingResponseDto;
import com.ufsc.webchat.model.ValidationMessage;
import com.ufsc.webchat.protocol.enums.Status;
import com.ufsc.webchat.rest.model.UserDto;
import com.ufsc.webchat.rest.service.ClientService;
import com.ufsc.webchat.rest.validator.UserRegisterValidator;
import com.ufsc.webchat.websocket.GatewayThread;
import com.ufsc.webchat.websocket.ServerHandler;

@RestController
@RequestMapping("/api/client")
@CrossOrigin(origins = "*")
public class ClientController {

	private final ClientService clientService;
	private final UserRegisterValidator userRegisterValidator = new UserRegisterValidator();

	public ClientController(ClientService clientService) {
		this.clientService = clientService;
	}

	@GetMapping("/login")
	public ResponseEntity<Object> login(UserDto userDto) {
		Long userId = this.clientService.login(userDto);
		if (isNull(userId)) {
			return ResponseEntity.badRequest().body("Falha no login, usuário não encontrado ou senha incorreta.");
		}

		CompletableFuture<RoutingResponseDto> serverResponse = new CompletableFuture<>();
		ServerHandler serverHandler = GatewayThread.getInstance().getServerHandler();
		serverHandler.processLoginRequestWithApplication(userId, userDto.getClientId(), serverResponse);
		RoutingResponseDto applicationResponse;

		try {
			applicationResponse = serverResponse.orTimeout(20000, TimeUnit.MILLISECONDS).join();
		} catch (CompletionException e) {
			return ResponseEntity.status(HttpStatus.REQUEST_TIMEOUT).body("Tempo limite excedido para a resposta do servidor de aplicação");
		}

		if (applicationResponse.getStatus() == Status.OK) {
			return ResponseEntity.ok(applicationResponse);
		} else {
			return ResponseEntity.internalServerError().body("Falha interna no roteamento");
		}

	}

	@PostMapping("/register")
	public ResponseEntity<String> register(@RequestBody UserDto userDto) {
		ValidationMessage validationMessage = this.userRegisterValidator.validate(userDto);
		if (!validationMessage.isValid()) {
			return ResponseEntity.badRequest().body(validationMessage.message());
		}

		Long userId = this.clientService.register(userDto);
		if (isNull(userId)) {
			return ResponseEntity.internalServerError().body("Erro ao cadastrar usuário, tente novamente.");
		}
		return ResponseEntity.status(HttpStatus.CREATED).body(userId.toString());
	}

}
