package ca.bc.gov.educ.pen.nominalroll.api.rest;

import ca.bc.gov.educ.pen.nominalroll.api.properties.ApplicationProperties;
import ca.bc.gov.educ.pen.nominalroll.api.struct.external.school.v1.FedProvSchoolCodes;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import javax.annotation.PostConstruct;
import java.util.List;

/**
 * The type Rest utils.
 */
@Component
@Slf4j
public class RestUtils {
  /**
   * The constant CONTENT_TYPE.
   */
  public static final String CONTENT_TYPE = "Content-Type";
  private final ApplicationProperties props;

  /**
   * The Web client.
   */
  private final WebClient webClient;

  /**
   * Instantiates a new Rest utils.
   *
   * @param props     the props
   * @param webClient the web client
   */
  public RestUtils(@Autowired final ApplicationProperties props, final WebClient webClient) {
    this.props = props;
    this.webClient = webClient;
  }


  /**
   * Init. let the main thread be free for starting up.
   */
  @PostConstruct
  public void init() {
    if (this.props.getIsHttpRampUp()) {
      val fedProvSchoolCodes = this.getFedProvSchoolCodes();
      log.info("got {} fed-prov school codes from school api.", fedProvSchoolCodes.size());
    }
  }


  /**
   * Gets latest pen number from student api.
   *
   * @return the latest pen number from student api
   */
  @Retryable(value = {Exception.class}, backoff = @Backoff(multiplier = 2, delay = 2000))
  public List<FedProvSchoolCodes> getFedProvSchoolCodes() {
    return this.webClient.get()
      .uri(this.props.getSchoolApiURL().concat("/federal-province-codes"))
      .header(CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
      .retrieve().bodyToFlux(FedProvSchoolCodes.class).buffer().blockLast();
  }
}
