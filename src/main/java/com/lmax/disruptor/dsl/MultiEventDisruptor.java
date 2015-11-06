package com.lmax.disruptor.dsl;

import com.lmax.disruptor.*;
import com.lmax.disruptor.multi.MultiEventProcessor;
import com.lmax.disruptor.multi.NoWaitStrategy;

import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by alex on 6/11/15.
 */
public class MultiEventDisruptor<T>
{


    private final RingBuffer<T> ringBuffer;
    private final Executor executor;
    private final MultiEventConsumerRepository<T> multiEventConsumerRepository = new MultiEventConsumerRepository<T>(this);
    private final AtomicBoolean started = new AtomicBoolean(false);
    private ExceptionHandler<? super T> exceptionHandler;

    public MultiEventDisruptor(final EventFactory<T> eventFactory,
                               final int ringBufferSize,
                               final Executor executor)
    {
        this(RingBuffer.createMultiProducer(eventFactory, ringBufferSize, new NoWaitStrategy()), executor);
    }

    public MultiEventDisruptor(final EventFactory<T> eventFactory,
                               final int ringBufferSize,
                               final Executor executor,
                               final ProducerType producerType,
                               final WaitStrategy waitStrategy)
    {
        this(RingBuffer.create(producerType, eventFactory, ringBufferSize, waitStrategy),
                executor);
    }

    private MultiEventDisruptor(final RingBuffer<T> ringBuffer,
                                final Executor executor)
    {
        this.ringBuffer = ringBuffer;
        this.executor = executor;
    }

    public MultiEventHandlerGroup<T> handleEventsWith(final MultiEventHandlerInfo... handlers)
    {
        return updateEventProcessors(new Sequence[0], handlers);
    }

    public MultiEventHandlerGroup<T> handleEventsWithWorkerPool(final WorkHandler<T>... workHandlers)
    {
        return createWorkerPool(new Sequence[0], workHandlers);
    }


    public void handleExceptionsWith(final ExceptionHandler<? super T> exceptionHandler)
    {
        this.exceptionHandler = exceptionHandler;
    }


    @SuppressWarnings("varargs")
    public MultiEventHandlerGroup<T> after(final MultiEventHandlerInfo<T> [] infos)
    {
        final Sequence[] sequences = new Sequence[infos.length];
        for (int i = 0, handlersLength = infos.length; i < handlersLength; i++)
        {

            sequences[i] = multiEventConsumerRepository.getSequenceFor(infos[i].handler);
        }

        return new MultiEventHandlerGroup<T>(this, multiEventConsumerRepository, sequences);
    }

    public void publishEvent(final EventTranslator<T> eventTranslator)
    {
        ringBuffer.publishEvent(eventTranslator);
    }

    public <A> void publishEvent(final EventTranslatorOneArg<T, A> eventTranslator, final A arg)
    {
        ringBuffer.publishEvent(eventTranslator, arg);
    }

    public <A> void publishEvents(final EventTranslatorOneArg<T, A> eventTranslator, final A[] arg)
    {
        ringBuffer.publishEvents(eventTranslator, arg);
    }

    public RingBuffer<T> start()
    {
        final Sequence[] gatingSequences = multiEventConsumerRepository.getLastSequenceInChain(true);
        ringBuffer.addGatingSequences(gatingSequences);

        checkOnlyStartedOnce();
        for (final ConsumerInfo consumerInfo : multiEventConsumerRepository)
        {
            consumerInfo.start(executor);
        }

        return ringBuffer;
    }

    public void halt()
    {
        for (final ConsumerInfo consumerInfo : multiEventConsumerRepository)
        {
            consumerInfo.halt();
        }
    }

    public void shutdown()
    {
        try
        {
            shutdown(-1, TimeUnit.MILLISECONDS);
        }
        catch (final TimeoutException e)
        {
            exceptionHandler.handleOnShutdownException(e);
        }
    }


    public void shutdown(final long timeout, final TimeUnit timeUnit) throws TimeoutException
    {
        final long timeOutAt = System.currentTimeMillis() + timeUnit.toMillis(timeout);
        while (hasBacklog())
        {
            if (timeout >= 0 && System.currentTimeMillis() > timeOutAt)
            {
                throw TimeoutException.INSTANCE;
            }
            // Busy spin
        }
        halt();
    }

    public RingBuffer<T> getRingBuffer()
    {
        return ringBuffer;
    }

    public long getCursor()
    {
        return ringBuffer.getCursor();
    }

    public long getBufferSize()
    {
        return ringBuffer.getBufferSize();
    }

    public T get(final long sequence)
    {
        return ringBuffer.get(sequence);
    }


    public SequenceBarrier getBarrierFor(final EventHandler handler)
    {
        return multiEventConsumerRepository.getBarrierFor(handler);
    }

    private boolean hasBacklog()
    {
        final long cursor = ringBuffer.getCursor();
        for (final Sequence consumer : multiEventConsumerRepository.getLastSequenceInChain(false))
        {
            if (cursor > consumer.get())
            {
                return true;
            }
        }
        return false;
    }

    MultiEventHandlerGroup<T> updateEventProcessors(final Sequence[] barrierSequences,
                                               final MultiEventHandlerInfo[] infos)
    {
        checkNotStarted();
        final Sequence[] processorSequences = new Sequence[infos.length];
        final SequenceBarrier barrier = ringBuffer.newBarrier(barrierSequences);

        for (int i = 0, processorsLength = infos.length; i < processorsLength; i++)
        {

            final MultiEventProcessor eventProcessor = infos[i].processor;
            final EventHandler<T> eventHandler = infos[i].handler;

            processorSequences[i] = eventProcessor.add(ringBuffer, barrier, eventHandler);
            multiEventConsumerRepository.add(eventProcessor, eventHandler, barrier);
        }

        if (processorSequences.length > 0)
        {
            multiEventConsumerRepository.unMarkEventProcessorsAsEndOfChain(barrierSequences);
        }

        return new MultiEventHandlerGroup<T>(this, multiEventConsumerRepository, processorSequences);
    }

    MultiEventHandlerGroup<T> createWorkerPool(final Sequence[] barrierSequences, final WorkHandler<? super T>[] workHandlers)
    {
        final SequenceBarrier sequenceBarrier = ringBuffer.newBarrier(barrierSequences);
        final WorkerPool<T> workerPool = new WorkerPool<T>(ringBuffer, sequenceBarrier, exceptionHandler, workHandlers);
        multiEventConsumerRepository.add(workerPool, sequenceBarrier);
        return new MultiEventHandlerGroup<T>(this, multiEventConsumerRepository, workerPool.getWorkerSequences());
    }

    private void checkNotStarted()
    {
        if (started.get())
        {
            throw new IllegalStateException("All event handlers must be added before calling starts.");
        }
    }

    private void checkOnlyStartedOnce()
    {
        if (!started.compareAndSet(false, true))
        {
            throw new IllegalStateException("Disruptor.start() must only be called once.");
        }
    }


    private Sequence[] getSequencesFor(final MultiEventProcessor... processors)
    {
        Sequence[] sequences = new Sequence[processors.length];
        for (int i = 0; i < sequences.length; i++)
        {
            sequences[i] = processors[i].getSequence(ringBuffer);
        }

        return sequences;
    }

}
