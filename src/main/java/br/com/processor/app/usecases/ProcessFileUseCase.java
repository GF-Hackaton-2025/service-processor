package br.com.processor.app.usecases;

import br.com.processor.app.usecases.models.UploadQueueMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.file.Path;

import static br.com.processor.enums.UploadFileStatus.UPLOAD_SUCCESS;
import static br.com.processor.utils.JsonUtils.toJson;
import static br.com.processor.webui.constants.Constants.UPLOADS_BUCKET_NAME;
import static java.nio.file.Paths.get;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProcessFileUseCase {

  private final BucketUseCase bucketUseCase;
  private final EmailUseCase emailUseCase;
  private final FileUseCase fileUseCase;

  public Mono<UploadQueueMessage> process(UploadQueueMessage message) {
    log.info("Processing message: {}", toJson(message));
    return Flux.fromIterable(message.getFiles())
      .flatMap(file -> {
        var key = message.getEmail() + "/" + file.getFileName();
        var destinationPath = get("/tmp", key.replace("/", "_"));
        return Mono.defer(() -> {
          if (!UPLOAD_SUCCESS.equals(file.getStatus()))
            return this.sendEmailNotification(message.getEmail(), String.format("Error to process file: %s", file.getFileName()), null);

          return this.bucketUseCase.getFile(UPLOADS_BUCKET_NAME, key, destinationPath)
            .flatMap(fileUseCase::processFile)
            .flatMap(zipFile -> this.bucketUseCase.uploadFile(UPLOADS_BUCKET_NAME, key.replace(".mp4", "_frames.zip"), zipFile))
            .doOnSuccess(path -> log.info("File processed successfully: {}", file.getFileName()))
            .doOnError(error -> log.error("Error processing file: {}", error.getMessage()))
            .flatMap(zipFile -> this.sendEmailNotification(message.getEmail(), String.format("Success to process file: %s", file.getFileName()), zipFile))
            .onErrorResume(error -> this.sendEmailNotification(message.getEmail(), String.format("Error to process file: %s", file.getFileName()), null));
        });
      })
      .then(Mono.just(message));
  }

  private Mono<Void> sendEmailNotification(String email, String message, Path zipFile) {
    emailUseCase.sendEmail(email, "File processor", message, zipFile)
      .subscribe();

    return Mono.empty();
  }

}
