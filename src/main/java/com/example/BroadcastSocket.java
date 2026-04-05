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
	private static final java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newSingleThreadExecutor();
	private static final java.util.concurrent.BlockingQueue<String> queue = new java.util.concurrent.LinkedBlockingQueue<>(10000);

	static {
		executor.submit(() -> {
			while (!Thread.currentThread().isInterrupted()) {
				try {
					String msg = queue.take();

					// cleanup dead sessions
					sessions.removeIf(s -> !s.isOpen());

					for (Session session : sessions) {
						session.getAsyncRemote().sendText(msg);
					}

					Thread.sleep(10L); // throttle

				} catch (InterruptedException e) {
					//สำคัญมาก
					Thread.currentThread().interrupt();
					break;
				} catch (Exception e) {
					System.out.println("Broadcast error");
				}
			}

			System.out.println("WebSocket broadcast thread stopped.");
		});
	}

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
		if (!queue.offer(message)) { //เก็บเข้า queue ก่อน
			System.out.println("Queue full, dropping message");
		}
	}

}