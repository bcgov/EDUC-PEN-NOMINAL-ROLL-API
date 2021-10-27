package ca.bc.gov.educ.pen.nominalroll.api.config;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.jboss.threads.EnhancedQueueExecutor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableAsync;

import java.time.Duration;
import java.util.concurrent.Executor;

@Configuration
@EnableAsync
@Profile("!test")
public class AsyncConfiguration {
  /**
   * Thread pool task executor executor.
   *
   * @return the executor
   */
  @Bean(name = "subscriberExecutor")
  public Executor threadPoolTaskExecutor() {
    return new EnhancedQueueExecutor.Builder()
      .setThreadFactory(new ThreadFactoryBuilder().setNameFormat("message-subscriber-%d").build())
      .setCorePoolSize(6).setMaximumPoolSize(6).setKeepAliveTime(Duration.ofSeconds(60)).build();
  }

  @Bean(name = "publisherExecutor")
  public Executor publisherExecutor() {
    return new EnhancedQueueExecutor.Builder()
      .setThreadFactory(new ThreadFactoryBuilder().setNameFormat("message-publisher-%d").build())
      .setCorePoolSize(4).setMaximumPoolSize(4).setKeepAliveTime(Duration.ofSeconds(60)).build();
  }

  /**
   * Controller task executor executor.
   *
   * @return the executor
   */
  @Bean(name = "taskExecutor")
  public Executor controllerTaskExecutor() {
    return new EnhancedQueueExecutor.Builder()
      .setThreadFactory(new ThreadFactoryBuilder().setNameFormat("async-executor-%d").build())
      .setCorePoolSize(4).setMaximumPoolSize(4).setKeepAliveTime(Duration.ofSeconds(60)).build();
  }

}
