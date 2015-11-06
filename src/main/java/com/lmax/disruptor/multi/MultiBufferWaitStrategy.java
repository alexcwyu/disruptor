package com.lmax.disruptor.multi;

import com.lmax.disruptor.*;

/**
 * Created by alex on 4/12/15.
 */
public abstract class MultiBufferWaitStrategy implements WaitStrategy
{

    boolean hasNext(long[] sequences, SequenceBarrier[] barriers)
    {
        final int barrierLength = barriers.length;
        for (int i = 0; i < barrierLength; i++)
        {

            try
            {
                long sequence = sequences[i];
                //long nextSequence = sequence.get() + 1L;
                //long available = barriers[i].waitFor(nextSequence);


                long available = barriers[i].waitFor(-1);

                if (available >= sequence)
                {
                    return true;
                }
            }
            catch(Exception e)
            {

            }

        }
        return false;
    }

    public abstract void waitNext(long[] sequences, SequenceBarrier[] barriers)throws AlertException, InterruptedException, TimeoutException;

    public long waitFor(long sequence, Sequence cursor, Sequence dependentSequence, SequenceBarrier barrier)
            throws AlertException, InterruptedException, TimeoutException
    {
        waitNext(new long[] {sequence}, new SequenceBarrier[]{barrier});
        return dependentSequence.get();
    }

    public abstract void signalAllWhenBlocking();
}
