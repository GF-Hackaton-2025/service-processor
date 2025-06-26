package br.com.processor.app.usecases;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

import static java.nio.file.Files.deleteIfExists;
import static java.nio.file.Files.writeString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

class BucketUseCaseTest {

  @Mock
  private S3AsyncClient s3AsyncClient;

  @InjectMocks
  private BucketUseCase bucketUseCase;

  @BeforeEach
  void setup() {
    openMocks(this);
  }

  @Test
  void shouldUploadFileSuccessfully() throws Exception {
    Path tempFile = Files.createTempFile("test", ".txt");
    writeString(tempFile, "Content Test");

    CompletableFuture<PutObjectResponse> future = CompletableFuture.completedFuture(PutObjectResponse.builder().build());
    when(s3AsyncClient.putObject(any(PutObjectRequest.class), any(AsyncRequestBody.class)))
      .thenReturn(future);

    Mono<Path> result = bucketUseCase.uploadFile("my-bucket", "key.txt", tempFile);

    StepVerifier.create(result)
      .expectNext(tempFile)
      .verifyComplete();

    verify(s3AsyncClient, times(1)).putObject((PutObjectRequest) any(), (AsyncRequestBody) any());

    deleteIfExists(tempFile);
  }

  @Test
  void shouldGetFileSuccessfully() {
    Path destination = Path.of("/tmp/downloaded.txt");

    CompletableFuture<Path> future = CompletableFuture.completedFuture(destination);
    when(s3AsyncClient.getObject(any(GetObjectRequest.class), any(AsyncResponseTransformer.class)))
      .thenReturn(future);

    Mono<Path> result = bucketUseCase.getFile("my-bucket", "key.txt", destination);

    StepVerifier.create(result)
      .expectNext(destination)
      .verifyComplete();

    verify(s3AsyncClient, times(1)).getObject((GetObjectRequest) any(), (AsyncResponseTransformer) any());
  }
}
