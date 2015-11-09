package com.lmax.disruptor.multi;

import com.lmax.disruptor.EventProcessor;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.sequenced.OneToOneSequencedLongArrayThroughputTest;

import static com.lmax.disruptor.RingBuffer.createSingleProducer;

/**
 * Created by alex on 11/10/15.
 */
public class OneToOneSequencedLongArrayMultiEventThroughputTest extends OneToOneSequencedLongArrayThroughputTest {

    protected RingBuffer<long[]> createRingBuffer(){
        return createSingleProducer(FACTORY, BUFFER_SIZE, new NoWaitStrategy());
    }

    protected EventProcessor createEventProcessor(RingBuffer<long[]> ringBuffer){
        MultiEventProcessor batchEventProcessor =
                new MultiEventProcessor(new YieldMultiBufferWaitStrategy());
        ringBuffer.addGatingSequences(batchEventProcessor.add(ringBuffer, handler));
        return batchEventProcessor;
    }


    public static void main(String[] args) throws Exception
    {
        OneToOneSequencedLongArrayMultiEventThroughputTest test = new OneToOneSequencedLongArrayMultiEventThroughputTest();
        test.testImplementations();
    }
}
