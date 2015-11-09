package com.lmax.disruptor.multi;

import com.lmax.disruptor.*;
import com.lmax.disruptor.sequenced.ThreeToOneSequencedBatchThroughputTest;
import com.lmax.disruptor.support.ValueEvent;

import static com.lmax.disruptor.RingBuffer.createMultiProducer;

/**
 * Created by alex on 11/10/15.
 */
//TODO
public class ThreeToOneSequencedBatchMultiEventThroughputTest extends ThreeToOneSequencedBatchThroughputTest {

    protected RingBuffer<ValueEvent> createRingBuffer(){
        return createMultiProducer(ValueEvent.EVENT_FACTORY, BUFFER_SIZE, new NoWaitStrategy());
    }

    protected EventProcessor createEventProcessor(RingBuffer<ValueEvent> ringBuffer) {
        SequenceBarrier sequenceBarrier = ringBuffer.newBarrier();

        MultiEventProcessor ep1 =
                new MultiEventProcessor(new BusySpinMultiBufferWaitStrategy());
        Sequence seq1 = ep1.add(ringBuffer, sequenceBarrier, handler);

        ringBuffer.addGatingSequences(seq1);
        return ep1;
    }

    public static void main(String[] args) throws Exception
    {
        new ThreeToOneSequencedBatchMultiEventThroughputTest().testImplementations();
    }
}
