package ca.bc.gov.educ.pen.nominalroll.api.orchestrator;

import ca.bc.gov.educ.pen.nominalroll.api.mappers.v1.NominalRollPostedStudentMapper;
import ca.bc.gov.educ.pen.nominalroll.api.messaging.MessagePublisher;
import ca.bc.gov.educ.pen.nominalroll.api.model.v1.NominalRollPostedStudentEntity;
import ca.bc.gov.educ.pen.nominalroll.api.model.v1.Saga;
import ca.bc.gov.educ.pen.nominalroll.api.model.v1.SagaEventStates;
import ca.bc.gov.educ.pen.nominalroll.api.rest.RestUtils;
import ca.bc.gov.educ.pen.nominalroll.api.service.v1.NominalRollService;
import ca.bc.gov.educ.pen.nominalroll.api.service.v1.SagaService;
import ca.bc.gov.educ.pen.nominalroll.api.struct.external.sld.v1.SldDiaStudent;
import ca.bc.gov.educ.pen.nominalroll.api.struct.v1.Event;
import ca.bc.gov.educ.pen.nominalroll.api.struct.v1.NominalRollPostSagaData;
import ca.bc.gov.educ.pen.nominalroll.api.util.JsonUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import static ca.bc.gov.educ.pen.nominalroll.api.constants.EventOutcome.NOMINAL_ROLL_POSTED_STUDENTS_SAVED;
import static ca.bc.gov.educ.pen.nominalroll.api.constants.EventOutcome.SLD_DIA_STUDENTS_CREATED;
import static ca.bc.gov.educ.pen.nominalroll.api.constants.EventType.*;
import static ca.bc.gov.educ.pen.nominalroll.api.constants.SagaEnum.NOMINAL_ROLL_POST_DATA_SAGA;
import static ca.bc.gov.educ.pen.nominalroll.api.constants.SagaStatusEnum.IN_PROGRESS;
import static ca.bc.gov.educ.pen.nominalroll.api.constants.TopicsEnum.*;

/**
 * The type Split pen orchestrator
 */
@Component
@Slf4j
public class PostNominalRollOrchestrator extends BaseUserActionsOrchestrator<NominalRollPostSagaData> {

  /**
   * The constant studentMapper.
   */
  protected static final NominalRollPostedStudentMapper postedStudentMapper = NominalRollPostedStudentMapper.mapper;
  private final NominalRollService nominalRollService;
  private final RestUtils restUtils;


  /**
   * Instantiates a new Base orchestrator.
   *
   * @param sagaService        the saga service
   * @param messagePublisher   the message publisher
   * @param nominalRollService the nominal roll service
   */
  public PostNominalRollOrchestrator(final SagaService sagaService, final MessagePublisher messagePublisher, final NominalRollService nominalRollService, final RestUtils restUtils) {
    super(sagaService, messagePublisher, NominalRollPostSagaData.class, NOMINAL_ROLL_POST_DATA_SAGA.toString(), NOMINAL_ROLL_POST_SAGA_TOPIC.toString());
    this.nominalRollService = nominalRollService;
    this.restUtils = restUtils;
  }

  /**
   * Populate steps to execute map.
   */
  @Override
  public void populateStepsToExecuteMap() {
    this.stepBuilder()
      .begin(SAVE_NOMINAL_ROLL_POSTED_STUDENTS, this::saveNominalRollPostedStudents)
      .step(SAVE_NOMINAL_ROLL_POSTED_STUDENTS, NOMINAL_ROLL_POSTED_STUDENTS_SAVED, CREATE_SLD_DIA_STUDENTS, this::createDIAStudents)
      .step(CREATE_SLD_DIA_STUDENTS, SLD_DIA_STUDENTS_CREATED, MARK_SAGA_COMPLETE, this::markSagaComplete);
  }

  /**
   * Save nominal roll posted students
   *
   * @param event                   the event
   * @param saga                    the saga
   * @param nominalRollPostSagaData the split pen saga data
   * @throws JsonProcessingException the json processing exception
   */
  private void saveNominalRollPostedStudents(final Event event, final Saga saga, final NominalRollPostSagaData nominalRollPostSagaData) throws InterruptedException, TimeoutException, IOException {
    final SagaEventStates eventStates = this.createEventState(saga, event.getEventType(), event.getEventOutcome(), event.getEventPayload());
    saga.setStatus(IN_PROGRESS.toString());
    saga.setSagaState(SAVE_NOMINAL_ROLL_POSTED_STUDENTS.toString());
    this.getSagaService().updateAttachedSagaWithEvents(saga, eventStates);

    this.nominalRollService.savePostedStudentsAndIgnoredStudents(nominalRollPostSagaData.getProcessingYear(), nominalRollPostSagaData.getUpdateUser(), saga.getSagaId().toString());

    val nextEvent = Event.builder().sagaId(saga.getSagaId())
      .eventType(SAVE_NOMINAL_ROLL_POSTED_STUDENTS)
      .eventOutcome(NOMINAL_ROLL_POSTED_STUDENTS_SAVED)
      .build();
    this.handleEvent(nextEvent);
  }

  /**
   * Create DIA students
   *
   * @param event                   the event
   * @param saga                    the saga
   * @param nominalRollPostSagaData the split pen saga data
   * @throws JsonProcessingException the json processing exception
   */
  public void createDIAStudents(final Event event, final Saga saga, final NominalRollPostSagaData nominalRollPostSagaData) throws JsonProcessingException {
    final SagaEventStates eventStates = this.createEventState(saga, event.getEventType(), event.getEventOutcome(), event.getEventPayload());
    saga.setSagaState(CREATE_SLD_DIA_STUDENTS.toString());
    this.getSagaService().updateAttachedSagaWithEvents(saga, eventStates);

    final List<NominalRollPostedStudentEntity> nominalRollStudents = this.nominalRollService.findPostedStudentsByProcessingYear(nominalRollPostSagaData.getProcessingYear());
    final List<SldDiaStudent> sldDiaStudents = nominalRollStudents.stream().map(student -> postedStudentMapper.toDiaStudent(student, this.restUtils)).collect(Collectors.toList());

    final Event nextEvent = Event.builder().sagaId(saga.getSagaId())
      .eventType(CREATE_SLD_DIA_STUDENTS)
      .replyTo(this.getTopicToSubscribe())
      .eventPayload(JsonUtil.getJsonStringFromObject(sldDiaStudents))
      .build();
    this.postMessageToTopic(SLD_API_TOPIC.toString(), nextEvent);
    log.info("message sent to SLD_API_TOPIC for CREATE_SLD_DIA_STUDENTS Event. :: {}", saga.getSagaId());
  }

}
