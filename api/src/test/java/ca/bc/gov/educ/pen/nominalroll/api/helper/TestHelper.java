package ca.bc.gov.educ.pen.nominalroll.api.helper;

import ca.bc.gov.educ.pen.nominalroll.api.repository.v1.*;
import lombok.Getter;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@Getter
public class TestHelper {
  private final NominalRollStudentRepository repository;
  private final NominalRollPostedStudentRepository postedStudentRepository;
  private final NominalRollEventRepository eventRepository;
  private final SagaRepository sagaRepository;
  private final SagaEventRepository sagaEventRepository;

  public TestHelper(final NominalRollStudentRepository repository, final NominalRollPostedStudentRepository postedStudentRepository, final NominalRollEventRepository eventRepository, final SagaRepository sagaRepository, final SagaEventRepository sagaEventRepository) {
    this.repository = repository;
    this.postedStudentRepository = postedStudentRepository;
    this.eventRepository = eventRepository;
    this.sagaRepository = sagaRepository;
    this.sagaEventRepository = sagaEventRepository;
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void cleanDB() {
    this.repository.deleteAll();
    this.postedStudentRepository.deleteAll();
    this.eventRepository.deleteAll();
    this.sagaEventRepository.deleteAll();
    this.sagaRepository.deleteAll();
  }
}
