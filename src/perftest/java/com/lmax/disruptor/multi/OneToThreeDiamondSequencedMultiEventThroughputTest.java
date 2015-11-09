package com.lmax.disruptor.multi;

import com.lmax.disruptor.*;
import com.lmax.disruptor.sequenced.OneToThreeDiamondSequencedThroughputTest;
import com.lmax.disruptor.support.FizzBuzzEvent;

import static com.lmax.disruptor.RingBuffer.createSingleProducer;

/**
 * Created by alex on 11/10/15.
 */
//TODO
public class OneToThreeDiamondSequencedMultiEventThroughputTest extends OneToThreeDiamondSequencedThroughputTest {


    protected RingBuffer<FizzBuzzEvent> createRingBuffer(){
        return createSingleProducer(FizzBuzzEvent.EVENT_FACTORY, BUFFER_SIZE, new NoWaitStrategy());
    }

    protected EventProcessor[] createEventProcessor(RingBuffer<FizzBuzzEvent> ringBuffer){
        //SequenceBarrier sequenceBarrier = ringBuffer.newBarrier();
        MultiEventProcessor batchProcessorFizz =
                new MultiEventProcessor(new YieldMultiBufferWaitStrategy());
        Sequence batchProcessorFizzSequence = batchProcessorFizz.add(ringBuffer, fizzHandler);

        MultiEventProcessor batchProcessorBuzz =
                new MultiEventProcessor(new YieldMultiBufferWaitStrategy());
        Sequence batchProcessorBuzzSequence = batchProcessorFizz.add(ringBuffer, buzzHandler);

        SequenceBarrier sequenceBarrierFizzBuzz =
                ringBuffer.newBarrier(batchProcessorFizzSequence, batchProcessorBuzzSequence);

        MultiEventProcessor batchProcessorFizzBuzz =
                new MultiEventProcessor(new YieldMultiBufferWaitStrategy());
        ringBuffer.addGatingSequences(batchProcessorFizzBuzz.add(ringBuffer, sequenceBarrierFizzBuzz, fizzBuzzHandler));


        EventProcessor[] result = {batchProcessorFizz, batchProcessorBuzz, batchProcessorFizzBuzz};
        return result;
    }

    public static void main(String[] args) throws Exception
    {
        new OneToThreeDiamondSequencedMultiEventThroughputTest().testImplementations();
    }
}
