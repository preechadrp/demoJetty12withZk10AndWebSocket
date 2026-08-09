package com.example;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Executors;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.jaxws.EndpointImpl;
import org.apache.cxf.transport.servlet.CXFNonSpringServlet;
import org.eclipse.jetty.ee10.servlet.DefaultServlet;
import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.ee10.websocket.jakarta.server.config.JakartaWebSocketServletContainerInitializer; // Import ที่ถูกต้อง
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.util.component.LifeCycle;
import org.eclipse.jetty.util.thread.VirtualThreadPool;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class Main {

    static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(Main.class);

    public Server server = null;
    private static int server_port = 8080;
    public static Main main = null;

    public static void main(String[] args) {
		main = new Main();
		main.startServer();
    }

    public void startServer() {
        try {
            var threadPool = new VirtualThreadPool();
            threadPool.setVirtualThreadsExecutor(Executors.newThreadPerTaskExecutor(Thread.ofVirtual().name("jetty-vt-", 0).factory()));

            server = new Server(threadPool);

            ServerConnector httpConnector = new ServerConnector(server);
            httpConnector.setPort(server_port);
            server.addConnector(httpConnector);

            addContext();

            server.setStopTimeout(60000l);
			server.setStopAtShutdown(true);
			server.addEventListener(new LifeCycle.Listener() {
				@Override
				public void lifeCycleStopping(LifeCycle event) {
					log.info("Jetty is stopping gracefully");
				}

				@Override
				public void lifeCycleStopped(LifeCycle event) {
					log.info("Jetty fully stopped");
				}
			});
            server.start();

            // ตัวอย่างการ Broadcast จาก Server
            new Thread(() -> {
                int count = 0;
                while (server.isRunning()) {
                    try {
                        Thread.sleep(1000);
                        String message = "Server Broadcast #" + (++count) + " @ " + System.currentTimeMillis();
                        // เรียกใช้ BroadcastSocket.broadcast
                        BroadcastSocket.broadcast(message); 
                        log.info("SERVER ACTION: Broadcasted: " + message);
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

	private void addContext() throws URISyntaxException, IOException {

		ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);

		// set web resource
		URL rscURL = Main.class.getResource("/webapp");
		log.info("Using BaseResource: " + rscURL.toExternalForm());
		context.setBaseResourceAsString(rscURL.toExternalForm());
		context.setContextPath("/");
		context.setWelcomeFiles(new String[] { "index.zul" });
		if (context.getSessionHandler() != null) {
			context.getSessionHandler().setMaxInactiveInterval(900);//กรณีใช้ ServletContextHandler จะผ่าน ,test 30/7/68
		}

		context.addServlet(new DefaultServlet(), "/");
		addZkConfig(context);
		addWebSocket(context);
		addApacheCXF(context);
		addApi(context);

		server.setHandler(context);
	}
    
	private void addZkConfig(ServletContextHandler context) throws IOException {
		//===config for support zk framework
		
		//สำคัญที่สุด (เพื่อให้สามารถใช้ org.zkoss.zul.Fileupload ใน jetty ได้)
		Path uploadDir = Paths.get("./temp/zk-upload");
        Files.createDirectories(uploadDir);
		context.setAttribute("jakarta.servlet.context.tempdir", uploadDir.toFile());
        context.setAttribute("org.zkoss.zk.ui.upload.tempdir", uploadDir.toAbsolutePath().toString());

	    org.eclipse.jetty.ee10.servlet.ServletHolder layoutHolder = new org.eclipse.jetty.ee10.servlet.ServletHolder(org.zkoss.zk.ui.http.DHtmlLayoutServlet.class);
	    layoutHolder.setInitParameter("update-uri", "/zkau");
	    layoutHolder.setInitOrder(1);
	    context.addServlet(layoutHolder, "*.zul");
	    
	    // AU servlet (รับ upload)
	    org.eclipse.jetty.ee10.servlet.ServletHolder auHolder = new org.eclipse.jetty.ee10.servlet.ServletHolder(new org.zkoss.zk.au.http.DHtmlUpdateServlet());
		long maxFileSize = 50 * 1024 * 1024; // 50 MB
		long maxRequestSize = 100L * 1024 * 1024; // 100 MB
        auHolder.getRegistration().setMultipartConfig(
				new jakarta.servlet.MultipartConfigElement(uploadDir.toAbsolutePath().toString(), maxFileSize, maxRequestSize, 0)
        );
        context.addServlet(auHolder, "/zkau/*");
	    context.addEventListener(new org.zkoss.zk.ui.http.HttpSessionListener()); //zk Listener
	}

	private void addWebSocket(ServletContextHandler context) {
		// 3. ตั้งค่า WebSocket (ต้องใช้ JakartaWebSocketServletContainerInitializer)
	    JakartaWebSocketServletContainerInitializer.configure(context, (servletContext, container) -> {
	        // ลงทะเบียน Endpoint
	        container.addEndpoint(BroadcastSocket.class); 
	    });
	}

	@SuppressWarnings("resource")
	private void addApacheCXF(ServletContextHandler context) {
		// 4. ตั้งค่า CXF (SOAP)
		// **ส่วนที่แก้ไขเพื่อให้ทำงานได้แน่นอน**
		Bus bus = BusFactory.getDefaultBus(); // สร้าง Bus หลัก
		context.setAttribute(BusFactory.class.getName(), bus); // ผูก Bus เข้ากับ Context

		CXFNonSpringServlet cxfServlet = new CXFNonSpringServlet();
		cxfServlet.setBus(bus); // ผูก Bus เข้ากับ Servlet โดยตรง **สำคัญ**

		org.eclipse.jetty.ee10.servlet.ServletHolder servletHolder = new org.eclipse.jetty.ee10.servlet.ServletHolder(cxfServlet);
		context.addServlet(servletHolder, "/soapapi/*"); // CXF จัดการที่ /soapapi/*

		// 5. ลงทะเบียน Service
		EndpointImpl endpoint = new EndpointImpl(bus, new SimpleServiceImpl());
		endpoint.publish("/simple1");

		log.info("Server Started at http://localhost:{}", server_port);
		log.info("WSDL 1 (Simple 1): http://localhost:{}/soapapi/simple1?wsdl", server_port);

	}
	
    private void addApi(ServletContextHandler context) {

        context.addServlet(new jakarta.servlet.http.HttpServlet() {

            @Override
            protected void doGet(HttpServletRequest request, HttpServletResponse response)
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