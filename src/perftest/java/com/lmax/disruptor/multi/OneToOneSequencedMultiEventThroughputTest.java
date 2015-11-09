package com.lmax.disruptor.multi;

import com.lmax.disruptor.EventProcessor;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.sequenced.OneToOneSequencedThroughputTest;
import com.lmax.disruptor.support.ValueEvent;

import static com.lmax.disruptor.RingBuffer.createSingleProducer;

/**
 * Created by alex on 11/10/15.
 */
public class OneToOneSequencedMultiEventThroughputTest extends OneToOneSequencedThroughputTest {
    protected RingBuffer<ValueEvent> createRingBuffer(){
        return createSingleProducer(ValueEvent.EVENT_FACTORY, BUFFER_SIZE, new NoWaitStrategy());
    }

    protected EventProcessor createEventProcessor(RingBuffer<ValueEvent> ringBuffer){
        MultiEventProcessor batchEventProcessor =
                new MultiEventProcessor(new YieldMultiBufferWaitStrategy());
        ringBuffer.addGatingSequences(batchEventProcessor.add(ringBuffer, handler));
        return batchEventProcessor;
    }


    public static void main(String[] args) throws Exception
    {
        OneToOneSequencedMultiEventThroughputTest test = new OneToOneSequencedMultiEventThroughputTest();
        test.testImplementations();
    }
}
