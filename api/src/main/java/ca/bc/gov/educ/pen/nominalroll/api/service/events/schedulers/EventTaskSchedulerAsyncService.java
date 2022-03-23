package ca.bc.gov.educ.pen.nominalroll.api.service.events.schedulers;

import ca.bc.gov.educ.pen.nominalroll.api.constants.SagaEnum;
import ca.bc.gov.educ.pen.nominalroll.api.constants.SagaStatusEnum;
import ca.bc.gov.educ.pen.nominalroll.api.constants.v1.NominalRollStudentStatus;
import ca.bc.gov.educ.pen.nominalroll.api.helpers.LogHelper;
import ca.bc.gov.educ.pen.nominalroll.api.model.v1.NominalRollStudentEntity;
import ca.bc.gov.educ.pen.nominalroll.api.model.v1.Saga;
import ca.bc.gov.educ.pen.nominalroll.api.orchestrator.base.Orchestrator;
import ca.bc.gov.educ.pen.nominalroll.api.repository.v1.NominalRollStudentRepository;
import ca.bc.gov.educ.pen.nominalroll.api.repository.v1.SagaRepository;
import ca.bc.gov.educ.pen.nominalroll.api.service.v1.NominalRollService;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import static lombok.AccessLevel.PRIVATE;

@Service
@Slf4j
public class EventTaskSchedulerAsyncService {
  @Getter(PRIVATE)
  private final SagaRepository sagaRepository;

  @Getter(PRIVATE)
  private final NominalRollStudentRepository nominalRollStudentRepository;

  @Getter(PRIVATE)
  private final Map<String, Orchestrator> sagaOrchestrators = new HashMap<>();

  @Getter(PRIVATE)
  private final NominalRollService nominalRollService;
  @Setter
  private List<String> statusFilters;

  public EventTaskSchedulerAsyncService(final List<Orchestrator> orchestrators, final SagaRepository sagaRepository, final NominalRollStudentRepository nominalRollStudentRepository, final NominalRollService nominalRollService) {
    this.sagaRepository = sagaRepository;
    this.nominalRollStudentRepository = nominalRollStudentRepository;
    this.nominalRollService = nominalRollService;
    orchestrators.forEach(orchestrator -> this.sagaOrchestrators.put(orchestrator.getSagaName(), orchestrator));
  }

  @Async("taskExecutor")
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void findAndProcessUncompletedSagas() {
    final var sagas = this.getSagaRepository().findTop100ByStatusInOrderByCreateDate(this.getStatusFilters());
    if (!sagas.isEmpty()) {
      this.processUncompletedSagas(sagas);
    }
  }

  private void processUncompletedSagas(final List<Saga> sagas) {
    for (val saga : sagas) {
      if (saga.getUpdateDate().isBefore(LocalDateTime.now().minusMinutes(2))
        && this.getSagaOrchestrators().containsKey(saga.getSagaName())) {
        try {
          this.setRetryCountAndLog(saga);
          this.getSagaOrchestrators().get(saga.getSagaName()).replaySaga(saga);
        } catch (final InterruptedException ex) {
          Thread.currentThread().interrupt();
          log.error("InterruptedException while findAndProcessPendingSagaEvents :: for saga :: {} :: {}", saga, ex);
        } catch (final IOException | TimeoutException e) {
          log.error("Exception while findAndProcessPendingSagaEvents :: for saga :: {} :: {}", saga, e);
        }
      }
    }
  }

  @Async("taskExecutor")
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void findAndPublishLoadedStudentRecordsForProcessing() {
    if (this.getSagaRepository().countAllByStatusIn(this.getStatusFilters()) > 20) { // at max there will be 40 parallel sagas.
      log.info("Saga count is greater than 20, so not processing student records");
      return;
    }
    final List<NominalRollStudentEntity> studentEntities = new ArrayList<>();
    final var nominalRollStudentEntities = this.getNominalRollStudentRepository().findTop100ByStatusOrderByCreateDate(NominalRollStudentStatus.LOADED.toString());
    log.info("found :: {}  records in loaded status", nominalRollStudentEntities.size());
    if (!nominalRollStudentEntities.isEmpty()) {
      for (val entity : nominalRollStudentEntities) {
        if (this.getSagaRepository().findByNominalRollStudentIDAndSagaName(entity.getNominalRollStudentID(), SagaEnum.NOMINAL_ROLL_PROCESS_STUDENT_SAGA.toString()).isEmpty()) {
          log.info("Adding student record :: {}", entity.toString());
          studentEntities.add(entity);
        }
      }
    }
    if (!studentEntities.isEmpty()) {
      this.getNominalRollService().prepareAndSendNominalRollStudentsForFurtherProcessing(studentEntities);
    }
  }


  public List<String> getStatusFilters() {
    if (this.statusFilters != null && !this.statusFilters.isEmpty()) {
      return this.statusFilters;
    } else {
      final var statuses = new ArrayList<String>();
      statuses.add(SagaStatusEnum.IN_PROGRESS.toString());
      statuses.add(SagaStatusEnum.STARTED.toString());
      return statuses;
    }
  }

  private void setRetryCountAndLog(final Saga saga) {
    Integer retryCount = saga.getRetryCount();
    if (retryCount == null || retryCount == 0) {
      retryCount = 1;
    } else {
      retryCount += 1;
    }
    saga.setRetryCount(retryCount);
    this.getSagaRepository().save(saga);
    LogHelper.logSagaRetry(saga);
  }
}
