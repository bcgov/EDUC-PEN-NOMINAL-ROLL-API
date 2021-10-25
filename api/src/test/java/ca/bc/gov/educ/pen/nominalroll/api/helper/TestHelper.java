package ca.bc.gov.educ.pen.nominalroll.api.helper;

import ca.bc.gov.educ.pen.nominalroll.api.repository.v1.NominalRollStudentRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
public class TestHelper {
  private final NominalRollStudentRepository repository;


  public TestHelper(NominalRollStudentRepository repository) {
    this.repository = repository;
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void cleanDB() {
    this.repository.deleteAll();
  }
}
