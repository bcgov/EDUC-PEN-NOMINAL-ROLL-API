package ca.bc.gov.educ.pen.nominalroll.api.support;

import io.nats.client.Connection;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@Profile("testWebclient")
public class MockConfigWebClient {

  /**
   * Message publisher message publisher.
   *
   * @return the message publisher
   */
  @Bean
  @Primary
  public WebClient webClient() {
    return Mockito.mock(WebClient.class);
  }


  @Bean
  @Primary
  public Connection connection() {
    return Mockito.mock(Connection.class);
  }

}
