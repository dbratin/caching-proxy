package me.txt.caching.proxy;

import java.util.LinkedList;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class ProxyCache {

    private final ReentrantLock rwlock = new ReentrantLock();
    private final Condition isNotEmpty = rwlock.newCondition();
    private final LinkedList<byte[]> queue = new LinkedList<>();

    public void dump(byte[] request) {
        rwlock.lock();
        queue.add(request);
        System.out.println("Cached: " + new String(request));
        isNotEmpty.signalAll();
        rwlock.unlock();
    }

    public void waitNotEmpty() throws InterruptedException {
        rwlock.lock();
        if(queue.isEmpty()) {
            isNotEmpty.await();
        }
        rwlock.unlock();
    }

    public boolean notEmpty() {
        return ! queue.isEmpty();
    }

    public byte[] getFirst() {
        return queue.getFirst();
    }

    public void removeFirst() {
        rwlock.lock();
        byte[] request = queue.removeFirst();
        System.out.println("Removed: " + new String(request));
        rwlock.unlock();
    }
}
