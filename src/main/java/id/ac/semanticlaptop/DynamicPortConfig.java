package id.ac.semanticlaptop;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.web.context.WebServerApplicationContext;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.servlet.server.ConfigurableServletWebServerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;

import java.awt.Desktop;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.URI;

@Configuration
public class DynamicPortConfig implements WebServerFactoryCustomizer<ConfigurableServletWebServerFactory> {
    private static final Logger log = LoggerFactory.getLogger(DynamicPortConfig.class);

    @Value("${app.server.preferred-port:8080}")
    private int preferredPort;

    @Value("${app.server.max-port:8180}")
    private int maxPort;

    @Value("${app.browser.auto-open:true}")
    private boolean autoOpenBrowser;

    private int selectedPort = -1;

    @Override
    public void customize(ConfigurableServletWebServerFactory factory) {
        this.selectedPort = findAvailablePort(preferredPort, maxPort);
        if (this.selectedPort != preferredPort && this.selectedPort != 0) {
            log.info("Port utama {} sedang digunakan. Beralih otomatis ke port kosong berikutnya: {}",
                    preferredPort, this.selectedPort);
        }
        factory.setPort(this.selectedPort);
    }

    /**
     * Mencari port kosong mulai dari startPort hingga endPort.
     * Jika seluruh port pada rentang tersebut penuh, fallback ke 0 (OS ephemeral port).
     */
    public static int findAvailablePort(int startPort, int endPort) {
        if (startPort <= 0) {
            return 0;
        }
        for (int port = startPort; port <= endPort; port++) {
            if (isPortAvailable(port)) {
                return port;
            }
        }
        return 0;
    }

    public static boolean isPortAvailable(int port) {
        try (ServerSocket wildcardSocket = new ServerSocket()) {
            wildcardSocket.setReuseAddress(false);
            wildcardSocket.bind(new InetSocketAddress((InetAddress) null, port), 1);
        } catch (Exception e) {
            return false;
        }

        try (ServerSocket loopbackSocket = new ServerSocket()) {
            loopbackSocket.setReuseAddress(false);
            loopbackSocket.bind(new InetSocketAddress(InetAddress.getLoopbackAddress(), port), 1);
        } catch (Exception e) {
            return false;
        }

        return true;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady(ApplicationReadyEvent event) {
        if (!(event.getApplicationContext() instanceof WebServerApplicationContext webContext)) {
            return;
        }

        int activePort = webContext.getWebServer().getPort();
        String baseUrl = "http://localhost:" + activePort;
        String portStatus = (activePort == preferredPort)
                ? activePort + " (Port Utama)"
                : activePort + " (Fallback Dinamis dari " + preferredPort + ")";

        String banner = """
                
                ======================================================================
                  SEMANTIC LAPTOP EXPLORER - WEB APPLICATION READY
                ----------------------------------------------------------------------
                  Status Server  : ONLINE (Embedded Apache Tomcat)
                  Port Terpilih  : %s
                  URL Web UI     : %s
                  Endpoint API   : %s/api/laptops
                                   %s/api/brands
                ======================================================================
                """.formatted(portStatus, baseUrl, baseUrl, baseUrl);

        System.out.println(banner);

        if (autoOpenBrowser) {
            openBrowserSilently(baseUrl);
        }
    }

    private void openBrowserSilently(String url) {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(URI.create(url));
            }
        } catch (Exception ignored) {
            // Abaikan jika dijalankan di lingkungan headless/tanpa GUI
        }
    }
}
