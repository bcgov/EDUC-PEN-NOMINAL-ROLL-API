package ca.bc.gov.educ.pen.nominalroll.api.schedulers;

import ca.bc.gov.educ.pen.nominalroll.api.model.v1.SagaEventStates;
import ca.bc.gov.educ.pen.nominalroll.api.repository.v1.NominalRollEventRepository;
import ca.bc.gov.educ.pen.nominalroll.api.repository.v1.SagaEventRepository;
import ca.bc.gov.educ.pen.nominalroll.api.repository.v1.SagaRepository;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import net.javacrumbs.shedlock.core.LockAssert;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static lombok.AccessLevel.PRIVATE;

@Component
@Slf4j
public class PurgeEventRecordsScheduler {
  @Getter(PRIVATE)
  private final SagaRepository sagaRepository;

  @Getter(PRIVATE)
  private final SagaEventRepository sagaEventRepository;
  private final NominalRollEventRepository nominalRollEventRepository;
  @Value("${purge.records.saga.after.days}")
  @Setter
  @Getter
  Integer sagaRecordStaleInDays;

  public PurgeEventRecordsScheduler(final SagaRepository sagaRepository, final SagaEventRepository sagaEventRepository, final NominalRollEventRepository nominalRollEventRepository) {
    this.sagaRepository = sagaRepository;
    this.sagaEventRepository = sagaEventRepository;
    this.nominalRollEventRepository = nominalRollEventRepository;
  }


  /**
   * run the job based on configured scheduler(a cron expression) and purge old saga and event records from DB.
   */
  @Scheduled(cron = "${scheduled.jobs.purge.old.event.records.cron}")
  @SchedulerLock(name = "PurgeOldSagaRecordsLock",
    lockAtLeastFor = "PT1H", lockAtMostFor = "PT1H") //midnight job so lock for an hour
  @Transactional
  public void purgeOldEventRecords() {
    LockAssert.assertLocked();
    final LocalDateTime createDateToCompare = this.calculateCreateDateBasedOnStaleSagaRecordInDays();
    val oldEvents = this.nominalRollEventRepository.findAllByCreateDateBefore(createDateToCompare);
    final List<SagaEventStates> sagaEventList = new CopyOnWriteArrayList<>();
    final var sagas = this.getSagaRepository().findAllByCreateDateBefore(createDateToCompare);
    if (!sagas.isEmpty()) {
      for (val saga : sagas) {
        sagaEventList.addAll(this.getSagaEventRepository().findBySaga(saga));
      }
    }
    this.sagaEventRepository.deleteAll(sagaEventList);
    this.sagaRepository.deleteAll(sagas);
    this.nominalRollEventRepository.deleteAll(oldEvents);
  }

  private LocalDateTime calculateCreateDateBasedOnStaleSagaRecordInDays() {
    final LocalDateTime currentTime = LocalDateTime.now();
    return currentTime.minusDays(this.getSagaRecordStaleInDays());
  }
}
