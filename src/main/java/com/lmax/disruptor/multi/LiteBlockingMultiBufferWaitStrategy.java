package com.lmax.disruptor.multi;

import com.lmax.disruptor.AlertException;
import com.lmax.disruptor.SequenceBarrier;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by alex on 4/12/15.
 */
public class LiteBlockingMultiBufferWaitStrategy extends MultiBufferWaitStrategy
{


    private final Lock lock = new ReentrantLock();
    private final Condition processorNotifyCondition = lock.newCondition();
    private final AtomicBoolean signalNeeded = new AtomicBoolean(false);

    @Override
    public void waitNext(long[] sequences, SequenceBarrier[] barriers)throws AlertException, InterruptedException
    {
        if (!hasNext(sequences,  barriers))
        {
            lock.lock();
            try
            {

                do
                {
                    signalNeeded.getAndSet(true);

                    if (hasNext(sequences,  barriers))
                    {
                        break;
                    }
                    final int barrierLength = barriers.length;
                    for (int i = 0; i < barrierLength; i++)
                        barriers[i].checkAlert();

                    processorNotifyCondition.await();
                }
                while (!hasNext(sequences,  barriers));

            }
            finally
            {
                lock.unlock();
            }
        }
    }


    @Override
    public void signalAllWhenBlocking()
    {
        if (signalNeeded.getAndSet(false))
        {
            lock.lock();
            try
            {
                processorNotifyCondition.signalAll();
            }
            finally
            {
                lock.unlock();
            }
        }
    }
}
