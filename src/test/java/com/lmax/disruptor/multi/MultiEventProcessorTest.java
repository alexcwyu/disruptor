package com.lmax.disruptor.multi;

import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.support.LongEvent;
import com.lmax.disruptor.support.StubEvent;
import com.lmax.disruptor.support.TestEvent;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.Sequence;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;

import static com.lmax.disruptor.RingBuffer.createMultiProducer;
import static com.lmax.disruptor.support.Actions.countDown;

public class MultiEventProcessorTest
{

    private final RingBuffer<StubEvent> ringBuffer1 = createMultiProducer(StubEvent.EVENT_FACTORY, 16, new NoWaitStrategy());
    private final RingBuffer<TestEvent> ringBuffer2 = createMultiProducer(TestEvent.EVENT_FACTORY, 16, new NoWaitStrategy());
    private final RingBuffer<LongEvent> ringBuffer3 = createMultiProducer(LongEvent.FACTORY, 16, new NoWaitStrategy());

    private final Mockery context = new Mockery();
    private final CountDownLatch latch = new CountDownLatch(1);
    private final EventHandler<Object> eventHandler = context.mock(EventHandler.class);

    private final Sequence lifecycleSequence1 = context.sequence("lifecycleSequence1");
    private final Sequence lifecycleSequence2 = context.sequence("lifecycleSequence2");
    private final Sequence lifecycleSequence3 = context.sequence("lifecycleSequence3");

    private MultiEventProcessor processor;

    @Before
    public void setup(){

        processor = new MultiEventProcessor(new BlockingMultiBufferWaitStrategy());
        ringBuffer1.addGatingSequences(processor.add(ringBuffer1, eventHandler));
        ringBuffer2.addGatingSequences(processor.add(ringBuffer2, eventHandler));
        ringBuffer3.addGatingSequences(processor.add(ringBuffer3, eventHandler));
    }


    @Test
    public void test()throws Exception
    {
        context.checking(
                new Expectations()
                {
                    {
                        oneOf(eventHandler).onEvent(ringBuffer1.get(0L), 0L, true);
                        inSequence(lifecycleSequence1);

                        oneOf(eventHandler).onEvent(ringBuffer2.get(0L), 0L, false);
                        inSequence(lifecycleSequence2);
                        oneOf(eventHandler).onEvent(ringBuffer2.get(1L), 1L, true);
                        inSequence(lifecycleSequence2);

                        oneOf(eventHandler).onEvent(ringBuffer3.get(0L), 0L, false);
                        inSequence(lifecycleSequence3);
                        oneOf(eventHandler).onEvent(ringBuffer3.get(1L), 1L, false);
                        inSequence(lifecycleSequence3);
                        oneOf(eventHandler).onEvent(ringBuffer3.get(2L), 2L, true);
                        inSequence(lifecycleSequence3);

                        will(countDown(latch));
                    }
                });

        ringBuffer1.publish(ringBuffer1.next());
        ringBuffer2.publish(ringBuffer2.next());
        ringBuffer3.publish(ringBuffer3.next());

        ringBuffer2.publish(ringBuffer2.next());
        ringBuffer3.publish(ringBuffer3.next());

        ringBuffer3.publish(ringBuffer3.next());


        Thread thread = new Thread(processor);
        thread.start();

        latch.await();

        processor.halt();
        thread.join();

    }

}
