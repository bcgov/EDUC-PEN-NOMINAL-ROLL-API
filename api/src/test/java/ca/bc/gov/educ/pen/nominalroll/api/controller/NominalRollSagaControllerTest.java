package ca.bc.gov.educ.pen.nominalroll.api.controller;

import ca.bc.gov.educ.pen.nominalroll.api.NominalRollApiApplication;
import ca.bc.gov.educ.pen.nominalroll.api.controller.v1.NominalRollSagaController;
import ca.bc.gov.educ.pen.nominalroll.api.repository.v1.SagaEventRepository;
import ca.bc.gov.educ.pen.nominalroll.api.repository.v1.SagaRepository;
import ca.bc.gov.educ.pen.nominalroll.api.service.v1.SagaService;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import static ca.bc.gov.educ.pen.nominalroll.api.constants.v1.URL.BASE_URL;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The Pen services saga controller tests
 */
@RunWith(SpringRunner.class)
@ActiveProfiles("test")
@SpringBootTest(classes = {NominalRollApiApplication.class})
@AutoConfigureMockMvc
@Slf4j
public class NominalRollSagaControllerTest {
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

  private final String studentID = "7f000101-7151-1d84-8171-5187006c0001";
  private final String mergedToPen = "123456789";
  private final String mergedStudentID = "7f000101-7151-1d84-8171-5187006c0003";
  private final String mergedFromPen = "987654321";

  @Before
  public void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  @After
  public void after() {
    sagaEventRepository.deleteAll();
    repository.deleteAll();
  }

  @Test
  public void testProcessStudentMerge_GivenInvalidPayload_ShouldReturnStatusBadRequest() throws Exception {
    this.mockMvc.perform(post(BASE_URL + "/post-saga")
            .with(jwt().jwt((jwt) -> jwt.claim("scope", "SCOPE_NOMINAL_ROLL")))
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .content(placeholderInvalidSagaData()))
            .andDo(print()).andExpect(status().isBadRequest());
  }


  protected String placeholderInvalidSagaData() {
    return " {\n" +
            "    \"createUser\": \"test\",\n" +
            "    \"updateUser\": \"test\",\n" +
            "  }";
  }

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


}
