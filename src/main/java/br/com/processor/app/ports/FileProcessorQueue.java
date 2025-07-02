package br.com.processor.app.ports;

import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

public interface FileProcessorQueue {

  Mono<SendMessageResponse> sendMessage(String body);

}
