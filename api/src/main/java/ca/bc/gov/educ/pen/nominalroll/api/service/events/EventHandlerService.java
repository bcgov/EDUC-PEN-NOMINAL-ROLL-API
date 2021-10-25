package ca.bc.gov.educ.pen.nominalroll.api.service.events;

import ca.bc.gov.educ.pen.nominalroll.api.service.v1.NominalRollService;
import ca.bc.gov.educ.pen.nominalroll.api.struct.v1.Event;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static lombok.AccessLevel.PRIVATE;
import static org.springframework.transaction.annotation.Propagation.REQUIRES_NEW;

/**
 * The type Event handler service.
 */
@Service
@Slf4j
public class EventHandlerService {

  /**
   * The constant RESPONDING_BACK.
   */
  public static final String RESPONDING_BACK = "responding back :: {}";

  /**
   * The Pen service.
   */
  @Getter(PRIVATE)
  private final NominalRollService nominalRollService;

  /**
   * Instantiates a new Event handler service.
   *
   * @param nominalRollService the pen service
   */
  @Autowired
  public EventHandlerService(final NominalRollService nominalRollService) {
    this.nominalRollService = nominalRollService;
  }

  /**
   * Handle get next PEN number event.
   *
   * @param event the event
   * @return the byte [ ]
   * @throws JsonProcessingException the json processing exception
   */
  @Transactional(propagation = REQUIRES_NEW)
  public byte[] handleCreateDIAStudents(@NonNull final Event event) throws JsonProcessingException {
    return new byte[0];
  }


}
