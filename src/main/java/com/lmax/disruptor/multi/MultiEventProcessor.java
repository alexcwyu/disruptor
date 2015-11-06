package com.lmax.disruptor.multi;

import com.lmax.disruptor.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.util.Arrays.fill;

/**
 * Created by alex on 4/12/15.
 */
public class MultiEventProcessor implements EventProcessor
{
    private final AtomicBoolean running = new AtomicBoolean(false);
    private ExceptionHandler exceptionHandler = new FatalExceptionHandler();
    private final AtomicBoolean sealed = new AtomicBoolean(false);
    private final MultiBufferWaitStrategy waitStrategy;
    private final List<RingBufferInfo<?>> ringBufferInfos = new ArrayList<RingBufferInfo<?>>();

    private long count;
    private RingBuffer[] providers;
    private SequenceBarrier[] barriers;
    private Sequence[] sequences;
    private EventHandler[] eventHandlers;

    public MultiEventProcessor()
    {
        this(new NoWaitStrategy());
    }

    public MultiEventProcessor(MultiBufferWaitStrategy waitStrategy)
    {
        this.waitStrategy = waitStrategy;
    }

    public Sequence add(RingBuffer provider, SequenceBarrier barriers, EventHandler eventHandler)
    {
        Sequence sequence = new Sequence(Sequencer.INITIAL_CURSOR_VALUE);
        this.ringBufferInfos.add(new RingBufferInfo(provider, barriers, sequence, eventHandler));
        return sequence;
    }

    public Sequence add(RingBufferInfo ringBufferInfo)
    {
        this.ringBufferInfos.add(ringBufferInfo);
        return ringBufferInfo.sequence;
    }

    public void seal()
    {
        if (sealed.compareAndSet(false, true))
        {
            int size = ringBufferInfos.size();

            this.providers = new RingBuffer[size];
            this.barriers = new SequenceBarrier[size];
            this.sequences = new Sequence[size];
            this.eventHandlers = new EventHandler[size];

            for (int i =0; i < size; i++)
            {
                RingBufferInfo ringBufferInfo = ringBufferInfos.get(i);
                this.providers[i]= ringBufferInfo.provider;
                this.barriers[i]= ringBufferInfo.barriers;
                this.sequences[i]= ringBufferInfo.sequence;
                this.eventHandlers[i]= ringBufferInfo.eventHandler;

                providers[i].addGatingSequences(sequences[i]);
            }
        }
    }

    @Override
    public void run()
    {
        if (!running.compareAndSet(false, true))
        {
            throw new RuntimeException("Already running");
        }

        seal();

        for (SequenceBarrier barrier : barriers)
        {
            barrier.clearAlert();
        }

        notifyStart();

        final int barrierLength = barriers.length;
        final long[] lastConsumed = new long[barrierLength];
        final long[] expNextSeq = new long[barrierLength];
        fill(lastConsumed, -1L);
        try
        {
            while (true)
            {
                int currentIdx = -1;
                long currentSeq = -1;
                Object event = null;
                try
                {
                    long batchCount = 0;

                    //RB event
                    for (int i = 0; i < barrierLength; i++)
                    {
                        currentIdx = i;
                        Sequence sequence = sequences[i];
                        long previous = sequence.get();
                        currentSeq = previous;
                        expNextSeq[i] = previous + 1L;
                        long available = barriers[i].waitFor(expNextSeq[i]);

                        if (available > previous)
                        {
                            for (long l = previous + 1; l <= available; l++)
                            {
                                event = providers[i].get(l);
                                eventHandlers[i].onEvent(event, l, l==available);
                            }
                            sequence.set(available);
                            batchCount += (available - previous);
                        }
                    }
                    count += batchCount;
                    currentIdx = -1;
                    currentSeq = -1;
                    event = null;

                    if (batchCount == 0)
                        if (waitStrategy == null)
                            Thread.yield();
                        else
                            waitStrategy.waitNext(expNextSeq, barriers);

                }
                catch (AlertException e)
                {
                    if (!isRunning())
                    {
                        break;
                    }
                }
                catch (TimeoutException e)
                {
                    notifyTimeout(currentIdx, currentSeq);
                }
                catch (Throwable ex)
                {
                    exceptionHandler.handleEventException(ex, currentSeq+1, event);
                    break;
                }
            }
        }
        finally
        {
            notifyShutdown();
            running.set(false);
        }
    }

    @Override
    public Sequence getSequence()
    {
        throw new UnsupportedOperationException();
    }

    public Sequence getSequence(RingBuffer buffer)
    {
        for (int i = 0; i < providers.length; i++)
        {
            if (providers[i] == buffer)
            {
                return sequences[i];
            }
        }
        throw new RuntimeException("buffer not found="+buffer);
    }

    public Sequence getSequence(int i)
    {
        return sequences[i];
    }

    public Sequence[] getSequences()
    {
        return sequences;
    }

    public long getCount()
    {
        return count;
    }

    @Override
    public void halt()
    {
        running.set(false);
        for (SequenceBarrier barrier : barriers)
        {
            barrier.alert();
        }
    }

    @Override
    public boolean isRunning()
    {
        return running.get();
    }

    public void setExceptionHandler(final ExceptionHandler exceptionHandler)
    {
        if (null == exceptionHandler)
        {
            throw new NullPointerException();
        }

        this.exceptionHandler = exceptionHandler;
    }

    private void notifyTimeout(final int currentIdx, final long availableSequence)
    {
        try
        {
            if (currentIdx != -1 && eventHandlers[currentIdx] instanceof TimeoutHandler)
            {
                ((TimeoutHandler)eventHandlers[currentIdx]).onTimeout(availableSequence);
            }
            else
            {

                final int barrierLength = barriers.length;
                for (int i = 0; i < barrierLength; i++)
                {
                    if (eventHandlers[i] instanceof TimeoutHandler)
                    {
                        ((TimeoutHandler)eventHandlers[i]).onTimeout(sequences[i].get());
                    }
                }
            }
        }
        catch (Throwable e)
        {
            exceptionHandler.handleEventException(e, availableSequence, null);
        }
    }


    /**
     * Notifies the EventHandler when this processor is starting up
     */
    private void notifyStart()
    {
        for (EventHandler eventHandler : eventHandlers)
        {
            if (eventHandler instanceof LifecycleAware)
            {
                try
                {
                    ((LifecycleAware) eventHandler).onStart();
                }
                catch (final Throwable ex)
                {
                    exceptionHandler.handleOnStartException(ex);
                }
            }
        }
    }

    /**
     * Notifies the EventHandler immediately prior to this processor shutting down
     */
    private void notifyShutdown()
    {
        for (EventHandler eventHandler : eventHandlers)
        {
            if (eventHandler instanceof LifecycleAware)
            {
                try
                {
                    ((LifecycleAware) eventHandler).onShutdown();
                }
                catch (final Throwable ex)
                {
                    exceptionHandler.handleOnShutdownException(ex);
                }
            }
        }
    }


}

