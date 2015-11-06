package com.lmax.disruptor.multi;

import com.lmax.disruptor.AlertException;
import com.lmax.disruptor.SequenceBarrier;

/**
 * Created by alex on 4/12/15.
 */
public class BusySpinMultiBufferWaitStrategy extends MultiBufferWaitStrategy
{

    @Override
    public void waitNext(long[] sequences, SequenceBarrier[] barriers)throws AlertException, InterruptedException
    {
        while (!hasNext(sequences,  barriers))
        {
            final int barrierLength = barriers.length;
            for (int i = 0; i < barrierLength; i++)
                barriers[i].checkAlert();
        }
    }

    @Override
    public void signalAllWhenBlocking()
    {
    }
}
