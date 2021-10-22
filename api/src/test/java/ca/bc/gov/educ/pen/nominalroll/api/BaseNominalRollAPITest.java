package ca.bc.gov.educ.pen.nominalroll.api;

import ca.bc.gov.educ.pen.nominalroll.api.rest.RestUtils;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@ActiveProfiles("test")
@SpringBootTest(classes = NominalRollApiApplication.class)
@AutoConfigureMockMvc
public abstract class BaseNominalRollAPITest {

  @MockBean
  protected RestUtils restUtils;

  @Before
  public void setup() {
    MockitoAnnotations.openMocks(this);
    Mockito.reset(restUtils);
  }
}
