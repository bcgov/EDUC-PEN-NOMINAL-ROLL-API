package ca.bc.gov.educ.pen.nominalroll.api.properties;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.jboss.threads.EnhancedQueueExecutor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.Executor;

/**
 * Class holds all application properties
 *
 * @author Marco Villeneuve
 */
@Component
@Getter
@Setter
public class ApplicationProperties {

  public static final Executor bgTask = new EnhancedQueueExecutor.Builder()
          .setThreadFactory(new ThreadFactoryBuilder().setNameFormat("bg-task-executor-%d").build())
          .setCorePoolSize(1).setMaximumPoolSize(1).setKeepAliveTime(Duration.ofSeconds(60)).build();

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

  @Value("${url.api.student}")
  private String studentApiURL;

  @Value("${url.api.institute}")
  private String instituteApiURL;
  @Value("${ramp.up.http}")
  private Boolean isHttpRampUp;
  @Value("${nats.server}")
  private String server;
  @Value("${nats.maxReconnect}")
  private int maxReconnect;
  @Value("NOMINAL-ROLL-API")
  private String connectionName;
  @Value("${nom.roll.field.invalid.threshold}")
  private Integer nominalRollInvalidFieldThreshold;

  @Value("${folder.base.path}")
  private String folderBasePath;
  @Value("${pause.time.before.burst.message}")
  private Integer pauseTimeBeforeBurstOfMessageInMillis;

  public String getFolderBasePath() {
    return StringUtils.isBlank(this.folderBasePath) ? "/temp" : this.folderBasePath;
  }

  public void setFolderBasePath(String folderBasePath) {
    this.folderBasePath = folderBasePath;
  }

  public boolean getIsHttpRampUp() {
    return isHttpRampUp != null && isHttpRampUp;
  }
}
