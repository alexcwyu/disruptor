package com.lmax.disruptor.multi;

import com.lmax.disruptor.EventProcessor;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.offheap.OneToOneOnHeapThroughputTest;

import java.nio.ByteBuffer;

public class OneToOneOnHeapMultiEventThroughputTest extends OneToOneOnHeapThroughputTest {

    protected RingBuffer<ByteBuffer> createRingBuffer(){
        return RingBuffer.createSingleProducer(BufferFactory.direct(BLOCK_SIZE), BUFFER_SIZE, new NoWaitStrategy());
    }

    protected EventProcessor createEventProcessor(RingBuffer<ByteBuffer> buffer) {
        MultiEventProcessor processor =
                new MultiEventProcessor(new YieldMultiBufferWaitStrategy());
        buffer.addGatingSequences(processor.add(buffer, handler));
        return processor;
    }


    public static void main(String[] args) throws Exception
    {
        new OneToOneOnHeapMultiEventThroughputTest().testImplementations();
    }
}
