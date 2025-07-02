package br.com.processor.infra.sqs;

import br.com.processor.app.ports.FileProcessorQueue;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

import static java.lang.String.format;

@Component
@RequiredArgsConstructor
@Slf4j
public class SqsPublisher implements FileProcessorQueue {

  private final SqsAsyncClient sqsClient;

  @Value("${aws.sqs.fileProcessorQueueUrl}")
  private String queueUrl;

  @Override
  public Mono<SendMessageResponse> sendMessage(String body) {
    var request = createSendMessageRequest(body);
    log.info(format("Sending message to SQS. Body: %s", request.messageBody()));
    return Mono.fromFuture(() -> sqsClient.sendMessage(request))
      .doOnSuccess(response -> log.info(format("Message sent successfully with ID: %s", response.messageId())))
      .doOnError(error -> log.error("Error sending message to SQS", error));
  }

  private SendMessageRequest createSendMessageRequest(String body) {
    return SendMessageRequest.builder()
      .queueUrl(queueUrl)
      .messageBody(body)
      .build();
  }

}
