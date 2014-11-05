package server;

import core.OptimizingSessionManager;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.util.thread.ThreadPool;
import org.eclipse.jetty.webapp.WebAppContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * Embedded Jetty Launcher
 * <p>
 * Launches the servlet container with the configuration
 * from the web.xml.
 * NOTE: The SessionManager is currently set from here.
 *
 * @author Sebastian Lindholm
 */
public class UtilityServer {

    private static final Logger logger = LoggerFactory.getLogger(UtilityServer.class);
    private static final String resourceBase = "WebContent";
    private static final String descriptor = resourceBase + File.pathSeparator + "WEB-INF" + File.pathSeparator + "web.xml";
    private static final String contextPath = "/";
    private static final int port = 8888;

    public static void main(String[] args) throws Exception {
        ThreadPool pool = new QueuedThreadPool(200, 20, 30);
        Server server = new Server(pool);
        ServerConnector connector = new ServerConnector(server);
        connector.setPort(port);
        Connector[] connectors = {connector};
        server.setConnectors(connectors);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                server.stop();
                server.join();
            } catch (Exception e) {
                logger.warn("Error while shutting down server.", e);
            }
        }, "shutdown"));

        WebAppContext webApp = new WebAppContext();
        webApp.setClassLoader(Thread.currentThread().getContextClassLoader());
        webApp.setContextPath(contextPath);
        webApp.setResourceBase(resourceBase);
        webApp.setDescriptor(descriptor);
        webApp.setInitParameter("org.eclipse.jetty.servlet.Default.dirAllowed", "false");
        webApp.getSessionHandler().setSessionManager(new OptimizingSessionManager());

        server.setHandler(webApp);
        server.start();
        server.join();

    }

}
