package br.com.processor.app.usecases;

import br.com.processor.app.usecases.models.UploadFileMessage;
import br.com.processor.app.usecases.models.UploadQueueMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.nio.file.Path;
import java.util.List;

import static br.com.processor.enums.UploadFileStatus.UPLOAD_FAILURE;
import static br.com.processor.enums.UploadFileStatus.UPLOAD_SUCCESS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

class ProcessFileUseCaseTest {

  @Mock
  private BucketUseCase bucketUseCase;

  @Mock
  private EmailUseCase emailUseCase;

  @Mock
  private FileUseCase fileUseCase;

  @InjectMocks
  private ProcessFileUseCase useCase;

  @BeforeEach
  void setup() {
    openMocks(this);
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
    when(emailUseCase.sendEmail(any(), any(), any(), any())).thenReturn(Mono.empty());
    when(fileUseCase.processFile(any())).thenReturn(Mono.just(zipPath));

    StepVerifier.create(useCase.process(message))
      .expectNext(message)
      .verifyComplete();

    verify(bucketUseCase).getFile(any(), any(), any());
    verify(bucketUseCase).uploadFile(any(), any(), any());
    verify(fileUseCase).processFile(any());
    verify(emailUseCase, atLeastOnce()).sendEmail(eq(email), any(), contains("Success"), eq(zipPath));
  }

  @Test
  void shouldSendErrorEmailWhenStatusIsNotSuccess() {
    var fileName = "video.mp4";
    var email = "user@example.com";

    var uploadedFile = UploadFileMessage.builder()
      .fileName(fileName)
      .status(UPLOAD_FAILURE)
      .build();
    var message = new UploadQueueMessage(email, List.of(uploadedFile));

    when(emailUseCase.sendEmail(any(), any(), any(), isNull())).thenReturn(Mono.empty());

    StepVerifier.create(useCase.process(message))
      .expectNext(message)
      .verifyComplete();

    verify(bucketUseCase, never()).getFile(any(), any(), any());
    verify(emailUseCase).sendEmail(eq(email), any(), contains("Error"), isNull());
  }

  @Test
  void shouldHandleExceptionDuringProcessing() {
    var fileName = "video.mp4";
    var email = "user@example.com";
    var tempPath = Path.of("/tmp/user@example.com_video.mp4");

    var uploadedFile = UploadFileMessage.builder()
      .fileName(fileName)
      .status(UPLOAD_SUCCESS)
      .build();
    var message = new UploadQueueMessage(email, List.of(uploadedFile));

    when(bucketUseCase.getFile(any(), any(), any())).thenReturn(Mono.just(tempPath));
    when(bucketUseCase.uploadFile(any(), any(), any())).thenReturn(Mono.error(new RuntimeException("Upload failed")));
    when(emailUseCase.sendEmail(any(), any(), any(), any())).thenReturn(Mono.empty());

    StepVerifier.create(useCase.process(message))
      .expectNext(message)
      .verifyComplete();

    verify(emailUseCase, atLeastOnce()).sendEmail(eq(email), any(), contains("Error"), isNull());
  }
}
