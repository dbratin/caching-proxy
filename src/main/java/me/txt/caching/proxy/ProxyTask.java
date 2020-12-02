package me.txt.caching.proxy;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.net.URI;
import java.util.concurrent.TimeUnit;

public class ProxyTask implements Runnable {

    private final Socket sourceSocket;
    private final HealthChecker healthChecker;
    private final URI targetServer;
    private final ProxyCache cache;

    public ProxyTask(Socket socket, HealthChecker healthChecker, URI targetServer, ProxyCache cache) {
        this.sourceSocket = socket;
        this.healthChecker = healthChecker;
        this.targetServer = targetServer;
        this.cache = cache;
    }

    @Override
    public void run() {
        try(var sourceInput = sourceSocket.getInputStream(); var sourceOutput = sourceSocket.getOutputStream()) {
            var request = readSocket(sourceInput);

            if(healthChecker.serviceIsNotAvailable()) {
                dumpAndRespond(sourceOutput, request);
            } else {
                try (var targetSocket = newTargetSocket()) {
                    try (var targetInput = targetSocket.getInputStream(); var targetOutput = targetSocket.getOutputStream()) {
                        writeSocket(targetOutput, request);

                        var response = readSocket(targetInput);

                        writeSocket(sourceOutput, response);

                        System.out.println("Received: " + new String(request));
                        System.out.println("Sent: " + new String(response));
                    }
                } catch (SocketException e) {
                    healthChecker.triggerAvailabilityCheck();
                    dumpAndRespond(sourceOutput, request);
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                sourceSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void dumpAndRespond(OutputStream sourceOutput, byte[] request) throws IOException {
        cache.dump(request);
        writeSocket(sourceOutput, HttpHelper.composeResponse("200 OK", ""));
    }

    private Socket newTargetSocket() throws IOException {
        return new Socket(targetServer.getHost(), HttpHelper.httpPort(targetServer));
    }

    private byte[] readSocket(InputStream input) throws IOException {
        return HttpHelper.readSocket(input, 60, TimeUnit.SECONDS);
    }

    private void writeSocket(OutputStream output, byte[] data) throws IOException {
        output.write(data);
        output.flush();
    }
}
