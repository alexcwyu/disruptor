package com.lmax.disruptor.multi;

import com.lmax.disruptor.EventProcessor;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.Sequence;
import com.lmax.disruptor.sequenced.ThreeToThreeSequencedThroughputTest;

public class ThreeToThreeSequencedMultiEventThroughputTest extends ThreeToThreeSequencedThroughputTest {

    protected RingBuffer<long[]>[] createRingBuffer() {
        RingBuffer<long[]>[] buffers = new RingBuffer[NUM_PUBLISHERS];
        for (int i = 0; i < NUM_PUBLISHERS; i++)
        {
            buffers[i] = RingBuffer.createSingleProducer(FACTORY, BUFFER_SIZE, new NoWaitStrategy());
        }
        return buffers;
    }

    protected EventProcessor createEventProcessor(RingBuffer<long[]>[] buffers) {
        MultiEventProcessor batchEventProcessor = new MultiEventProcessor(new YieldMultiBufferWaitStrategy());

        for (int i = 0; i < NUM_PUBLISHERS; i++)
        {
            Sequence seq = batchEventProcessor.add(buffers[i], handler);
            buffers[i].addGatingSequences(seq);
        }

        return batchEventProcessor;
    }

    public static void main(String[] args) throws Exception
    {
        new ThreeToThreeSequencedMultiEventThroughputTest().testImplementations();
    }

}
