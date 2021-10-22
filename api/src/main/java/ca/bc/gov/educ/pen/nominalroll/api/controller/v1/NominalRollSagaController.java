package ca.bc.gov.educ.pen.nominalroll.api.controller.v1;

import ca.bc.gov.educ.pen.nominalroll.api.endpoint.v1.NominalRollSagaEndpoint;
import ca.bc.gov.educ.pen.nominalroll.api.mappers.SagaMapper;
import ca.bc.gov.educ.pen.nominalroll.api.orchestrator.base.Orchestrator;
import ca.bc.gov.educ.pen.nominalroll.api.service.v1.SagaService;
import ca.bc.gov.educ.pen.nominalroll.api.struct.v1.NominalRollPostSagaData;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static lombok.AccessLevel.PRIVATE;

/**
 * The type Pen services saga controller.
 */
@RestController
@Slf4j
public class NominalRollSagaController implements NominalRollSagaEndpoint {

  /**
   * The Saga service.
   */
  @Getter(PRIVATE)
  private final SagaService sagaService;
  /**
   * The Handlers.
   */
  @Getter(PRIVATE)
  private final Map<String, Orchestrator> orchestratorMap = new HashMap<>();

  /**
   * Instantiates a new Pen services saga controller.
   *
   * @param sagaService   the saga service
   * @param orchestrators the orchestrators
   */
  @Autowired
  public NominalRollSagaController(final SagaService sagaService, final List<Orchestrator> orchestrators) {
    this.sagaService = sagaService;
    orchestrators.forEach(orchestrator -> this.orchestratorMap.put(orchestrator.getSagaName(), orchestrator));
    log.info("'{}' Saga Orchestrators are loaded.", String.join(",", this.orchestratorMap.keySet()));
  }

  @Override
  public ResponseEntity<String> postNominalRoll(NominalRollPostSagaData nominalRollPostSagaData) {
    return null;
  }
}
