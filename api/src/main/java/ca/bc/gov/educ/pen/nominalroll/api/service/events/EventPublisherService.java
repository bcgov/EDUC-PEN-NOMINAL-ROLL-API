package ca.bc.gov.educ.pen.nominalroll.api.service.events;


import ca.bc.gov.educ.pen.nominalroll.api.constants.EventOutcome;
import ca.bc.gov.educ.pen.nominalroll.api.constants.EventType;
import ca.bc.gov.educ.pen.nominalroll.api.messaging.MessagePublisher;
import ca.bc.gov.educ.pen.nominalroll.api.model.v1.NominalRollEvent;
import ca.bc.gov.educ.pen.nominalroll.api.struct.v1.Event;
import ca.bc.gov.educ.pen.nominalroll.api.util.JsonUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import static lombok.AccessLevel.PRIVATE;

@Service
@Slf4j
public class EventPublisherService {

  /**
   * The constant RESPONDING_BACK_TO_NATS_ON_CHANNEL.
   */
  public static final String RESPONDING_BACK_TO_NATS_ON_CHANNEL = "responding back to NATS on {} channel ";

  @Getter(PRIVATE)
  private final MessagePublisher messagePublisher;

  @Autowired
  public EventPublisherService(final MessagePublisher messagePublisher) {
    this.messagePublisher = messagePublisher;
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void send(final NominalRollEvent event) throws JsonProcessingException {
    if (event.getReplyChannel() != null) {
      log.info(RESPONDING_BACK_TO_NATS_ON_CHANNEL, event.getReplyChannel());
      this.getMessagePublisher().dispatchMessage(event.getReplyChannel(), this.nominalRollEventProcessed(event));
    }
  }

  private byte[] nominalRollEventProcessed(final NominalRollEvent nominalRollEvent) throws JsonProcessingException {
    final Event event = Event.builder()
        .sagaId(nominalRollEvent.getSagaId())
        .eventType(EventType.valueOf(nominalRollEvent.getEventType()))
        .eventOutcome(EventOutcome.valueOf(nominalRollEvent.getEventOutcome()))
        .eventPayload(nominalRollEvent.getEventPayload()).build();
    return JsonUtil.getJsonStringFromObject(event).getBytes();
  }

}
