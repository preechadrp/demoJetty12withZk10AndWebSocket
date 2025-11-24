package com.example;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.concurrent.Executors;

import org.eclipse.jetty.ee10.servlet.DefaultServlet;
import org.eclipse.jetty.ee10.webapp.WebAppContext;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class Main {

	static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(Main.class);

	public Server server = null;
	private int server_port = 8080;
	public static Main main = null;

	/**
	 * นำไปใช้กับ apache procrun ตอน start service ได้ด้วย
	 * @param args
	 */
	public static void main(String[] args) {
		if (args != null && args.length > 0 && args[0].trim().equals("shutdown")) {
			stopServiceByUrl();
		} else {
			main = new Main();
			main.startServer();
		}
	}

	private static void stopServiceByUrl() {
		try {
			java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();

			java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
					.uri(java.net.URI.create("http://127.0.0.1:8080/shutdown?token=secret123"))
					.GET()
					.build();

			// ส่ง Request และรับ Response
			java.net.http.HttpResponse<String> response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());

			// แสดงผลลัพธ์
			if (response.statusCode() != 200) {
				log.info("Status code: {}", response.statusCode());
				log.info(response.body());
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}

	/**
	 * นำไปใช้กับ apache procrun ตอน stop service
	 * @param args
	 */
	public static void stopService(String[] args) {
		main.stopServer();
	}

	public void startServer() {
		try {

			// == ตัวอย่าง
			// ใช้ jetty-ee10-webapp 12.0.23
			// การหา resource แบบปลอดภัย
			// ทำ stop gracefull

			var threadPool = new QueuedThreadPool();
			//กำหนดให้ทำงานแบบ Virtual Threads
			//สามารถกำหนดชื่อ prefix ของ Virtual Threads ได้เพื่อการ Debug
			threadPool.setVirtualThreadsExecutor(Executors.newThreadPerTaskExecutor(Thread.ofVirtual().name("jetty-vt-", 0).factory()));

			server = new Server(threadPool);

			ServerConnector httpConnector = new ServerConnector(server);
			httpConnector.setPort(server_port);
			server.addConnector(httpConnector);

			addContext();

			Runtime.getRuntime().addShutdownHook(new Thread(() -> stopServer()));

			server.setStopTimeout(60000l);// รอ 60 นาทีก่อนจะบังคับปิด
			server.start();

			// ตัวอย่างการ Broadcast จาก Server (เหมือนเดิม)
			new Thread(() -> {
				int count = 0;
				while (server.isRunning()) {
					try {
						Thread.sleep(5000);
						String message = "Server Broadcast #" + (++count) + " @ " + System.currentTimeMillis();
						BroadcastSocket.broadcast(message);
						System.out.println("SERVER ACTION: Broadcasted: " + message);
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
						break;
					}
				}
			}).start();

			server.join();

		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}

	public void stopServer() {
		try {
			// ใช้เวลาหยุดเซิร์ฟเวอร์
			if (server != null && server.isRunning()) {
				log.warn("init stop");
				server.stop();
				log.info("Jetty server stopped gracefully");
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}

	private void addContext() throws URISyntaxException, IOException {

		var context = new WebAppContext();

		// set web resource
		URL rscURL = Main.class.getResource("/webapp");
		log.info("Using BaseResource: " + rscURL.toExternalForm());
		context.setBaseResourceAsString(rscURL.toExternalForm());
		context.setContextPath("/");
		context.setWelcomeFiles(new String[] { "index.zul" });
		context.setParentLoaderPriority(true);
		// context.getSessionHandler().setMaxInactiveInterval(900);//ไม่ผ่านต้องใช้ไฟล์ /WEB-INF/web.xml ถึงจะผ่าน ,test 7/7/68

		// เพิ่ม Default Servlet เพื่อจัดการไฟล์ Static (เช่น HTML, CSS) และเป็นตัว fallback 
		context.addServlet(new DefaultServlet(), "/");

		// add servlet ที่เป็น api ของเรา
		addServlet(context);

		//=== cofig for support zk framework ===//
		//เพิ่ม ServletHolder ของ zk framework แทนการใช้ web.xml
		org.eclipse.jetty.ee10.servlet.ServletHolder zkLoaderHolder = new org.eclipse.jetty.ee10.servlet.ServletHolder(org.zkoss.zk.ui.http.DHtmlLayoutServlet.class);
		zkLoaderHolder.setInitParameter("update-uri", "/zkau");
		zkLoaderHolder.setInitOrder(1);
		context.addServlet(zkLoaderHolder, "*.zul");
		context.addServlet(org.zkoss.zk.au.http.DHtmlUpdateServlet.class, "/zkau/*");
		context.addEventListener(new org.zkoss.zk.ui.http.HttpSessionListener()); //zk Listener
		//=== end config for support zk framework ===//

		// ตั้งค่า WebSocket
		org.eclipse.jetty.ee10.websocket.jakarta.server.config.JakartaWebSocketServletContainerInitializer.configure(context, (servletContext, container) -> {
			// ลงทะเบียน Endpoint
			container.addEndpoint(BroadcastSocket.class);
		});

		server.setHandler(context);

	}

	private void addServlet(WebAppContext context) {

		//สำหรับ shutdown ด้วย winsw/curl ด้วย
		context.addServlet(new jakarta.servlet.http.HttpServlet() {

			private static final long serialVersionUID = -1079681049977214895L;

			@Override
			protected void doGet(HttpServletRequest request, HttpServletResponse response)
					throws ServletException, IOException {

				log.info("Requested Shutdown");

				//จำกัดให้เรียกได้เฉพาะ localhost
				String remoteAddr = request.getRemoteAddr();
				if (!"127.0.0.1".equals(remoteAddr) && !"0:0:0:0:0:0:0:1".equals(remoteAddr)) {
					response.setStatus(HttpServletResponse.SC_FORBIDDEN);
					response.getWriter().println("Access denied");
					log.warn("Rejected shutdown from: {}", remoteAddr);
					return;
				}

				//ตรวจ token
				String tokenParam = request.getParameter("token");
				if (!"secret123".equals(tokenParam)) {
					response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
					response.getWriter().println("Invalid token");
					log.warn("Invalid token");
					return;
				}

				//เริ่มหยุด Jetty
				response.setStatus(HttpServletResponse.SC_OK);
				response.getWriter().println("Shutting down Jetty...");

				log.warn(">>> Shutdown requested via /shutdown from {}", remoteAddr);

				new Thread(() -> {
					try {
						Thread.sleep(500); // รอให้ response ส่งกลับ
						server.stop();
						log.info("Jetty server stopped gracefully.");
					} catch (Exception e) {
						log.error(e.getMessage(), e);
					}
				}).start();

			}

		}, "/shutdown");// test link = http://localhost:8080/shutdown

		context.addServlet(new jakarta.servlet.http.HttpServlet() {

			private static final long serialVersionUID = -1079681049977214895L;

			@Override
			protected void doPost(HttpServletRequest request, HttpServletResponse response)
					throws ServletException, IOException {

				log.info("Request handled by thread: {}", Thread.currentThread().getName());
				log.info("call /api/blocking");
				log.info("request.getSession().getId() : {}", request.getSession(true).getId());
				log.info("session timeout : {}", request.getSession().getMaxInactiveInterval());// seconds unit

				response.setContentType("application/json");
				response.setStatus(HttpServletResponse.SC_OK);
				response.getWriter().println("{ \"status\": \"ok\"}");

			}

		}, "/api/blocking");// test link = http://localhost:8080/api/blocking

	}

}