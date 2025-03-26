package ca.bc.gov.educ.pen.nominalroll.api.controller;

import ca.bc.gov.educ.pen.nominalroll.api.BaseNominalRollAPITest;
import ca.bc.gov.educ.pen.nominalroll.api.constants.v1.NominalRollStudentStatus;
import ca.bc.gov.educ.pen.nominalroll.api.controller.v1.NominalRollApiController;
import ca.bc.gov.educ.pen.nominalroll.api.filter.FilterOperation;
import ca.bc.gov.educ.pen.nominalroll.api.mappers.v1.NominalRollStudentMapper;
import ca.bc.gov.educ.pen.nominalroll.api.model.v1.NominalRollPostedStudentEntity;
import ca.bc.gov.educ.pen.nominalroll.api.model.v1.NominalRollStudentEntity;
import ca.bc.gov.educ.pen.nominalroll.api.model.v1.NominalRollStudentValidationErrorEntity;
import ca.bc.gov.educ.pen.nominalroll.api.properties.ApplicationProperties;
import ca.bc.gov.educ.pen.nominalroll.api.repository.v1.NominalRollPostedStudentRepository;
import ca.bc.gov.educ.pen.nominalroll.api.repository.v1.NominalRollStudentRepository;
import ca.bc.gov.educ.pen.nominalroll.api.repository.v1.NominalRollStudentValidationErrorRepository;
import ca.bc.gov.educ.pen.nominalroll.api.service.v1.NominalRollService;
import ca.bc.gov.educ.pen.nominalroll.api.struct.external.school.v1.FedProvSchoolCode;
import ca.bc.gov.educ.pen.nominalroll.api.struct.v1.*;
import ca.bc.gov.educ.pen.nominalroll.api.util.JsonUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import lombok.val;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.File;
import java.io.FileInputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static ca.bc.gov.educ.pen.nominalroll.api.constants.v1.URL.*;
import static ca.bc.gov.educ.pen.nominalroll.api.struct.v1.Condition.AND;
import static ca.bc.gov.educ.pen.nominalroll.api.struct.v1.Condition.OR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@RunWith(JUnitParamsRunner.class)
public class NominalRollStudentControllerTest extends BaseNominalRollAPITest {
  @ClassRule
  public static final SpringClassRule SPRING_CLASS_RULE = new SpringClassRule();

  @Rule
  public final SpringMethodRule springMethodRule = new SpringMethodRule();
  private static final NominalRollStudentMapper mapper = NominalRollStudentMapper.mapper;
  @Autowired
  NominalRollApiController controller;
  @Autowired
  NominalRollStudentRepository repository;
  @Autowired
  NominalRollStudentValidationErrorRepository nominalRollStudentValidationErrorRepository;
  @Autowired
  NominalRollPostedStudentRepository postedStudentRepository;
  @Autowired
  NominalRollService studentService;
  @Autowired
  private MockMvc mockMvc;

  @Autowired
  ApplicationProperties applicationProperties;

  public static String asJsonString(final Object obj) {
    try {
      return new ObjectMapper().writeValueAsString(obj);
    } catch (final Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Before
  public void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  public void testReadStudentPaginated_givenValueNull_ShouldReturnStatusOk() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_NOMINAL_ROLL_READ_STUDENT";
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
    final GrantedAuthority grantedAuthority = () -> "SCOPE_NOMINAL_ROLL_READ_STUDENT";
    final var mockAuthority = oidcLogin().authorities(grantedAuthority);
    final var file = new File(
      Objects.requireNonNull(this.getClass().getClassLoader().getResource("mock_nom_students.json")).getFile()
    );
    final List<NominalRollStudent> entities = new ObjectMapper().readValue(file, new TypeReference<>() {
    });

    this.repository.saveAll(entities.stream().map(mapper::toModel).collect(Collectors.toList()));
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
    final GrantedAuthority grantedAuthority = () -> "SCOPE_NOMINAL_ROLL_READ_STUDENT";
    final var mockAuthority = oidcLogin().authorities(grantedAuthority);
    final File file = new File(
      Objects.requireNonNull(this.getClass().getClassLoader().getResource("mock_nom_students.json")).getFile()
    );
    final List<NominalRollStudent> entities = new ObjectMapper().readValue(file, new TypeReference<>() {
    });
    this.repository.saveAll(entities.stream().map(mapper::toModel).collect(Collectors.toList()));
    final MvcResult result = this.mockMvc
      .perform(get(BASE_URL + PAGINATED + "?pageSize=2").with(mockAuthority)
        .contentType(APPLICATION_JSON))
      .andReturn();
    this.mockMvc.perform(asyncDispatch(result)).andDo(print()).andExpect(status().isOk()).andExpect(jsonPath("$.content", hasSize(2)));
  }

  @Test
  public void testReadStudentPaginated_whenNoDataInDB_ShouldReturnStatusOk() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_NOMINAL_ROLL_READ_STUDENT";
    final var mockAuthority = oidcLogin().authorities(grantedAuthority);
    final MvcResult result = this.mockMvc
      .perform(get(BASE_URL + PAGINATED).with(mockAuthority)
        .contentType(APPLICATION_JSON))
      .andReturn();
    this.mockMvc.perform(asyncDispatch(result)).andDo(print()).andExpect(status().isOk()).andExpect(jsonPath("$.content", hasSize(0)));
  }

  @Test
  public void testReadStudentPaginatedWithSorting_Always_ShouldReturnStatusOk() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_NOMINAL_ROLL_READ_STUDENT";
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
    final GrantedAuthority grantedAuthority = () -> "SCOPE_NOMINAL_ROLL_READ_STUDENT";
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
    final GrantedAuthority grantedAuthority = () -> "SCOPE_NOMINAL_ROLL_READ_STUDENT";
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
    final GrantedAuthority grantedAuthority = () -> "SCOPE_NOMINAL_ROLL_READ_STUDENT";
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
    final GrantedAuthority grantedAuthority = () -> "SCOPE_NOMINAL_ROLL_READ_STUDENT";
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
    final GrantedAuthority grantedAuthority = () -> "SCOPE_NOMINAL_ROLL_READ_STUDENT";
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
    final GrantedAuthority grantedAuthority = () -> "SCOPE_NOMINAL_ROLL_READ_STUDENT";
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
    final GrantedAuthority grantedAuthority = () -> "SCOPE_NOMINAL_ROLL_READ_STUDENT";
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
    final GrantedAuthority grantedAuthority = () -> "SCOPE_NOMINAL_ROLL_READ_STUDENT";
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
    final GrantedAuthority grantedAuthority = () -> "SCOPE_NOMINAL_ROLL_READ_STUDENT";
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
    final GrantedAuthority grantedAuthority = () -> "SCOPE_NOMINAL_ROLL_READ_STUDENT";
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
    final GrantedAuthority grantedAuthority = () -> "SCOPE_NOMINAL_ROLL_READ_STUDENT";
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
    final GrantedAuthority grantedAuthority = () -> "SCOPE_NOMINAL_ROLL_READ_STUDENT";
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
    final GrantedAuthority grantedAuthority = () -> "SCOPE_NOMINAL_ROLL_READ_STUDENT";
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
    final GrantedAuthority grantedAuthority = () -> "SCOPE_NOMINAL_ROLL_READ_STUDENT";
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
    final GrantedAuthority grantedAuthority = () -> "SCOPE_NOMINAL_ROLL_READ_STUDENT";
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
    final GrantedAuthority grantedAuthority = () -> "SCOPE_NOMINAL_ROLL_READ_STUDENT";
    final var mockAuthority = oidcLogin().authorities(grantedAuthority);
    this.mockMvc
      .perform(get(BASE_URL + PAGINATED).with(mockAuthority).param("searchCriteriaList", "{test}")
        .contentType(APPLICATION_JSON)).andDo(print()).andExpect(status().isBadRequest());
  }

  @Test
  public void testProcessNominalRollFile_givenValidPayload_ShouldReturnStatusOk() throws Exception {
    final FileInputStream fis = new FileInputStream("src/test/resources/test-data.xlsx");
    final String fileContents = Base64.getEncoder().encodeToString(IOUtils.toByteArray(fis));
    assertThat(fileContents).isNotEmpty();
    val body = FileUpload.builder().fileContents(fileContents).fileExtension("xlsx").createUser("test").updateUser("test").build();
    this.mockMvc.perform(post(BASE_URL)
      .with(jwt().jwt((jwt) -> jwt.claim("scope", "NOMINAL_ROLL_UPLOAD_FILE")))
      .header("correlationID", UUID.randomUUID().toString())
      .content(JsonUtil.getJsonStringFromObject(body))
      .contentType(APPLICATION_JSON)).andExpect(status().isOk()).andExpect(jsonPath("$.nominalRollStudents", hasSize(6872))).andExpect(jsonPath("$.nominalRollStudents[0].validationErrors", is(nullValue())));
  }

  @Test
  public void testProcessNominalRollFile_givenValidPayload2_ShouldReturnStatusOk() throws Exception {
    final FileInputStream fis = new FileInputStream("src/test/resources/test-data-xls.xls");
    final String fileContents = Base64.getEncoder().encodeToString(IOUtils.toByteArray(fis));
    assertThat(fileContents).isNotEmpty();
    val body = FileUpload.builder().fileContents(fileContents).fileExtension("xls").createUser("test").updateUser("test").build();
    this.mockMvc.perform(post(BASE_URL)
      .with(jwt().jwt((jwt) -> jwt.claim("scope", "NOMINAL_ROLL_UPLOAD_FILE")))
      .header("correlationID", UUID.randomUUID().toString())
      .content(JsonUtil.getJsonStringFromObject(body))
      .contentType(APPLICATION_JSON)).andExpect(status().isOk()).andExpect(jsonPath("$.nominalRollStudents", hasSize(6872))).andExpect(jsonPath("$.nominalRollStudents[0].validationErrors", is(nullValue())));
  }

  @Test
  @Parameters({
    "src/test/resources/test-data-with-password.xlsx, File is password protected",
    "src/test/resources/test-data-missing-header.xlsx, Missing required header Surname",
    "src/test/resources/test-data-header-not-configured.xlsx, Missing required header Given Name(s)",
    "src/test/resources/test-data-header-blank.xlsx, Heading row has a blank cell at column 7",
  })
  public void testProcessNominalRollFile_givenEncryptedFile_ShouldReturnStatusBadRequest(final String filePath, final String errorMessage) throws Exception {
    final FileInputStream fis = new FileInputStream(filePath);
    final String fileContents = Base64.getEncoder().encodeToString(IOUtils.toByteArray(fis));
    assertThat(fileContents).isNotEmpty();
    val body = FileUpload.builder().fileContents(fileContents).fileExtension("xlsx").createUser("test").updateUser("test").build();
    this.mockMvc.perform(post(BASE_URL)
        .with(jwt().jwt((jwt) -> jwt.claim("scope", "NOMINAL_ROLL_UPLOAD_FILE")))
        .header("correlationID", UUID.randomUUID().toString())
        .content(JsonUtil.getJsonStringFromObject(body))
        .contentType(APPLICATION_JSON))
      .andExpect(status().isBadRequest())
      .andExpect(jsonPath("$.message", is(equalToIgnoringCase(errorMessage))));
  }


  @Test
  public void testProcessNominalRollFile_givenInvalidFileDirectory_ShouldThrowInternalServerError() throws Exception {
    final FileInputStream fis = new FileInputStream("src/test/resources/test-data-invalid-birthdate.xlsx");
    val basePath = this.applicationProperties.getFolderBasePath();
    this.applicationProperties.setFolderBasePath("test");
    final String fileContents = Base64.getEncoder().encodeToString(IOUtils.toByteArray(fis));
    assertThat(fileContents).isNotEmpty();
    val body = FileUpload.builder().fileContents(fileContents).fileExtension("xlsx").createUser("test").updateUser("test").build();
    this.mockMvc.perform(post(BASE_URL)
        .with(jwt().jwt((jwt) -> jwt.claim("scope", "NOMINAL_ROLL_UPLOAD_FILE")))
        .header("correlationID", UUID.randomUUID().toString())
        .content(JsonUtil.getJsonStringFromObject(body))
        .contentType(APPLICATION_JSON))
      .andExpect(status().isBadRequest());
    final FileInputStream fis2 = new FileInputStream("src/test/resources/test-data-xls.xls");
    final String fileContents2 = Base64.getEncoder().encodeToString(IOUtils.toByteArray(fis2));
    assertThat(fileContents2).isNotEmpty();
    val body2 = FileUpload.builder().fileContents(fileContents2).fileExtension("xls").createUser("test").updateUser("test").build();
    this.mockMvc.perform(post(BASE_URL)
        .with(jwt().jwt((jwt) -> jwt.claim("scope", "NOMINAL_ROLL_UPLOAD_FILE")))
        .header("correlationID", UUID.randomUUID().toString())
        .content(JsonUtil.getJsonStringFromObject(body2))
        .contentType(APPLICATION_JSON))
      .andExpect(status().isBadRequest());
    this.applicationProperties.setFolderBasePath(basePath);// reset to original value here
  }


  @Test
  public void testProcessNominalRollFile_givenInvalidPayload_ShouldReturnStatusBadRequest() throws Exception {
    val body = FileUpload.builder().fileExtension("xlsx").createUser("test").updateUser("test").build();
    this.mockMvc.perform(post(BASE_URL)
      .with(jwt().jwt((jwt) -> jwt.claim("scope", "NOMINAL_ROLL_UPLOAD_FILE")))
      .header("correlationID", UUID.randomUUID().toString())
      .content(JsonUtil.getJsonStringFromObject(body))
      .contentType(APPLICATION_JSON)).andExpect(status().isBadRequest());
  }

  @Test
  public void testProcessNominalRollFile_givenMissingCorrelationID_ShouldReturnStatusBadRequest() throws Exception {
    val body = FileUpload.builder().fileExtension("xlsx").createUser("test").updateUser("test").build();
    this.mockMvc.perform(post(BASE_URL)
      .with(jwt().jwt((jwt) -> jwt.claim("scope", "NOMINAL_ROLL_UPLOAD_FILE")))
      .content(JsonUtil.getJsonStringFromObject(body))
      .contentType(APPLICATION_JSON)).andExpect(status().isBadRequest());
  }

  @Test
  public void testReadStudentIDs_surnameEndWith_ShouldReturnStatusOkAndRecord() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_NOMINAL_ROLL_READ_STUDENT";
    final var mockAuthority = oidcLogin().authorities(grantedAuthority);
    final File file = new File(
      Objects.requireNonNull(this.getClass().getClassLoader().getResource("mock_nom_students.json")).getFile()
    );
    final List<NominalRollStudent> entities = new ObjectMapper().readValue(file, new TypeReference<>() {
    });
    final Map<String, String> searchCriteria = Map.of("surname", "B%");
    final ObjectMapper objectMapper = new ObjectMapper();
    final String criteriaJSON = objectMapper.writeValueAsString(searchCriteria);
    this.repository.saveAll(entities.stream().map(mapper::toModel).collect(Collectors.toList()));
    this.mockMvc
      .perform(get(BASE_URL + NOM_ROLL_STUDENT_IDS).with(mockAuthority).param("searchCriteria", criteriaJSON)
        .param("processingYear", "2021")
        .param("statusCodes", NominalRollStudentStatus.MATCHEDUSR.getCode() + "," + NominalRollStudentStatus.MATCHEDSYS.getCode())
        .contentType(APPLICATION_JSON))
        .andDo(print()).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(2)));
  }

  @Test
  public void testIsBeingProcessed_givenProcessingYear_ShouldReturnStatusOkAndStatusCounts() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_NOMINAL_ROLL_READ_STUDENT";
    final var mockAuthority = oidcLogin().authorities(grantedAuthority);
    final File file = new File(
      Objects.requireNonNull(this.getClass().getClassLoader().getResource("mock_nom_students.json")).getFile()
    );
    final List<NominalRollStudent> entities = new ObjectMapper().readValue(file, new TypeReference<>() {
    });
    this.repository.saveAll(entities.stream().map(mapper::toModel).collect(Collectors.toList()));
    this.mockMvc
      .perform(get(BASE_URL).with(mockAuthority).param("processingYear", "2021")
        .contentType(APPLICATION_JSON))
      .andDo(print()).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(3)))
      .andExpect(jsonPath("$[?(@.status == 'LOADED')].count", contains(1)));
  }

  @Test
  public void testIsBeingProcessed_givenNotProcessingYear_ShouldReturnStatusOkAndEmptyStatusCounts() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_NOMINAL_ROLL_READ_STUDENT";
    final var mockAuthority = oidcLogin().authorities(grantedAuthority);
    final File file = new File(
      Objects.requireNonNull(this.getClass().getClassLoader().getResource("mock_nom_students.json")).getFile()
    );
    final List<NominalRollStudent> entities = new ObjectMapper().readValue(file, new TypeReference<>() {
    });
    this.repository.saveAll(entities.stream().map(mapper::toModel).collect(Collectors.toList()));
    this.mockMvc
      .perform(get(BASE_URL).with(mockAuthority).param("processingYear", "2020")
        .contentType(APPLICATION_JSON))
      .andDo(print()).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(0)));
  }

  @Test
  public void testCheckForDuplicateNominalRollStudents_givenProcessingYear_ShouldReturnStatusOkAndTrue() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_NOMINAL_ROLL_READ_STUDENT";
    final var mockAuthority = oidcLogin().authorities(grantedAuthority);
    final File file = new File(
      Objects.requireNonNull(this.getClass().getClassLoader().getResource("mock_nom_students.json")).getFile()
    );
    final List<NominalRollStudent> entities = new ObjectMapper().readValue(file, new TypeReference<>() {
    });
    this.repository.saveAll(entities.stream().map(mapper::toModel).collect(Collectors.toList()));
    this.mockMvc
      .perform(get(BASE_URL + DUPLICATES).with(mockAuthority).header("correlationID", UUID.randomUUID().toString()).param("processingYear", "2021"))
      .andDo(print()).andExpect(status().isOk()).andExpect(content().string("true"));
  }

  @Test
  public void testCheckForDuplicateNominalRollStudents_givenNoProcessingYear_ShouldReturnStatusOkAndFalse() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_NOMINAL_ROLL_READ_STUDENT";
    final var mockAuthority = oidcLogin().authorities(grantedAuthority);
    final File file = new File(
      Objects.requireNonNull(this.getClass().getClassLoader().getResource("mock_nom_students.json")).getFile()
    );
    final List<NominalRollStudent> entities = new ObjectMapper().readValue(file, new TypeReference<>() {
    });
    this.repository.saveAll(entities.stream().map(mapper::toModel).collect(Collectors.toList()));
    this.mockMvc
      .perform(get(BASE_URL + DUPLICATES).with(mockAuthority).header("correlationID", UUID.randomUUID().toString()))
      .andDo(print()).andExpect(status().isOk()).andExpect(content().string("false"));
  }

  @Test
  public void testCheckForNominalRollPostedStudents_givenProcessingYear_ShouldReturnStatusOkAndTrue() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_NOMINAL_ROLL_READ_STUDENT";
    final var mockAuthority = oidcLogin().authorities(grantedAuthority);
    this.postedStudentRepository.save(this.createNominalRollPostedStudent());
    this.mockMvc
      .perform(get(BASE_URL + POSTED_STUDENTS + EXIST).with(mockAuthority).param("processingYear", "2021"))
      .andDo(print()).andExpect(status().isOk()).andExpect(content().string("true"));
  }

  @Test
  public void testCheckForNominalRollPostedStudents_givenInvalidProcessingYear_ShouldReturnStatusOkAndFalse() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_NOMINAL_ROLL_READ_STUDENT";
    final var mockAuthority = oidcLogin().authorities(grantedAuthority);
    this.postedStudentRepository.save(this.createNominalRollPostedStudent());
    this.mockMvc
      .perform(get(BASE_URL + POSTED_STUDENTS + EXIST).with(mockAuthority).param("processingYear", "2022"))
      .andDo(print()).andExpect(status().isOk()).andExpect(content().string("false"));
  }

  @Test
  public void testValidateNomRollStudent_givenNomRollStudent_ShouldReturnStatusOk() throws Exception {
    final var nomRollStudent = this.createMockNominalRollStudent();
    this.mockMvc
      .perform(post(BASE_URL + VALIDATE).with(jwt().jwt((jwt) -> jwt.claim("scope", "NOMINAL_ROLL_VALIDATE")))
        .content(JsonUtil.getJsonStringFromObject(nomRollStudent))
        .contentType(APPLICATION_JSON))
      .andDo(print()).andExpect(status().isOk()).andExpect(jsonPath("$.validationErrors", notNullValue()));
  }

  @Test
  public void testUpdateNominalRollStudent_givenNomRollStudent_ShouldReturnStatusOk() throws Exception {
    final var nomRollStudent = this.repository.save(this.createNominalRollStudent());
    this.mockMvc
      .perform(put(BASE_URL + "/" + nomRollStudent.getNominalRollStudentID().toString()).with(jwt().jwt((jwt) -> jwt.claim("scope", "NOMINAL_ROLL_WRITE_STUDENT")))
        .content(JsonUtil.getJsonStringFromObject(mapper.toStruct(nomRollStudent)))
        .contentType(APPLICATION_JSON))
      .andDo(print()).andExpect(status().isBadRequest()).andExpect(jsonPath("$.validationErrors", notNullValue()));
  }

  @Test
  public void testUpdateNominalRollStudent_givenNomRollStudentIgnored_ShouldReturnStatusOk() throws Exception {
    NominalRollStudentEntity student = this.createNominalRollStudent();
    student.setStatus(NominalRollStudentStatus.IGNORED.toString());
    final var nomRollStudent = this.repository.save(student);
    this.mockMvc
      .perform(put(BASE_URL + "/" + nomRollStudent.getNominalRollStudentID().toString()).with(jwt().jwt((jwt) -> jwt.claim("scope", "NOMINAL_ROLL_WRITE_STUDENT")))
        .content(JsonUtil.getJsonStringFromObject(mapper.toStruct(nomRollStudent)))
        .contentType(APPLICATION_JSON))
      .andDo(print()).andExpect(status().isOk());
  }

  @Test
  public void testUpdateNominalRollStudent_givenNomRollStudentError_ShouldReturnStatusOk() throws Exception {
    NominalRollStudentEntity student = this.createNominalRollStudent();
    student.setStatus(NominalRollStudentStatus.ERROR.toString());
    final var nomRollStudent = this.repository.save(student);
    this.mockMvc
      .perform(put(BASE_URL + "/" + nomRollStudent.getNominalRollStudentID().toString()).with(jwt().jwt((jwt) -> jwt.claim("scope", "NOMINAL_ROLL_WRITE_STUDENT")))
        .content(JsonUtil.getJsonStringFromObject(mapper.toStruct(nomRollStudent)))
        .contentType(APPLICATION_JSON))
      .andDo(print()).andExpect(status().isOk());
  }

  @Test
  public void testAddFedProvCodes_ShouldReturnStatusOk() throws Exception {
    FedProvSchoolCode fedProvSchoolCode = new FedProvSchoolCode();
    fedProvSchoolCode.setProvincialCode("654987");
    fedProvSchoolCode.setFederalCode("1234");
    fedProvSchoolCode.setKey("NOM_SCHL");

    NominalRollStudentEntity student = this.createNominalRollStudent();

    final var nomRollStudent = this.repository.save(student);
    NominalRollStudentValidationErrorEntity entity = new NominalRollStudentValidationErrorEntity();
    entity.setFieldName("School Number");
    entity.setFieldError("Invalid School Number");
    entity.setNominalRollStudent(nomRollStudent);
    entity.setCreateDate(LocalDateTime.now());
    entity.setCreateUser("ABC");
    entity.setUpdateDate(LocalDateTime.now());
    entity.setUpdateUser("ABC");
    student.getNominalRollStudentValidationErrors().add(entity);

    this.repository.save(student);

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

  private NominalRollPostedStudentEntity createNominalRollPostedStudent() {
    final NominalRollPostedStudentEntity student = new NominalRollPostedStudentEntity();
    student.setGivenNames("John");
    student.setSurname("Wayne");
    student.setBirthDate(LocalDate.of(1995, 8, 2));
    student.setGender("M");
    student.setBandOfResidence("4664");
    student.setFte(BigDecimal.valueOf(1.5));
    student.setGrade("01");
    student.setProcessingYear(LocalDateTime.now().withYear(2021));
    student.setStatus("MATCHEDSYS");
    return student;
  }

}
