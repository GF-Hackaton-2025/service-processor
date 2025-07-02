package br.com.processor.app.usecases.models;

import br.com.processor.enums.FileStatusEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileProcessorQueueMessage {

  private String email;
  private String fileId;
  private String fileName;
  private String zipFileName;
  private FileStatusEnum status;

}
