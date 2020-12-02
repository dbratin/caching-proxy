package me.txt.caching.proxy.tests;

import me.txt.caching.proxy.HttpHelper;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class EchoBackend {
    public static void main(String[] args) {
        try {
            var executor = Executors.newCachedThreadPool();

            var serverSocket = new ServerSocket(8081);

            while (true) {
                Socket socket = serverSocket.accept();
                executor.execute(() -> {
                    try(var input = socket.getInputStream(); var output = socket.getOutputStream()) {
                        var message = new String(HttpHelper.readSocket(input, 60, TimeUnit.SECONDS));
                        output.write((header(message.length()) + message).getBytes());
                        output.flush();

                        System.out.println("Echoed: " + message);
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        try {
                            socket.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static String header(int length) {
        return "HTTP/1.1 200 OK\r\n" +
                "Server: EchoBackend\r\n" +
                "Content-Type: application/text\r\n" +
                "Content-Length: " + length + "\r\n" +
                "Connection: close\r\n\r\n";
    }
}
