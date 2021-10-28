package ca.bc.gov.educ.pen.nominalroll.api.service.v1;

import ca.bc.gov.educ.pen.nominalroll.api.constants.EventOutcome;
import ca.bc.gov.educ.pen.nominalroll.api.constants.EventType;
import ca.bc.gov.educ.pen.nominalroll.api.constants.TopicsEnum;
import ca.bc.gov.educ.pen.nominalroll.api.constants.v1.NominalRollStudentStatus;
import ca.bc.gov.educ.pen.nominalroll.api.exception.EntityNotFoundException;
import ca.bc.gov.educ.pen.nominalroll.api.mappers.v1.NominalRollStudentMapper;
import ca.bc.gov.educ.pen.nominalroll.api.messaging.MessagePublisher;
import ca.bc.gov.educ.pen.nominalroll.api.model.v1.NominalRollPostedStudentEntity;
import ca.bc.gov.educ.pen.nominalroll.api.model.v1.NominalRollStudentEntity;
import ca.bc.gov.educ.pen.nominalroll.api.properties.ApplicationProperties;
import ca.bc.gov.educ.pen.nominalroll.api.repository.v1.NominalRollPostedStudentRepository;
import ca.bc.gov.educ.pen.nominalroll.api.repository.v1.NominalRollStudentRepository;
import ca.bc.gov.educ.pen.nominalroll.api.struct.v1.Event;
import ca.bc.gov.educ.pen.nominalroll.api.struct.v1.NominalRollStudentSagaData;
import ca.bc.gov.educ.pen.nominalroll.api.util.JsonUtil;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.jboss.threads.EnhancedQueueExecutor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

@Service
@Slf4j
public class NominalRollService {
  private static final String STUDENT_ID_ATTRIBUTE = "nominalRollStudentID";
  private final MessagePublisher messagePublisher;
  private final NominalRollStudentRepository repository;
  private final NominalRollPostedStudentRepository postedStudentRepository;
  private final Executor paginatedQueryExecutor = new EnhancedQueueExecutor.Builder()
    .setThreadFactory(new ThreadFactoryBuilder().setNameFormat("async-pagination-query-executor-%d").build())
    .setCorePoolSize(2).setMaximumPoolSize(10).setKeepAliveTime(Duration.ofSeconds(60)).build();

  public NominalRollService(final MessagePublisher messagePublisher, final NominalRollStudentRepository repository, final NominalRollPostedStudentRepository postedStudentRepository) {
    this.messagePublisher = messagePublisher;
    this.repository = repository;
    this.postedStudentRepository = postedStudentRepository;
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
    return this.repository.findById(nominalRollStudentID).orElseThrow(() -> new EntityNotFoundException(NominalRollStudentEntity.class, STUDENT_ID_ATTRIBUTE, nominalRollStudentID.toString()));
  }

  public long countAllNominalRollStudents(final String processingYear) {
    return this.repository.countAllByProcessingYear(processingYear);
  }

  public void deleteAllNominalRollStudents(final String processingYear) {
    this.repository.deleteAllByProcessingYear(processingYear);
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public List<NominalRollStudentEntity> saveNominalRollStudents(final List<NominalRollStudentEntity> nomRollStudentEntities, final String correlationID) {
    log.debug("creating nominal roll entities in transient table for transaction ID :: {}", correlationID);
    return this.repository.saveAll(nomRollStudentEntities);
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

  public NominalRollStudentEntity updateNominalRollStudent(final NominalRollStudentEntity entity) {
    return this.repository.save(entity);
  }

  public void publishUnprocessedStudentRecordsForProcessing(final List<NominalRollStudentSagaData> nominalRollStudentSagaDatas) {
    nominalRollStudentSagaDatas.forEach(this::sendIndividualStudentAsMessageToTopic);
  }

  @Async("publisherExecutor")
  public void prepareAndSendNominalRollStudentsForFurtherProcessing(final List<NominalRollStudentEntity> nominalRollStudentEntities) {
    final List<NominalRollStudentSagaData> nominalRollStudentSagaDatas = nominalRollStudentEntities.stream()
      .map(el -> {
        val nominalRollStudentSagaData = new NominalRollStudentSagaData();
        nominalRollStudentSagaData.setNominalRollStudent(NominalRollStudentMapper.mapper.toStruct(el));
        return nominalRollStudentSagaData;
      })
      .collect(Collectors.toList());
    this.publishUnprocessedStudentRecordsForProcessing(nominalRollStudentSagaDatas);
  }

  /**
   * Send individual student as message to topic consumer.
   */
  private void sendIndividualStudentAsMessageToTopic(final NominalRollStudentSagaData nominalRollStudentSagaData) {
    final var eventPayload = JsonUtil.getJsonString(nominalRollStudentSagaData);
    if (eventPayload.isPresent()) {
      final Event event = Event.builder().eventType(EventType.READ_FROM_TOPIC).eventOutcome(EventOutcome.READ_FROM_TOPIC_SUCCESS).eventPayload(eventPayload.get()).nominalRollStudentID(nominalRollStudentSagaData.getNominalRollStudent().getNominalRollStudentID()).build();
      final var eventString = JsonUtil.getJsonString(event);
      if (eventString.isPresent()) {
        this.messagePublisher.dispatchMessage(TopicsEnum.NOMINAL_ROLL_API_TOPIC.toString(), eventString.get().getBytes());
      } else {
        log.error("Event String is empty, skipping the publish to topic :: {}", nominalRollStudentSagaData);
      }
    } else {
      log.error("Event payload is empty, skipping the publish to topic :: {}", nominalRollStudentSagaData);
    }
  }

  public Optional<NominalRollStudentEntity> findByNominalRollStudentID(final String nominalRollStudentID) {
    return this.repository.findById(UUID.fromString(nominalRollStudentID));
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void saveNominalRollStudent(final NominalRollStudentEntity nomRollStud) {
    this.repository.save(nomRollStud);
  }

  public List<NominalRollPostedStudentEntity> findAllBySurnameAndGivenNamesAndBirthDateAndGenderAndGrade(final String surname, final String givenNames, final LocalDate birthDate, final String gender, final String grade) {
    return this.postedStudentRepository.findAllBySurnameAndGivenNamesAndBirthDateAndGenderAndGradeOrderByCreateDateDesc(surname, givenNames, birthDate, gender, grade);
  }
}
