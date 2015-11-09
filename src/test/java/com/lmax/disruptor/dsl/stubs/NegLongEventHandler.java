package com.lmax.disruptor.dsl.stubs;

import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.WorkHandler;
import com.lmax.disruptor.support.LongEvent;

import java.util.concurrent.CountDownLatch;
import java.util.logging.Logger;

public class NegLongEventHandler implements EventHandler<LongEvent>, WorkHandler<LongEvent>
{

    private static final Logger LOGGER = Logger.getLogger(NegLongEventHandler.class.getName());
    private long count = 0;
    private long value;

    public NegLongEventHandler()
    {
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
        //LOGGER.info("negating long, value "+value+", count "+count);
        event.set(-value);
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