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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.lmax.disruptor.*;
import com.lmax.disruptor.support.*;
import com.lmax.disruptor.util.DaemonThreadFactory;

/**
 * <pre>
 *
 * Sequence a series of events from multiple publishers going to one event processor.
 *
 * Disruptor:
 * ==========
 *             track to prevent wrap
 *             +--------------------+
 *             |                    |
 *             |                    |
 * +----+    +====+    +====+       |
 * | P1 |--->| RB |--->| SB |--+    |
 * +----+    +====+    +====+  |    |
 *                             |    v
 * +----+    +====+    +====+  | +----+
 * | P2 |--->| RB |--->| SB |--+>| EP |
 * +----+    +====+    +====+  | +----+
 *                             |
 * +----+    +====+    +====+  |
 * | P3 |--->| RB |--->| SB |--+
 * +----+    +====+    +====+
 *
 * P1 - Publisher 1
 * P2 - Publisher 2
 * P3 - Publisher 3
 * RB - RingBuffer
 * SB - SequenceBarrier
 * EP - EventProcessor
 *
 * </pre>
 */
public class ThreeToThreeSequencedThroughputTest extends AbstractPerfTestDisruptor
{
    protected static final int NUM_PUBLISHERS = 3;
    protected static final int ARRAY_SIZE = 3;
    protected static final int BUFFER_SIZE = 1024 * 64;
    protected static final long ITERATIONS = 1000L * 1000L * 180L;
    private final ExecutorService executor =
        Executors.newFixedThreadPool(NUM_PUBLISHERS + 1, DaemonThreadFactory.INSTANCE);
    private final CyclicBarrier cyclicBarrier = new CyclicBarrier(NUM_PUBLISHERS + 1);

    ///////////////////////////////////////////////////////////////////////////////////////////////


    protected final LongArrayEventHandler handler = new LongArrayEventHandler();

    protected static final EventFactory<long[]> FACTORY = new EventFactory<long[]>()
    {
        @Override
        public long[] newInstance()
        {
            return new long[ARRAY_SIZE];
        }
    };

    protected SequenceBarrier[] barriers = new SequenceBarrier[NUM_PUBLISHERS];

//
//    @SuppressWarnings("unchecked")
//    private final RingBuffer<long[]>[] buffers = new RingBuffer[NUM_PUBLISHERS];
//    private final LongArrayPublisher[] valuePublishers = new LongArrayPublisher[NUM_PUBLISHERS];
//    private final MultiBufferBatchEventProcessor<long[]> batchEventProcessor;
//
//    {
//        for (int i = 0; i < NUM_PUBLISHERS; i++)
//        {
//            buffers[i] = RingBuffer.createSingleProducer(FACTORY, BUFFER_SIZE, new YieldingWaitStrategy());
//            barriers[i] = buffers[i].newBarrier();
//            valuePublishers[i] = new LongArrayPublisher(
//                cyclicBarrier,
//                buffers[i],
//                ITERATIONS / NUM_PUBLISHERS,
//                ARRAY_SIZE);
//        }
//
//        batchEventProcessor = new MultiBufferBatchEventProcessor<long[]>(buffers, barriers, handler);
//
//        for (int i = 0; i < NUM_PUBLISHERS; i++)
//        {
//            buffers[i].addGatingSequences(batchEventProcessor.getSequences()[i]);
//        }
//    }

    ///////////////////////////////////////////////////////////////////////////////////////////////

    protected LongArrayPublisher[] createValuePublishers(RingBuffer<long[]>[] buffers){
        LongArrayPublisher[] valuePublishers = new LongArrayPublisher[NUM_PUBLISHERS];
        for (int i = 0; i < NUM_PUBLISHERS; i++)
        {
            barriers[i] = buffers[i].newBarrier();
            valuePublishers[i] = new LongArrayPublisher(
                    cyclicBarrier,
                    buffers[i],
                    ITERATIONS / NUM_PUBLISHERS,
                    ARRAY_SIZE);
        }
        return valuePublishers;
    }

    protected RingBuffer<long[]>[] createRingBuffer() {
        RingBuffer<long[]>[] buffers = new RingBuffer[NUM_PUBLISHERS];
        for (int i = 0; i < NUM_PUBLISHERS; i++)
        {
            buffers[i] = RingBuffer.createSingleProducer(FACTORY, BUFFER_SIZE, new YieldingWaitStrategy());
        }
        return buffers;
    }

    protected EventProcessor createEventProcessor(RingBuffer<long[]>[] buffers) {
        MultiBufferBatchEventProcessor<long[]> batchEventProcessor = new MultiBufferBatchEventProcessor<long[]>(buffers, barriers, handler);

        for (int i = 0; i < NUM_PUBLISHERS; i++)
        {
            buffers[i].addGatingSequences(batchEventProcessor.getSequences()[i]);
        }

        return batchEventProcessor;
    }


    @Override
    protected int getRequiredProcessorCount()
    {
        return 4;
    }

    @Override
    protected long runDisruptorPass() throws Exception
    {
        RingBuffer<long[]>[] ringBuffer = createRingBuffer();
        EventProcessor batchEventProcessor = createEventProcessor(ringBuffer);
        LongArrayPublisher[] valuePublishers = createValuePublishers(ringBuffer);

        final CountDownLatch latch = new CountDownLatch(1);
        handler.reset(latch, ITERATIONS);

        Future<?>[] futures = new Future[NUM_PUBLISHERS];
        for (int i = 0; i < NUM_PUBLISHERS; i++)
        {
            futures[i] = executor.submit(valuePublishers[i]);
        }
        executor.submit(batchEventProcessor);

        long start = System.currentTimeMillis();
        cyclicBarrier.await();

        for (int i = 0; i < NUM_PUBLISHERS; i++)
        {
            futures[i].get();
        }

        latch.await();

        long opsPerSecond = (ITERATIONS * 1000L * ARRAY_SIZE) / (System.currentTimeMillis() - start);
        batchEventProcessor.halt();

        return opsPerSecond;
    }

    public static void main(String[] args) throws Exception
    {
        new ThreeToThreeSequencedThroughputTest().testImplementations();
    }
}
