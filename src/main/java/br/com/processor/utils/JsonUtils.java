package br.com.processor.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.NoArgsConstructor;

import static lombok.AccessLevel.PRIVATE;

@NoArgsConstructor(access = PRIVATE)
public class JsonUtils {

  public static String toJson(Object obj) {
    try {
      return new ObjectMapper().writeValueAsString(obj);
    } catch (Exception e) {
      return null;
    }
  }

  public static <T> T fromJson(String json, Class<T> clazz) {
    try {
      return new ObjectMapper().readValue(json, clazz);
    } catch (Exception e) {
      return null;
    }
  }
}
