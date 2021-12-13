package ca.bc.gov.educ.pen.nominalroll.api.service.events;

import ca.bc.gov.educ.pen.nominalroll.api.constants.EventType;
import ca.bc.gov.educ.pen.nominalroll.api.constants.TopicsEnum;
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
   * The Event publisher service.
   */
  @Getter(PRIVATE)
  private final EventPublisherService eventPublisherService;

  /**
   * Instantiates a new Event handler delegator service.
   *
   * @param eventHandlerService the event handler service
   * @param eventPublisherService the message publisher service
   */
  @Autowired
  public EventHandlerDelegatorService(final EventHandlerService eventHandlerService, final EventPublisherService eventPublisherService) {
    this.eventHandlerService = eventHandlerService;
    this.eventPublisherService = eventPublisherService;
  }

  /**
   * Handle event.
   *
   * @param event the event
   */
  @Async("subscriberExecutor")
  @Override
  public void handleEvent(final Event event) {
    try {
      if (event.getEventType() == EventType.READ_FROM_TOPIC) {
        log.info("received read from topic event :: ");
        log.trace(PAYLOAD_LOG, event.getEventPayload());
        this.getEventHandlerService().handleReadFromTopicEvent(event); // no response in this event.
      } else {
        log.info("silently ignoring other event :: {}", event);
      }
    } catch (final Exception e) {
      log.error("Exception", e);
    }
  }

  @Override
  public String getTopicToSubscribe() {
    return TopicsEnum.NOMINAL_ROLL_API_TOPIC.toString();
  }


}
