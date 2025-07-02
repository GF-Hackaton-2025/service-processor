package br.com.processor.app.usecases;

import br.com.processor.app.ports.FileProcessorQueue;
import br.com.processor.app.usecases.models.UploadFileMessage;
import br.com.processor.app.usecases.models.UploadQueueMessage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.nio.file.Path;
import java.util.List;

import static br.com.processor.enums.UploadFileStatus.UPLOAD_SUCCESS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

class ProcessFileUseCaseTest {

  @Mock
  private BucketUseCase bucketUseCase;

  @Mock
  private FileUseCase fileUseCase;

  @Mock
  private FileProcessorQueue fileProcessorQueue;

  @InjectMocks
  private ProcessFileUseCase useCase;

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
  void shouldProcessValidFileSuccessfully() {
    var fileName = "video.mp4";
    var email = "user@example.com";
    var tempPath = Path.of("/tmp/user@example.com_video.mp4");
    var zipPath = Path.of("/tmp/video_frames.zip");

    var uploadedFile = UploadFileMessage.builder()
      .fileName(fileName)
      .status(UPLOAD_SUCCESS)
      .build();
    var message = new UploadQueueMessage(email, List.of(uploadedFile));

    when(bucketUseCase.getFile(any(), any(), any())).thenReturn(Mono.just(tempPath));
    when(bucketUseCase.uploadFile(any(), any(), any())).thenReturn(Mono.just(zipPath));
    when(fileUseCase.processFile(any())).thenReturn(Mono.just(zipPath));
    when(fileProcessorQueue.sendMessage(any())).thenReturn(Mono.empty());

    StepVerifier.create(useCase.process(message))
      .expectNext(message)
      .verifyComplete();

    verify(bucketUseCase).getFile(any(), any(), any());
    verify(bucketUseCase).uploadFile(any(), any(), any());
    verify(fileUseCase).processFile(any());
    verify(fileProcessorQueue).sendMessage(contains("\"status\":\"PROCESSED\""));
  }

  @Test
  void shouldHandleExceptionDuringProcessing() {
    var fileName = "video.mp4";
    var email = "user@example.com";
    var tempPath = Path.of("/tmp/user@example.com_video.mp4");
    var zipPath = Path.of("/tmp/video_frames.zip");

    var uploadedFile = UploadFileMessage.builder()
      .fileName(fileName)
      .status(UPLOAD_SUCCESS)
      .build();
    var message = new UploadQueueMessage(email, List.of(uploadedFile));

    when(bucketUseCase.getFile(any(), any(), any())).thenReturn(Mono.just(tempPath));
    when(fileUseCase.processFile(any())).thenReturn(Mono.just(zipPath));
    when(bucketUseCase.uploadFile(any(), any(), any())).thenReturn(Mono.error(new RuntimeException("Upload failed")));
    when(fileProcessorQueue.sendMessage(any())).thenReturn(Mono.empty());

    StepVerifier.create(useCase.process(message))
      .expectErrorMatches(throwable -> throwable instanceof RuntimeException && throwable.getMessage().equals("Upload failed"))
      .verify();

    verify(fileProcessorQueue).sendMessage(contains("\"status\":\"FAILED\""));
  }
}
