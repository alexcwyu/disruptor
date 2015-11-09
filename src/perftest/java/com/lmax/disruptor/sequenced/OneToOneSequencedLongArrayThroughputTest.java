/*
 * Copyright 2011 LMAX Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.lmax.disruptor.sequenced;

import static com.lmax.disruptor.RingBuffer.createSingleProducer;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.lmax.disruptor.*;
import com.lmax.disruptor.support.LongArrayEventHandler;
import com.lmax.disruptor.support.PerfTestUtil;
import com.lmax.disruptor.support.ValueEvent;
import com.lmax.disruptor.util.DaemonThreadFactory;

/**
 * <pre>
 * UniCast a series of items between 1 publisher and 1 event processor.
 *
 * +----+    +-----+
 * | P1 |--->| EP1 |
 * +----+    +-----+
 *
 * Disruptor:
 * ==========
 *              track to prevent wrap
 *              +------------------+
 *              |                  |
 *              |                  v
 * +----+    +====+    +====+   +-----+
 * | P1 |--->| RB |<---| SB |   | EP1 |
 * +----+    +====+    +====+   +-----+
 *      claim      get    ^        |
 *                        |        |
 *                        +--------+
 *                          waitFor
 *
 * P1  - Publisher 1
 * RB  - RingBuffer
 * SB  - SequenceBarrier
 * EP1 - EventProcessor 1
 *
 * </pre>
 */
public class OneToOneSequencedLongArrayThroughputTest extends AbstractPerfTestDisruptor
{
    protected static final int BUFFER_SIZE = 1024 * 1;
    protected static final long ITERATIONS = 1000L * 1000L * 1L;
    protected static final int ARRAY_SIZE = 2 * 1024;
    protected final ExecutorService executor = Executors.newSingleThreadExecutor(DaemonThreadFactory.INSTANCE);

    protected static final EventFactory<long[]> FACTORY = new EventFactory<long[]>()
    {
        @Override
        public long[] newInstance()
        {
            return new long[ARRAY_SIZE];
        }
    };
    ///////////////////////////////////////////////////////////////////////////////////////////////

//    private final RingBuffer<long[]> ringBuffer =
//        createSingleProducer(FACTORY, BUFFER_SIZE, new YieldingWaitStrategy());
//    private final SequenceBarrier sequenceBarrier = ringBuffer.newBarrier();
    protected final LongArrayEventHandler handler = new LongArrayEventHandler();
//    private final BatchEventProcessor<long[]> batchEventProcessor =
//        new BatchEventProcessor<long[]>(ringBuffer, sequenceBarrier, handler);
//
//    {
//        ringBuffer.addGatingSequences(batchEventProcessor.getSequence());
//    }

    protected RingBuffer<long[]> createRingBuffer(){
        return createSingleProducer(FACTORY, BUFFER_SIZE, new YieldingWaitStrategy());
    }

    protected EventProcessor createEventProcessor(RingBuffer<long[]> ringBuffer){
        SequenceBarrier sequenceBarrier = ringBuffer.newBarrier();
        BatchEventProcessor<long[]> batchEventProcessor =
                new BatchEventProcessor<long[]>(ringBuffer, sequenceBarrier, handler);
        ringBuffer.addGatingSequences(batchEventProcessor.getSequence());
        return batchEventProcessor;
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    protected int getRequiredProcessorCount()
    {
        return 2;
    }

    @Override
    protected long runDisruptorPass() throws InterruptedException
    {
        RingBuffer<long[]> ringBuffer = createRingBuffer();
        EventProcessor batchEventProcessor = createEventProcessor(ringBuffer);

        final CountDownLatch latch = new CountDownLatch(1);
        long expectedCount = batchEventProcessor.getSequence().get() + ITERATIONS;
        handler.reset(latch, ITERATIONS);
        executor.submit(batchEventProcessor);
        long start = System.currentTimeMillis();

        final RingBuffer<long[]> rb = ringBuffer;

        for (long i = 0; i < ITERATIONS; i++)
        {
            long next = rb.next();
            long[] event = rb.get(next);
            for (int j = 0; j < event.length; j++)
            {
                event[j] = i;
            }
            rb.publish(next);
        }

        latch.await();
        long opsPerSecond = (ITERATIONS * ARRAY_SIZE * 1000L) / (System.currentTimeMillis() - start);
        waitForEventProcessorSequence(batchEventProcessor, expectedCount);
        batchEventProcessor.halt();

        PerfTestUtil.failIf(0, handler.getValue());

        return opsPerSecond;
    }

    private void waitForEventProcessorSequence(EventProcessor batchEventProcessor, long expectedCount) throws InterruptedException
    {
        while (batchEventProcessor.getSequence().get() != expectedCount)
        {
            Thread.sleep(1);
        }
    }

    public static void main(String[] args) throws Exception
    {
        OneToOneSequencedLongArrayThroughputTest test = new OneToOneSequencedLongArrayThroughputTest();
        test.testImplementations();
    }
}
