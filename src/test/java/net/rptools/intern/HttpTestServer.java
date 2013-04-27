package net.rptools.intern;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

import javax.imageio.ImageIO;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;

public class HttpTestServer {
    private static Server server;
    private final static String SEP = System.getProperty("file.separator");
    private final static String TEST_DIR = ".maptool" + SEP + "resources" + SEP;
    private final static String TEST_IMAGE = "Test.png";
    private final static String userDir = System.getProperty("user.dir") + SEP;

    public static void start() throws Exception
    {
        server = new Server(8080);
        server.setHandler(new TestHandler());
        server.start();
    }

    public static void stop() throws Exception {
        if (server != null)
            server.stop();
    }

    private static class TestHandler extends AbstractHandler {
        public void handle(String target, Request baseRequest,HttpServletRequest request,HttpServletResponse response) throws IOException, ServletException {
            if (request.getRequestURL().toString().equals("http://localhost:8080/index")) {
                response.setContentType("text/text;charset=utf-8");
                response.setStatus(HttpServletResponse.SC_OK);
                baseRequest.setHandled(true);
                response.getWriter().println("1234=Test.png");
            }
            else if (request.getRequestURL().toString().equals("http://localhost:8080/Test.png")) {
                response.setContentType("image/png");
                response.setStatus(HttpServletResponse.SC_OK);
                response.flushBuffer(); // Do this so that the header can be read already
                baseRequest.setHandled(true);
                BufferedImage img = ImageIO.read(new File(userDir + TEST_DIR + TEST_IMAGE));
                ImageIO.write(img, "png", new SlowOutputStream(response.getOutputStream()));
            }
            else {
                System.err.println(request.getRequestURL());
            }
        }
    }
    private static class SlowOutputStream extends OutputStream {
        private OutputStream os;
        private int sleeper = 0;
        private SlowOutputStream(OutputStream os) {
            this.os = os;
        }
        @Override
        public void write(int b) throws IOException {
            os.write(b);
            if ((sleeper++)%1000 == 0) {
                try {
                    Thread.sleep(250);
                }
                catch (InterruptedException e) {                    
                }
            }
        }
    }
}
