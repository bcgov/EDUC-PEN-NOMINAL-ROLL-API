package ca.bc.gov.educ.pen.nominalroll.api.messaging;

import ca.bc.gov.educ.pen.nominalroll.api.helpers.LogHelper;
import ca.bc.gov.educ.pen.nominalroll.api.orchestrator.base.EventHandler;
import ca.bc.gov.educ.pen.nominalroll.api.struct.v1.Event;
import ca.bc.gov.educ.pen.nominalroll.api.util.JsonUtil;
import io.nats.client.Connection;
import io.nats.client.Message;
import io.nats.client.MessageHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * The type Message subscriber.
 */
@Component
@Slf4j
@SuppressWarnings("java:S2142")
public class MessageSubscriber {


  private final Connection connection;
  private final List<String> topics = new ArrayList<>();

  @Autowired
  public MessageSubscriber(final Connection con, final List<EventHandler> eventHandlers) {
    this.connection = con;
    eventHandlers.forEach(this::subscribe);
  }

  public void subscribe(final EventHandler eventHandler) {
    if (topics.contains(eventHandler.getTopicToSubscribe())) {
      throw new IllegalArgumentException("Topic already subscribed");
    } else {
      topics.add(eventHandler.getTopicToSubscribe());
    }
    final String queue = eventHandler.getTopicToSubscribe().replace("_", "-");
    final var dispatcher = this.connection.createDispatcher(this.onMessage(eventHandler));
    dispatcher.subscribe(eventHandler.getTopicToSubscribe(), queue);
  }

  /**
   * On message handler.
   *
   * @return the message handler
   */
  public MessageHandler onMessage(final EventHandler eventHandler) {
    return (Message message) -> {
      if (message != null) {
        log.info("Message received subject :: {},  replyTo :: {}, subscriptionID :: {}", message.getSubject(), message.getReplyTo(), message.getSID());
        try {
          final var eventString = new String(message.getData());
          LogHelper.logMessagingEventDetails(eventString);
          final var event = JsonUtil.getJsonObjectFromString(Event.class, eventString);
          eventHandler.handleEvent(event);
        } catch (final Exception e) {
          log.error("Exception ", e);
        }
      }
    };
  }
}
