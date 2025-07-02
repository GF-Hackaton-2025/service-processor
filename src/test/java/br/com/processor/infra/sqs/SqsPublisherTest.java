package br.com.processor.infra.sqs;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import reactor.test.StepVerifier;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

import java.util.Objects;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

class SqsPublisherTest {

  @Mock
  private SqsAsyncClient sqsClient;

  @InjectMocks
  private SqsPublisher sqsPublisher;

  private AutoCloseable closeable;

  @BeforeEach
  void setUp() {
    closeable = openMocks(this);
  }

  @AfterEach
  void closeService() throws Exception {
    closeable.close();
  }

  @Test
  void sendMessage_shouldSendMessageAndReturnResponse() {
    SendMessageResponse response = SendMessageResponse.builder().messageId("123").build();
    when(sqsClient.sendMessage(any(SendMessageRequest.class)))
      .thenReturn(completedFuture(response));

    StepVerifier.create(sqsPublisher.sendMessage("test-message-body"))
      .expectNextMatches(Objects::nonNull)
      .verifyComplete();

    verify(sqsClient, times(1)).sendMessage(any(SendMessageRequest.class));
  }

}
