package ca.bc.gov.educ.pen.nominalroll.api.service.events;

import ca.bc.gov.educ.pen.nominalroll.api.constants.TopicsEnum;
import ca.bc.gov.educ.pen.nominalroll.api.messaging.MessagePublisher;
import ca.bc.gov.educ.pen.nominalroll.api.orchestrator.base.EventHandler;
import ca.bc.gov.educ.pen.nominalroll.api.struct.v1.Event;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import static lombok.AccessLevel.PRIVATE;

/**
 * The type Event handler service.
 */
@Service
@Slf4j
public class EventHandlerDelegatorService implements EventHandler {
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
   * @param event the event
   */
  @Async("subscriberExecutor")
  @Override
  public void handleEvent(final Event event) {
    final byte[] response;
    try {
      switch (event.getEventType()) {
        case CREATE_DIA_STUDENTS:
          log.info("received get next pen number event :: ");
          log.trace(PAYLOAD_LOG, event.getEventPayload());
          response = this.getEventHandlerService().handleCreateDIAStudents(event);
          this.publishToNATS(event, response);
          break;
        case READ_FROM_TOPIC:
          log.info("received read from topic event :: ");
          log.trace(PAYLOAD_LOG, event.getEventPayload());
          this.getEventHandlerService().handleReadFromTopicEvent(event); // no response in this event.
          break;
        default:
          log.info("silently ignoring other event :: {}", event);
          break;
      }
    } catch (final Exception e) {
      log.error("Exception", e);
    }
  }

  @Override
  public String getTopicToSubscribe() {
    return TopicsEnum.NOMINAL_ROLL_API_TOPIC.toString();
  }

  /**
   * Publish to nats.
   *
   * @param event    the event
   * @param response the response
   */
  private void publishToNATS(final Event event, final byte[] response) {
    this.getMessagePublisher().dispatchMessage(event.getReplyTo(), response);
  }

}
