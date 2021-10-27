package ca.bc.gov.educ.pen.nominalroll.api.orchestrator;

import ca.bc.gov.educ.pen.nominalroll.api.BaseNominalRollAPITest;
import ca.bc.gov.educ.pen.nominalroll.api.constants.EventOutcome;
import ca.bc.gov.educ.pen.nominalroll.api.constants.EventType;
import ca.bc.gov.educ.pen.nominalroll.api.messaging.MessagePublisher;
import ca.bc.gov.educ.pen.nominalroll.api.model.v1.Saga;
import ca.bc.gov.educ.pen.nominalroll.api.repository.v1.SagaEventRepository;
import ca.bc.gov.educ.pen.nominalroll.api.repository.v1.SagaRepository;
import ca.bc.gov.educ.pen.nominalroll.api.service.v1.SagaService;
import ca.bc.gov.educ.pen.nominalroll.api.struct.v1.Event;
import ca.bc.gov.educ.pen.nominalroll.api.struct.v1.NominalRollPostSagaData;
import ca.bc.gov.educ.pen.nominalroll.api.util.JsonUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

import static ca.bc.gov.educ.pen.nominalroll.api.constants.EventType.CREATE_DIA_STUDENTS;
import static ca.bc.gov.educ.pen.nominalroll.api.constants.EventType.MARK_SAGA_COMPLETE;
import static ca.bc.gov.educ.pen.nominalroll.api.constants.SagaEnum.NOMINAL_ROLL_POST_SAGA;
import static ca.bc.gov.educ.pen.nominalroll.api.constants.SagaStatusEnum.COMPLETED;
import static ca.bc.gov.educ.pen.nominalroll.api.constants.TopicsEnum.NOMINAL_ROLL_API_TOPIC;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class NominalRollOrchestratorTest extends BaseNominalRollAPITest {
  /**
   * The Repository.
   */
  @Autowired
  SagaRepository repository;
  /**
   * The Saga event repository.
   */
  @Autowired
  SagaEventRepository sagaEventRepository;
  /**
   * The Saga service.
   */
  @Autowired
  private SagaService sagaService;

  /**
   * The Message publisher.
   */
  @Autowired
  private MessagePublisher messagePublisher;

  @Autowired
  private PostNominalRollOrchestrator orchestrator;

  /**
   * The Saga.
   */
  private Saga saga;
  /**
   * The Saga data.
   */
  private NominalRollPostSagaData sagaData;

  /**
   * The Event captor.
   */
  @Captor
  ArgumentCaptor<byte[]> eventCaptor;

  String sagaPayload;

  String studentID = UUID.randomUUID().toString();

  /**
   * Sets up.
   */
  @Before
  public void setUp() throws JsonProcessingException {
    MockitoAnnotations.openMocks(this);
    var payload = placeholderSplitPenSagaData();
    sagaData = getNominalRollSagaDataFromJsonString(payload);
    sagaData.setStudents(new ArrayList<>());
    sagaPayload = JsonUtil.getJsonStringFromObject(sagaData);
    saga = sagaService.createSagaRecordInDB(NOMINAL_ROLL_POST_SAGA.toString(), "Test",
      sagaPayload, UUID.fromString(studentID));
  }

  /**
   * After.
   */
  @After
  public void after() {
    sagaEventRepository.deleteAll();
    repository.deleteAll();
  }

  @Test
  public void testUpdateOriginalStudent_givenEventAndSagaData_shouldPostEventToStudentApi() throws IOException, InterruptedException, TimeoutException {
    var invocations = mockingDetails(messagePublisher).getInvocations().size();
    var event = Event.builder()
      .eventType(CREATE_DIA_STUDENTS)
      .eventOutcome(EventOutcome.DIA_STUDENTS_CREATED)
      .sagaId(saga.getSagaId())
      .nominalRollStudentID(studentID)
      .build();

    orchestrator.handleEvent(event);
    verify(messagePublisher, atMost(invocations + 1)).dispatchMessage(eq(orchestrator.getTopicToSubscribe()), eventCaptor.capture());
    var newEvent = JsonUtil.getJsonObjectFromString(Event.class, new String(eventCaptor.getValue()));
    assertThat(newEvent.getEventType()).isEqualTo(MARK_SAGA_COMPLETE);
    var sagaFromDB = sagaService.findSagaById(saga.getSagaId());
    assertThat(sagaFromDB).isPresent();
    assertThat(sagaFromDB.get().getSagaState()).isEqualTo(COMPLETED.toString());
    var sagaStates = sagaService.findAllSagaStates(saga);
    assertThat(sagaStates.size()).isEqualTo(1);
    assertThat(sagaStates.get(0).getSagaEventState()).isEqualTo(EventType.CREATE_DIA_STUDENTS.toString());
    assertThat(sagaStates.get(0).getSagaEventOutcome()).isEqualTo(EventOutcome.DIA_STUDENTS_CREATED.toString());
  }

  /**
   * Dummy split pen saga data json string.
   *
   * @return the string
   */
  protected String placeholderSplitPenSagaData() {
    return " {\n" +
      "    \"createUser\": \"test\",\n" +
      "    \"updateUser\": \"test\",\n" +
      "    \"studentID\": \"" + studentID + "\",\n" +
      "    \"historyActivityCode\": \"MERGE\",\n" +
      "    \"legalFirstName\": \"Jack\",\n" +
      "    \"newStudent\": {\n" +
      "       \"studentID\": \"" + studentID + "\",\n" +
      "       \"legalFirstName\": \"Jack\"\n" +
      "    }\n" +
      "  }";
  }

  /**
   * Gets split pen saga data from json string.
   *
   * @param json the json
   * @return the split pen saga data from json string
   */
  protected NominalRollPostSagaData getNominalRollSagaDataFromJsonString(String json) {
    try {
      return JsonUtil.getJsonObjectFromString(NominalRollPostSagaData.class, json);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

}
