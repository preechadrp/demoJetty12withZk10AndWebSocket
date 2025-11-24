package com.example;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;

@ServerEndpoint("/websocket/broadcast")
public class BroadcastSocket {

	private static final Set<Session> sessions = Collections.synchronizedSet(new HashSet<>());

	@OnOpen
	public void onOpen(Session session) {
		sessions.add(session);
		System.out.println("WebSocket session opened: " + session.getId());
	}

	@OnClose
	public void onClose(Session session) {
		sessions.remove(session);
		System.out.println("WebSocket session closed: " + session.getId());
	}

	@OnError
	public void onError(Session session, Throwable throwable) {
		System.err.println("WebSocket Error: " + throwable.getMessage());
	}

	// เมธอดสำหรับ Broadcast
	public static void broadcast(String message) {
		synchronized (sessions) {
			for (Session session : sessions) {
				if (session.isOpen()) {
					try {
						session.getAsyncRemote().sendText(message);
					} catch (Exception e) {
						System.err.println("Error broadcasting: " + e.getMessage());
					}
				}
			}
		}
	}
}