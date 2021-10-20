package ca.bc.gov.educ.pen.nominalroll.api.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Class holds all application properties
 *
 * @author Marco Villeneuve
 */
@Component
@Getter
@Setter
public class ApplicationProperties {
  public static final String API_NAME = "PEN_NOMINAL_ROLL_API";
  public static final String CORRELATION_ID = "correlationID";
  /**
   * The Client id.
   */
  @Value("${client.id}")
  private String clientID;
  /**
   * The Client secret.
   */
  @Value("${client.secret}")
  private String clientSecret;
  /**
   * The Token url.
   */
  @Value("${url.token}")
  private String tokenURL;

  @Value("${url.api.school}")
  private String schoolApiURL;
  @Value("${ramp.up.http}")
  private Boolean isHttpRampUp;
  @Value("${nats.url}")
  String natsUrl;

  @Value("${nats.maxReconnect}")
  Integer natsMaxReconnect;
  public boolean getIsHttpRampUp() {
    return isHttpRampUp != null && isHttpRampUp;
  }
}
