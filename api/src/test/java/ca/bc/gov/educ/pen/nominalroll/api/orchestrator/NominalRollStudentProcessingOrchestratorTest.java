package ca.bc.gov.educ.pen.nominalroll.api.orchestrator;

import ca.bc.gov.educ.pen.nominalroll.api.BaseNominalRollAPITest;
import ca.bc.gov.educ.pen.nominalroll.api.constants.EventOutcome;
import ca.bc.gov.educ.pen.nominalroll.api.constants.EventType;
import ca.bc.gov.educ.pen.nominalroll.api.constants.GradeCodes;
import ca.bc.gov.educ.pen.nominalroll.api.mappers.v1.NominalRollStudentMapper;
import ca.bc.gov.educ.pen.nominalroll.api.messaging.MessagePublisher;
import ca.bc.gov.educ.pen.nominalroll.api.model.v1.NominalRollStudentEntity;
import ca.bc.gov.educ.pen.nominalroll.api.properties.ApplicationProperties;
import ca.bc.gov.educ.pen.nominalroll.api.struct.external.penmatch.v1.PenMatchRecord;
import ca.bc.gov.educ.pen.nominalroll.api.struct.external.penmatch.v1.PenMatchResult;
import ca.bc.gov.educ.pen.nominalroll.api.struct.external.student.v1.GenderCode;
import ca.bc.gov.educ.pen.nominalroll.api.struct.external.student.v1.GradeCode;
import ca.bc.gov.educ.pen.nominalroll.api.struct.v1.Event;
import ca.bc.gov.educ.pen.nominalroll.api.struct.v1.NominalRollStudent;
import ca.bc.gov.educ.pen.nominalroll.api.struct.v1.NominalRollStudentSagaData;
import ca.bc.gov.educ.pen.nominalroll.api.util.JsonUtil;
import lombok.SneakyThrows;
import lombok.val;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

import static ca.bc.gov.educ.pen.nominalroll.api.constants.EventOutcome.*;
import static ca.bc.gov.educ.pen.nominalroll.api.constants.EventType.*;
import static ca.bc.gov.educ.pen.nominalroll.api.constants.SagaStatusEnum.COMPLETED;
import static ca.bc.gov.educ.pen.nominalroll.api.constants.SagaStatusEnum.IN_PROGRESS;
import static ca.bc.gov.educ.pen.nominalroll.api.constants.TopicsEnum.PEN_MATCH_API_TOPIC;
import static ca.bc.gov.educ.pen.nominalroll.api.constants.v1.NominalRollStudentStatus.FIXABLE;
import static ca.bc.gov.educ.pen.nominalroll.api.constants.v1.NominalRollStudentStatus.MATCHEDSYS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class NominalRollStudentProcessingOrchestratorTest extends BaseNominalRollAPITest {

  @Autowired
  NominalRollStudentProcessingOrchestrator nominalRollStudentProcessingOrchestrator;

  @Autowired
  MessagePublisher messagePublisher;
  @Captor
  ArgumentCaptor<byte[]> eventCaptor;

  @Before
  public void setUp() throws Exception {
    Mockito.reset(this.messagePublisher);
    Mockito.reset(this.restUtils);
  }

  @SneakyThrows
  @Test
  public void testHandleEvent_givenEventTypeInitiated_shouldExecuteValidateStudentWithEventOutComeVALIDATION_SUCCESS_WITH_ERROR() {
    val entity = NominalRollStudentMapper.mapper.toModel(this.createMockNominalRollStudent());
    entity.setCreateDate(LocalDateTime.now().minusMinutes(14));
    entity.setUpdateDate(LocalDateTime.now());
    entity.setCreateUser(ApplicationProperties.API_NAME);
    entity.setUpdateUser(ApplicationProperties.API_NAME);
    this.testHelper.getRepository().save(entity);
    val saga = this.creatMockSaga(NominalRollStudentMapper.mapper.toStruct(entity));
    saga.setSagaId(null);
    this.testHelper.getSagaRepository().save(saga);
    final NominalRollStudentSagaData sagaData = NominalRollStudentSagaData.builder().nominalRollStudent(NominalRollStudentMapper.mapper.toStruct(entity)).build();
    val event = Event.builder()
      .sagaId(saga.getSagaId())
      .eventType(EventType.INITIATED)
      .eventOutcome(EventOutcome.INITIATE_SUCCESS)
      .eventPayload(JsonUtil.getJsonStringFromObject(sagaData)).build();
    this.nominalRollStudentProcessingOrchestrator.handleEvent(event);
    val savedSagaInDB = this.testHelper.getSagaRepository().findById(saga.getSagaId());
    assertThat(savedSagaInDB).isPresent();
    assertThat(savedSagaInDB.get().getStatus()).isEqualTo(IN_PROGRESS.toString());
    assertThat(savedSagaInDB.get().getSagaState()).isEqualTo(VALIDATE_NOMINAL_ROLL_STUDENT.toString());
    verify(this.messagePublisher, atMost(2)).dispatchMessage(eq(this.nominalRollStudentProcessingOrchestrator.getTopicToSubscribe()), this.eventCaptor.capture());
    final var newEvent = JsonUtil.getJsonObjectFromString(Event.class, new String(this.eventCaptor.getValue()));
    assertThat(newEvent.getEventType()).isEqualTo(VALIDATE_NOMINAL_ROLL_STUDENT);
    assertThat(newEvent.getEventOutcome()).isEqualTo(VALIDATION_SUCCESS_WITH_ERROR);
    assertThat(newEvent.getEventPayload()).isNotBlank();
  }

  @SneakyThrows
  @Test
  public void testHandleEvent_givenEventTypeInitiated_shouldExecuteValidateStudentWithEventOutComeVALIDATION_SUCCESS_NO_ERROR() {
    when(this.restUtils.getActiveGenderCodes()).thenReturn(List.of(GenderCode.builder().genderCode("M").build()));
    final List<GradeCode> gradeCodes = new ArrayList<>();
    for (final GradeCodes grade : GradeCodes.values()) {
      gradeCodes.add(GradeCode.builder().gradeCode(grade.getCode()).build());
    }
    when(this.restUtils.getActiveGradeCodes()).thenReturn(gradeCodes);
    when(this.restUtils.getFedProvSchoolCodes()).thenReturn(Map.of("5465", "10200001"));
    when(this.restUtils.districtCodes()).thenReturn(List.of("102", "103", "021", "006", "005"));
    val entity = NominalRollStudentMapper.mapper.toModel(this.createMockNominalRollStudent());
    entity.setCreateDate(LocalDateTime.now().minusMinutes(14));
    entity.setUpdateDate(LocalDateTime.now());
    entity.setCreateUser(ApplicationProperties.API_NAME);
    entity.setUpdateUser(ApplicationProperties.API_NAME);
    this.testHelper.getRepository().save(entity);
    val saga = this.creatMockSaga(NominalRollStudentMapper.mapper.toStruct(entity));
    saga.setSagaId(null);
    this.testHelper.getSagaRepository().save(saga);
    final NominalRollStudentSagaData sagaData = NominalRollStudentSagaData.builder().nominalRollStudent(NominalRollStudentMapper.mapper.toStruct(entity)).build();
    val event = Event.builder()
      .sagaId(saga.getSagaId())
      .eventType(EventType.INITIATED)
      .eventOutcome(EventOutcome.INITIATE_SUCCESS)
      .eventPayload(JsonUtil.getJsonStringFromObject(sagaData)).build();
    this.nominalRollStudentProcessingOrchestrator.handleEvent(event);
    val savedSagaInDB = this.testHelper.getSagaRepository().findById(saga.getSagaId());
    assertThat(savedSagaInDB).isPresent();
    assertThat(savedSagaInDB.get().getStatus()).isEqualTo(IN_PROGRESS.toString());
    assertThat(savedSagaInDB.get().getSagaState()).isEqualTo(VALIDATE_NOMINAL_ROLL_STUDENT.toString());
    verify(this.messagePublisher, atMost(2)).dispatchMessage(eq(this.nominalRollStudentProcessingOrchestrator.getTopicToSubscribe()), this.eventCaptor.capture());
    final var newEvent = JsonUtil.getJsonObjectFromString(Event.class, new String(this.eventCaptor.getValue()));
    assertThat(newEvent.getEventType()).isEqualTo(VALIDATE_NOMINAL_ROLL_STUDENT);
    assertThat(newEvent.getEventOutcome()).isEqualTo(VALIDATION_SUCCESS_NO_ERROR);
  }

  @SneakyThrows
  @Test
  public void testHandleEvent_givenEventTypeVALIDATE_NOMINAL_ROLL_STUDENTAndEventOutComeVALIDATION_SUCCESS_WITH_ERROR_shouldExecuteMarkSagaComplete() {
    final NominalRollStudent nominalRollStudent = this.createMockNominalRollStudent();
    NominalRollStudentEntity entity = NominalRollStudentMapper.mapper.toModel(nominalRollStudent);
    entity.setCreateDate(LocalDateTime.now().minusMinutes(14));
    entity.setUpdateDate(LocalDateTime.now());
    entity.setCreateUser(ApplicationProperties.API_NAME);
    entity.setUpdateUser(ApplicationProperties.API_NAME);
    entity = this.testHelper.getRepository().save(entity);
    nominalRollStudent.setNominalRollStudentID(entity.getNominalRollStudentID().toString());

    val saga = this.creatMockSaga(nominalRollStudent);
    saga.setSagaId(null);
    saga.setStatus(IN_PROGRESS.toString());
    saga.setNominalRollStudentID(UUID.fromString(nominalRollStudent.getNominalRollStudentID()));
    this.testHelper.getSagaRepository().save(saga);

    val event = Event.builder()
      .sagaId(saga.getSagaId())
      .eventType(EventType.VALIDATE_NOMINAL_ROLL_STUDENT)
      .eventOutcome(VALIDATION_SUCCESS_WITH_ERROR)
      .eventPayload("{\"Gender\":\"Gender code M is not recognized.\",\"Grade\":\"Grade code 01 is not recognized.\",\"School Number\":\"Field value 5465 is not recognized.\",\"School District\":\"School District Number 5 is not recognized.\"}").build();
    this.nominalRollStudentProcessingOrchestrator.handleEvent(event);
    val savedSagaInDB = this.testHelper.getSagaRepository().findById(saga.getSagaId());
    assertThat(savedSagaInDB).isPresent();
    assertThat(savedSagaInDB.get().getStatus()).isEqualTo(COMPLETED.toString());
    assertThat(savedSagaInDB.get().getSagaState()).isEqualTo(COMPLETED.toString());
    val savedNominalRollStudent = this.testHelper.getRepository().findById(entity.getNominalRollStudentID());
    assertThat(savedNominalRollStudent).isPresent();
    assertThat(savedNominalRollStudent.get().getStatus()).isEqualTo("ERROR");
  }

  @SneakyThrows
  @Test
  public void testHandleEvent_givenEventTypeVALIDATE_NOMINAL_ROLL_STUDENTAndEventOutComeVALIDATION_SUCCESS_NO_ERROR_shouldExecuteProcessPenMatch() {
    when(this.restUtils.getFedProvSchoolCodes()).thenReturn(Map.of("5465", "10200001"));
    final NominalRollStudent nominalRollStudent = this.createMockNominalRollStudent();
    NominalRollStudentEntity entity = NominalRollStudentMapper.mapper.toModel(nominalRollStudent);
    entity.setCreateDate(LocalDateTime.now().minusMinutes(14));
    entity.setUpdateDate(LocalDateTime.now());
    entity.setCreateUser(ApplicationProperties.API_NAME);
    entity.setUpdateUser(ApplicationProperties.API_NAME);
    entity = this.testHelper.getRepository().save(entity);
    nominalRollStudent.setNominalRollStudentID(entity.getNominalRollStudentID().toString());

    val saga = this.creatMockSaga(nominalRollStudent);
    saga.setSagaId(null);
    saga.setStatus(IN_PROGRESS.toString());
    saga.setNominalRollStudentID(UUID.fromString(nominalRollStudent.getNominalRollStudentID()));
    this.testHelper.getSagaRepository().save(saga);

    val event = Event.builder()
      .sagaId(saga.getSagaId())
      .eventType(EventType.VALIDATE_NOMINAL_ROLL_STUDENT)
      .eventOutcome(VALIDATION_SUCCESS_NO_ERROR)
      .eventPayload("").build();
    this.nominalRollStudentProcessingOrchestrator.handleEvent(event);
    val savedSagaInDB = this.testHelper.getSagaRepository().findById(saga.getSagaId());
    assertThat(savedSagaInDB).isPresent();
    assertThat(savedSagaInDB.get().getStatus()).isEqualTo(IN_PROGRESS.toString());
    assertThat(savedSagaInDB.get().getSagaState()).isEqualTo(PROCESS_PEN_MATCH.toString());
    val savedNominalRollStudent = this.testHelper.getRepository().findById(entity.getNominalRollStudentID());
    assertThat(savedNominalRollStudent).isPresent();
    assertThat(savedNominalRollStudent.get().getStatus()).isEqualTo("LOADED");
    verify(this.messagePublisher, atMost(1)).dispatchMessage(eq(PEN_MATCH_API_TOPIC.toString()), this.eventCaptor.capture());
    final var newEvent = JsonUtil.getJsonObjectFromString(Event.class, new String(this.eventCaptor.getValue()));
    assertThat(newEvent.getEventType()).isEqualTo(PROCESS_PEN_MATCH);
    assertThat(newEvent.getEventOutcome()).isNull();
    assertThat(newEvent.getEventPayload()).isEqualTo("{\"pen\":null,\"dob\":\"19070526\",\"sex\":\"M\",\"enrolledGradeCode\":\"01\",\"surname\":\"Wayne\",\"givenName\":\"John\",\"middleName\":null,\"usualSurname\":null,\"usualGivenName\":null,\"usualMiddleName\":null,\"mincode\":\"10200001\",\"localID\":null,\"postal\":null}");
  }

  @SneakyThrows
  @Test
  public void testHandleEvent_givenEventTypeVALIDATE_NOMINAL_ROLL_STUDENTAndEventOutComeVALIDATION_SUCCESS_NO_ERRORAndExactMatchInPostedStudent_shouldMarkSagaComplete() {
    final NominalRollStudent nominalRollStudent = this.createMockNominalRollStudent();
    NominalRollStudentEntity entity = NominalRollStudentMapper.mapper.toModel(nominalRollStudent);
    entity.setCreateDate(LocalDateTime.now().minusMinutes(14));
    entity.setUpdateDate(LocalDateTime.now());
    entity.setCreateUser(ApplicationProperties.API_NAME);
    entity.setUpdateUser(ApplicationProperties.API_NAME);
    entity = this.testHelper.getRepository().save(entity);
    val postedEntity = NominalRollStudentMapper.mapper.toPostedEntity(entity);
    postedEntity.setAssignedPEN("123456789");
    this.testHelper.getPostedStudentRepository().save(postedEntity);
    nominalRollStudent.setNominalRollStudentID(entity.getNominalRollStudentID().toString());

    val saga = this.creatMockSaga(nominalRollStudent);
    saga.setSagaId(null);
    saga.setStatus(IN_PROGRESS.toString());
    saga.setNominalRollStudentID(UUID.fromString(nominalRollStudent.getNominalRollStudentID()));
    this.testHelper.getSagaRepository().save(saga);

    val event = Event.builder()
      .sagaId(saga.getSagaId())
      .eventType(EventType.VALIDATE_NOMINAL_ROLL_STUDENT)
      .eventOutcome(VALIDATION_SUCCESS_NO_ERROR)
      .eventPayload("").build();
    this.nominalRollStudentProcessingOrchestrator.handleEvent(event);
    val savedSagaInDB = this.testHelper.getSagaRepository().findById(saga.getSagaId());
    assertThat(savedSagaInDB).isPresent();
    assertThat(savedSagaInDB.get().getStatus()).isEqualTo(COMPLETED.toString());
    assertThat(savedSagaInDB.get().getSagaState()).isEqualTo(COMPLETED.toString());
    val savedNominalRollStudent = this.testHelper.getRepository().findById(entity.getNominalRollStudentID());
    assertThat(savedNominalRollStudent).isPresent();
    assertThat(savedNominalRollStudent.get().getAssignedPEN()).isEqualTo(postedEntity.getAssignedPEN());
    assertThat(savedNominalRollStudent.get().getStatus()).isEqualTo(MATCHEDSYS.toString());
  }

  @SneakyThrows
  @Test
  public void testHandleEvent_givenEventTypePROCESS_PEN_MATCHAndEventOutComePEN_MATCH_PROCESSEDAA_shouldExecutePROCESS_PEN_MATCH_RESULTS() {
    this.runBasedOnPenStatus("AA", MATCHEDSYS.toString(), "123456789");
  }

  @SneakyThrows
  @Test
  public void testHandleEvent_givenEventTypePROCESS_PEN_MATCHAndEventOutComePEN_MATCH_PROCESSEDB1_shouldExecutePROCESS_PEN_MATCH_RESULTS() {
    this.runBasedOnPenStatus("B1", MATCHEDSYS.toString(), "123456789");
  }

  @SneakyThrows
  @Test
  public void testHandleEvent_givenEventTypePROCESS_PEN_MATCHAndEventOutComePEN_MATCH_PROCESSEDC1_shouldExecutePROCESS_PEN_MATCH_RESULTS() {
    this.runBasedOnPenStatus("C1", MATCHEDSYS.toString(), "123456789");
  }

  @SneakyThrows
  @Test
  public void testHandleEvent_givenEventTypePROCESS_PEN_MATCHAndEventOutComePEN_MATCH_PROCESSEDD1_shouldExecutePROCESS_PEN_MATCH_RESULTS() {
    this.runBasedOnPenStatus("D1", MATCHEDSYS.toString(), "123456789");
  }

  @SneakyThrows
  @Test
  public void testHandleEvent_givenEventTypePROCESS_PEN_MATCHAndEventOutComePEN_MATCH_PROCESSEDOther_shouldExecutePROCESS_PEN_MATCH_RESULTS() {
    this.runBasedOnPenStatus("F1", FIXABLE.toString(), null);
  }

  @SneakyThrows
  @Test
  public void testHandleEvent_givenEventTypePROCESS_PEN_MATCH_RESULTSAndEventOutComePEN_MATCH_RESULTS_PROCESSED_shouldExecuteMarkSagaComplete() {
    when(this.restUtils.getFedProvSchoolCodes()).thenReturn(Map.of("5465", "10200001"));
    final NominalRollStudent nominalRollStudent = this.createMockNominalRollStudent();
    NominalRollStudentEntity entity = NominalRollStudentMapper.mapper.toModel(nominalRollStudent);
    entity.setCreateDate(LocalDateTime.now().minusMinutes(14));
    entity.setUpdateDate(LocalDateTime.now());
    entity.setCreateUser(ApplicationProperties.API_NAME);
    entity.setUpdateUser(ApplicationProperties.API_NAME);
    entity = this.testHelper.getRepository().save(entity);
    nominalRollStudent.setNominalRollStudentID(entity.getNominalRollStudentID().toString());

    val saga = this.creatMockSaga(nominalRollStudent);
    saga.setSagaId(null);
    saga.setStatus(IN_PROGRESS.toString());
    saga.setNominalRollStudentID(UUID.fromString(nominalRollStudent.getNominalRollStudentID()));
    this.testHelper.getSagaRepository().save(saga);

    val event = Event.builder()
      .sagaId(saga.getSagaId())
      .eventType(PROCESS_PEN_MATCH_RESULTS)
      .eventOutcome(PEN_MATCH_RESULTS_PROCESSED)
      .eventPayload("D1").build();
    this.nominalRollStudentProcessingOrchestrator.handleEvent(event);
    val savedSagaInDB = this.testHelper.getSagaRepository().findById(saga.getSagaId());
    assertThat(savedSagaInDB).isPresent();
    assertThat(savedSagaInDB.get().getStatus()).isEqualTo(COMPLETED.toString());
    assertThat(savedSagaInDB.get().getSagaState()).isEqualTo(COMPLETED.toString());
    val savedNominalRollStudent = this.testHelper.getRepository().findById(entity.getNominalRollStudentID());
    assertThat(savedNominalRollStudent).isPresent();
    assertThat(savedNominalRollStudent.get().getStatus()).isEqualTo("LOADED");
    verify(this.messagePublisher, atMost(1)).dispatchMessage(eq(nominalRollStudentProcessingOrchestrator.getTopicToSubscribe()), this.eventCaptor.capture());
    final var newEvent = JsonUtil.getJsonObjectFromString(Event.class, new String(this.eventCaptor.getValue()));
    assertThat(newEvent.getEventType()).isEqualTo(MARK_SAGA_COMPLETE);
    assertThat(newEvent.getEventOutcome()).isEqualTo(SAGA_COMPLETED);
    assertThat(newEvent.getEventPayload()).isBlank();
  }

  private void runBasedOnPenStatus(final String penStatus, final String status, final String pen) throws InterruptedException, IOException, TimeoutException {
    final NominalRollStudent nominalRollStudent = this.createMockNominalRollStudent();
    NominalRollStudentEntity entity = NominalRollStudentMapper.mapper.toModel(nominalRollStudent);
    entity.setCreateDate(LocalDateTime.now().minusMinutes(14));
    entity.setUpdateDate(LocalDateTime.now());
    entity.setCreateUser(ApplicationProperties.API_NAME);
    entity.setUpdateUser(ApplicationProperties.API_NAME);
    entity = this.testHelper.getRepository().save(entity);
    nominalRollStudent.setNominalRollStudentID(entity.getNominalRollStudentID().toString());

    val saga = this.creatMockSaga(nominalRollStudent);
    saga.setSagaId(null);
    saga.setStatus(IN_PROGRESS.toString());
    saga.setNominalRollStudentID(UUID.fromString(nominalRollStudent.getNominalRollStudentID()));
    this.testHelper.getSagaRepository().save(saga);
    final List<PenMatchRecord> matchRecords = new ArrayList<>();
    final PenMatchRecord record = new PenMatchRecord();
    record.setMatchingPEN(pen);
    record.setStudentID(UUID.randomUUID().toString());
    matchRecords.add(record);
    final var eventPayload = new PenMatchResult();
    eventPayload.setPenStatus(penStatus);
    eventPayload.setMatchingRecords(matchRecords);
    val event = Event.builder()
      .sagaId(saga.getSagaId())
      .eventType(PROCESS_PEN_MATCH)
      .eventOutcome(PEN_MATCH_PROCESSED)
      .eventPayload(JsonUtil.getJsonStringFromObject(eventPayload))
      .build();
    this.nominalRollStudentProcessingOrchestrator.handleEvent(event);
    val savedSagaInDB = this.testHelper.getSagaRepository().findById(saga.getSagaId());
    assertThat(savedSagaInDB).isPresent();
    assertThat(savedSagaInDB.get().getStatus()).isEqualTo(IN_PROGRESS.toString());
    assertThat(savedSagaInDB.get().getSagaState()).isEqualTo(PROCESS_PEN_MATCH_RESULTS.toString());
    val savedNominalRollStudent = this.testHelper.getRepository().findById(entity.getNominalRollStudentID());
    assertThat(savedNominalRollStudent).isPresent();
    assertThat(savedNominalRollStudent.get().getAssignedPEN()).isEqualTo(pen);
    assertThat(savedNominalRollStudent.get().getStatus()).isEqualTo(status);
    verify(this.messagePublisher, atMost(1)).dispatchMessage(eq(this.nominalRollStudentProcessingOrchestrator.getTopicToSubscribe()), this.eventCaptor.capture());
    final var newEvent = JsonUtil.getJsonObjectFromString(Event.class, new String(this.eventCaptor.getValue()));
    assertThat(newEvent.getEventType()).isEqualTo(PROCESS_PEN_MATCH_RESULTS);
    assertThat(newEvent.getEventOutcome()).isEqualTo(PEN_MATCH_RESULTS_PROCESSED);
    assertThat(newEvent.getEventPayload()).isEqualTo(penStatus);
  }

}
