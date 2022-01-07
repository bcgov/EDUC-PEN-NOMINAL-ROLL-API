package ca.bc.gov.educ.pen.nominalroll.api.service.v1;

import ca.bc.gov.educ.pen.nominalroll.api.filter.SagaFilterSpecs;
import ca.bc.gov.educ.pen.nominalroll.api.model.v1.Saga;
import org.springframework.stereotype.Service;

/**
 * The type Saga search service.
 */
@Service
public class SagaSearchService extends SearchService<Saga>{

  /**
   * Instantiates a new Saga search service.
   *
   * @param sagaFilterSpecs the saga filter specs
   */
  public SagaSearchService(SagaFilterSpecs sagaFilterSpecs) {
    super(sagaFilterSpecs, Saga.class);
  }
}
