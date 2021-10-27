package ca.bc.gov.educ.pen.nominalroll.api.orchestrator;

import ca.bc.gov.educ.pen.nominalroll.api.constants.EventOutcome;
import ca.bc.gov.educ.pen.nominalroll.api.mappers.v1.NominalRollStudentMapper;
import ca.bc.gov.educ.pen.nominalroll.api.messaging.MessagePublisher;
import ca.bc.gov.educ.pen.nominalroll.api.model.v1.Saga;
import ca.bc.gov.educ.pen.nominalroll.api.service.v1.SagaService;
import ca.bc.gov.educ.pen.nominalroll.api.struct.v1.Event;
import ca.bc.gov.educ.pen.nominalroll.api.struct.v1.NominalRollPostSagaData;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import static ca.bc.gov.educ.pen.nominalroll.api.constants.EventType.CREATE_DIA_STUDENTS;
import static ca.bc.gov.educ.pen.nominalroll.api.constants.EventType.MARK_SAGA_COMPLETE;
import static ca.bc.gov.educ.pen.nominalroll.api.constants.SagaEnum.NOMINAL_ROLL_POST_SAGA;
import static ca.bc.gov.educ.pen.nominalroll.api.constants.TopicsEnum.NOMINAL_ROLL_POST_SAGA_TOPIC;

/**
 * The type Split pen orchestrator
 */
@Component
@Slf4j
public class PostNominalRollOrchestrator extends BaseUserActionsOrchestrator<NominalRollPostSagaData> {

  /**
   * The constant studentMapper.
   */
  protected static final NominalRollStudentMapper studentMapper = NominalRollStudentMapper.mapper;

  /**
   * Instantiates a new Base orchestrator.
   *
   * @param sagaService      the saga service
   * @param messagePublisher the message publisher
   */
  public PostNominalRollOrchestrator(final SagaService sagaService, final MessagePublisher messagePublisher) {
    super(sagaService, messagePublisher, NominalRollPostSagaData.class, NOMINAL_ROLL_POST_SAGA.toString(), NOMINAL_ROLL_POST_SAGA_TOPIC.toString());
  }

  /**
   * Populate steps to execute map.
   */
  @Override
  public void populateStepsToExecuteMap() {
    this.stepBuilder().begin(CREATE_DIA_STUDENTS, this::createDIAStudents)
      .step(CREATE_DIA_STUDENTS, EventOutcome.DIA_STUDENTS_CREATED, MARK_SAGA_COMPLETE, this::markSagaComplete);
  }

  /**
   * Update the original student record
   *
   * @param event             the event
   * @param saga              the saga
   * @param nominalRollPostSagaData  the split pen saga data
   * @throws JsonProcessingException the json processing exception
   */
  public void createDIAStudents(final Event event, final Saga saga, final NominalRollPostSagaData nominalRollPostSagaData) throws JsonProcessingException {
//    final SagaEventStates eventStates = this.createEventState(saga, event.getEventType(), event.getEventOutcome(), event.getEventPayload());
//    saga.setStatus(IN_PROGRESS.toString());
    //DO MORE!
  }

}
