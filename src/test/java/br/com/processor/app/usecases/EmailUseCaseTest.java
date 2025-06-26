package br.com.processor.app.usecases;

import br.com.processor.app.exception.BusinessException;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.mail.javamail.JavaMailSender;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

class EmailUseCaseTest {

  @Mock
  private JavaMailSender mailSender;

  @InjectMocks
  private EmailUseCase emailUseCase;

  @BeforeEach
  void setup() {
    openMocks(this);
  }

  @Test
  void shouldSendEmailWithoutAttachment() {
    MimeMessage mimeMessage = mock(MimeMessage.class);
    when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

    emailUseCase.sendEmail("test@example.com", "Subject", "Body", null).block();

    verify(mailSender).send(mimeMessage);
  }

  @Test
  void shouldSendEmailWithAttachment() throws Exception {
    MimeMessage mimeMessage = mock(MimeMessage.class);
    when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

    Path tempFile = Files.createTempFile("attachment", ".txt");
    Files.writeString(tempFile, "Test attachment content");

    emailUseCase.sendEmail("test@example.com", "Subject", "Body", tempFile).block();

    verify(mailSender).send(mimeMessage);

    Files.deleteIfExists(tempFile);
  }

  @Test
  void shouldThrowBusinessExceptionWhenMessagingFails() {
    when(mailSender.createMimeMessage()).thenThrow(new RuntimeException("Simulated failure"));

    Runnable sendEmail = () -> emailUseCase.sendEmail("test@example.com", "Subject", "Body", null).block();

    assertThrows(BusinessException.class, sendEmail::run);
  }
}

