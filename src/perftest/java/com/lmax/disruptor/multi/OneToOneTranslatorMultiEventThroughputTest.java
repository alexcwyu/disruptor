package com.lmax.disruptor.multi;

import com.lmax.disruptor.dsl.MultiEventDisruptor;
import com.lmax.disruptor.dsl.ProducerType;
import com.lmax.disruptor.support.ValueEvent;
import com.lmax.disruptor.translator.OneToOneTranslatorThroughputTest;

public class OneToOneTranslatorMultiEventThroughputTest extends OneToOneTranslatorThroughputTest {
    protected void init(){
        MultiEventDisruptor<ValueEvent> disruptor =
                new MultiEventDisruptor<ValueEvent>(
                        ValueEvent.EVENT_FACTORY,
                        BUFFER_SIZE, executor,
                        ProducerType.SINGLE);
        disruptor.handleEventsWith(handler);
        this.ringBuffer = disruptor.start();
    }
}
