package com.lmax.disruptor.dsl;

import com.lmax.disruptor.EventTranslatorOneArg;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.stubs.*;
import com.lmax.disruptor.support.DoubleEvent;
import com.lmax.disruptor.support.LongEvent;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class MultiEventDisruptorTest
{
    private MultiEventDisruptor<DoubleEvent> disruptor1;
    private MultiEventDisruptor<LongEvent> disruptor2;

    private RingBuffer<DoubleEvent> ringBuffer1;
    private RingBuffer<LongEvent> ringBuffer2;

    private StubExecutor executor;

    private static final EventTranslatorOneArg<LongEvent, ByteBuffer> LONG_TRANSLATOR =
            new EventTranslatorOneArg<LongEvent, ByteBuffer>()
            {
                public void translateTo(LongEvent event, long sequence, ByteBuffer bb)
                {
                    event.set(bb.getLong(0));
                }
            };

    private static final EventTranslatorOneArg<DoubleEvent, ByteBuffer> DOUBLE_TRANSLATOR =
            new EventTranslatorOneArg<DoubleEvent, ByteBuffer>()
            {
                public void translateTo(DoubleEvent event, long sequence, ByteBuffer bb)
                {
                    event.set(bb.getDouble(0));
                }
            };

    @Before
    public void setUp() throws Exception
    {
        createDisruptor();
    }

    @After
    public void tearDown() throws Exception
    {

        disruptor1.halt();
        disruptor2.halt();
        executor.joinAllThreads();
    }

    @Test
    public void testMultiple() throws Exception
    {
        final CountDownLatch latch = new CountDownLatch(3);

        final double[] doubleValues = {11,22,33,44,55,66};
        final long[] longValues = {100,200,300,400,500,600};

        int expectedLongCount = longValues.length;
        int expectedDoubleCount = doubleValues.length;

        long expectedLong = Arrays.stream(longValues).sum();
        double expectedDouble = Arrays.stream(doubleValues).sum();

        final LongDoubleSumEventHandler longDoubleSumEventHandler = createLongDoubleSumEventHandler(expectedDoubleCount, -expectedDouble, expectedLongCount, -expectedLong, latch);

        final NegDoubleEventHandler negDoubleEventHandler1 = createNegDoubleEventHandler();
        final NegDoubleEventHandler negDoubleEventHandler2 = createNegDoubleEventHandler();
        final SumDoubleEventHandler sumDoubleEventHandler = createSumDoubleEventHandler();
        final DoubleEventHandler doubleEventHandler = createDoubleEventHandler(expectedDoubleCount, latch);

        disruptor1.handleEventsWithWorkerPool(negDoubleEventHandler1, negDoubleEventHandler2)
                .then(sumDoubleEventHandler)
                .then(longDoubleSumEventHandler)
                .then(doubleEventHandler);

        final NegLongEventHandler negLongEventHandler1 = createNegLongEventHandler();
        final NegLongEventHandler negLongEventHandler2 = createNegLongEventHandler();
        final SumLongEventHandler sumLongEventHandler = createSumLongEventHandler();
        final LongEventHandler longEventHandler = createLongEventHandler(expectedLongCount, latch);

        disruptor2.handleEventsWithWorkerPool(negLongEventHandler1, negLongEventHandler2)
                .then(sumLongEventHandler)
                .then(longDoubleSumEventHandler)
                .then(longEventHandler);


        ringBuffer1 = disruptor1.start();
        ringBuffer2 = disruptor2.start();


        executor.execute(new Runnable (){
                    public void run(){
                    try {
                        ByteBuffer bb = ByteBuffer.allocate(8);
                        for (double value : doubleValues) {
                            bb.putDouble(0, value);
                            ringBuffer1.publishEvent(DOUBLE_TRANSLATOR, bb);
                            Thread.sleep(200);
                        }
                    }
                    catch (InterruptedException ignored){

                    }
                    catch (Exception e){
                        throw new RuntimeException(e);
                    }
                }}
        );

        executor.execute(new Runnable (){
                             public void run(){
                                 try {
                                     ByteBuffer bb = ByteBuffer.allocate(8);
                                     for (long value : longValues) {
                                         bb.putLong(0, value);
                                         ringBuffer2.publishEvent(LONG_TRANSLATOR, bb);
                                         Thread.sleep(200);
                                     }
                                 }
                                 catch (InterruptedException ignored){

                                 }
                                 catch (Exception e){
                                     throw new RuntimeException(e);
                                 }
                             }}
        );

        latch.await(5000, TimeUnit.MILLISECONDS);

        double exp = -expectedDouble-expectedLong;
        assertEquals(exp, longDoubleSumEventHandler.doubleValue() +longDoubleSumEventHandler.longValue(), 0);
        assertTrue(exp == doubleEventHandler.getValue() || exp == (double) longEventHandler.getValue());
    }


    @Test
    public void testPref() throws Exception
    {
        final CountDownLatch latch = new CountDownLatch(3);


        int ITERATIONS = 10000000;
        int expectedLongCount = ITERATIONS;
        int expectedDoubleCount = ITERATIONS;

        long expectedLong = 0;
        double expectedDouble = 0;

        final LongDoubleSumEventHandler longDoubleSumEventHandler = createLongDoubleSumEventHandler(expectedDoubleCount, -expectedDouble, expectedLongCount, -expectedLong, latch);

        final NegDoubleEventHandler negDoubleEventHandler1 = createNegDoubleEventHandler();
        final NegDoubleEventHandler negDoubleEventHandler2 = createNegDoubleEventHandler();
        final SumDoubleEventHandler sumDoubleEventHandler = createSumDoubleEventHandler();
        final DoubleEventHandler doubleEventHandler = createDoubleEventHandler(expectedDoubleCount, latch);

        disruptor1.handleEventsWithWorkerPool(negDoubleEventHandler1, negDoubleEventHandler2)
                .then(sumDoubleEventHandler)
                .then(longDoubleSumEventHandler)
                .then(doubleEventHandler);

        final NegLongEventHandler negLongEventHandler1 = createNegLongEventHandler();
        final NegLongEventHandler negLongEventHandler2 = createNegLongEventHandler();
        final SumLongEventHandler sumLongEventHandler = createSumLongEventHandler();
        final LongEventHandler longEventHandler = createLongEventHandler(expectedLongCount, latch);

        disruptor2.handleEventsWithWorkerPool(negLongEventHandler1, negLongEventHandler2)
                .then(sumLongEventHandler)
                .then(longDoubleSumEventHandler)
                .then(longEventHandler);


        ringBuffer1 = disruptor1.start();
        ringBuffer2 = disruptor2.start();


        ByteBuffer bb1 = ByteBuffer.allocate(8);
        ByteBuffer bb2 = ByteBuffer.allocate(8);

        bb1.putDouble(0, 0);
        bb2.putLong(0, 0);

        long start = System.currentTimeMillis();

        for (int i = 0; i < ITERATIONS; i++) {
            ringBuffer1.publishEvent(DOUBLE_TRANSLATOR, bb1);
            ringBuffer2.publishEvent(LONG_TRANSLATOR, bb2);
        }

        latch.await();
        long opsPerSecond = (ITERATIONS * 1000L) / (System.currentTimeMillis() - start);

        System.out.println(opsPerSecond);

        double exp = -expectedDouble-expectedLong;
        assertEquals(exp, longDoubleSumEventHandler.doubleValue() +longDoubleSumEventHandler.longValue(), 0);
        assertTrue(exp == doubleEventHandler.getValue() || exp == (double) longEventHandler.getValue());
    }

    private <T> Runnable createPublisher(final EventTranslatorOneArg<T, ByteBuffer> translatorOneArg, final RingBuffer<T> rb, final long count) {
        return new Runnable() {
            public void run() {
                try {
                    ByteBuffer bb = ByteBuffer.allocate(8);
                    for (int i = 0; i < count; i++) {
                        bb.putLong(0, 0);
                        rb.publishEvent(translatorOneArg, bb);
                    }
                } catch (Exception e) {
                }
            }
        };
    }

    private LongDoubleSumEventHandler createLongDoubleSumEventHandler(int expectedDoubleCount, double expectedDoubleValue, int expectedLongCount, long expectedLongValue, CountDownLatch countDownLatch)
    {
        LongDoubleSumEventHandler longDoubleSumEventHandler = new LongDoubleSumEventHandler(expectedDoubleCount, expectedDoubleValue, expectedLongCount, expectedLongValue, countDownLatch);
        return longDoubleSumEventHandler;
    }


    private DoubleEventHandler createDoubleEventHandler(int expectedCount, CountDownLatch countDownLatch)
    {
        DoubleEventHandler doubleEventHandler = new DoubleEventHandler(expectedCount, countDownLatch);
        return doubleEventHandler;
    }

    private LongEventHandler createLongEventHandler(int expectedCount, CountDownLatch countDownLatch)
    {
        LongEventHandler longEventHandler = new LongEventHandler(expectedCount, countDownLatch);
        return longEventHandler;
    }

    private SumLongEventHandler createSumLongEventHandler()
    {
        SumLongEventHandler sumLongEventHandler = new SumLongEventHandler();
        return sumLongEventHandler;
    }

    private SumDoubleEventHandler createSumDoubleEventHandler()
    {
        SumDoubleEventHandler sumDoubleEventHandler = new SumDoubleEventHandler();
        return sumDoubleEventHandler;
    }

    private NegLongEventHandler createNegLongEventHandler()
    {
        NegLongEventHandler negLongEventHandler = new NegLongEventHandler();
        return negLongEventHandler;
    }

    private NegDoubleEventHandler createNegDoubleEventHandler()
    {
        NegDoubleEventHandler negDoubleEventHandler = new NegDoubleEventHandler();
        return negDoubleEventHandler;
    }

    private void createDisruptor()
    {
        executor = new StubExecutor();
        createDisruptor(executor);
    }

    private void createDisruptor(final Executor executor)
    {
        disruptor1 = new MultiEventDisruptor<DoubleEvent>(
                DoubleEvent.FACTORY, 4096, executor,
                ProducerType.SINGLE);

        disruptor2 = new MultiEventDisruptor<LongEvent>(
                LongEvent.FACTORY, 4096, executor,
                ProducerType.SINGLE);
    }

    public static void translateLong(LongEvent event, long sequence, ByteBuffer buffer)
    {
        event.set(buffer.getLong(0));
    }

    public static void translateDouble(DoubleEvent event, long sequence, ByteBuffer buffer)
    {
        event.set(buffer.getDouble(0));
    }


}
