package me.txt.caching.proxy;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.concurrent.TimeUnit;

public class HttpHelper {
    public static void response500(OutputStream output, String message) throws IOException {
        writeResponse("500 Internal Server Error", message, output);
    }

    public static void writeResponse(String statusString, String body, OutputStream output) throws IOException {
        output.write(composeResponse(statusString, body));
        output.flush();
    }

    public static byte[] readSocket(InputStream input, long timeout, TimeUnit units) throws IOException {
        long timeoutNano = TimeUnit.NANOSECONDS.convert(timeout, units);
        long start = System.nanoTime();
        long t = 0;

        while(input.available() == 0 && t < timeoutNano) t = System.nanoTime() - start;
        return input.readNBytes(input.available());
    }

    public static byte[] composeResponse(String statusString, String body) {
        var response = "HTTP/1.1 " + statusString + "\r\n" +
                "Server: CachingProxy\r\n" +
                "Content-Type: text/html\r\n" +
                "Content-Length: " + body.length() + "\r\n" +
                "Connection: close\r\n\r\n" +
                body;
        return response.getBytes();
    }

    public static int httpPort(URI targetServer) {
        int port = targetServer.getPort();
        if(port < 0) port = 80;

        return port;
    }
}
