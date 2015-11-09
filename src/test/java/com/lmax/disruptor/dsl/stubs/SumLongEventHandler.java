package com.lmax.disruptor.dsl.stubs;

import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.support.LongEvent;

import java.util.logging.Logger;

public class SumLongEventHandler implements EventHandler<LongEvent>
{

    private static final Logger LOGGER = Logger.getLogger(SumLongEventHandler.class.getName());

    private long count;
    private long total;

    public SumLongEventHandler()
    {
    }

    @Override
    public void onEvent(LongEvent event, long sequence, boolean endOfBatch) throws Exception
    {
        count ++;
        total += event.get();
        //LOGGER.info("summing long, value "+event.get()+", count "+count);
        event.set(total);


    }

    public long getCount()
    {
        return count;
    }

    public long getTotal()
    {
        return total;
    }
}