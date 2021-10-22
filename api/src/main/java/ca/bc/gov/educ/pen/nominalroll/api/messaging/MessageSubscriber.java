package ca.bc.gov.educ.pen.nominalroll.api.messaging;

import ca.bc.gov.educ.pen.nominalroll.api.orchestrator.base.EventHandler;
import ca.bc.gov.educ.pen.nominalroll.api.struct.v1.Event;
import ca.bc.gov.educ.pen.nominalroll.api.util.JsonUtil;
import io.nats.client.Connection;
import io.nats.client.Message;
import io.nats.client.MessageHandler;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static lombok.AccessLevel.PRIVATE;

/**
 * The type Message subscriber.
 */
@Component
@Slf4j
public class MessageSubscriber {

  /**
   * The Event Handlers as orchestrator for SAGA
   */
  @Getter(PRIVATE)
  private final Map<String, EventHandler> handlerMap = new HashMap<>();

  /**
   * The Connection.
   */
  private final Connection connection;

  /**
   * Instantiates a new Message subscriber.
   *
   * @param con                          the con
   * @param eventHandlers                the event handlers
   */
  @Autowired
  public MessageSubscriber(final Connection con, final List<EventHandler> eventHandlers) {
    this.connection = con;
    eventHandlers.forEach(handler -> {
      this.handlerMap.put(handler.getTopicToSubscribe(), handler);
    });
  }

  /**
   * On message, event handler for SAGA
   *
   * @param eventHandler the orchestrator
   * @return the message handler
   */
  private static MessageHandler onMessageForSAGA(final EventHandler eventHandler) {
    return (Message message) -> {
      if (message != null) {
        log.info("Message received subject :: {},  replyTo :: {}, subscriptionID :: {}", message.getSubject(), message.getReplyTo(), message.getSID());
        try {
          final var eventString = new String(message.getData());
          final var event = JsonUtil.getJsonObjectFromString(Event.class, eventString);
          eventHandler.handleEvent(event);
        } catch (final Exception e) {
          log.error("Exception ", e);
        }
      }
    };
  }

}
