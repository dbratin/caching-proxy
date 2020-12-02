package me.txt.caching.proxy;

import java.io.IOException;
import java.net.Socket;
import java.net.URI;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

public class HealthChecker {
    private final URI targetServer;
    private final ReentrantLock triggerLock = new ReentrantLock();
    private final ReentrantLock isAvailableFlagLock = new ReentrantLock();
    private final Condition serviceIsAvailableCondition = isAvailableFlagLock.newCondition();
    private final Executor delayedExecutor = CompletableFuture.delayedExecutor(5, TimeUnit.SECONDS);
    private final Executor immediateExecutor = CompletableFuture.delayedExecutor(0, TimeUnit.SECONDS);
    private boolean checkAlreadyTriggered = false;
    private boolean serviceIsAvailable = true;


    public HealthChecker(URI targetServer) {
        this.targetServer = targetServer;
        triggerAvailabilityCheck();
    }

    public void triggerAvailabilityCheck() {
        triggerLock.lock();

        try {
            if (checkAlreadyTriggered) return;

            checkAlreadyTriggered = true;
            serviceIsAvailable = false;
            scheduleAvailabilityCheck(immediateExecutor);
        } finally {
            triggerLock.unlock();
        }
    }

    private void scheduleAvailabilityCheck(Executor executor) {
        try {
            CompletableFuture
                    .supplyAsync(new ConnectionTestTask(), executor)
                    .thenAccept(status -> {
                        System.out.println("Last checked status: isAvailable=" + status);

                        isAvailableFlagLock.lock();
                        try {
                            checkAlreadyTriggered = false;
                            serviceIsAvailable = status;
                            if (!serviceIsAvailable)
                                scheduleAvailabilityCheck(delayedExecutor);
                            else
                                serviceIsAvailableCondition.signalAll();
                        } finally {
                            isAvailableFlagLock.unlock();
                        }
                    });
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public boolean serviceIsNotAvailable() {
        return !serviceIsAvailable;
    }

    public void waitUntilServiceIsAvailable() throws InterruptedException {
        isAvailableFlagLock.lock();
        if(!serviceIsAvailable) {
            serviceIsAvailableCondition.await();
        }
        isAvailableFlagLock.unlock();
    }

    public URI getTargetServer() {
        return targetServer;
    }

    private class ConnectionTestTask implements Supplier<Boolean> {

        @Override
        public Boolean get() {
            try (var socket = new Socket(targetServer.getHost(), HttpHelper.httpPort(targetServer))) {
                return socket.isConnected();
            } catch (IOException e) {
                return false;
            }
        }
    }
}
