package com.lmax.disruptor.multi;

import com.lmax.disruptor.*;
import com.lmax.disruptor.offheap.OneToOneOffHeapThroughputTest;
import com.lmax.disruptor.offheap.OneToOneOnHeapThroughputTest;

import java.nio.ByteBuffer;

public class OneToOneOffHeapMultiEventThroughputTest extends OneToOneOffHeapThroughputTest {

    protected OffHeapRingBuffer createRingBuffer(){
        return new OffHeapRingBuffer(new SingleProducerSequencer(BUFFER_SIZE, new NoWaitStrategy()), BLOCK_SIZE);
    }

    protected EventProcessor createEventProcessor(OffHeapRingBuffer buffer) {
        MultiEventProcessor processor =
                new MultiEventProcessor(new YieldMultiBufferWaitStrategy());
        buffer.addGatingSequences(processor.add(buffer, buffer.newBarrier(), handler));
        return processor;
    }

    public static void main(String[] args) throws Exception
    {
        new OneToOneOffHeapMultiEventThroughputTest().testImplementations();
    }
}
