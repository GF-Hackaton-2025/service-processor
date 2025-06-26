package br.com.processor.app.usecases;

import br.com.processor.app.exception.BusinessException;
import br.com.processor.app.usecases.models.UploadQueueMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static br.com.processor.enums.UploadFileStatus.UPLOAD_SUCCESS;
import static br.com.processor.utils.JsonUtils.toJson;
import static br.com.processor.webui.constants.Constants.UPLOADS_BUCKET_NAME;
import static java.nio.file.Files.createDirectories;
import static java.nio.file.Paths.get;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProcessFileUseCase {

  private final BucketUseCase bucketUseCase;
  private final EmailUseCase emailUseCase;

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
            .flatMap(this::processFile)
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

  private Mono<Path> processFile(Path file) {
    try {
      var baseName = file.getFileName().toString().replace(".mp4", "");
      var framesDir = get("/tmp", baseName + "_frames");
      var zipPath = get("/tmp", baseName + "_frames.zip");
      createDirectories(framesDir);
      var command = String.format("ffmpeg -i %s %s/frame_%%03d.jpg", file.toAbsolutePath(), framesDir.toAbsolutePath());

      return Mono.fromCallable(() -> {
          Process process = new ProcessBuilder(command.split(" "))
            .redirectErrorStream(true)
            .start();
          int exitCode = process.waitFor();
          if (exitCode != 0) throw new BusinessException("FFmpeg failed");
          return framesDir;
        }).subscribeOn(Schedulers.boundedElastic())
        .flatMap(dir -> zipDirectory(dir, zipPath));
    } catch (Exception e) {
      return Mono.error(e);
    }
  }

  private Mono<Path> zipDirectory(Path sourceDir, Path zipPath) {
    return Mono.fromCallable(() -> {
      try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipPath.toFile()))) {
        Files.walk(sourceDir)
            .filter(Files::isRegularFile)
            .forEach(file -> {
              ZipEntry zipEntry = new ZipEntry(sourceDir.relativize(file).toString());
              try (InputStream is = Files.newInputStream(file)) {
                zos.putNextEntry(zipEntry);
                is.transferTo(zos);
                zos.closeEntry();
              } catch (IOException e) {
                throw new UncheckedIOException(e);
              }
            });
        return zipPath;
      }
    }).subscribeOn(Schedulers.boundedElastic());
  }

}
