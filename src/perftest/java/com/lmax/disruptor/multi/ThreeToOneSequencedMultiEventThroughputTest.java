package com.lmax.disruptor.multi;

import com.lmax.disruptor.EventProcessor;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.Sequence;
import com.lmax.disruptor.SequenceBarrier;
import com.lmax.disruptor.sequenced.ThreeToOneSequencedBatchThroughputTest;
import com.lmax.disruptor.sequenced.ThreeToOneSequencedThroughputTest;
import com.lmax.disruptor.support.ValueEvent;

import static com.lmax.disruptor.RingBuffer.createMultiProducer;

public class ThreeToOneSequencedMultiEventThroughputTest  extends ThreeToOneSequencedThroughputTest {

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
        new ThreeToOneSequencedMultiEventThroughputTest().testImplementations();
    }
}