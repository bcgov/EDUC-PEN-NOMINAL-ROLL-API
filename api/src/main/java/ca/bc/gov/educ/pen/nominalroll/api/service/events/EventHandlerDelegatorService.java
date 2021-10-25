package ca.bc.gov.educ.pen.nominalroll.api.service.events;

import ca.bc.gov.educ.pen.nominalroll.api.messaging.MessagePublisher;
import ca.bc.gov.educ.pen.nominalroll.api.model.v1.NominalRollEvent;
import ca.bc.gov.educ.pen.nominalroll.api.struct.v1.Event;
import io.nats.client.Message;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Optional;

import static lombok.AccessLevel.PRIVATE;

/**
 * The type Event handler service.
 */
@Service
@Slf4j
public class EventHandlerDelegatorService {
  /**
   * The constant PAYLOAD_LOG.
   */
  public static final String PAYLOAD_LOG = "Payload is :: {}";
  /**
   * The Event handler service.
   */
  @Getter
  private final EventHandlerService eventHandlerService;

  /**
   * The Message publisher.
   */
  @Getter(PRIVATE)
  private final MessagePublisher messagePublisher;

  /**
   * Instantiates a new Event handler delegator service.
   *
   * @param eventHandlerService the event handler service
   * @param messagePublisher    the message publisher
   */
  @Autowired
  public EventHandlerDelegatorService(final EventHandlerService eventHandlerService, final MessagePublisher messagePublisher) {
    this.eventHandlerService = eventHandlerService;
    this.messagePublisher = messagePublisher;
  }

  /**
   * Handle event.
   *
   * @param event   the event
   * @param message the message
   */
  @Async("subscriberExecutor")
  public void handleEvent(final Event event, final Message message) {
    final boolean isSynchronous = message.getReplyTo() != null;
    final byte[] response;
    try {
      switch (event.getEventType()) {
        case CREATE_DIA_STUDENTS:
          log.info("received get next pen number event :: ");
          log.trace(PAYLOAD_LOG, event.getEventPayload());
          response = this.getEventHandlerService().handleCreateDIAStudents(event);
          this.publishToNATS(event, message, isSynchronous, response);
          break;
        default:
          log.info("silently ignoring other event :: {}", event);
          break;
      }
    } catch (final Exception e) {
      log.error("Exception", e);
    }
  }

  /**
   * Publish to nats.
   *
   * @param event         the event
   * @param message       the message
   * @param isSynchronous the is synchronous
   * @param response      the response
   */
  private void publishToNATS(final Event event, final Message message, final boolean isSynchronous, final byte[] response) {
    if (isSynchronous) { // this is for synchronous request/reply pattern.
      this.getMessagePublisher().dispatchMessage(message.getReplyTo(), response);
    } else { // this is for async.
      this.getMessagePublisher().dispatchMessage(event.getReplyTo(), response);
    }
  }

}
