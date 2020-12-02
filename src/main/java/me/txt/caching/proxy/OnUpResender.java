package me.txt.caching.proxy;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.net.URI;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class OnUpResender {

    private boolean started = false;

    private final ProxyCache cache;
    private final HealthChecker healthChecker;
    private final ExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    public OnUpResender(ProxyCache cache, HealthChecker healthChecker) {
        this.cache = cache;
        this.healthChecker = healthChecker;
    }

    public synchronized OnUpResender start() {
        started = true;
        executor.execute(() -> {
            try {
                while (started) {
                    System.out.println("OnUpResent job started");

                    cache.waitNotEmpty();
                    healthChecker.waitUntilServiceIsAvailable();

                    while (cache.notEmpty()) {
                        byte[] request = cache.getFirst();

                        URI targetServer = healthChecker.getTargetServer();
                        try (Socket socket = new Socket(targetServer.getHost(), HttpHelper.httpPort(targetServer))) {
                            try (var input = socket.getInputStream(); var output = socket.getOutputStream()) {
                                output.write(request);
                                output.flush();

                                byte[] response = HttpHelper.readSocket(input, 60, TimeUnit.SECONDS);

                                System.out.println("Response to resent: " + new String(response));
                            }
                        } catch (SocketException e) {
                            healthChecker.triggerAvailabilityCheck();
                            break;
                        } catch (IOException e) {
                            e.printStackTrace();
                            break;
                        }
                        cache.removeFirst();
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        return this;
    }

    public synchronized OnUpResender stop() {
        started = false;
        return this;
    }
}
