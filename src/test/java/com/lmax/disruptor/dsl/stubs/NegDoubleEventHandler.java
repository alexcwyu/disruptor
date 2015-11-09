package com.lmax.disruptor.dsl.stubs;

import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.WorkHandler;
import com.lmax.disruptor.support.DoubleEvent;
import com.lmax.disruptor.support.LongEvent;

import java.util.logging.Logger;

public class NegDoubleEventHandler implements EventHandler<DoubleEvent>, WorkHandler<DoubleEvent>
{

    private static final Logger LOGGER = Logger.getLogger(NegDoubleEventHandler.class.getName());
    private long count = 0;
    private double value;

    public NegDoubleEventHandler()
    {
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

        //LOGGER.info("negating double, value "+value+", count "+count);

        event.set(-value);

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