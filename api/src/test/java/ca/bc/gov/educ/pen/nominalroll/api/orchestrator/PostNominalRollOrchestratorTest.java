package ca.bc.gov.educ.pen.nominalroll.api.orchestrator;

import ca.bc.gov.educ.pen.nominalroll.api.BaseNominalRollAPITest;
import ca.bc.gov.educ.pen.nominalroll.api.constants.EventOutcome;
import ca.bc.gov.educ.pen.nominalroll.api.constants.EventType;
import ca.bc.gov.educ.pen.nominalroll.api.constants.TopicsEnum;
import ca.bc.gov.educ.pen.nominalroll.api.constants.v1.NominalRollStudentStatus;
import ca.bc.gov.educ.pen.nominalroll.api.helpers.NominalRollHelper;
import ca.bc.gov.educ.pen.nominalroll.api.mappers.v1.NominalRollStudentMapper;
import ca.bc.gov.educ.pen.nominalroll.api.messaging.MessagePublisher;
import ca.bc.gov.educ.pen.nominalroll.api.model.v1.Saga;
import ca.bc.gov.educ.pen.nominalroll.api.repository.v1.NominalRollPostedStudentRepository;
import ca.bc.gov.educ.pen.nominalroll.api.repository.v1.NominalRollStudentRepository;
import ca.bc.gov.educ.pen.nominalroll.api.repository.v1.SagaEventRepository;
import ca.bc.gov.educ.pen.nominalroll.api.repository.v1.SagaRepository;
import ca.bc.gov.educ.pen.nominalroll.api.service.v1.SagaService;
import ca.bc.gov.educ.pen.nominalroll.api.struct.v1.Event;
import ca.bc.gov.educ.pen.nominalroll.api.struct.v1.NominalRollPostSagaData;
import ca.bc.gov.educ.pen.nominalroll.api.struct.v1.NominalRollStudent;
import ca.bc.gov.educ.pen.nominalroll.api.util.JsonUtil;
import ca.bc.gov.educ.pen.nominalroll.api.util.TransformUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;


import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import static ca.bc.gov.educ.pen.nominalroll.api.constants.EventType.CREATE_SLD_DIA_STUDENTS;
import static ca.bc.gov.educ.pen.nominalroll.api.constants.SagaEnum.NOMINAL_ROLL_POST_DATA_SAGA;
import static net.javacrumbs.jsonunit.assertj.JsonAssertions.value;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class PostNominalRollOrchestratorTest extends BaseNominalRollAPITest {
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

  @Autowired
  NominalRollStudentRepository studentRepository;

  @Autowired
  NominalRollPostedStudentRepository postedStudentRepository;

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

  private static final NominalRollStudentMapper mapper = NominalRollStudentMapper.mapper;

  /**
   * Sets up.
   */
  @Before
  public void setUp() throws IOException {
    MockitoAnnotations.openMocks(this);
    var payload = placeholderNominalRollPostSagaData();
    sagaData = getNominalRollSagaDataFromJsonString(payload);
    sagaPayload = JsonUtil.getJsonStringFromObject(sagaData);
    saga = sagaService.createSagaRecordInDB(NOMINAL_ROLL_POST_DATA_SAGA.toString(), "Test",
      sagaPayload, UUID.fromString(studentID), "2021");
    final var file = new File(
      Objects.requireNonNull(this.getClass().getClassLoader().getResource("mock_nom_students.json")).getFile()
    );
    final List<NominalRollStudent> entities = new ObjectMapper().readValue(file, new TypeReference<>() {
    });

    this.studentRepository.saveAll(entities.stream().map(student -> {
      var entity = mapper.toModel(student);
      return TransformUtil.uppercaseFields(entity);
    }).collect(Collectors.toList()));
  }

  /**
   * After.
   */
  @After
  public void after() {
    sagaEventRepository.deleteAll();
    repository.deleteAll();
    studentRepository.deleteAll();
    postedStudentRepository.deleteAll();
  }

  @Test
  public void testSaveNominalRollPostedStudents_givenEventAndSagaData_shouldSavePostedStudentsAndPostEventToSldApi() throws IOException, InterruptedException, TimeoutException {
    var invocations = mockingDetails(messagePublisher).getInvocations().size();
    when(this.restUtils.getFedProvSchoolCodes()).thenReturn(Map.of("4858", "10200001", "4859", "10200002", "9654", "10300001"));

    var event = Event.builder()
      .eventType(EventType.INITIATED)
      .eventOutcome(EventOutcome.INITIATE_SUCCESS)
      .sagaId(saga.getSagaId())
      .nominalRollStudentID(studentID)
      .build();

    orchestrator.handleEvent(event);
    verify(messagePublisher, atMost(invocations + 1)).dispatchMessage(eq(TopicsEnum.SLD_API_TOPIC.toString()), eventCaptor.capture());
    var newEvent = JsonUtil.getJsonObjectFromString(Event.class, new String(eventCaptor.getValue()));
    assertThat(newEvent.getEventType()).isEqualTo(CREATE_SLD_DIA_STUDENTS);
    assertThat(newEvent.getEventPayload()).isNotEmpty();
    assertThatJson(newEvent.getEventPayload()).isArray().hasSize(4);
    assertThatJson(newEvent.getEventPayload()).inPath("$[*].distNo").isArray().contains(value("102"), value("103"));
    assertThatJson(newEvent.getEventPayload()).inPath("$[*].recordNumber").isArray().containsExactly(value(1), value(2), value(3), value(4));
    assertThatJson(newEvent.getEventPayload()).inPath("$[*].reportDate").isArray().contains(value(20200930),value(20200930),value(20200930),value(20200930));

    var firstAndLastDays = NominalRollHelper.getFirstAndLastDateTimesOfYear("2021");
    var postedStudents = postedStudentRepository.findAllByProcessingYearBetween(firstAndLastDays.getLeft(), firstAndLastDays.getRight());
    assertThat(postedStudents.size()).isEqualTo(4);
    assertThat(postedStudents.get(0).getUpdateUser()).isEqualTo("test");
    assertThat(postedStudents.get(0).getCreateUser()).isEqualTo("test");

    var ignoredStudents = studentRepository.findTop100ByStatusOrderByCreateDate(NominalRollStudentStatus.IGNORED.getCode());
    assertThat(ignoredStudents.size()).isEqualTo(2);
    assertThat(ignoredStudents.get(0).getStatus()).isEqualTo(NominalRollStudentStatus.IGNORED.toString());
    assertThat(ignoredStudents.get(0).getUpdateUser()).isEqualTo("test");

    var sagaFromDB = sagaService.findSagaById(saga.getSagaId());
    assertThat(sagaFromDB).isPresent();
    assertThat(sagaFromDB.get().getSagaState()).isEqualTo(CREATE_SLD_DIA_STUDENTS.toString());
    var sagaStates = sagaService.findAllSagaStates(saga);
    assertThat(sagaStates.size()).isEqualTo(2);
    assertThat(sagaStates.get(0).getSagaEventState()).isEqualTo(EventType.INITIATED.toString());
    assertThat(sagaStates.get(0).getSagaEventOutcome()).isEqualTo(EventOutcome.INITIATE_SUCCESS.toString());
    assertThat(sagaStates.get(1).getSagaEventState()).isEqualTo(EventType.SAVE_NOMINAL_ROLL_POSTED_STUDENTS.toString());
    assertThat(sagaStates.get(1).getSagaEventOutcome()).isEqualTo(EventOutcome.NOMINAL_ROLL_POSTED_STUDENTS_SAVED.toString());
  }

  @Test
  public void testSaveNominalRollPostedStudents_givenPostedStudentsExist_shouldPostEventToSldApi() throws IOException, InterruptedException, TimeoutException {
    var invocations = mockingDetails(messagePublisher).getInvocations().size();
    when(this.restUtils.getFedProvSchoolCodes()).thenReturn(Map.of("4858", "10200001", "4859", "10200002", "9654", "10300001"));

    var students = studentRepository.findAllByProcessingYear("2021");
    var studentsToBePosted = students.stream()
      .filter(student -> !StringUtils.isBlank(student.getAssignedPEN()))
      .map(NominalRollStudentMapper.mapper::toPostedEntity)
      .collect(Collectors.toList());
    postedStudentRepository.saveAll(studentsToBePosted);

    var event = Event.builder()
      .eventType(EventType.INITIATED)
      .eventOutcome(EventOutcome.INITIATE_SUCCESS)
      .sagaId(saga.getSagaId())
      .nominalRollStudentID(studentID)
      .build();

    orchestrator.handleEvent(event);
    verify(messagePublisher, atMost(invocations + 1)).dispatchMessage(eq(TopicsEnum.SLD_API_TOPIC.toString()), eventCaptor.capture());
    var newEvent = JsonUtil.getJsonObjectFromString(Event.class, new String(eventCaptor.getValue()));
    assertThat(newEvent.getEventType()).isEqualTo(CREATE_SLD_DIA_STUDENTS);
    assertThat(newEvent.getEventPayload()).isNotEmpty();
    assertThatJson(newEvent.getEventPayload()).isArray().hasSize(4);
    assertThatJson(newEvent.getEventPayload()).inPath("$[*].distNo").isArray().contains(value("102"), value("103"));

    var firstAndLastDays = NominalRollHelper.getFirstAndLastDateTimesOfYear("2021");
    var postedStudents = postedStudentRepository.findAllByProcessingYearBetween(firstAndLastDays.getLeft(), firstAndLastDays.getRight());
    assertThat(postedStudents.size()).isEqualTo(4);

    var sagaFromDB = sagaService.findSagaById(saga.getSagaId());
    assertThat(sagaFromDB).isPresent();
    assertThat(sagaFromDB.get().getSagaState()).isEqualTo(CREATE_SLD_DIA_STUDENTS.toString());
    var sagaStates = sagaService.findAllSagaStates(saga);
    assertThat(sagaStates.size()).isEqualTo(2);
    assertThat(sagaStates.get(0).getSagaEventState()).isEqualTo(EventType.INITIATED.toString());
    assertThat(sagaStates.get(0).getSagaEventOutcome()).isEqualTo(EventOutcome.INITIATE_SUCCESS.toString());
    assertThat(sagaStates.get(1).getSagaEventState()).isEqualTo(EventType.SAVE_NOMINAL_ROLL_POSTED_STUDENTS.toString());
    assertThat(sagaStates.get(1).getSagaEventOutcome()).isEqualTo(EventOutcome.NOMINAL_ROLL_POSTED_STUDENTS_SAVED.toString());
  }

  /**
   * Dummy split pen saga data json string.
   *
   * @return the string
   */
  protected String placeholderNominalRollPostSagaData() {
    return " {\n" +
      "    \"processingYear\": \"2021\",\n" +
      "    \"updateUser\": \"test\"\n" +
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
