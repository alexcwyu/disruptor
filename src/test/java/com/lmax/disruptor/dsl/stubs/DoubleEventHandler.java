package com.lmax.disruptor.dsl.stubs;

import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.WorkHandler;
import com.lmax.disruptor.support.DoubleEvent;

import java.util.concurrent.CountDownLatch;
import java.util.logging.Logger;

public class DoubleEventHandler implements EventHandler<DoubleEvent>, WorkHandler<DoubleEvent>
{

    private static final Logger LOGGER = Logger.getLogger(DoubleEventHandler.class.getName());
    private final int expectedCount;
    private final CountDownLatch countDownLatch;

    private int count = 0;
    private double value;

    public DoubleEventHandler(int expectedCount, CountDownLatch countDownLatch)
    {
        this.expectedCount = expectedCount;
        this.countDownLatch = countDownLatch;
    }

    @Override
    public void onEvent(DoubleEvent event, long sequence, boolean endOfBatch) throws Exception
    {
        onEvent(event);
    }

    @Override
    public void onEvent(DoubleEvent event) throws Exception
    {
        count++;
        value = event.get();

        if (count % 1000000 == 0)
        LOGGER.info("counting double, value "+event.get()+", count "+count);
        if (count >= expectedCount)
        {
            countDownLatch.countDown();
        }

    }

    public long getCount()
    {
        return count;
    }

    public double getValue()
    {
        return value;
    }
}