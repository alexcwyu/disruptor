package com.lmax.disruptor.dsl;

import com.lmax.disruptor.*;
import com.lmax.disruptor.multi.MultiEventProcessor;
import com.lmax.disruptor.multi.NoWaitStrategy;

import java.util.concurrent.Executor;

/**
 * Created by alex on 6/11/15.
 */
public class MultiEventDisruptor<T> extends Disruptor<T>
{

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

    protected MultiEventDisruptor(final RingBuffer<T> ringBuffer, final Executor executor)
    {
        super(ringBuffer, executor);
        this.consumerRepository = new MultiEventConsumerRepository<T>(this);
    }

    public EventHandlerGroup<T> handleEventsWith(final MultiEventHandlerInfo<T>... handlers)
    {
        return updateEventProcessors(new Sequence[0], handlers);
    }


    @SuppressWarnings("varargs")
    public MultiEventHandlerGroup<T> after(final MultiEventHandlerInfo<T>... infos)
    {
        final Sequence[] sequences = new Sequence[infos.length];
        for (int i = 0, handlersLength = infos.length; i < handlersLength; i++)
        {

            sequences[i] = consumerRepository.getSequenceFor(infos[i].handler);
        }

        return new MultiEventHandlerGroup<T>(this, consumerRepository, sequences);
    }

    MultiEventHandlerGroup<T> updateEventProcessors(
            final Sequence[] barrierSequences,
            final MultiEventHandlerInfo[] infos)
    {
        checkNotStarted();
        final Sequence[] processorSequences = new Sequence[infos.length];
        final SequenceBarrier barrier = ringBuffer.newBarrier(barrierSequences);

        for (int i = 0, processorsLength = infos.length; i < processorsLength; i++)
        {

            final MultiEventProcessor eventProcessor = infos[i].processor;
            final EventHandler<T> eventHandler = infos[i].handler;
            if (exceptionHandler != null)
            {
                eventProcessor.setExceptionHandler(exceptionHandler);
            }
            processorSequences[i] = eventProcessor.add(ringBuffer, barrier, eventHandler);
            consumerRepository.add(eventProcessor, eventHandler, barrier);
        }

        if (processorSequences.length > 0)
        {
            consumerRepository.unMarkEventProcessorsAsEndOfChain(barrierSequences);
        }

        return new MultiEventHandlerGroup<T>(this, consumerRepository, processorSequences);
    }

}
