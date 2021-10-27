package ca.bc.gov.educ.pen.nominalroll.api.service.events;

import ca.bc.gov.educ.pen.nominalroll.api.constants.EventOutcome;
import ca.bc.gov.educ.pen.nominalroll.api.constants.SagaEnum;
import ca.bc.gov.educ.pen.nominalroll.api.orchestrator.NominalRollStudentProcessingOrchestrator;
import ca.bc.gov.educ.pen.nominalroll.api.properties.ApplicationProperties;
import ca.bc.gov.educ.pen.nominalroll.api.service.v1.NominalRollService;
import ca.bc.gov.educ.pen.nominalroll.api.service.v1.SagaService;
import ca.bc.gov.educ.pen.nominalroll.api.struct.v1.Event;
import ca.bc.gov.educ.pen.nominalroll.api.struct.v1.NominalRollStudentSagaData;
import ca.bc.gov.educ.pen.nominalroll.api.util.JsonUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static lombok.AccessLevel.PRIVATE;
import static org.springframework.transaction.annotation.Propagation.REQUIRES_NEW;

/**
 * The type Event handler service.
 */
@Service
@Slf4j
public class EventHandlerService {


  /**
   * The Pen service.
   */
  @Getter(PRIVATE)
  private final NominalRollService nominalRollService;

  @Getter(PRIVATE)
  private final SagaService sagaService;

  private final NominalRollStudentProcessingOrchestrator studentProcessingOrchestrator;

  /**
   * Instantiates a new Event handler service.
   *
   * @param nominalRollService            the nominal roll service
   * @param sagaService                   the saga service
   * @param studentProcessingOrchestrator the student processing orchestrator
   */
  @Autowired
  public EventHandlerService(final NominalRollService nominalRollService, final SagaService sagaService, NominalRollStudentProcessingOrchestrator studentProcessingOrchestrator) {
    this.nominalRollService = nominalRollService;
    this.sagaService = sagaService;
    this.studentProcessingOrchestrator = studentProcessingOrchestrator;
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


  @Transactional(propagation = REQUIRES_NEW)
  public void handleReadFromTopicEvent(final Event event) throws JsonProcessingException {
    if (event.getEventOutcome() == EventOutcome.READ_FROM_TOPIC_SUCCESS) {
      final NominalRollStudentSagaData sagaData = JsonUtil.getJsonObjectFromString(NominalRollStudentSagaData.class, event.getEventPayload());
      final var sagaOptional = this.getSagaService().findByNominalRollStudentIDAndSagaName(UUID.fromString(sagaData.getNominalRollStudent().getNominalRollStudentID()), SagaEnum.NOMINAL_ROLL_PROCESS_STUDENT_SAGA.toString());
      if (sagaOptional.isPresent()) { // possible duplicate message.
        log.trace("Execution is not required for this message returning EVENT is :: {}", event);
        return;
      }
      val saga = this.studentProcessingOrchestrator.createSaga(event.getEventPayload(), UUID.fromString(sagaData.getNominalRollStudent().getNominalRollStudentID()), ApplicationProperties.API_NAME);
      this.studentProcessingOrchestrator.startSaga(saga);
    }
  }
}
