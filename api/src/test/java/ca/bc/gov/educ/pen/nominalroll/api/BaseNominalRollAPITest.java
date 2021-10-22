package ca.bc.gov.educ.pen.nominalroll.api;

import org.junit.runner.RunWith;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@ActiveProfiles("test")
@SpringBootTest(classes = NominalRollApiApplication.class)
@AutoConfigureMockMvc
public abstract class BaseNominalRollAPITest {
}
