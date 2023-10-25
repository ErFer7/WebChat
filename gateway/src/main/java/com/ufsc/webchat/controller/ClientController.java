package com.ufsc.webchat.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ufsc.webchat.server.GatewayThread;

@RestController
@RequestMapping("/api/client")
@CrossOrigin(origins = "*")
public class ClientController {

	@GetMapping("/register")
	public ResponseEntity<String> register() {
		// TODO: Usar instância aqui (Cuidado, é uma thread, avalie se tudo é threadsafe).
		GatewayThread gatewayThread = GatewayThread.getInstance();

		return ResponseEntity.ok("Register test");
	}

	@GetMapping("/route")
	public ResponseEntity<String> route() {
		// TODO: Usar instância aqui (Cuidado, é uma thread, avalie se tudo é threadsafe).
		GatewayThread gatewayThread = GatewayThread.getInstance();

		return ResponseEntity.ok("Route test");
	}
}
