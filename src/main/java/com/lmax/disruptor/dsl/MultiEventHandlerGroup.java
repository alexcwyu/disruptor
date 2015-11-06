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
package com.lmax.disruptor.dsl;

import com.lmax.disruptor.Sequence;

/**
 * A group of {@link MultiEventHandlerGroup}s used as part of the {@link Disruptor}.
 *
 * @param <T> the type of entry used by the event processors.
 */
public class MultiEventHandlerGroup<T> extends EventHandlerGroup<T>
{
    private final MultiEventDisruptor<T> multiEventDisruptor;

    MultiEventHandlerGroup(final MultiEventDisruptor<T> disruptor,
                           final ConsumerRepository<T> consumerRepository,
                           final Sequence[] sequences)
    {
        super(disruptor, consumerRepository, sequences);
        this.multiEventDisruptor = disruptor;
    }


    public EventHandlerGroup<T> and(final EventHandlerGroup<T> otherHandlerGroup)
    {
        final Sequence[] combinedSequences = new Sequence[this.sequences.length + otherHandlerGroup.sequences.length];
        System.arraycopy(this.sequences, 0, combinedSequences, 0, this.sequences.length);
        System.arraycopy(otherHandlerGroup.sequences, 0, combinedSequences, this.sequences.length, otherHandlerGroup.sequences.length);
        return new MultiEventHandlerGroup<T>(multiEventDisruptor, consumerRepository, combinedSequences);
    }

    public EventHandlerGroup<T> and(final MultiEventHandlerInfo<T>... infos)
    {
        return and(multiEventDisruptor.handleEventsWith(infos));
    }

    public MultiEventHandlerGroup<T> then(final MultiEventHandlerInfo<T>... infos)
    {
        return handleEventsWith(infos);
    }

    public MultiEventHandlerGroup<T> handleEventsWith(final MultiEventHandlerInfo<T> ... infos)
    {
        return multiEventDisruptor.updateEventProcessors(sequences, infos);
    }
}
