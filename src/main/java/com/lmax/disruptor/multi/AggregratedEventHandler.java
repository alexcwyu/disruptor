package com.lmax.disruptor.multi;

import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.LifecycleAware;
import com.lmax.disruptor.TimeoutHandler;

/**
 * Created by alex on 12/5/15.
 */
public class AggregratedEventHandler<T> implements EventHandler<T> , TimeoutHandler, LifecycleAware {

    private final EventHandler[] eventHandlers;

    public AggregratedEventHandler(EventHandler... eventHandlers) {
        this.eventHandlers = eventHandlers;
    }

    @Override
    public void onEvent(T event, long sequence, boolean endOfBatch) throws Exception {
        for (EventHandler eventHandler : eventHandlers)
        {
            eventHandler.onEvent(event, sequence, endOfBatch);

        }
    }

    @Override
    public void onShutdown() {
        for (EventHandler eventHandler : eventHandlers)
        {
            if (eventHandler instanceof LifecycleAware)
            {
                ((LifecycleAware) eventHandler).onShutdown();
            }
        }
    }

    @Override
    public void onStart() {
        for (EventHandler eventHandler : eventHandlers)
        {
            if (eventHandler instanceof LifecycleAware)
            {
                ((LifecycleAware) eventHandler).onStart();
            }
        }
    }

    @Override
    public void onTimeout(long sequence) throws Exception {
        for (EventHandler eventHandler : eventHandlers)
        {
            if (eventHandler instanceof TimeoutHandler)
            {
                ((TimeoutHandler) eventHandler).onTimeout(sequence);
            }
        }
    }
}
