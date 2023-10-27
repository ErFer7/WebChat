package com.ufsc.webchat.websocket;

import static java.util.UUID.randomUUID;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;

import org.snf4j.websocket.IWebSocketSession;

import com.ufsc.webchat.model.RoutingResponseDto;
import com.ufsc.webchat.protocol.Packet;
import com.ufsc.webchat.protocol.PacketFactory;
import com.ufsc.webchat.protocol.enums.HostType;
import com.ufsc.webchat.server.Handler;
import com.ufsc.webchat.websocket.utils.ApplicationContextMap;

public class ServerHandler extends Handler {

	private final String id;
	private final PacketFactory packetFactory;
	private final ApplicationContextMap applicationContextMap;
	private final SecureRandom secureRandom;
	private final Base64.Encoder encoder;
	private final ApplicationPacketProcessor applicationPacketProcessor;
	private final HashMap<Long, String> userIdClientIdMap;
	private final HashMap<String, CompletableFuture<RoutingResponseDto>> pendingRequestsByClientId;

	public ServerHandler() {
		this.id = randomUUID().toString();
		this.packetFactory = new PacketFactory(this.id, HostType.GATEWAY);
		this.applicationContextMap = new ApplicationContextMap();
		this.secureRandom = new SecureRandom();
		this.encoder = Base64.getUrlEncoder();
		this.userIdClientIdMap = new HashMap<>();
		this.pendingRequestsByClientId = new HashMap<>();
		this.applicationPacketProcessor = new ApplicationPacketProcessor(this, this.packetFactory, this.applicationContextMap, this.userIdClientIdMap);
	}

	@Override
	public void readPacket(Packet packet) {
		if (packet.getHostType() == HostType.APPLICATION) {
			this.applicationPacketProcessor.process(packet);
		}
	}

	@Override
	protected void sessionReady(IWebSocketSession session) {
		super.sessionReady(session);
		this.sendPacketBySession(session, this.packetFactory.createHandshakeInfo(session.getRemoteAddress().toString()));
	}

	public String generateToken() {
		this.secureRandom.setSeed(System.currentTimeMillis());

		byte[] randomBytes = new byte[24];
		this.secureRandom.nextBytes(randomBytes);
		return this.encoder.encodeToString(randomBytes);
	}

	public void processLoginRequestWithApplication(Long userId, String clientId, CompletableFuture<RoutingResponseDto> futureRequest) {
		this.pendingRequestsByClientId.put(clientId, futureRequest);
		// TODO: Avaliar necessidade, uso só pra recuperar o clientId (mas aplicação poderia me repassar direto)
		this.userIdClientIdMap.put(userId, clientId);
		String serverId = this.applicationContextMap.chooseLeastLoadedApplication();
		var applicationRequestPacket = this.packetFactory.createClientRoutingRequest(userId, this.generateToken());
		this.sendPacketById(serverId, applicationRequestPacket);
	}

	public void completeClientLoginRequest(String clientId, RoutingResponseDto routingResponseDto) {
		CompletableFuture<RoutingResponseDto> future = this.pendingRequestsByClientId.remove(clientId);
		future.complete(routingResponseDto);
	}

}
