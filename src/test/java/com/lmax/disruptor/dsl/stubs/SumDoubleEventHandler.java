package com.lmax.disruptor.dsl.stubs;

import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.support.DoubleEvent;

import java.util.logging.Logger;

public class SumDoubleEventHandler implements EventHandler<DoubleEvent>
{

    private static final Logger LOGGER = Logger.getLogger(SumDoubleEventHandler.class.getName());
    private long count;
    private double total;

    public SumDoubleEventHandler()
    {
    }

    @Override
    public void onEvent(DoubleEvent event, long sequence, boolean endOfBatch) throws Exception
    {
        count ++;
        total += event.get();
        //LOGGER.info("summing double, value "+event.get()+", count "+count);
        event.set(total);
    }

    public long getCount()
    {
        return count;
    }

    public double getTotal()
    {
        return total;
    }
}