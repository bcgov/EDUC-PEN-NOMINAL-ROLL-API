package ca.bc.gov.educ.pen.nominalroll.api.schedulers;

import ca.bc.gov.educ.pen.nominalroll.api.BaseNominalRollAPITest;
import ca.bc.gov.educ.pen.nominalroll.api.constants.SagaEnum;
import ca.bc.gov.educ.pen.nominalroll.api.mappers.v1.NominalRollStudentMapper;
import ca.bc.gov.educ.pen.nominalroll.api.messaging.MessagePublisher;
import ca.bc.gov.educ.pen.nominalroll.api.model.v1.Saga;
import ca.bc.gov.educ.pen.nominalroll.api.properties.ApplicationProperties;
import ca.bc.gov.educ.pen.nominalroll.api.struct.v1.NominalRollStudent;
import ca.bc.gov.educ.pen.nominalroll.api.struct.v1.NominalRollStudentSagaData;
import ca.bc.gov.educ.pen.nominalroll.api.util.JsonUtil;
import lombok.SneakyThrows;
import lombok.val;
import net.javacrumbs.shedlock.core.LockAssert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static ca.bc.gov.educ.pen.nominalroll.api.constants.EventType.INITIATED;
import static ca.bc.gov.educ.pen.nominalroll.api.constants.SagaStatusEnum.IN_PROGRESS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class EventTaskSchedulerTest extends BaseNominalRollAPITest {

  @Autowired
  EventTaskScheduler eventTaskScheduler;
  @Autowired
  MessagePublisher messagePublisher;

  @Before
  public void init() {
    Mockito.reset(this.messagePublisher);
    LockAssert.TestHelper.makeAllAssertsPass(true);
  }

  @Test
  public void testFindAndProcessPendingSagaEvents_givenInProgressSagas_shouldBeRetried() {
    var studentEntity = this.testHelper.getRepository().save(this.createNominalRollStudentEntity());
    val saga = this.testHelper.getSagaRepository().save(this.creatMockSaga(createMockNominalRollStudent(studentEntity.getNominalRollStudentID().toString())));
    this.eventTaskScheduler.findAndProcessPendingSagaEvents();
    val sagaEvents = this.testHelper.getSagaEventRepository().findBySaga(saga);
    assertThat(sagaEvents).isNotEmpty();
    assertThat(sagaEvents.size()).isEqualTo(1);
  }

  @Test
  public void testProcessLoadedNominalRollStudents_givenRecordInLOADEDSTATE_shouldBeRetried() throws InterruptedException {
    val entity = NominalRollStudentMapper.mapper.toModel(this.createMockNominalRollStudent(null));
    entity.setCreateDate(LocalDateTime.now().minusMinutes(14));
    entity.setUpdateDate(LocalDateTime.now());
    entity.setCreateUser(ApplicationProperties.API_NAME);
    entity.setUpdateUser(ApplicationProperties.API_NAME);
    this.testHelper.getRepository().save(entity);
    this.eventTaskScheduler.processLoadedNominalRollStudents();
    verify(this.messagePublisher, times(1)).dispatchMessage(anyString(), any());
  }
}
