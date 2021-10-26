package ca.bc.gov.educ.pen.nominalroll.api.orchestrator;

import ca.bc.gov.educ.pen.nominalroll.api.BaseNominalRollAPITest;
import ca.bc.gov.educ.pen.nominalroll.api.constants.EventOutcome;
import ca.bc.gov.educ.pen.nominalroll.api.constants.EventType;
import ca.bc.gov.educ.pen.nominalroll.api.constants.GradeCodes;
import ca.bc.gov.educ.pen.nominalroll.api.mappers.v1.NominalRollStudentMapper;
import ca.bc.gov.educ.pen.nominalroll.api.messaging.MessagePublisher;
import ca.bc.gov.educ.pen.nominalroll.api.properties.ApplicationProperties;
import ca.bc.gov.educ.pen.nominalroll.api.struct.external.student.v1.GenderCode;
import ca.bc.gov.educ.pen.nominalroll.api.struct.external.student.v1.GradeCode;
import ca.bc.gov.educ.pen.nominalroll.api.struct.v1.Event;
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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static ca.bc.gov.educ.pen.nominalroll.api.constants.EventOutcome.VALIDATION_SUCCESS_NO_ERROR;
import static ca.bc.gov.educ.pen.nominalroll.api.constants.EventOutcome.VALIDATION_SUCCESS_WITH_ERROR;
import static ca.bc.gov.educ.pen.nominalroll.api.constants.EventType.VALIDATE_NOMINAL_ROLL_STUDENT;
import static ca.bc.gov.educ.pen.nominalroll.api.constants.SagaStatusEnum.IN_PROGRESS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.when;

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
    when(restUtils.getActiveGenderCodes()).thenReturn(List.of(GenderCode.builder().genderCode("M").build()));
    List<GradeCode> gradeCodes = new ArrayList<>();
    for (GradeCodes grade : GradeCodes.values()) {
      gradeCodes.add(GradeCode.builder().gradeCode(grade.getCode()).build());
    }
    when(restUtils.getActiveGradeCodes()).thenReturn(gradeCodes);
    when(restUtils.getFedProvSchoolCodes()).thenReturn(Map.of("5465", "10200001"));
    when(restUtils.districtCodes()).thenReturn(List.of("102", "103", "021", "006","005"));
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
}
