package com.lmax.disruptor.multi;

import com.lmax.disruptor.AlertException;
import com.lmax.disruptor.SequenceBarrier;

import java.util.concurrent.locks.LockSupport;

/**
 * Created by alex on 4/12/15.
 */
public class SleepingMultiBufferWaitStrategy extends MultiBufferWaitStrategy
{

    private static final int DEFAULT_RETRIES = 200;

    private final int retries;

    public SleepingMultiBufferWaitStrategy()
    {
        this(DEFAULT_RETRIES);
    }

    public SleepingMultiBufferWaitStrategy(int retries)
    {
        this.retries = retries;
    }

    @Override
    public void waitNext(long[] sequences, SequenceBarrier[] barriers)throws AlertException, InterruptedException
    {
        int counter = retries;
        while (!hasNext(sequences,  barriers))
        {
            counter = applyWaitMethod(barriers, counter);
        }
    }

    private int applyWaitMethod(final SequenceBarrier[] barriers, int counter)
            throws AlertException
    {
        for (SequenceBarrier barrier : barriers)
        {
            barrier.checkAlert();
        }

        if (counter > 100)
        {
            --counter;
        }
        else if (counter > 0)
        {
            --counter;
            Thread.yield();
        }
        else
        {
            LockSupport.parkNanos(1L);
        }

        return counter;
    }

    @Override
    public void signalAllWhenBlocking()
    {
    }
}
