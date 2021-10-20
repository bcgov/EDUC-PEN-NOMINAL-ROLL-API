package ca.bc.gov.educ.pen.nominalroll.api.support;


import ca.bc.gov.educ.pen.nominalroll.api.rest.RestUtils;
import io.nats.client.Connection;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

@Profile("test")
@Configuration
public class MockConfiguration {


  @Bean
  @Primary
  public Connection connection() {
    return Mockito.mock(Connection.class);
  }
  @Bean
  @Primary
  public RestUtils restUtils() {
    return Mockito.mock(RestUtils.class);
  }

}