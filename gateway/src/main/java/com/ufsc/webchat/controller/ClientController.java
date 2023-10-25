package com.ufsc.webchat.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ufsc.webchat.server.GatewayThread;
import com.ufsc.webchat.server.ServerHandler;

@RestController
@RequestMapping("/api/client")
@CrossOrigin(origins = "*")
public class ClientController {

	@GetMapping("/register")
	public ResponseEntity<String> register() {
		// TODO: Usar instância aqui (Cuidado, é uma thread, avalie se tudo é threadsafe).
		// Sugiro esperar com um lock ou qualquer coisa assim, só deve ser avaliado se o spring não vai morrer com isso.
		ServerHandler serverHandler = GatewayThread.getInstance().getServerHandler();

		return ResponseEntity.ok("Register test");
	}

	@GetMapping("/route")
	public ResponseEntity<String> route() {
		// TODO: Usar instância aqui (Cuidado, é uma thread, avalie se tudo é threadsafe).
		ServerHandler serverHandler = GatewayThread.getInstance().getServerHandler();

		return ResponseEntity.ok("Route test");
	}
}
