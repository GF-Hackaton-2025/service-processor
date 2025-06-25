package br.com.processor.app.usecases.models;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UploadQueueMessage {

  private String email;

  @Builder.Default
  private List<FileUploadMessage> files = new ArrayList<>();

}
