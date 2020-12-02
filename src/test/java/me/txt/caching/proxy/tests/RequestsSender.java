package me.txt.caching.proxy.tests;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class RequestsSender {
    public static void main(String[] args) {
        int sendersNumber = 10;
        var executor = Executors.newScheduledThreadPool(sendersNumber);
        var sendingTask = (Runnable) () -> {
            System.out.println(Thread.currentThread().getName());
            try {
                HttpClient client = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder().uri(new URI("http://localhost:8080/?" + System.nanoTime())).build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                System.out.println(response.headers());
                System.out.println(response.body());
            } catch (Exception e) {
                e.printStackTrace();
            }
        };

        //executor.scheduleAtFixedRate(sendingTask, 100, 10000, TimeUnit.MILLISECONDS);

        for(int i = 0; i < sendersNumber; i++)
            executor.scheduleAtFixedRate(sendingTask, new Random().nextInt(100), new Random().nextInt(10000), TimeUnit.MILLISECONDS);
    }
}
