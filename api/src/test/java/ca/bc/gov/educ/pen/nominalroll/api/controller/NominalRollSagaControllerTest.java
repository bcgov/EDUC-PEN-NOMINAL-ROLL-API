package ca.bc.gov.educ.pen.nominalroll.api.controller;

import ca.bc.gov.educ.pen.nominalroll.api.BaseNominalRollAPITest;
import ca.bc.gov.educ.pen.nominalroll.api.controller.v1.NominalRollSagaController;
import ca.bc.gov.educ.pen.nominalroll.api.filter.FilterOperation;
import ca.bc.gov.educ.pen.nominalroll.api.mappers.SagaMapper;
import ca.bc.gov.educ.pen.nominalroll.api.repository.v1.NominalRollStudentRepository;
import ca.bc.gov.educ.pen.nominalroll.api.repository.v1.SagaEventRepository;
import ca.bc.gov.educ.pen.nominalroll.api.repository.v1.SagaRepository;
import ca.bc.gov.educ.pen.nominalroll.api.service.v1.SagaService;
import ca.bc.gov.educ.pen.nominalroll.api.struct.v1.Saga;
import ca.bc.gov.educ.pen.nominalroll.api.struct.v1.Search;
import ca.bc.gov.educ.pen.nominalroll.api.struct.v1.SearchCriteria;
import ca.bc.gov.educ.pen.nominalroll.api.struct.v1.ValueType;
import ca.bc.gov.educ.pen.nominalroll.api.util.JsonUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.val;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.File;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static ca.bc.gov.educ.pen.nominalroll.api.constants.SagaEnum.NOMINAL_ROLL_POST_DATA_SAGA;
import static ca.bc.gov.educ.pen.nominalroll.api.constants.v1.URL.BASE_URL;
import static ca.bc.gov.educ.pen.nominalroll.api.constants.v1.URL.SAGA;
import static ca.bc.gov.educ.pen.nominalroll.api.struct.v1.Condition.AND;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


public class NominalRollSagaControllerTest extends BaseNominalRollAPITest {
  @Autowired
  private MockMvc mockMvc;

  @Autowired
  NominalRollSagaController controller;

  @Autowired
  SagaRepository repository;

  @Autowired
  SagaEventRepository sagaEventRepository;

  @Autowired
  SagaService sagaService;

  @Autowired
  NominalRollStudentRepository studentRepository;

  private SagaMapper mapper = SagaMapper.mapper;

  private final String nominalRollStudentID = "7f000101-7151-1d84-8171-5187006c0001";

  @Before
  public void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  @After
  public void after() {
    sagaEventRepository.deleteAll();
    repository.deleteAll();
    studentRepository.deleteAll();
  }

  @Test
  public void testPostData_GivenInvalidPayload_ShouldReturnStatusBadRequest() throws Exception {
    this.mockMvc.perform(post(BASE_URL + SAGA + "/post-data")
            .with(jwt().jwt((jwt) -> jwt.claim("scope", "NOMINAL_ROLL_POST_DATA_SAGA")))
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .content(placeholderInvalidSagaData()))
            .andDo(print()).andExpect(status().isBadRequest());
  }

  @Test
  public void testPostData_GivenValidPayload_ShouldReturnStatusOk() throws Exception {
    this.mockMvc.perform(post(BASE_URL + SAGA + "/post-data")
      .with(jwt().jwt((jwt) -> jwt.claim("scope", "NOMINAL_ROLL_POST_DATA_SAGA")))
      .contentType(MediaType.APPLICATION_JSON)
      .accept(MediaType.APPLICATION_JSON).content(this.placeholderPostDataSagaData())).andDo(print())
      .andExpect(status().isOk()).andExpect(jsonPath("$").exists());
  }

  @Test
  public void testPostData_GivenOtherSagaWithSameProcessingYearInProcess_ShouldReturnStatusConflict() throws Exception {
    final var payload = this.placeholderPostDataSagaData();
    this.sagaService.createSagaRecordInDB(NOMINAL_ROLL_POST_DATA_SAGA.toString(), "Test", payload, UUID.fromString(this.nominalRollStudentID), "2021");
    this.mockMvc.perform(post(BASE_URL + SAGA + "/post-data")
      .with(jwt().jwt((jwt) -> jwt.claim("scope", "NOMINAL_ROLL_POST_DATA_SAGA")))
      .contentType(MediaType.APPLICATION_JSON)
      .accept(MediaType.APPLICATION_JSON).content(payload)).andDo(print()).andExpect(status().isConflict());
  }

  @Test
  public void testReadSaga_GivenInValidID_ShouldReturnStatusNotFound() throws Exception {
    this.mockMvc.perform(get(BASE_URL + SAGA + "/" + UUID.randomUUID().toString())
      .with(jwt().jwt((jwt) -> jwt.claim("scope", "NOMINAL_ROLL_READ_SAGA")))
      .contentType(MediaType.APPLICATION_JSON)
      .accept(MediaType.APPLICATION_JSON))
      .andDo(print()).andExpect(status().isNotFound());
  }

  @Test
  public void testReadSaga_GivenValidID_ShouldReturnStatusOK() throws Exception {
    final var payload = this.placeholderPostDataSagaData();
    final var sagaFromDB = this.sagaService.createSagaRecordInDB(NOMINAL_ROLL_POST_DATA_SAGA.toString(), "Test", payload, UUID.fromString(this.nominalRollStudentID), "2021");
    this.mockMvc.perform(get(BASE_URL + SAGA + "/" +  sagaFromDB.getSagaId().toString())
      .with(jwt().jwt((jwt) -> jwt.claim("scope", "NOMINAL_ROLL_READ_SAGA")))
      .contentType(MediaType.APPLICATION_JSON)
      .accept(MediaType.APPLICATION_JSON))
      .andDo(print()).andExpect(status().isOk());
  }

  @Test
  @SuppressWarnings("java:S100")
  public void testGetSagaPaginated_givenNoSearchCriteria_shouldReturnAllWithStatusOk() throws Exception {
    final File file = new File(
      Objects.requireNonNull(this.getClass().getClassLoader().getResource("mock_multiple_sagas.json")).getFile()
    );
    final List<Saga> sagaStructs = new ObjectMapper().readValue(file, new TypeReference<>() {
    });
    final List<ca.bc.gov.educ.pen.nominalroll.api.model.v1.Saga> sagaEntities = sagaStructs.stream().map(mapper::toModel).collect(Collectors.toList());

    for (val saga : sagaEntities) {
      saga.setSagaId(null);
      saga.setCreateDate(LocalDateTime.now());
      saga.setUpdateDate(LocalDateTime.now());
    }
    this.repository.saveAll(sagaEntities);
    final MvcResult result = this.mockMvc
      .perform(get(BASE_URL + SAGA + "/paginated")
        .with(jwt().jwt((jwt) -> jwt.claim("scope", "NOMINAL_ROLL_READ_SAGA")))
        .contentType(APPLICATION_JSON))
      .andReturn();
    this.mockMvc.perform(asyncDispatch(result)).andDo(print()).andExpect(status().isOk()).andExpect(jsonPath("$.content", hasSize(3)));
  }

  @Test
  @SuppressWarnings("java:S100")
  public void testGetSagaPaginated_givenNoData_shouldReturnStatusOk() throws Exception {
    final MvcResult result = this.mockMvc
      .perform(get(BASE_URL + SAGA + "/paginated")
        .with(jwt().jwt((jwt) -> jwt.claim("scope", "NOMINAL_ROLL_READ_SAGA")))
        .contentType(APPLICATION_JSON))
      .andReturn();
    this.mockMvc.perform(asyncDispatch(result)).andDo(print()).andExpect(status().isOk()).andExpect(jsonPath("$.content", hasSize(0)));
  }

  @Test
  @SuppressWarnings("java:S100")
  public void testGetSagaPaginated_givenSearchCriteria_shouldReturnStatusOk() throws Exception {
    final File file = new File(
      Objects.requireNonNull(this.getClass().getClassLoader().getResource("mock_multiple_sagas.json")).getFile()
    );
    final List<Saga> sagaStructs = new ObjectMapper().readValue(file, new TypeReference<>() {
    });
    final List<ca.bc.gov.educ.pen.nominalroll.api.model.v1.Saga> sagaEntities = sagaStructs.stream().map(mapper::toModel).collect(Collectors.toList());

    for (val saga : sagaEntities) {
      saga.setSagaId(null);
      saga.setCreateDate(LocalDateTime.now());
      saga.setUpdateDate(LocalDateTime.now());
    }
    this.repository.saveAll(sagaEntities);

    final SearchCriteria criteria = SearchCriteria.builder().key("sagaState").operation(FilterOperation.IN).value("IN_PROGRESS").valueType(ValueType.STRING).build();
    final SearchCriteria criteria2 = SearchCriteria.builder().key("sagaName").condition(AND).operation(FilterOperation.EQUAL).value("NOMINAL_ROLL_POST_DATA_SAGA").valueType(ValueType.STRING).build();
    final List<SearchCriteria> criteriaList = new ArrayList<>();
    criteriaList.add(criteria);
    criteriaList.add(criteria2);
    final List<Search> searches = new LinkedList<>();
    searches.add(Search.builder().searchCriteriaList(criteriaList).build());
    final ObjectMapper objectMapper = new ObjectMapper();
    final String criteriaJSON = objectMapper.writeValueAsString(searches);

    final MvcResult result = this.mockMvc
      .perform(get(BASE_URL + SAGA + "/paginated")
        .with(jwt().jwt((jwt) -> jwt.claim("scope", "NOMINAL_ROLL_READ_SAGA")))
        .param("searchCriteriaList", criteriaJSON)
        .contentType(APPLICATION_JSON))
      .andReturn();
    this.mockMvc.perform(asyncDispatch(result)).andDo(print()).andExpect(status().isOk()).andExpect(jsonPath("$.content", hasSize(1)));
  }

  @Test
  @SuppressWarnings("java:S100")
  public void testGetSagaEventsBySagaID_givenSagaDoesntExist_shouldReturnStatusNotFound() throws Exception {
    this.mockMvc.perform(get(BASE_URL + SAGA + "/{sagaId}/saga-events", UUID.randomUUID())
      .with(jwt().jwt((jwt) -> jwt.claim("scope", "NOMINAL_ROLL_READ_SAGA")))
      .contentType(APPLICATION_JSON)).andDo(print()).andExpect(status().isNotFound());
  }

  @Test
  @SuppressWarnings("java:S100")
  public void testGetSagaEventsBySagaID_givenSagaIDIsValid_shouldReturnStatusOk() throws Exception {
    final File sagEventsFile = new File(
      Objects.requireNonNull(this.getClass().getClassLoader().getResource("mock-saga-events.json")).getFile()
    );
    final File sagFile = new File(
      Objects.requireNonNull(this.getClass().getClassLoader().getResource("mock-saga.json")).getFile()
    );
    val sagaEvents = Arrays.asList(JsonUtil.mapper.readValue(sagEventsFile, ca.bc.gov.educ.pen.nominalroll.api.model.v1.SagaEventStates[].class));
    val saga = JsonUtil.mapper.readValue(sagFile, ca.bc.gov.educ.pen.nominalroll.api.model.v1.Saga.class);
    saga.setSagaId(null);
    saga.setCreateDate(LocalDateTime.now());
    saga.setUpdateDate(LocalDateTime.now());
    this.repository.save(saga);
    for (val sagaEvent : sagaEvents) {
      sagaEvent.setSaga(saga);
      sagaEvent.setCreateDate(LocalDateTime.now());
      sagaEvent.setUpdateDate(LocalDateTime.now());
    }
    this.sagaEventRepository.saveAll(sagaEvents);
    this.mockMvc.perform(get(BASE_URL + SAGA + "/{sagaId}/saga-events", saga.getSagaId())
      .with(jwt().jwt((jwt) -> jwt.claim("scope", "NOMINAL_ROLL_READ_SAGA"))))
      .andDo(print()).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(3)));
  }

  @Test
  public void testUpdateSaga_givenNoBody_shouldReturn400() throws Exception {
    this.mockMvc.perform(put(BASE_URL + SAGA + "/{sagaId}", UUID.randomUUID())
      .with(jwt().jwt((jwt) -> jwt.claim("scope", "NOMINAL_ROLL_WRITE_SAGA")))
      .contentType(APPLICATION_JSON)).andDo(print()).andExpect(status().isBadRequest());
  }

  @Test
  public void testUpdateSaga_givenInvalidID_shouldReturn404() throws Exception {
    val saga = createMockSaga();
    this.mockMvc.perform(put(BASE_URL + SAGA + "/{sagaId}", UUID.randomUUID()).content(JsonUtil.mapper.writeValueAsBytes(saga))
      .with(jwt().jwt((jwt) -> jwt.claim("scope", "NOMINAL_ROLL_WRITE_SAGA")))
      .contentType(APPLICATION_JSON)).andDo(print()).andExpect(status().isNotFound());
  }

  @Test
  public void testUpdateSaga_givenPastUpdateDate_shouldReturn409() throws Exception {
    final var sagaFromDB = this.sagaService.createSagaRecordInDB(NOMINAL_ROLL_POST_DATA_SAGA.toString(), "Test", "Test", UUID.fromString(this.nominalRollStudentID), "2021");
    sagaFromDB.setUpdateDate(LocalDateTime.now());
    this.mockMvc.perform(put(BASE_URL + SAGA + "/{sagaId}", sagaFromDB.getSagaId()).content(JsonUtil.mapper.writeValueAsBytes(mapper.toStruct(sagaFromDB)))
      .with(jwt().jwt((jwt) -> jwt.claim("scope", "NOMINAL_ROLL_WRITE_SAGA")))
      .contentType(APPLICATION_JSON)).andDo(print()).andExpect(status().isConflict());
  }

  @Test
  public void testUpdateSaga_givenValidData_shouldReturnOk() throws Exception {
    final var sagaFromDB = this.sagaService.createSagaRecordInDB(NOMINAL_ROLL_POST_DATA_SAGA.toString(), "Test", "Test", UUID.fromString(this.nominalRollStudentID), "2021");
    sagaFromDB.setUpdateDate(sagaFromDB.getUpdateDate().withNano((int)Math.round(sagaFromDB.getUpdateDate().getNano()/1000.00)*1000)); //db limits precision, so need to adjust
    this.mockMvc.perform(put(BASE_URL + SAGA + "/{sagaId}", sagaFromDB.getSagaId()).content(JsonUtil.mapper.writeValueAsBytes(mapper.toStruct(sagaFromDB)))
      .with(jwt().jwt((jwt) -> jwt.claim("scope", "NOMINAL_ROLL_WRITE_SAGA")))
      .contentType(APPLICATION_JSON)).andDo(print()).andExpect(status().isOk());
  }


  private Saga createMockSaga() {
    return Saga.builder().sagaId(UUID.randomUUID()).payload("test").updateDate(LocalDateTime.now().toString()).build();
  }

  protected String placeholderInvalidSagaData() {
    return " {\n" +
            "    \"createUser\": \"test\",\n" +
            "    \"updateUser\": \"test\",\n" +
            "  }";
  }

  protected String placeholderPostDataSagaData() {
    return " {\n" +
      "    \"createUser\": \"test\",\n" +
      "    \"updateUser\": \"test\",\n" +
      "    \"processingYear\": \"2021\"\n" +
      "  }";
  }


}
