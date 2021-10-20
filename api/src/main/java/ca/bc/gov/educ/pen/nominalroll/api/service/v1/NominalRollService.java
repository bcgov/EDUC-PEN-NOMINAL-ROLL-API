package ca.bc.gov.educ.pen.nominalroll.api.service.v1;

import ca.bc.gov.educ.pen.nominalroll.api.constants.v1.NominalRollStudentStatus;
import ca.bc.gov.educ.pen.nominalroll.api.exception.EntityNotFoundException;
import ca.bc.gov.educ.pen.nominalroll.api.model.v1.NominalRollStudentEntity;
import ca.bc.gov.educ.pen.nominalroll.api.repository.v1.NominalRollStudentRepository;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.extern.slf4j.Slf4j;
import org.jboss.threads.EnhancedQueueExecutor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;

@Service
@Slf4j
public class NominalRollService {
  private static final String STUDENT_ID_ATTRIBUTE = "nominalRollStudentID";
  private final NominalRollStudentRepository repository;
  private final Executor paginatedQueryExecutor = new EnhancedQueueExecutor.Builder()
    .setThreadFactory(new ThreadFactoryBuilder().setNameFormat("async-pagination-query-executor-%d").build())
    .setCorePoolSize(2).setMaximumPoolSize(10).setKeepAliveTime(Duration.ofSeconds(60)).build();

  public NominalRollService(final NominalRollStudentRepository repository) {
    this.repository = repository;
  }

  public boolean isAllRecordsProcessed() {
    final long count = this.repository.countByStatus(NominalRollStudentStatus.LOADED.toString());
    return count < 1;
  }


  public boolean hasDuplicateRecords() {
    final long count = this.repository.countForDuplicateAssignedPENs(Integer.toString(LocalDateTime.now().getYear()));
    return count > 1;
  }

  public List<NominalRollStudentEntity> getAllNominalRollStudents() {
    return this.repository.findAll();
  }

  public NominalRollStudentEntity getNominalRollStudentByID(final UUID nominalRollStudentID) {
    Optional<NominalRollStudentEntity> result = repository.findById(nominalRollStudentID);
    if (result.isPresent()) {
      return result.get();
    } else {
      throw new EntityNotFoundException(NominalRollStudentEntity.class, STUDENT_ID_ATTRIBUTE, nominalRollStudentID.toString());
    }
  }

  public long countAllNominalRollStudents(final String processingYear) {
    return this.repository.countAllByProcessingYear(processingYear);
  }

  public void deleteAllNominalRollStudents(final String processingYear) {
    this.repository.deleteAllByProcessingYear(processingYear);
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void saveNominalRollStudents(final List<NominalRollStudentEntity> nomRollStudentEntities, final String correlationID) {
    log.debug("creating nominal roll entities in transient table for transaction ID :: {}", correlationID);
    this.repository.saveAll(nomRollStudentEntities);
  }

  /**
   * Find all completable future.
   *
   * @param studentSpecs the student specs
   * @param pageNumber   the page number
   * @param pageSize     the page size
   * @param sorts        the sorts
   * @return the completable future
   */
  @Transactional(propagation = Propagation.SUPPORTS)
  public CompletableFuture<Page<NominalRollStudentEntity>> findAll(final Specification<NominalRollStudentEntity> studentSpecs, final Integer pageNumber, final Integer pageSize, final List<Sort.Order> sorts) {
    return CompletableFuture.supplyAsync(() -> {
      final Pageable paging = PageRequest.of(pageNumber, pageSize, Sort.by(sorts));
      try {
        return this.repository.findAll(studentSpecs, paging);
      } catch (final Exception ex) {
        throw new CompletionException(ex);
      }
    }, this.paginatedQueryExecutor);

  }
}
