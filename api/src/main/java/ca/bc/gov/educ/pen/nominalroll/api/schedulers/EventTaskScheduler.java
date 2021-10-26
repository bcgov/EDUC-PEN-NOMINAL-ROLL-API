package ca.bc.gov.educ.pen.nominalroll.api.schedulers;

import ca.bc.gov.educ.pen.nominalroll.api.service.events.schedulers.EventTaskSchedulerAsyncService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.core.LockAssert;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import static lombok.AccessLevel.PRIVATE;

/**
 * The type Event task scheduler.
 */
@Component
@Slf4j
public class EventTaskScheduler {
  /**
   * The Task scheduler async service.
   */
  @Getter(PRIVATE)
  private final EventTaskSchedulerAsyncService taskSchedulerAsyncService;

  /**
   * Instantiates a new Event task scheduler.
   *
   * @param taskSchedulerAsyncService the task scheduler async service
   */
  @Autowired
  public EventTaskScheduler(final EventTaskSchedulerAsyncService taskSchedulerAsyncService) {
    this.taskSchedulerAsyncService = taskSchedulerAsyncService;
  }


  @Scheduled(cron = "${scheduled.jobs.extract.uncompleted.sagas.cron}") // 1 * * * * *
  @SchedulerLock(name = "EXTRACT_UNCOMPLETED_SAGAS",
    lockAtLeastFor = "${scheduled.jobs.extract.uncompleted.sagas.cron.lockAtLeastFor}", lockAtMostFor = "${scheduled.jobs.extract.uncompleted.sagas.cron.lockAtMostFor}")
  public void findAndProcessPendingSagaEvents() {
    LockAssert.assertLocked();
    this.getTaskSchedulerAsyncService().findAndProcessUncompletedSagas();
  }


  @Scheduled(cron = "${scheduled.jobs.process.loaded.nom.roll.students.cron}") // every 1 minutes "0 0/1 * * * *"
  @SchedulerLock(name = "PROCESS_LOADED_STUDENTS", lockAtLeastFor = "${scheduled.jobs.process.loaded.nom.roll.students.cron.lockAtLeastFor}", lockAtMostFor = "${scheduled.jobs.process.loaded.nom.roll.students.cron.lockAtMostFor}")
  public void processLoadedNominalRollStudents() throws InterruptedException {
    LockAssert.assertLocked();
    this.getTaskSchedulerAsyncService().findAndPublishLoadedStudentRecordsForProcessing();
  }


}
