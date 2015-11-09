package com.lmax.disruptor.multi;

import com.lmax.disruptor.EventProcessor;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.Sequence;
import com.lmax.disruptor.SequenceBarrier;
import com.lmax.disruptor.sequenced.OneToThreePipelineSequencedThroughputTest;
import com.lmax.disruptor.support.FunctionEvent;

import static com.lmax.disruptor.RingBuffer.createSingleProducer;

/**
 * Created by alex on 11/10/15.
 */
public class OneToThreePipelineSequencedMultiEventThroughputTest extends OneToThreePipelineSequencedThroughputTest {


    protected RingBuffer<FunctionEvent> createRingBuffer(){
        return createSingleProducer(FunctionEvent.EVENT_FACTORY, BUFFER_SIZE, new NoWaitStrategy());
    }

    protected EventProcessor[] createEventProcessor(RingBuffer<FunctionEvent> ringBuffer){
        SequenceBarrier sequenceBarrier = ringBuffer.newBarrier();
        MultiEventProcessor stepOneBatchProcessor =
                new MultiEventProcessor(new YieldMultiBufferWaitStrategy());
        Sequence stepOneSeq = stepOneBatchProcessor.add(ringBuffer, sequenceBarrier, stepOneFunctionHandler);

        SequenceBarrier stepTwoSequenceBarrier = ringBuffer.newBarrier(stepOneSeq);
        MultiEventProcessor stepTwoBatchProcessor =
                new MultiEventProcessor(new YieldMultiBufferWaitStrategy());
        Sequence stepTwoSeq = stepTwoBatchProcessor.add(ringBuffer, stepTwoSequenceBarrier, stepTwoFunctionHandler);

        SequenceBarrier stepThreeSequenceBarrier = ringBuffer.newBarrier(stepTwoSeq);

        MultiEventProcessor stepThreeBatchProcessor =
                new MultiEventProcessor(new YieldMultiBufferWaitStrategy());
        ringBuffer.addGatingSequences(stepThreeBatchProcessor.add(ringBuffer, stepThreeSequenceBarrier, stepThreeFunctionHandler));


        EventProcessor[] result = {stepOneBatchProcessor, stepTwoBatchProcessor, stepThreeBatchProcessor};
        return result;
    }
    public static void main(String[] args) throws Exception
    {
        new OneToThreePipelineSequencedMultiEventThroughputTest().testImplementations();
    }
}
