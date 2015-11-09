package com.lmax.disruptor.multi;

import com.lmax.disruptor.*;
import com.lmax.disruptor.sequenced.OneToThreeSequencedThroughputTest;
import com.lmax.disruptor.support.ValueEvent;

import static com.lmax.disruptor.RingBuffer.createSingleProducer;

/**
 * Created by alex on 11/10/15.
 */
public class OneToThreeSequencedMultiEventThroughputTest extends OneToThreeSequencedThroughputTest {

    protected RingBuffer<ValueEvent> createRingBuffer(){
        return createSingleProducer(ValueEvent.EVENT_FACTORY, BUFFER_SIZE, new NoWaitStrategy());
    }

    protected EventProcessor[] createEventProcessor(RingBuffer<ValueEvent> ringBuffer) {


        SequenceBarrier sequenceBarrier = ringBuffer.newBarrier();

        MultiEventProcessor ep1 =
                new MultiEventProcessor(new YieldMultiBufferWaitStrategy());
        Sequence seq1 = ep1.add(ringBuffer, sequenceBarrier, handlers[0]);

        MultiEventProcessor ep2 =
                new MultiEventProcessor(new YieldMultiBufferWaitStrategy());
        Sequence seq2 = ep2.add(ringBuffer, sequenceBarrier, handlers[1]);

        MultiEventProcessor ep3 =
                new MultiEventProcessor(new YieldMultiBufferWaitStrategy());
        Sequence seq3 = ep3.add(ringBuffer, sequenceBarrier, handlers[2]);


        ringBuffer.addGatingSequences(seq1, seq2, seq3);
        EventProcessor[] batchEventProcessors = {ep1, ep2, ep3};
        return batchEventProcessors;
    }


    public static void main(String[] args) throws Exception
    {
        new OneToThreeSequencedMultiEventThroughputTest().testImplementations();
    }
}
