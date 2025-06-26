package br.com.processor.app.exception;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {

  public BusinessException(String message) {
    super(message);
  }

}
