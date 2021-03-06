package ca.bc.gov.educ.pen.nominalroll.api.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.Optional;

/**
 * The type Json util.
 */
@Slf4j
public class JsonUtil {
  public static final ObjectMapper mapper = new ObjectMapper();

  private JsonUtil() {
  }

  /**
   * Gets json string from object.
   *
   * @param payload the payload
   * @return the json string from object
   * @throws JsonProcessingException the json processing exception
   */
  public static String getJsonStringFromObject(final Object payload) throws JsonProcessingException {
    return mapper.writeValueAsString(payload);
  }

  /**
   * Gets json object from string.
   *
   * @param <T>     the type parameter
   * @param clazz   the clazz
   * @param payload the payload
   * @return the json object from string
   * @throws JsonProcessingException the json processing exception
   */
  public static <T> T getJsonObjectFromString(final Class<T> clazz, final String payload) throws JsonProcessingException {
    return mapper.readValue(payload, clazz);
  }

  /**
   * Get json bytes from object byte [ ].
   *
   * @param payload the payload
   * @return the byte [ ]
   * @throws JsonProcessingException the json processing exception
   */
  public static byte[] getJsonBytesFromObject(final Object payload) throws JsonProcessingException {
    return mapper.writeValueAsBytes(payload);
  }

  /**
   * Get object from json byte [ ].
   *
   * @param <T>     the type parameter
   * @param clazz   the clazz
   * @param payload the byte [ ]
   * @return the json object from byte []
   * @throws JsonProcessingException the json processing exception
   */
  public static <T> T getObjectFromJsonBytes(final Class<T> clazz, final byte[] payload) throws IOException {
    return mapper.readValue(payload, clazz);
  }

  /**
   * Get json string optional.
   *
   * @param payload the payload
   * @return the optional
   */
  public static Optional<String> getJsonString(final Object payload) {
    try {
      return Optional.ofNullable(mapper.writeValueAsString(payload));
    } catch (final Exception ex) {
      log.error("Exception while converting object to JSON String :: {}", payload);
    }
    return Optional.empty();
  }
}
