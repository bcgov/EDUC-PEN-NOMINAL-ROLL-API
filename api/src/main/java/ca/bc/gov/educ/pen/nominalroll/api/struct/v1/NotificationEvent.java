package ca.bc.gov.educ.pen.nominalroll.api.struct.v1;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * The type Notification event.
 */
@EqualsAndHashCode(callSuper = true)
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@ToString(callSuper = true)
public class NotificationEvent extends Event {
  /**
   * The Saga status.
   */
  private String sagaStatus;
  /**
   * The Saga name.
   */
  private String sagaName;
}
