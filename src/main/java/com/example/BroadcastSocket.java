package com.example;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;

@ServerEndpoint("/websocket/broadcast")
public class BroadcastSocket {

	private static final Set<Session> sessions = ConcurrentHashMap.newKeySet();
	private static final Map<Session, Boolean> sending = new ConcurrentHashMap<>();
	private static final java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newSingleThreadExecutor();
	private static final java.util.concurrent.BlockingQueue<String> queue = new java.util.concurrent.LinkedBlockingQueue<>(10000);
	
	static {
		executor.submit(() -> {
			while (!Thread.currentThread().isInterrupted()) {
				try {
					String msg = queue.take();

					// cleanup dead sessions + flags
                    sessions.removeIf(s -> {
                        if (!s.isOpen()) {
                            sending.remove(s);
                            return true;
                        }
                        return false;
                    });

					for (Session session : sessions) {
						
						Boolean isSending = sending.get(session);
					    if (isSending == null || isSending) {
					        continue; // ยังส่งไม่เสร็จ → ข้าม
					    }

					    sending.put(session, true);
					    
					    session.getAsyncRemote().sendText(msg, result -> {
					        sending.put(session, false);

					        if (!result.isOK()) {
					        	System.out.println("Send failed session=" + session.getId() + result.getException());
					        }
					    });
					    
					}

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
		session.getAsyncRemote().setSendTimeout(5000);
		session.setMaxIdleTimeout(30 * 60 * 1000L);
		
		sessions.add(session);
		sending.put(session, false);
		System.out.println("WebSocket session opened: " + session.getId());
	}

	@OnClose
	public void onClose(Session session) {
		sessions.remove(session);
		sending.remove(session);
		System.out.println("WebSocket session closed: " + session.getId());
	}

	@OnError
	public void onError(Session session, Throwable throwable) {
		sessions.remove(session);
		sending.remove(session);
		System.err.println("WebSocket Error: " + throwable.getMessage());
	}

	// เมธอดสำหรับ Broadcast
	public static void broadcast(String message) {
		if (!queue.offer(message)) { //เก็บเข้า queue ก่อน
			System.out.println("Queue full, dropping message");
		}
	}

}