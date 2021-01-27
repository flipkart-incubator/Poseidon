package com.flipkart.poseidon;

import org.eclipse.jetty.util.component.AbstractLifeCycle;
import org.eclipse.jetty.util.thread.ThreadPool;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;


public class VirtualThreadPool extends AbstractLifeCycle implements ThreadPool {
    private final ExecutorService executorService;

    public VirtualThreadPool()
    {
        ThreadFactory factory = Thread.builder().virtual().name("LoomThreadPool-").factory();
        executorService = Executors.newThreadExecutor(factory);
    }

    @Override
    public void execute(Runnable command)
    {
        executorService.execute(command);
    }

    @Override
    public void join()
    {
        while (!executorService.isTerminated())
        {
            Thread.onSpinWait();
        }
    }

    @Override
    protected void doStop() throws Exception
    {
        super.doStop();
        executorService.shutdown();
    }

    @Override
    public int getThreads()
    {
        // TODO: always report a value?
        return Integer.MAX_VALUE;
    }

    @Override
    public int getIdleThreads()
    {
        // TODO: always report available?
        return Integer.MAX_VALUE;
    }

    @Override
    public boolean isLowOnThreads()
    {
        return false;
    }
}