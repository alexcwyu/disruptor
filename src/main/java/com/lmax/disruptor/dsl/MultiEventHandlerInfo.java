package com.lmax.disruptor.dsl;

import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.multi.MultiEventProcessor;

public class MultiEventHandlerInfo<T>
{

    public final MultiEventProcessor processor;
    public final EventHandler<T> handler;

    public MultiEventHandlerInfo(MultiEventProcessor processor, EventHandler<T> handler)
    {
        this.processor = processor;
        this.handler = handler;
    }
}
