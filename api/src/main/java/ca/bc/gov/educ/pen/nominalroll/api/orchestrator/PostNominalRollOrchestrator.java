package ca.bc.gov.educ.pen.nominalroll.api.orchestrator;

import ca.bc.gov.educ.pen.nominalroll.api.constants.EventOutcome;
import ca.bc.gov.educ.pen.nominalroll.api.constants.v1.NominalRollStudentStatus;
import ca.bc.gov.educ.pen.nominalroll.api.mappers.v1.NominalRollStudentMapper;
import ca.bc.gov.educ.pen.nominalroll.api.messaging.MessagePublisher;
import ca.bc.gov.educ.pen.nominalroll.api.model.v1.NominalRollPostedStudentEntity;
import ca.bc.gov.educ.pen.nominalroll.api.model.v1.NominalRollStudentEntity;
import ca.bc.gov.educ.pen.nominalroll.api.model.v1.Saga;
import ca.bc.gov.educ.pen.nominalroll.api.service.v1.NominalRollService;
import ca.bc.gov.educ.pen.nominalroll.api.service.v1.SagaService;
import ca.bc.gov.educ.pen.nominalroll.api.struct.v1.Event;
import ca.bc.gov.educ.pen.nominalroll.api.struct.v1.NominalRollPostSagaData;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static ca.bc.gov.educ.pen.nominalroll.api.constants.EventType.*;
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
  private final NominalRollService nominalRollService;

  /**
   * Instantiates a new Base orchestrator.
   *
   * @param sagaService        the saga service
   * @param messagePublisher   the message publisher
   * @param nominalRollService the nominal roll service
   */
  public PostNominalRollOrchestrator(final SagaService sagaService, final MessagePublisher messagePublisher, final NominalRollService nominalRollService) {
    super(sagaService, messagePublisher, NominalRollPostSagaData.class, NOMINAL_ROLL_POST_SAGA.toString(), NOMINAL_ROLL_POST_SAGA_TOPIC.toString());
    this.nominalRollService = nominalRollService;
  }

  /**
   * Populate steps to execute map.
   */
  @Override
  public void populateStepsToExecuteMap() {
    this.stepBuilder()
      .begin(SAVE_NOMINAL_ROLL_POSTED_STUDENTS, this::saveNominalRollPostedStudents)
      .step(SAVE_NOMINAL_ROLL_POSTED_STUDENTS, EventOutcome.NOMINAL_ROLL_POSTED_STUDENTS_SAVED, CREATE_SLD_DIA_STUDENTS, this::createDIAStudents)
      .step(CREATE_SLD_DIA_STUDENTS, EventOutcome.SLD_DIA_STUDENTS_CREATED, MARK_SAGA_COMPLETE, this::markSagaComplete);
  }

  private void saveNominalRollPostedStudents(final Event event, final Saga saga, final NominalRollPostSagaData nominalRollPostSagaData) {
    if (nominalRollPostSagaData.getIsSavedToPosterityTable()) { //already saved to posterity table, next step is to create SLD/DIA students, this can happen in replay process.

    } else {
      val students = this.nominalRollService.findAllByProcessingYear(String.valueOf(LocalDate.now().getYear()));
      final List<NominalRollStudentEntity> ignoredStudents = new ArrayList<>();
      final List<NominalRollPostedStudentEntity> studentsToBePosted = new ArrayList<>();
      for (val student : students) {
        if (StringUtils.isBlank(student.getAssignedPEN())) {
          student.setStatus(NominalRollStudentStatus.IGNORED.toString());
          ignoredStudents.add(student);
        } else {
          studentsToBePosted.add(NominalRollStudentMapper.mapper.toPostedEntity(student));
        }
      }
      if (!ignoredStudents.isEmpty()) {
        this.nominalRollService.saveNominalRollStudents(ignoredStudents, saga.getSagaId().toString());
      }
      if (!studentsToBePosted.isEmpty()) {
        this.nominalRollService.savePostedStudents(studentsToBePosted);
      }
    }


  }

  /**
   * Update the original student record
   *
   * @param event                   the event
   * @param saga                    the saga
   * @param nominalRollPostSagaData the split pen saga data
   * @throws JsonProcessingException the json processing exception
   */
  public void createDIAStudents(final Event event, final Saga saga, final NominalRollPostSagaData nominalRollPostSagaData) throws JsonProcessingException {
//    final SagaEventStates eventStates = this.createEventState(saga, event.getEventType(), event.getEventOutcome(), event.getEventPayload());
//    saga.setStatus(IN_PROGRESS.toString());
    //DO MORE!
  }

}
