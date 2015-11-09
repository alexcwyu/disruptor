package com.lmax.disruptor.dsl.stubs;

import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.WorkHandler;
import com.lmax.disruptor.support.LongEvent;

import java.util.concurrent.CountDownLatch;
import java.util.logging.Logger;

public class LongEventHandler implements EventHandler<LongEvent>, WorkHandler<LongEvent>
{

    private static final Logger LOGGER = Logger.getLogger(LongEventHandler.class.getName());
    private final int expectedCount;
    private final CountDownLatch countDownLatch;

    private int count = 0;
    private long value;

    public LongEventHandler(int expectedCount, CountDownLatch countDownLatch)
    {
        this.expectedCount = expectedCount;
        this.countDownLatch = countDownLatch;
    }

    @Override
    public void onEvent(LongEvent event, long sequence, boolean endOfBatch) throws Exception
    {
        onEvent(event);
    }

    @Override
    public void onEvent(LongEvent event) throws Exception
    {

        count++;
        value = event.get();

        //LOGGER.info("counting long, value "+event.get()+", count "+count);
        if (count >= expectedCount)
        {
            countDownLatch.countDown();
        }

    }

    public long getCount()
    {
        return count;
    }

    public long getValue()
    {
        return value;
    }
}