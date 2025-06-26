package br.com.processor.app.usecases;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.nio.file.Path;

import static software.amazon.awssdk.core.async.AsyncRequestBody.fromFile;
import static software.amazon.awssdk.core.async.AsyncResponseTransformer.toFile;

@Service
@RequiredArgsConstructor
public class BucketUseCase {

  private final S3AsyncClient s3AsyncClient;

  private static final Logger log = LoggerFactory.getLogger(BucketUseCase.class);

  public Mono<Path> getFile(String bucket, String key, Path destinationPath) {
    var request = GetObjectRequest.builder()
      .bucket(bucket)
      .key(key)
      .build();

    return Mono.fromFuture(s3AsyncClient.getObject(request, toFile(destinationPath)))
      .thenReturn(destinationPath)
      .doOnSuccess(path -> log.info("Get file successfully: {}", path))
      .doOnError(error -> log.error("Error processing get file: {}", error.getMessage()));
  }

  public Mono<Path> uploadFile(String bucket, String key, Path filePath) {
    var request = PutObjectRequest.builder()
      .bucket(bucket)
      .key(key)
      .build();

    return Mono.fromFuture(s3AsyncClient.putObject(request, fromFile(filePath)))
      .doOnSuccess(path -> log.info("Upload file successfully: {}", path))
      .doOnError(error -> log.error("Error processing upload file: {}", error.getMessage()))
      .then(Mono.just(filePath));
  }

}
