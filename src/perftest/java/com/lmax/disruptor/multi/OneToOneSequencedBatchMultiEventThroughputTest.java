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
package com.lmax.disruptor.multi;

import com.lmax.disruptor.*;
import com.lmax.disruptor.sequenced.OneToOneSequencedBatchThroughputTest;
import com.lmax.disruptor.support.ValueEvent;

import static com.lmax.disruptor.RingBuffer.createSingleProducer;


public class OneToOneSequencedBatchMultiEventThroughputTest extends OneToOneSequencedBatchThroughputTest
{

    protected RingBuffer<ValueEvent> createRingBuffer(){
        return createSingleProducer(ValueEvent.EVENT_FACTORY, BUFFER_SIZE, new NoWaitStrategy());
    }

    protected EventProcessor createEventProcessor(RingBuffer<ValueEvent> ringBuffer){
        MultiEventProcessor batchEventProcessor =
                new MultiEventProcessor(new YieldMultiBufferWaitStrategy());
        ringBuffer.addGatingSequences(batchEventProcessor.add(ringBuffer, handler));
        return batchEventProcessor;
    }


    public static void main(String[] args) throws Exception
    {
        OneToOneSequencedBatchMultiEventThroughputTest test = new OneToOneSequencedBatchMultiEventThroughputTest();
        test.testImplementations();
    }
}
