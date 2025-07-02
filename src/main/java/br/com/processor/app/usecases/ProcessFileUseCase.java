package br.com.processor.app.usecases;

import br.com.processor.app.ports.FileProcessorQueue;
import br.com.processor.app.usecases.models.FileProcessorQueueMessage;
import br.com.processor.app.usecases.models.UploadFileMessage;
import br.com.processor.app.usecases.models.UploadQueueMessage;
import br.com.processor.enums.FileStatusEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.file.Path;

import static br.com.processor.enums.FileStatusEnum.FAILED;
import static br.com.processor.enums.FileStatusEnum.PROCESSED;
import static br.com.processor.utils.JsonUtils.toJson;
import static br.com.processor.webui.constants.Constants.UPLOADS_BUCKET_NAME;
import static java.lang.System.getProperty;
import static java.nio.file.Files.createTempDirectory;
import static java.nio.file.Files.createTempFile;
import static java.nio.file.Files.deleteIfExists;
import static java.nio.file.Path.of;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProcessFileUseCase {

  private final BucketUseCase bucketUseCase;
  private final FileUseCase fileUseCase;
  private final FileProcessorQueue fileProcessorQueue;

  public Mono<UploadQueueMessage> process(UploadQueueMessage message) {
    log.info("Processing message: {}", toJson(message));
    return Flux.fromIterable(message.getFiles())
      .flatMap(file -> {
        var key = message.getEmail() + "/" + file.getFileName();
        return Mono.defer(() -> Mono.fromCallable(() -> {
            Path safeTempDir = createTempDirectory(of(getProperty("user.dir")), "processor_");
            return createTempFile(safeTempDir, "upload_", "_" + file.getFileName());
          })
          .flatMap(destinationPath -> this.bucketUseCase.getFile(UPLOADS_BUCKET_NAME, key, destinationPath)
            .flatMap(fileUseCase::processFile)
            .flatMap(zipFile -> this.bucketUseCase.uploadFile(UPLOADS_BUCKET_NAME, key.replace(".mp4", "_frames.zip"), zipFile))
            .doFinally(signal -> this.deleteTempFile(destinationPath))
            .flatMap(path -> this.fileProcessorQueue.sendMessage(toJson(createQueueMessage(message.getEmail(), file, PROCESSED))))
            .onErrorResume(error -> this.fileProcessorQueue.sendMessage(toJson(createQueueMessage(message.getEmail(), file, FAILED)))
              .then(Mono.error(error)))
            .doOnSuccess(path -> log.info("File processed successfully: {}", file.getFileName()))
            .doOnError(error -> log.error("Error processing file: {}", error.getMessage()))
          ));
      })
      .then(Mono.just(message));
  }

  private FileProcessorQueueMessage createQueueMessage(String email, UploadFileMessage file, FileStatusEnum statusEnum) {
    return FileProcessorQueueMessage.builder()
      .email(email)
      .fileId(file.getFileId())
      .fileName(file.getFileName())
      .zipFileName(file.getFileName().replace(".mp4", "_frames.zip"))
      .status(statusEnum)
      .build();
  }

  private void deleteTempFile(Path destinationPath) {
    Mono.fromRunnable(() -> {
      try {
        deleteIfExists(destinationPath);
        deleteIfExists(destinationPath.getParent());
      } catch (Exception e) {
        log.error("Error deleting temporary file: {}", e.getMessage());
      }
    }).subscribe();
  }

}
