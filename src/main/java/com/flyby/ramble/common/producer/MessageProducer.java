package com.flyby.ramble.common.producer;

public interface MessageProducer {
    void send(String topic, Object message);
}
