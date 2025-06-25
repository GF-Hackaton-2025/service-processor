package br.com.processor.utils;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonUtilsTest {

  @Test
  void toJson_shouldReturnJsonString_whenObjectIsValid() {
    Map<String, Object> map = new HashMap<>();
    map.put("key", "value");
    String json = JsonUtils.toJson(map);
    assertNotNull(json);
    assertTrue(json.contains("\"key\":\"value\""));
  }

  @Test
  void toJson_shouldReturnNull_whenObjectIsNotSerializable() {
    Object notSerializable = new Object() {
      private void writeObject(java.io.ObjectOutputStream out) throws java.io.IOException {
        throw new java.io.NotSerializableException();
      }
    };
    String json = JsonUtils.toJson(notSerializable);
    assertNull(json);
  }
}

