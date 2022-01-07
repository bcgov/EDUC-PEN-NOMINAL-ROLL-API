package ca.bc.gov.educ.pen.nominalroll.api.controller.v1;

import ca.bc.gov.educ.pen.nominalroll.api.constants.SagaStatusEnum;
import ca.bc.gov.educ.pen.nominalroll.api.endpoint.v1.NominalRollSagaEndpoint;
import ca.bc.gov.educ.pen.nominalroll.api.exception.SagaRuntimeException;
import ca.bc.gov.educ.pen.nominalroll.api.mappers.SagaMapper;
import ca.bc.gov.educ.pen.nominalroll.api.orchestrator.base.Orchestrator;
import ca.bc.gov.educ.pen.nominalroll.api.service.v1.SagaSearchService;
import ca.bc.gov.educ.pen.nominalroll.api.service.v1.SagaService;
import ca.bc.gov.educ.pen.nominalroll.api.struct.v1.NominalRollPostSagaData;
import ca.bc.gov.educ.pen.nominalroll.api.struct.v1.Saga;
import ca.bc.gov.educ.pen.nominalroll.api.struct.v1.SagaEvent;
import ca.bc.gov.educ.pen.nominalroll.api.util.JsonUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static ca.bc.gov.educ.pen.nominalroll.api.constants.SagaEnum.NOMINAL_ROLL_POST_DATA_SAGA;
import static lombok.AccessLevel.PRIVATE;

/**
 * The type Nominal roll saga controller.
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
   * The Saga search service.
   */
  @Getter(PRIVATE)
  private final SagaSearchService searchService;

  private static final SagaMapper sagaMapper = SagaMapper.mapper;

  /**
   * The Handlers.
   */
  @Getter(PRIVATE)
  private final Map<String, Orchestrator> orchestratorMap = new HashMap<>();

  /**
   * Instantiates a new nominal roll saga controller.
   *
   * @param sagaService   the saga service
   * @param searchService the saga search service
   * @param orchestrators the orchestrators
   */
  @Autowired
  public NominalRollSagaController(final SagaService sagaService, final SagaSearchService searchService, final List<Orchestrator> orchestrators) {
    this.sagaService = sagaService;
    this.searchService = searchService;
    orchestrators.forEach(orchestrator -> this.orchestratorMap.put(orchestrator.getSagaName(), orchestrator));
    log.info("'{}' Saga Orchestrators are loaded.", String.join(",", this.orchestratorMap.keySet()));
  }

  @Override
  public ResponseEntity<String> postNominalRoll(NominalRollPostSagaData nominalRollPostSagaData) {
    final var processingYear = nominalRollPostSagaData.getProcessingYear();

    final var sagaInProgress = !this.getSagaService().findAllByProcessingYearAndStatusIn(processingYear, this.getSagaStatusesFilter()).isEmpty();

    if (sagaInProgress) {
      return ResponseEntity.status(HttpStatus.CONFLICT).build();
    }

    final var orchestrator = this.getOrchestratorMap().get(NOMINAL_ROLL_POST_DATA_SAGA.toString());
    try {
      final var saga = orchestrator.createSaga(JsonUtil.getJsonStringFromObject(nominalRollPostSagaData),
        null, nominalRollPostSagaData.getCreateUser(), processingYear);
      orchestrator.startSaga(saga);
      return ResponseEntity.ok(saga.getSagaId().toString());
    } catch (final JsonProcessingException e) {
      log.error("JsonProcessingException while processStudentRequest", e);
      throw new SagaRuntimeException(e.getMessage());
    }
  }

  @Override
  public ResponseEntity<Saga> readSaga(final UUID sagaID) {
    return this.getSagaService().findSagaById(sagaID)
      .map(sagaMapper::toStruct)
      .map(ResponseEntity::ok)
      .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
  }

  /**
   * Find all sagas completable future.
   *
   * @param pageNumber             the page number
   * @param pageSize               the page size
   * @param sortCriteriaJson       the sort criteria json
   * @param searchCriteriaListJson the search criteria list json
   * @return the completable future
   */
  @Override
  public CompletableFuture<Page<Saga>> findAllSagas(final Integer pageNumber, final Integer pageSize, final String sortCriteriaJson, final String searchCriteriaListJson) {
    final List<Sort.Order> sorts = new ArrayList<>();
    final Specification<ca.bc.gov.educ.pen.nominalroll.api.model.v1.Saga> sagaSpecs = this.searchService.setSpecificationAndSortCriteria(sortCriteriaJson, searchCriteriaListJson, JsonUtil.mapper, sorts);
    return this.getSagaService().findAll(sagaSpecs, pageNumber, pageSize, sorts).thenApplyAsync(sagas -> sagas.map(sagaMapper::toStruct));
  }

  /**
   * Find all saga events for a given saga id
   *
   * @param sagaId - the saga id
   * @return - the list of saga events
   */
  @Override
  public ResponseEntity<List<SagaEvent>> getSagaEventsBySagaID(final UUID sagaId) {
    val sagaOptional = this.getSagaService().findSagaById(sagaId);
    return sagaOptional.map(saga -> ResponseEntity.ok(this.getSagaService().findAllSagaStates(saga).stream()
      .map(SagaMapper.mapper::toEventStruct).collect(Collectors.toList())))
      .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
  }

  /**
   * Update saga
   *
   * @param saga - the saga
   * @return - the updated saga
   */
  @Override
  @Transactional
  public ResponseEntity<Saga> updateSaga(final Saga saga, final UUID sagaId) {
    final var sagaOptional = this.getSagaService().findSagaById(sagaId);
    if (sagaOptional.isPresent()) {
      val sagaFromDB = sagaOptional.get();
      if (!sagaMapper.toStruct(sagaFromDB).getUpdateDate().equals(saga.getUpdateDate())) {
        log.error("Updating saga failed. The saga has already been updated by another process :: " + saga.getSagaId());
        return ResponseEntity.status(HttpStatus.CONFLICT).build();
      }
      sagaFromDB.setPayload(saga.getPayload());
      sagaFromDB.setUpdateDate(LocalDateTime.now());
      this.getSagaService().updateSagaRecord(sagaFromDB);
      return ResponseEntity.ok(sagaMapper.toStruct(sagaFromDB));
    } else {
      log.error("Error attempting to get saga. Saga id does not exist :: " + saga.getSagaId());
      return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }
  }


  protected List<String> getSagaStatusesFilter() {
    final var statuses = new ArrayList<String>();
    statuses.add(SagaStatusEnum.IN_PROGRESS.toString());
    statuses.add(SagaStatusEnum.STARTED.toString());
    return statuses;
  }
}
