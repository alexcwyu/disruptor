package com.lmax.disruptor.multi;

import com.lmax.disruptor.AlertException;
import com.lmax.disruptor.SequenceBarrier;
import com.lmax.disruptor.TimeoutException;

/**
 * Created by alex on 4/12/15.
 */
public class NoWaitStrategy extends MultiBufferWaitStrategy
{
    @Override
    public void waitNext(long[] sequences, SequenceBarrier[] barriers) throws AlertException, InterruptedException, TimeoutException
    {

    }

    @Override
    public void signalAllWhenBlocking()
    {

    }
}
