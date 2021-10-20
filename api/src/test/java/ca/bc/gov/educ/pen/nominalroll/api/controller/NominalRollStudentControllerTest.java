package ca.bc.gov.educ.pen.nominalroll.api.controller;

import ca.bc.gov.educ.pen.nominalroll.api.NominalRollApiApplication;
import ca.bc.gov.educ.pen.nominalroll.api.controller.v1.NominalRollApiController;
import ca.bc.gov.educ.pen.nominalroll.api.filter.FilterOperation;
import ca.bc.gov.educ.pen.nominalroll.api.mappers.v1.NominalRollStudentMapper;
import ca.bc.gov.educ.pen.nominalroll.api.rest.RestUtils;
import ca.bc.gov.educ.pen.nominalroll.api.struct.v1.*;
import ca.bc.gov.educ.pen.nominalroll.api.model.v1.NominalRollStudentEntity;
import ca.bc.gov.educ.pen.nominalroll.api.repository.v1.NominalRollStudentRepository;
import ca.bc.gov.educ.pen.nominalroll.api.service.v1.NominalRollService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.val;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.BeforeAll;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

import static ca.bc.gov.educ.pen.nominalroll.api.constants.v1.URL.*;
import static ca.bc.gov.educ.pen.nominalroll.api.struct.v1.Condition.AND;
import static ca.bc.gov.educ.pen.nominalroll.api.struct.v1.Condition.OR;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@ActiveProfiles("testWebclient")
@SpringBootTest(classes = NominalRollApiApplication.class)
@AutoConfigureMockMvc
public class NominalRollStudentControllerTest {
  private static final NominalRollStudentMapper mapper = NominalRollStudentMapper.mapper;
  @Autowired
  private MockMvc mockMvc;

  @Autowired
  NominalRollApiController controller;

  @Autowired
  NominalRollStudentRepository repository;

  @Autowired
  NominalRollService studentService;

  @Before
  public void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  /**
   * need to delete the records to make it working in unit tests assertion, else the records will keep growing and assertions will fail.
   */
  @After
  public void after() {
    this.repository.deleteAll();
  }

  @Test
  public void testReadStudentPaginated_givenValueNull_ShouldReturnStatusOk() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_NOMINAL_ROLL";
    final var mockAuthority = oidcLogin().authorities(grantedAuthority);
    final var file = new File(
            Objects.requireNonNull(this.getClass().getClassLoader().getResource("mock_nom_students.json")).getFile()
    );
    final List<NominalRollStudent> entities = new ObjectMapper().readValue(file, new TypeReference<>() {
    });

    this.repository.saveAll(entities.stream().map(mapper::toModel).collect(Collectors.toList()));
    val entitiesFromDB = this.repository.findAll();
    final SearchCriteria criteria = SearchCriteria.builder().key("localID").operation(FilterOperation.EQUAL).value(null).valueType(ValueType.STRING).build();
    final List<SearchCriteria> criteriaList = new ArrayList<>();
    criteriaList.add(criteria);
    final List<Search> searches = new LinkedList<>();
    searches.add(Search.builder().searchCriteriaList(criteriaList).build());
    final ObjectMapper objectMapper = new ObjectMapper();
    final String criteriaJSON = objectMapper.writeValueAsString(searches);
    this.mockMvc.perform(get(BASE_URL + PAGINATED).with(mockAuthority).param("searchCriteriaList", criteriaJSON)
            .contentType(APPLICATION_JSON)).andDo(print()).andExpect(status().isOk());
  }

  @Test
  public void testReadStudentPaginated_givenValueNotNull_ShouldReturnStatusOk() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_NOMINAL_ROLL";
    final var mockAuthority = oidcLogin().authorities(grantedAuthority);
    final var file = new File(
            Objects.requireNonNull(this.getClass().getClassLoader().getResource("mock_nom_students.json")).getFile()
    );
    final List<NominalRollStudent> entities = new ObjectMapper().readValue(file, new TypeReference<>() {
    });

    this.repository.saveAll(entities.stream().map(mapper::toModel).collect(Collectors.toList()));
    val entitiesFromDB = this.repository.findAll();
    final SearchCriteria criteria = SearchCriteria.builder().key("localID").operation(FilterOperation.NOT_EQUAL).value(null).valueType(ValueType.STRING).build();
    final List<SearchCriteria> criteriaList = new ArrayList<>();
    criteriaList.add(criteria);
    final List<Search> searches = new LinkedList<>();
    searches.add(Search.builder().searchCriteriaList(criteriaList).build());
    final ObjectMapper objectMapper = new ObjectMapper();
    final String criteriaJSON = objectMapper.writeValueAsString(searches);
    this.mockMvc.perform(get(BASE_URL + PAGINATED).with(mockAuthority).param("searchCriteriaList", criteriaJSON)
            .contentType(APPLICATION_JSON)).andDo(print()).andExpect(status().isOk());
  }

  @Test
  public void testReadStudentPaginated_Always_ShouldReturnStatusOk() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_NOMINAL_ROLL";
    final var mockAuthority = oidcLogin().authorities(grantedAuthority);
    final File file = new File(
        Objects.requireNonNull(this.getClass().getClassLoader().getResource("mock_nom_students.json")).getFile()
    );
    final List<NominalRollStudent> entities = new ObjectMapper().readValue(file, new TypeReference<>() {
    });
    this.repository.saveAll(entities.stream().map(mapper::toModel).collect(Collectors.toList()));
    final MvcResult result = this.mockMvc
        .perform(get(BASE_URL+PAGINATED+"?pageSize=2").with(mockAuthority)
            .contentType(APPLICATION_JSON))
        .andReturn();
    this.mockMvc.perform(asyncDispatch(result)).andDo(print()).andExpect(status().isOk()).andExpect(jsonPath("$.content", hasSize(2)));
  }

  @Test
  public void testReadStudentPaginated_whenNoDataInDB_ShouldReturnStatusOk() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_NOMINAL_ROLL";
    final var mockAuthority = oidcLogin().authorities(grantedAuthority);
    final MvcResult result = this.mockMvc
        .perform(get(BASE_URL + PAGINATED).with(mockAuthority)
            .contentType(APPLICATION_JSON))
        .andReturn();
    this.mockMvc.perform(asyncDispatch(result)).andDo(print()).andExpect(status().isOk()).andExpect(jsonPath("$.content", hasSize(0)));
  }

  @Test
  public void testReadStudentPaginatedWithSorting_Always_ShouldReturnStatusOk() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_NOMINAL_ROLL";
    final var mockAuthority = oidcLogin().authorities(grantedAuthority);
    final File file = new File(
        Objects.requireNonNull(this.getClass().getClassLoader().getResource("mock_nom_students.json")).getFile()
    );
    final List<NominalRollStudent> entities = new ObjectMapper().readValue(file, new TypeReference<>() {
    });
    this.repository.saveAll(entities.stream().map(mapper::toModel).collect(Collectors.toList()));
    final Map<String, String> sortMap = new HashMap<>();
    sortMap.put("surname", "ASC");
    sortMap.put("givenNames", "DESC");
    final String sort = new ObjectMapper().writeValueAsString(sortMap);
    final MvcResult result = this.mockMvc
        .perform(get(BASE_URL + PAGINATED).with(mockAuthority).param("pageNumber", "1").param("pageSize", "5").param("sort", sort)
            .contentType(APPLICATION_JSON))
        .andReturn();
    this.mockMvc.perform(asyncDispatch(result)).andDo(print()).andExpect(status().isOk()).andExpect(jsonPath("$.content", hasSize(1)));
  }

  @Test
  public void testReadStudentPaginated_GivenFirstNameFilter_ShouldReturnStatusOk() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_NOMINAL_ROLL";
    final var mockAuthority = oidcLogin().authorities(grantedAuthority);
    final File file = new File(
        Objects.requireNonNull(this.getClass().getClassLoader().getResource("mock_nom_students.json")).getFile()
    );
    final List<NominalRollStudent> entities = new ObjectMapper().readValue(file, new TypeReference<>() {
    });
    final SearchCriteria criteria = SearchCriteria.builder().key("givenNames").operation(FilterOperation.EQUAL).value("Leonor").valueType(ValueType.STRING).build();
    final List<SearchCriteria> criteriaList = new ArrayList<>();
    criteriaList.add(criteria);
    final List<Search> searches = new LinkedList<>();
    searches.add(Search.builder().searchCriteriaList(criteriaList).build());
    final ObjectMapper objectMapper = new ObjectMapper();
    final String criteriaJSON = objectMapper.writeValueAsString(searches);
    System.out.println(criteriaJSON);
    this.repository.saveAll(entities.stream().map(mapper::toModel).collect(Collectors.toList()));
    final MvcResult result = this.mockMvc
        .perform(get(BASE_URL + PAGINATED).with(mockAuthority).param("searchCriteriaList", criteriaJSON)
            .contentType(APPLICATION_JSON))
        .andReturn();
    this.mockMvc.perform(asyncDispatch(result)).andDo(print()).andExpect(status().isOk()).andExpect(jsonPath("$.content", hasSize(1)));
  }

  @Test
  public void testReadStudentPaginated_GivenLastNameFilter_ShouldReturnStatusOk() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_NOMINAL_ROLL";
    final var mockAuthority = oidcLogin().authorities(grantedAuthority);
    final File file = new File(
        Objects.requireNonNull(this.getClass().getClassLoader().getResource("mock_nom_students.json")).getFile()
    );
    final List<NominalRollStudent> entities = new ObjectMapper().readValue(file, new TypeReference<>() {
    });
    final SearchCriteria criteria = SearchCriteria.builder().key("surname").operation(FilterOperation.EQUAL).value("Warner").valueType(ValueType.STRING).build();
    final List<SearchCriteria> criteriaList = new ArrayList<>();
    criteriaList.add(criteria);
    final List<Search> searches = new LinkedList<>();
    searches.add(Search.builder().searchCriteriaList(criteriaList).build());
    final ObjectMapper objectMapper = new ObjectMapper();
    final String criteriaJSON = objectMapper.writeValueAsString(searches);
    System.out.println(criteriaJSON);
    this.repository.saveAll(entities.stream().map(mapper::toModel).collect(Collectors.toList()));
    final MvcResult result = this.mockMvc
        .perform(get(BASE_URL + PAGINATED).with(mockAuthority).param("searchCriteriaList", criteriaJSON)
            .contentType(APPLICATION_JSON))
        .andReturn();
    this.mockMvc.perform(asyncDispatch(result)).andDo(print()).andExpect(status().isOk()).andExpect(jsonPath("$.content", hasSize(1)));
  }

  @Test
  public void testReadStudentPaginated_GivenSubmitDateBetween_ShouldReturnStatusOk() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_NOMINAL_ROLL";
    final var mockAuthority = oidcLogin().authorities(grantedAuthority);
    final File file = new File(
        Objects.requireNonNull(this.getClass().getClassLoader().getResource("mock_nom_students.json")).getFile()
    );
    final List<NominalRollStudent> entities = new ObjectMapper().readValue(file, new TypeReference<>() {
    });
    final String fromDate = "2017-04-01";
    final String toDate = "2018-04-15";
    final SearchCriteria criteria = SearchCriteria.builder().key("birthDate").operation(FilterOperation.BETWEEN).value(fromDate + "," + toDate).valueType(ValueType.STRING).build();
    final List<SearchCriteria> criteriaList = new ArrayList<>();
    criteriaList.add(criteria);
    final List<Search> searches = new LinkedList<>();
    searches.add(Search.builder().searchCriteriaList(criteriaList).build());
    final ObjectMapper objectMapper = new ObjectMapper();
    final String criteriaJSON = objectMapper.writeValueAsString(searches);
    System.out.println(criteriaJSON);
    this.repository.saveAll(entities.stream().map(mapper::toModel).collect(Collectors.toList()));
    final MvcResult result = this.mockMvc
        .perform(get(BASE_URL + PAGINATED).with(mockAuthority).param("searchCriteriaList", criteriaJSON)
            .contentType(APPLICATION_JSON))
        .andReturn();
    this.mockMvc.perform(asyncDispatch(result)).andDo(print()).andExpect(status().isOk()).andExpect(jsonPath("$.content", hasSize(1)));
  }

  @Test
  public void testReadStudentPaginated_GivenFirstAndLast_ShouldReturnStatusOk() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_NOMINAL_ROLL";
    final var mockAuthority = oidcLogin().authorities(grantedAuthority);
    final File file = new File(
        Objects.requireNonNull(this.getClass().getClassLoader().getResource("mock_nom_students.json")).getFile()
    );
    final List<NominalRollStudent> entities = new ObjectMapper().readValue(file, new TypeReference<>() {
    });
    final String fromDate = "1990-04-01";
    final String toDate = "2020-04-15";
    final SearchCriteria criteria = SearchCriteria.builder().key("birthDate").operation(FilterOperation.BETWEEN).value(fromDate + "," + toDate).valueType(ValueType.STRING).build();
    final SearchCriteria criteriaFirstName = SearchCriteria.builder().condition(AND).key("givenNames").operation(FilterOperation.CONTAINS).value("a").valueType(ValueType.STRING).build();
    final SearchCriteria criteriaLastName = SearchCriteria.builder().condition(AND).key("surname").operation(FilterOperation.CONTAINS).value("l").valueType(ValueType.STRING).build();
    final List<SearchCriteria> criteriaList = new ArrayList<>();
    criteriaList.add(criteria);
    criteriaList.add(criteriaFirstName);
    criteriaList.add(criteriaLastName);
    final List<Search> searches = new LinkedList<>();
    searches.add(Search.builder().searchCriteriaList(criteriaList).build());
    final ObjectMapper objectMapper = new ObjectMapper();
    final String criteriaJSON = objectMapper.writeValueAsString(searches);
    System.out.println(criteriaJSON);
    this.repository.saveAll(entities.stream().map(mapper::toModel).collect(Collectors.toList()));
    final MvcResult result = this.mockMvc
        .perform(get(BASE_URL + PAGINATED).with(mockAuthority).param("searchCriteriaList", criteriaJSON)
            .contentType(APPLICATION_JSON))
        .andReturn();
    this.mockMvc.perform(asyncDispatch(result)).andDo(print()).andExpect(status().isOk()).andExpect(jsonPath("$.content", hasSize(3)));
  }

  @Test
  public void testReadStudentPaginated_GivenFirstAndLastNull_ShouldReturnStatusOk() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_NOMINAL_ROLL";
    final var mockAuthority = oidcLogin().authorities(grantedAuthority);
    final File file = new File(
            Objects.requireNonNull(this.getClass().getClassLoader().getResource("mock_nom_students.json")).getFile()
    );
    final List<NominalRollStudent> entities = new ObjectMapper().readValue(file, new TypeReference<>() {
    });
    final String fromDate = "1990-04-01";
    final String toDate = "2020-04-15";
    final SearchCriteria criteria = SearchCriteria.builder().key("birthDate").operation(FilterOperation.BETWEEN).value(fromDate + "," + toDate).valueType(ValueType.STRING).build();
    final SearchCriteria criteriaFirstName = SearchCriteria.builder().condition(AND).key("givenNames").operation(FilterOperation.CONTAINS).value("a").valueType(ValueType.STRING).build();
    final SearchCriteria criteriaLastName = SearchCriteria.builder().condition(AND).key("surname").operation(FilterOperation.EQUAL).value(null).valueType(ValueType.STRING).build();
    final List<SearchCriteria> criteriaList = new ArrayList<>();
    criteriaList.add(criteria);
    criteriaList.add(criteriaFirstName);
    criteriaList.add(criteriaLastName);
    final List<Search> searches = new LinkedList<>();
    searches.add(Search.builder().searchCriteriaList(criteriaList).build());
    final ObjectMapper objectMapper = new ObjectMapper();
    final String criteriaJSON = objectMapper.writeValueAsString(searches);
    System.out.println(criteriaJSON);
    this.repository.saveAll(entities.stream().map(mapper::toModel).collect(Collectors.toList()));
    final MvcResult result = this.mockMvc
            .perform(get(BASE_URL + PAGINATED).with(mockAuthority).param("searchCriteriaList", criteriaJSON)
                    .contentType(APPLICATION_JSON))
            .andReturn();
    this.mockMvc.perform(asyncDispatch(result)).andDo(print()).andExpect(status().isOk()).andExpect(jsonPath("$.content", hasSize(0)));
  }

  @Test
  public void testReadStudentPaginated_GivenFirstAndLastOrbirthDate_ShouldReturnStatusOk() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_NOMINAL_ROLL";
    final var mockAuthority = oidcLogin().authorities(grantedAuthority);
    final File file = new File(
        Objects.requireNonNull(this.getClass().getClassLoader().getResource("mock_nom_students.json")).getFile()
    );
    final List<NominalRollStudent> entities = new ObjectMapper().readValue(file, new TypeReference<>() {
    });

    final SearchCriteria criteriaFirstName = SearchCriteria.builder().key("givenNames").operation(FilterOperation.CONTAINS).value("a").valueType(ValueType.STRING).build();
    final SearchCriteria criteriaLastName = SearchCriteria.builder().condition(AND).key("surname").operation(FilterOperation.CONTAINS).value("l").valueType(ValueType.STRING).build();
    final List<SearchCriteria> criteriaList = new LinkedList<>();
    criteriaList.add(criteriaFirstName);
    criteriaList.add(criteriaLastName);

    final String fromDate = "1990-04-01";
    final String toDate = "2020-04-15";
    final SearchCriteria birthDateCriteria = SearchCriteria.builder().key("birthDate").operation(FilterOperation.BETWEEN).value(fromDate + "," + toDate).valueType(ValueType.STRING).build();
    final List<SearchCriteria> criteriaList1 = new LinkedList<>();
    criteriaList1.add(birthDateCriteria);

    final List<Search> searches = new LinkedList<>();
    searches.add(Search.builder().searchCriteriaList(criteriaList).build());
    searches.add(Search.builder().condition(OR).searchCriteriaList(criteriaList1).build());

    final ObjectMapper objectMapper = new ObjectMapper();
    final String criteriaJSON = objectMapper.writeValueAsString(searches);
    System.out.println(criteriaJSON);
    this.repository.saveAll(entities.stream().map(mapper::toModel).collect(Collectors.toList()));
    final MvcResult result = this.mockMvc
        .perform(get(BASE_URL + PAGINATED).with(mockAuthority).param("searchCriteriaList", criteriaJSON)
            .contentType(APPLICATION_JSON))
        .andReturn();
    this.mockMvc.perform(asyncDispatch(result)).andDo(print()).andExpect(status().isOk()).andExpect(jsonPath("$.content", hasSize(6)));
  }

  @Test
  public void testReadStudentPaginated_GivenFirstORLastANDbirthDate_ShouldReturnStatusOk() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_NOMINAL_ROLL";
    final var mockAuthority = oidcLogin().authorities(grantedAuthority);
    final File file = new File(
        Objects.requireNonNull(this.getClass().getClassLoader().getResource("mock_nom_students.json")).getFile()
    );
    final List<NominalRollStudent> entities = new ObjectMapper().readValue(file, new TypeReference<>() {
    });

    final SearchCriteria criteriaFirstName = SearchCriteria.builder().key("givenNames").operation(FilterOperation.CONTAINS).value("a").valueType(ValueType.STRING).build();
    final SearchCriteria criteriaLastName = SearchCriteria.builder().condition(OR).key("surname").operation(FilterOperation.CONTAINS).value("l").valueType(ValueType.STRING).build();
    final List<SearchCriteria> criteriaList = new LinkedList<>();
    criteriaList.add(criteriaFirstName);
    criteriaList.add(criteriaLastName);

    final String fromDate = "1990-04-01";
    final String toDate = "2020-04-15";
    final SearchCriteria birthDateCriteria = SearchCriteria.builder().key("birthDate").operation(FilterOperation.BETWEEN).value(fromDate + "," + toDate).valueType(ValueType.STRING).build();
    final List<SearchCriteria> criteriaList1 = new LinkedList<>();
    criteriaList1.add(birthDateCriteria);

    final List<Search> searches = new LinkedList<>();
    searches.add(Search.builder().searchCriteriaList(criteriaList).build());
    searches.add(Search.builder().condition(AND).searchCriteriaList(criteriaList1).build());

    final ObjectMapper objectMapper = new ObjectMapper();
    final String criteriaJSON = objectMapper.writeValueAsString(searches);
    System.out.println(criteriaJSON);
    this.repository.saveAll(entities.stream().map(mapper::toModel).collect(Collectors.toList()));
    final MvcResult result = this.mockMvc
        .perform(get(BASE_URL + PAGINATED).with(mockAuthority).param("searchCriteriaList", criteriaJSON)
            .contentType(APPLICATION_JSON))
        .andReturn();
    this.mockMvc.perform(asyncDispatch(result)).andDo(print()).andExpect(status().isOk()).andExpect(jsonPath("$.content", hasSize(5)));
  }

  @Test
  public void testReadStudentPaginated_surnameFilterIgnoreCase_ShouldReturnStatusOk() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_NOMINAL_ROLL";
    final var mockAuthority = oidcLogin().authorities(grantedAuthority);
    final File file = new File(
        Objects.requireNonNull(this.getClass().getClassLoader().getResource("mock_nom_students.json")).getFile()
    );
    final List<NominalRollStudent> entities = new ObjectMapper().readValue(file, new TypeReference<>() {
    });
    final SearchCriteria criteria = SearchCriteria.builder().key("surname").operation(FilterOperation.CONTAINS).value("b").valueType(ValueType.STRING).build();
    final List<SearchCriteria> criteriaList = new ArrayList<>();
    criteriaList.add(criteria);
    final List<Search> searches = new LinkedList<>();
    searches.add(Search.builder().searchCriteriaList(criteriaList).build());
    final ObjectMapper objectMapper = new ObjectMapper();
    final String criteriaJSON = objectMapper.writeValueAsString(searches);
    System.out.println(criteriaJSON);
    this.repository.saveAll(entities.stream().map(mapper::toModel).collect(Collectors.toList()));
    final MvcResult result = this.mockMvc
        .perform(get(BASE_URL + PAGINATED).with(mockAuthority).param("searchCriteriaList", criteriaJSON)
            .contentType(APPLICATION_JSON))
        .andReturn();
    this.mockMvc.perform(asyncDispatch(result)).andDo(print()).andExpect(status().isOk()).andExpect(jsonPath("$.content", hasSize(2)));
  }

  @Test
  public void testReadStudentPaginated_surnameStartWith_ShouldReturnStatusOk() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_NOMINAL_ROLL";
    final var mockAuthority = oidcLogin().authorities(grantedAuthority);
    final File file = new File(
        Objects.requireNonNull(this.getClass().getClassLoader().getResource("mock_nom_students.json")).getFile()
    );
    final List<NominalRollStudent> entities = new ObjectMapper().readValue(file, new TypeReference<>() {
    });
    final SearchCriteria criteria = SearchCriteria.builder().key("surname").operation(FilterOperation.STARTS_WITH).value("Ham").valueType(ValueType.STRING).build();
    final List<SearchCriteria> criteriaList = new ArrayList<>();
    criteriaList.add(criteria);
    final List<Search> searches = new LinkedList<>();
    searches.add(Search.builder().searchCriteriaList(criteriaList).build());
    final ObjectMapper objectMapper = new ObjectMapper();
    final String criteriaJSON = objectMapper.writeValueAsString(searches);
    System.out.println(criteriaJSON);
    this.repository.saveAll(entities.stream().map(mapper::toModel).collect(Collectors.toList()));
    final MvcResult result = this.mockMvc
        .perform(get(BASE_URL + PAGINATED).with(mockAuthority).param("searchCriteriaList", criteriaJSON)
            .contentType(APPLICATION_JSON))
        .andReturn();
    this.mockMvc.perform(asyncDispatch(result)).andDo(print()).andExpect(status().isOk()).andExpect(jsonPath("$.content", hasSize(1)));
  }

  @Test
  public void testReadStudentPaginated_surnameNotStartWith_ShouldReturnStatusOk() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_NOMINAL_ROLL";
    final var mockAuthority = oidcLogin().authorities(grantedAuthority);
    final File file = new File(
            Objects.requireNonNull(this.getClass().getClassLoader().getResource("mock_nom_students.json")).getFile()
    );
    final List<NominalRollStudent> entities = new ObjectMapper().readValue(file, new TypeReference<>() {
    });
    final SearchCriteria criteria = SearchCriteria.builder().key("surname").operation(FilterOperation.NOT_STARTS_WITH).value("Ham").valueType(ValueType.STRING).build();
    final List<SearchCriteria> criteriaList = new ArrayList<>();
    criteriaList.add(criteria);
    final List<Search> searches = new LinkedList<>();
    searches.add(Search.builder().searchCriteriaList(criteriaList).build());
    final ObjectMapper objectMapper = new ObjectMapper();
    final String criteriaJSON = objectMapper.writeValueAsString(searches);
    System.out.println(criteriaJSON);
    this.repository.saveAll(entities.stream().map(mapper::toModel).collect(Collectors.toList()));
    final MvcResult result = this.mockMvc
            .perform(get(BASE_URL + PAGINATED).with(mockAuthority).param("searchCriteriaList", criteriaJSON)
                    .contentType(APPLICATION_JSON))
            .andReturn();
    this.mockMvc.perform(asyncDispatch(result)).andDo(print()).andExpect(status().isOk()).andExpect(jsonPath("$.content", hasSize(5)));
  }

  @Test
  public void testReadStudentPaginated_surnameStartWith2_ShouldReturnStatusOk() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_NOMINAL_ROLL";
    final var mockAuthority = oidcLogin().authorities(grantedAuthority);
    final File file = new File(
        Objects.requireNonNull(this.getClass().getClassLoader().getResource("mock_nom_students.json")).getFile()
    );
    final List<NominalRollStudent> entities = new ObjectMapper().readValue(file, new TypeReference<>() {
    });
    final SearchCriteria criteria = SearchCriteria.builder().key("surname").operation(FilterOperation.STARTS_WITH).value("hem").valueType(ValueType.STRING).build();
    final List<SearchCriteria> criteriaList = new ArrayList<>();
    criteriaList.add(criteria);
    final List<Search> searches = new LinkedList<>();
    searches.add(Search.builder().searchCriteriaList(criteriaList).build());
    final ObjectMapper objectMapper = new ObjectMapper();
    final String criteriaJSON = objectMapper.writeValueAsString(searches);
    System.out.println(criteriaJSON);
    this.repository.saveAll(entities.stream().map(mapper::toModel).collect(Collectors.toList()));
    final MvcResult result = this.mockMvc
        .perform(get(BASE_URL + PAGINATED).with(mockAuthority).param("searchCriteriaList", criteriaJSON)
            .contentType(APPLICATION_JSON))
        .andReturn();
    this.mockMvc.perform(asyncDispatch(result)).andDo(print()).andExpect(status().isOk()).andExpect(jsonPath("$.content", hasSize(0)));
  }

  @Test
  public void testReadStudentPaginated_surnameStartWithIgnoreCase2_ShouldReturnStatusOk() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_NOMINAL_ROLL";
    final var mockAuthority = oidcLogin().authorities(grantedAuthority);
    final File file = new File(
        Objects.requireNonNull(this.getClass().getClassLoader().getResource("mock_nom_students.json")).getFile()
    );
    final List<NominalRollStudent> entities = new ObjectMapper().readValue(file, new TypeReference<>() {
    });

    this.repository.saveAll(entities.stream().map(mapper::toModel).collect(Collectors.toList()));
    val entitiesFromDB = this.repository.findAll();
    final SearchCriteria criteria = SearchCriteria.builder().key("nominalRollStudentID").operation(FilterOperation.EQUAL).value(entitiesFromDB.get(0).getNominalRollStudentID().toString()).valueType(ValueType.UUID).build();
    final List<SearchCriteria> criteriaList = new ArrayList<>();
    criteriaList.add(criteria);
    final List<Search> searches = new LinkedList<>();
    searches.add(Search.builder().searchCriteriaList(criteriaList).build());
    final ObjectMapper objectMapper = new ObjectMapper();
    final String criteriaJSON = objectMapper.writeValueAsString(searches);
    final MvcResult result = this.mockMvc
        .perform(get(BASE_URL + PAGINATED).with(mockAuthority).param("searchCriteriaList", criteriaJSON)
            .contentType(APPLICATION_JSON))
        .andReturn();
    this.mockMvc.perform(asyncDispatch(result)).andDo(print()).andExpect(status().isOk()).andExpect(jsonPath("$.content", hasSize(1)));
  }

  @Test
  public void testReadStudentPaginated_surnameEndWith_ShouldReturnStatusOkAndRecord() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_NOMINAL_ROLL";
    final var mockAuthority = oidcLogin().authorities(grantedAuthority);
    final File file = new File(
        Objects.requireNonNull(this.getClass().getClassLoader().getResource("mock_nom_students.json")).getFile()
    );
    final List<NominalRollStudent> entities = new ObjectMapper().readValue(file, new TypeReference<>() {
    });
    final SearchCriteria criteria = SearchCriteria.builder().key("surname").operation(FilterOperation.ENDS_WITH).value("ton").valueType(ValueType.STRING).build();
    final List<SearchCriteria> criteriaList = new ArrayList<>();
    criteriaList.add(criteria);
    final List<Search> searches = new LinkedList<>();
    searches.add(Search.builder().searchCriteriaList(criteriaList).build());
    final ObjectMapper objectMapper = new ObjectMapper();
    final String criteriaJSON = objectMapper.writeValueAsString(searches);
    System.out.println(criteriaJSON);
    this.repository.saveAll(entities.stream().map(mapper::toModel).collect(Collectors.toList()));
    final MvcResult result = this.mockMvc
        .perform(get(BASE_URL + PAGINATED).with(mockAuthority).param("searchCriteriaList", criteriaJSON)
            .contentType(APPLICATION_JSON))
        .andReturn();
    this.mockMvc.perform(asyncDispatch(result)).andDo(print()).andExpect(status().isOk()).andExpect(jsonPath("$.content", hasSize(2)));
  }

  @Test
  public void testReadStudentPaginated_surnameEndWith_ShouldReturnStatusOkButNoRecord() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_NOMINAL_ROLL";
    final var mockAuthority = oidcLogin().authorities(grantedAuthority);
    final File file = new File(
        Objects.requireNonNull(this.getClass().getClassLoader().getResource("mock_nom_students.json")).getFile()
    );
    final List<NominalRollStudent> entities = new ObjectMapper().readValue(file, new TypeReference<>() {
    });
    final SearchCriteria criteria = SearchCriteria.builder().key("surname").operation(FilterOperation.ENDS_WITH).value("son").valueType(ValueType.STRING).build();
    final List<SearchCriteria> criteriaList = new ArrayList<>();
    criteriaList.add(criteria);
    final List<Search> searches = new LinkedList<>();
    searches.add(Search.builder().searchCriteriaList(criteriaList).build());
    final ObjectMapper objectMapper = new ObjectMapper();
    final String criteriaJSON = objectMapper.writeValueAsString(searches);
    System.out.println(criteriaJSON);
    this.repository.saveAll(entities.stream().map(mapper::toModel).collect(Collectors.toList()));
    final MvcResult result = this.mockMvc
        .perform(get(BASE_URL + PAGINATED).with(mockAuthority).param("searchCriteriaList", criteriaJSON)
            .contentType(APPLICATION_JSON))
        .andReturn();
    this.mockMvc.perform(asyncDispatch(result)).andDo(print()).andExpect(status().isOk()).andExpect(jsonPath("$.content", hasSize(0)));
  }

  @Test
  public void testReadStudentPaginated_givenOperationTypeNull_ShouldReturnStatusBadRequest() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_NOMINAL_ROLL";
    final var mockAuthority = oidcLogin().authorities(grantedAuthority);
    final var file = new File(
        Objects.requireNonNull(this.getClass().getClassLoader().getResource("mock_nom_students.json")).getFile()
    );
    final List<NominalRollStudent> entities = new ObjectMapper().readValue(file, new TypeReference<>() {
    });

    this.repository.saveAll(entities.stream().map(mapper::toModel).collect(Collectors.toList()));
    val entitiesFromDB = this.repository.findAll();
    final SearchCriteria criteria = SearchCriteria.builder().key("nominalRollStudentID").operation(null).value(entitiesFromDB.get(0).getNominalRollStudentID().toString()).valueType(ValueType.UUID).build();
    final List<SearchCriteria> criteriaList = new ArrayList<>();
    criteriaList.add(criteria);
    final List<Search> searches = new LinkedList<>();
    searches.add(Search.builder().searchCriteriaList(criteriaList).build());
    final ObjectMapper objectMapper = new ObjectMapper();
    final String criteriaJSON = objectMapper.writeValueAsString(searches);
    this.mockMvc.perform(get(BASE_URL + PAGINATED).with(mockAuthority).param("searchCriteriaList", criteriaJSON)
        .contentType(APPLICATION_JSON)).andDo(print()).andExpect(status().isBadRequest());
  }

  @Test
  public void testReadStudentPaginated_givenInvalidSearchCriteria_ShouldReturnStatusBadRequest() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_NOMINAL_ROLL";
    final var mockAuthority = oidcLogin().authorities(grantedAuthority);
    this.mockMvc
        .perform(get(BASE_URL + PAGINATED).with(mockAuthority).param("searchCriteriaList", "{test}")
            .contentType(APPLICATION_JSON)).andDo(print()).andExpect(status().isBadRequest());
  }

  private NominalRollStudentEntity createNominalRollStudent() {
    final NominalRollStudentEntity student = new NominalRollStudentEntity();
    student.setGivenNames("John");
    student.setSurname("Wayne");
    student.setBirthDate("1907-05-26");
    student.setGender("M");
    student.setBandOfResidence("4664");
    student.setFte("1.0");
    student.setLeaProvincial("Provincial");
    student.setGrade("01");
    student.setProcessingYear("2021");
    student.setSchoolName("Test Highschool");
    student.setSchoolNumber("5465");
    student.setSchoolDistrictNumber("5");
    student.setRecipientName("Test FN Band");
    student.setRecipientNumber("8554");
    student.setStatus("LOADED");
    return student;
  }

  public static String asJsonString(final Object obj) {
    try {
      return new ObjectMapper().writeValueAsString(obj);
    } catch (final Exception e) {
      throw new RuntimeException(e);
    }
  }

}
