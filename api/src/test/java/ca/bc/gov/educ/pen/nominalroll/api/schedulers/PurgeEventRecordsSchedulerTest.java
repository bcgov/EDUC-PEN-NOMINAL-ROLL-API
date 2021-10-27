package ca.bc.gov.educ.pen.nominalroll.api.schedulers;

import ca.bc.gov.educ.pen.nominalroll.api.BaseNominalRollAPITest;
import lombok.val;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

public class PurgeEventRecordsSchedulerTest extends BaseNominalRollAPITest {

  @Autowired
  PurgeEventRecordsScheduler purgeEventRecordsScheduler;

  @Test
  public void testPurgeOldEventRecords_givenOldRecordsPresentInDB_shouldBeDeleted() {
    val saga = this.creatMockSaga(null);
    saga.setCreateDate(LocalDateTime.now().minusDays(2));
    this.testHelper.getSagaRepository().save(saga);
    this.purgeEventRecordsScheduler.purgeOldEventRecords();
    assertThat(this.testHelper.getSagaRepository().findAll()).isEmpty();
  }
}
