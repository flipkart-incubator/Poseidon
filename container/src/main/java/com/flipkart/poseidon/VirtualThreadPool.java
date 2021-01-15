package com.flipkart.poseidon;

import org.eclipse.jetty.util.thread.ThreadPool;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class VirtualThreadPool implements ThreadPool {

    private final ExecutorService executor;

    public VirtualThreadPool() {
        executor = Executors.newThreadExecutor(
                Thread.builder().virtual().name("jetty-vt#", 0).factory());
        // too early for logging libs
        System.out.println("VirtualThreadExecutor is active.");
    }

    @Override
    public void execute(Runnable command) {
        executor.execute(command);
    }

    @Override
    public void join() throws InterruptedException {
        executor.shutdown();
        executor.awaitTermination(3, TimeUnit.SECONDS);
    }

    // those are hopefully only used for stats/dashboards etc
    @Override
    public int getThreads() { return -1; }

    @Override
    public int getIdleThreads() { return -1; }

    @Override
    public boolean isLowOnThreads() { return false; }

}