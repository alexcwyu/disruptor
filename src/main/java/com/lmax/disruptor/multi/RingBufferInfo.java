package com.lmax.disruptor.multi;

import com.lmax.disruptor.*;

public class RingBufferInfo<T>
{
    final DataProvider<T> provider;
    final SequenceBarrier barriers;
    final Sequence sequence;
    final EventHandler<? super T> eventHandler;

    public RingBufferInfo(DataProvider<T> provider, SequenceBarrier barriers, Sequence sequence, EventHandler<? super T> eventHandler)
    {
        this.provider = provider;
        this.barriers = barriers;
        this.sequence = sequence;
        this.eventHandler = eventHandler;
    }


}
