package com.lmax.disruptor.multi;

import com.lmax.disruptor.*;
import com.lmax.disruptor.sequenced.PingPongSequencedLatencyTest;
import com.lmax.disruptor.support.ValueEvent;

import static com.lmax.disruptor.RingBuffer.createSingleProducer;

public class PingPongSequencedLatencyMultiEventTest extends PingPongSequencedLatencyTest {

    protected RingBuffer<ValueEvent> createBuffer(){
        return createSingleProducer(ValueEvent.EVENT_FACTORY, BUFFER_SIZE, new NoWaitStrategy());
    }

    protected EventProcessor[] createEventProcessor(RingBuffer<ValueEvent> pingBuffer, RingBuffer<ValueEvent> pongBuffer, Pinger pinger, Ponger ponger){

        MultiEventProcessor pingProcessor =
                new MultiEventProcessor(new YieldMultiBufferWaitStrategy());
        Sequence pingProcessorSeq = pingProcessor.add(pongBuffer, pinger);

        MultiEventProcessor pongProcessor =
                new MultiEventProcessor(new YieldMultiBufferWaitStrategy());
        Sequence pongProcessorSeq = pongProcessor.add(pingBuffer, ponger);

        pingBuffer.addGatingSequences(pongProcessorSeq);
        pongBuffer.addGatingSequences(pingProcessorSeq);

        EventProcessor[] result = {
                pingProcessor,
                pongProcessor
        };
        return result;
    }

    public static void main(final String[] args) throws Exception
    {
        final PingPongSequencedLatencyMultiEventTest test = new PingPongSequencedLatencyMultiEventTest();
        test.shouldCompareDisruptorVsQueues();
    }

}
