package br.com.processor.app.usecases.models;

import br.com.processor.enums.UploadFileStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UploadFileMessage {

  private String fileId;
  private String fileName;
  private UploadFileStatus status;

}
