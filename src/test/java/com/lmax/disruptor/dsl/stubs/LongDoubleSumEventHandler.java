package com.lmax.disruptor.dsl.stubs;

import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.support.DoubleEvent;
import com.lmax.disruptor.support.LongEvent;

import java.util.concurrent.CountDownLatch;
import java.util.logging.Logger;

public class LongDoubleSumEventHandler implements EventHandler<Object>
{

    private static final Logger LOGGER = Logger.getLogger(LongDoubleSumEventHandler.class.getName());
    private final int expectedDoubleCount;
    private final double expectedDoubleValue;
    private final int expectedLongCount;
    private final long expectedLongValue;
    private final CountDownLatch countDownLatch;

    private int doubleCount = 0;
    private double doubleValue = 0.0;
    private int longCount = 0;
    private long longValue = 0;

    public LongDoubleSumEventHandler(int expectedDoubleCount, double expectedDoubleValue, int expectedLongCount, long expectedLongValue, CountDownLatch countDownLatch)
    {
        this.expectedDoubleCount = expectedDoubleCount;
        this.expectedDoubleValue = expectedDoubleValue;
        this.expectedLongCount = expectedLongCount;
        this.expectedLongValue = expectedLongValue;
        this.countDownLatch = countDownLatch;
    }


    @Override
    public void onEvent(Object event, long sequence, boolean endOfBatch) throws Exception
    {
        if (event instanceof LongEvent) {
            LongEvent longEvent = (LongEvent) event;
            longCount++;
            longValue = longEvent.get();
            //LOGGER.info("aggregating long, value "+longValue+", count "+longCount);
            longEvent.set(longValue+(long)doubleValue);
        } else if (event instanceof DoubleEvent) {
            DoubleEvent doubleEvent = (DoubleEvent) event;
            doubleCount++;
            doubleValue = doubleEvent.get();
            //LOGGER.info("aggregating double, value "+doubleValue+", count "+doubleCount);
            doubleEvent.set(longValue+doubleValue);
        } else {
            throw new RuntimeException("Unsupported event: " + event);
        }

        if (doubleCount >= expectedDoubleCount
                && longCount >= expectedLongCount)
        {
            if (longValue != expectedLongValue)
            {
                throw new RuntimeException("last long value (" + longValue +") is not equals to the expected value ("+expectedLongValue+")");
            }
            if (doubleValue != expectedDoubleValue)
            {
                throw new RuntimeException("last double value (" + doubleValue +") is not equals to the expected value ("+expectedDoubleValue+")");
            }
            countDownLatch.countDown();
        }
    }

    public int doubleCount()
    {
        return doubleCount;
    }

    public int longCount()
    {
        return longCount;
    }

    public double doubleValue()
    {
        return doubleValue;
    }

    public long longValue()
    {
        return longValue;
    }
}