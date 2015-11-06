package com.lmax.disruptor.multi;

import com.lmax.disruptor.AlertException;
import com.lmax.disruptor.SequenceBarrier;

/**
 * Created by alex on 4/12/15.
 */
public class YieldMultiBufferWaitStrategy extends MultiBufferWaitStrategy
{

    private static final int SPIN_TRIES = 100;

    @Override
    public void waitNext(long[] sequences, SequenceBarrier[] barriers)throws AlertException, InterruptedException
    {
        int counter = SPIN_TRIES;
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

        if (0 == counter)
        {
            Thread.yield();
        }
        else
        {
            --counter;
        }

        return counter;
    }

    @Override
    public void signalAllWhenBlocking()
    {
    }
}
