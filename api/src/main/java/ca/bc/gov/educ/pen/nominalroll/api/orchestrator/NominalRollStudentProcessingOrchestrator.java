package ca.bc.gov.educ.pen.nominalroll.api.orchestrator;

import ca.bc.gov.educ.pen.nominalroll.api.constants.SagaEnum;
import ca.bc.gov.educ.pen.nominalroll.api.constants.TopicsEnum;
import ca.bc.gov.educ.pen.nominalroll.api.constants.v1.NominalRollStudentStatus;
import ca.bc.gov.educ.pen.nominalroll.api.exception.NominalRollAPIRuntimeException;
import ca.bc.gov.educ.pen.nominalroll.api.helpers.NominalRollHelper;
import ca.bc.gov.educ.pen.nominalroll.api.mappers.v1.NominalRollStudentMapper;
import ca.bc.gov.educ.pen.nominalroll.api.mappers.v1.PenMatchSagaMapper;
import ca.bc.gov.educ.pen.nominalroll.api.messaging.MessagePublisher;
import ca.bc.gov.educ.pen.nominalroll.api.model.v1.Saga;
import ca.bc.gov.educ.pen.nominalroll.api.model.v1.SagaEventStates;
import ca.bc.gov.educ.pen.nominalroll.api.orchestrator.base.BaseOrchestrator;
import ca.bc.gov.educ.pen.nominalroll.api.rest.RestUtils;
import ca.bc.gov.educ.pen.nominalroll.api.rules.RulesProcessor;
import ca.bc.gov.educ.pen.nominalroll.api.service.v1.NominalRollService;
import ca.bc.gov.educ.pen.nominalroll.api.service.v1.SagaService;
import ca.bc.gov.educ.pen.nominalroll.api.struct.external.penmatch.v1.PenMatchResult;
import ca.bc.gov.educ.pen.nominalroll.api.struct.v1.Event;
import ca.bc.gov.educ.pen.nominalroll.api.struct.v1.NominalRollStudentSagaData;
import ca.bc.gov.educ.pen.nominalroll.api.util.JsonUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static ca.bc.gov.educ.pen.nominalroll.api.constants.EventOutcome.*;
import static ca.bc.gov.educ.pen.nominalroll.api.constants.EventType.*;
import static ca.bc.gov.educ.pen.nominalroll.api.constants.SagaStatusEnum.IN_PROGRESS;
import static ca.bc.gov.educ.pen.nominalroll.api.constants.TopicsEnum.PEN_MATCH_API_TOPIC;

@Component
@Slf4j
public class NominalRollStudentProcessingOrchestrator extends BaseOrchestrator<NominalRollStudentSagaData> {
  private final RulesProcessor rulesProcessor;
  private final NominalRollService nominalRollService;
  private final RestUtils restUtils;

  /**
   * Instantiates a new Base orchestrator.
   *
   * @param sagaService        the saga service
   * @param messagePublisher   the message publisher
   * @param rulesProcessor     the rules processor
   * @param nominalRollService the nominal roll service
   * @param restUtils          the rest utils
   */
  protected NominalRollStudentProcessingOrchestrator(final SagaService sagaService, final MessagePublisher messagePublisher, final RulesProcessor rulesProcessor, final NominalRollService nominalRollService, final RestUtils restUtils) {
    super(sagaService, messagePublisher, NominalRollStudentSagaData.class, SagaEnum.NOMINAL_ROLL_PROCESS_STUDENT_SAGA.toString(), TopicsEnum.NOMINAL_ROLL_PROCESS_STUDENT_SAGA_TOPIC.toString());
    this.rulesProcessor = rulesProcessor;
    this.nominalRollService = nominalRollService;
    this.restUtils = restUtils;
  }

  @Override
  public void populateStepsToExecuteMap() {
    this.stepBuilder()
      .begin(VALIDATE_NOMINAL_ROLL_STUDENT, this::validateStudent)
      .step(VALIDATE_NOMINAL_ROLL_STUDENT, VALIDATION_SUCCESS_NO_ERROR, PROCESS_PEN_MATCH, this::processPenMatch)
      .end(VALIDATE_NOMINAL_ROLL_STUDENT, VALIDATION_SUCCESS_WITH_ERROR, this::completeNominalRollStudentSagaWithError)
      .or()
      .step(PROCESS_PEN_MATCH, PEN_MATCH_PROCESSED, PROCESS_PEN_MATCH_RESULTS, this::processPenMatchResults)
      .end(PROCESS_PEN_MATCH_RESULTS, PEN_MATCH_RESULTS_PROCESSED);
  }

  protected void processPenMatchResults(final Event event, final Saga saga, final NominalRollStudentSagaData nominalRollStudentSagaData) throws JsonProcessingException {
    final SagaEventStates eventStates = this.createEventState(saga, event.getEventType(), event.getEventOutcome(), event.getEventPayload());
    saga.setSagaState(PROCESS_PEN_MATCH_RESULTS.toString());
    final var penMatchResult = JsonUtil.getJsonObjectFromString(PenMatchResult.class, event.getEventPayload());
    nominalRollStudentSagaData.setPenMatchResult(penMatchResult); // update the original payload with response from PEN_MATCH_API
    saga.setPayload(JsonUtil.getJsonStringFromObject(nominalRollStudentSagaData)); // save the updated payload to DB...
    this.getSagaService().updateAttachedSagaWithEvents(saga, eventStates);
    val algorithmStatusCode = penMatchResult.getPenStatus();
    Optional<String> assignedPEN = Optional.empty();
    //system matched status.
    if (StringUtils.equalsIgnoreCase(algorithmStatusCode, "AA")
      || StringUtils.equalsIgnoreCase(algorithmStatusCode, "B1")
      || StringUtils.equalsIgnoreCase(algorithmStatusCode, "C1")
      || StringUtils.equalsIgnoreCase(algorithmStatusCode, "D1")) {
      final var penMatchRecordOptional = penMatchResult.getMatchingRecords().stream().findFirst();
      if (penMatchRecordOptional.isPresent()) {
        assignedPEN = Optional.of(penMatchRecordOptional.get().getMatchingPEN());
      } else {
        log.error("PenMatchRecord in priority queue is empty for matched status, this should not have happened.");
        throw new NominalRollAPIRuntimeException("PenMatchRecord in priority queue is empty for matched status, this should not have happened.");
      }
    }
    val nomRollStudOptional = this.nominalRollService.findByNominalRollStudentID(nominalRollStudentSagaData.getNominalRollStudent().getNominalRollStudentID());
    if (nomRollStudOptional.isPresent()) {
      val nomRollStud = nomRollStudOptional.get();
      if (assignedPEN.isPresent()) {
        nomRollStud.setAssignedPEN(assignedPEN.get());
        nomRollStud.setStatus(NominalRollStudentStatus.MATCHEDSYS.toString());
      } else {
        nomRollStud.setStatus(NominalRollStudentStatus.FIXABLE.toString());
      }
      this.nominalRollService.saveNominalRollStudent(nomRollStud);
    }
    this.postMessageToTopic(this.getTopicToSubscribe(), Event.builder().sagaId(saga.getSagaId())
      .eventType(PROCESS_PEN_MATCH_RESULTS).eventOutcome(PEN_MATCH_RESULTS_PROCESSED)
      .eventPayload(penMatchResult.getPenStatus()).build());
  }

  private void completeNominalRollStudentSagaWithError(final Event event, final Saga saga, final NominalRollStudentSagaData nominalRollStudentSagaData) throws JsonProcessingException {
    final TypeReference<Map<String, String>> responseType = new TypeReference<>() {
    };
    val validationResults = JsonUtil.mapper.readValue(event.getEventPayload(), responseType);
    this.nominalRollService.saveNominalRollStudentValidationErrors(nominalRollStudentSagaData.getNominalRollStudent().getNominalRollStudentID(), validationResults, null);
  }

  protected void processPenMatch(final Event event, final Saga saga, final NominalRollStudentSagaData nominalRollStudentSagaData) throws JsonProcessingException {
    final SagaEventStates eventStates = this.createEventState(saga, event.getEventType(), event.getEventOutcome(), event.getEventPayload());
    saga.setSagaState(PROCESS_PEN_MATCH.toString());
    this.getSagaService().updateAttachedSagaWithEvents(saga, eventStates);
    val nominalRollStudent = nominalRollStudentSagaData.getNominalRollStudent();
    val nomRollPostedStudents = this.nominalRollService.findAllBySurnameAndGivenNamesAndBirthDateAndGender(StringUtils.upperCase(nominalRollStudent.getSurname()), StringUtils.upperCase(nominalRollStudent.getGivenNames()), LocalDate.parse(nominalRollStudent.getBirthDate()), StringUtils.upperCase(nominalRollStudent.getGender()));
    if (nomRollPostedStudents.size() != 1) { // proceed to actual pen match logic by calling api.
      this.postToPenMatchAPI(saga, nominalRollStudentSagaData);
    } else {
      final String matchingPEN = nomRollPostedStudents.get(0).getAssignedPEN();
      if (StringUtils.isNotEmpty(matchingPEN)) {
        log.info("Found exact match from posterity table and pen is :: {}", matchingPEN);
        val nomRollStudOptional = this.nominalRollService.findByNominalRollStudentID(nominalRollStudent.getNominalRollStudentID());
        if (nomRollStudOptional.isPresent()) {
          val nomRollStud = nomRollStudOptional.get();
          nomRollStud.setAssignedPEN(matchingPEN);
          nomRollStud.setStatus(NominalRollStudentStatus.MATCHEDSYS.toString());
          this.nominalRollService.saveNominalRollStudent(nomRollStud);
          //fastest route to success, complete the saga.
          this.markSagaComplete(event, saga, nominalRollStudentSagaData);
        }
      } else {
        this.postToPenMatchAPI(saga, nominalRollStudentSagaData);
      }
    }

  }

  protected void postToPenMatchAPI(final Saga saga, final NominalRollStudentSagaData nominalRollStudentSagaData) throws JsonProcessingException {
    val nominalRollStudent = nominalRollStudentSagaData.getNominalRollStudent();
    final String mincode = this.restUtils.getFedProvSchoolCodes().get(nominalRollStudent.getSchoolNumber());
    val penMatchRequest = PenMatchSagaMapper.mapper.toPenMatchStudent(nominalRollStudent, mincode);
    penMatchRequest.setDob(StringUtils.replace(penMatchRequest.getDob(), "-", "")); // pen-match api expects yyyymmdd
    val penMatchRequestJson = JsonUtil.mapper.writeValueAsString(penMatchRequest);
    final Event nextEvent = Event.builder().sagaId(saga.getSagaId())
      .eventType(PROCESS_PEN_MATCH)
      .replyTo(this.getTopicToSubscribe())
      .eventPayload(penMatchRequestJson)
      .build();
    this.postMessageToTopic(PEN_MATCH_API_TOPIC.toString(), nextEvent);
    log.info("message sent to PEN_MATCH_API_TOPIC for PROCESS_PEN_MATCH Event. :: {}", saga.getSagaId());
  }


  protected void validateStudent(final Event event, final Saga saga, final NominalRollStudentSagaData nominalRollStudentSagaData) throws JsonProcessingException {
    final SagaEventStates eventStates = this.createEventState(saga, event.getEventType(), event.getEventOutcome(), event.getEventPayload());
    saga.setSagaState(VALIDATE_NOMINAL_ROLL_STUDENT.toString());
    saga.setStatus(IN_PROGRESS.toString());
    this.getSagaService().updateAttachedSagaWithEvents(saga, eventStates);
    log.info("Saga {} DB write is updated for validateStudent at time {}", saga.getSagaId(), LocalDateTime.now());
    val validationErrors = this.rulesProcessor.processRules(NominalRollStudentMapper.mapper.toModel(nominalRollStudentSagaData.getNominalRollStudent()));
    final Event.EventBuilder eventBuilder = Event.builder();
    eventBuilder.sagaId(saga.getSagaId()).eventType(VALIDATE_NOMINAL_ROLL_STUDENT);
    if (validationErrors.isEmpty()) {
      eventBuilder.eventOutcome(VALIDATION_SUCCESS_NO_ERROR);
      eventBuilder.eventPayload("");
    } else {
      eventBuilder.eventOutcome(VALIDATION_SUCCESS_WITH_ERROR);
      eventBuilder.eventPayload(JsonUtil.getJsonStringFromObject(validationErrors));
    }
    val nextEvent = eventBuilder.build();
    this.postMessageToTopic(this.getTopicToSubscribe(), nextEvent);
    log.info("message sent to {} for {} Event. :: {}", this.getTopicToSubscribe(), nextEvent, saga.getSagaId());
  }
}
