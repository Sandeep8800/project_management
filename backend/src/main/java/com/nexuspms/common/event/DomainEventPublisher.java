package com.nexuspms.common.event;

/**
 * The single abstraction every module publishes cross-module coordination events
 * through (HLD S8.6). Implemented via Spring's ApplicationEventPublisher for v1 --
 * no module calls another module's services directly to react to a state change,
 * and no module reaches for a different mechanism (direct method calls into
 * another module's internals) to get an effect that belongs here instead.
 *
 * Swapping the implementation for an external broker (Kafka/RabbitMQ) later, if a
 * module is ever extracted into its own service (HLD S2), is a change to this
 * interface's implementation only -- not a rewrite of every publisher/consumer.
 */
public interface DomainEventPublisher {

    void publish(Object domainEvent);
}
