package ca.bc.gov.educ.pen.nominalroll.api.service.events;

import ca.bc.gov.educ.pen.nominalroll.api.BaseNominalRollAPITest;
import ca.bc.gov.educ.pen.nominalroll.api.constants.EventOutcome;
import ca.bc.gov.educ.pen.nominalroll.api.constants.EventType;
import ca.bc.gov.educ.pen.nominalroll.api.constants.TopicsEnum;
import ca.bc.gov.educ.pen.nominalroll.api.mappers.v1.NominalRollStudentMapper;
import ca.bc.gov.educ.pen.nominalroll.api.struct.v1.Event;
import ca.bc.gov.educ.pen.nominalroll.api.struct.v1.NominalRollStudentSagaData;
import ca.bc.gov.educ.pen.nominalroll.api.util.JsonUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.val;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

public class EventHandlerDelegatorServiceTest extends BaseNominalRollAPITest {
  @Autowired
  EventHandlerDelegatorService eventHandlerDelegatorService;

  @Before
  public void setUp() throws Exception {
  }

  @Test
  public void testHandleEvent_givenEventTypeREADFROMTOPIC_shouldProcess() throws JsonProcessingException {
    val mockStudent = this.createMockNominalRollStudent(null);
    val savedStudent = this.testHelper.getRepository().save(NominalRollStudentMapper.mapper.toModel(mockStudent));
    val event = Event.builder().eventType(EventType.READ_FROM_TOPIC).eventOutcome(EventOutcome.READ_FROM_TOPIC_SUCCESS).eventPayload(JsonUtil.getJsonStringFromObject(NominalRollStudentSagaData.builder().nominalRollStudent(NominalRollStudentMapper.mapper.toStruct(savedStudent)).build())).build();
    this.eventHandlerDelegatorService.handleEvent(event);
    assertThat(this.testHelper.getSagaRepository().findAll()).isNotEmpty();
  }

  @Test
  public void testGetTopicToSubscribe__shouldReturnAPITopic() {
    assertThat(this.eventHandlerDelegatorService.getTopicToSubscribe()).isEqualTo(TopicsEnum.NOMINAL_ROLL_API_TOPIC.toString());
  }
}
