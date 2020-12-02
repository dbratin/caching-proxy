package me.txt.caching.proxy;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;

public class ProxyServer {
    public static void main(String[] args) {
        try {
            var config = parseArgs(args);
            var executor = Executors.newCachedThreadPool();

            var serverSocket = new ServerSocket(config.port);

            var healthChecker = new HealthChecker(config.targetServer);
            var cache = new ProxyCache();

            new OnUpResender(cache, healthChecker).start();

            while (true) {
                Socket socket = serverSocket.accept();
                try {
                    executor.execute(new ProxyTask(socket, healthChecker, config.targetServer, cache));
                } catch (RejectedExecutionException e) {
                    try (var output = socket.getOutputStream()) {
                        HttpHelper.response500(output, e.getMessage());
                    } finally {
                        socket.close();
                    }
                }
            }
        } catch (IOException e) {
            error(e.getMessage());
        }
    }

    private static void error(String message) {
        System.err.println(message);
        System.exit(1);
    }

    private static void usage() {
        System.out.print(
                "Usage:\n" +
                "  caching-proxy port-number target-server-url\n"
        );
        System.exit(1);
    }

    private static Config parseArgs(String[] args) {
        if(args == null || args.length < 2) {
            usage();
        }

        int portNumber = Integer.parseInt(args[0]);
        URI targetServer = URI.create(args[1]);

        return new Config(portNumber, targetServer);
    }

    private static class Config {
        final int port;
        final URI targetServer;

        private Config(int port, URI targetServer) {
            this.port = port;
            this.targetServer = targetServer;
        }
    }
}
