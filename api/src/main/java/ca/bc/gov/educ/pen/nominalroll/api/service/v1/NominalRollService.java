package ca.bc.gov.educ.pen.nominalroll.api.service.v1;

import ca.bc.gov.educ.pen.nominalroll.api.constants.CacheNames;
import ca.bc.gov.educ.pen.nominalroll.api.constants.EventOutcome;
import ca.bc.gov.educ.pen.nominalroll.api.constants.EventType;
import ca.bc.gov.educ.pen.nominalroll.api.constants.TopicsEnum;
import ca.bc.gov.educ.pen.nominalroll.api.constants.v1.NominalRollStudentStatus;
import ca.bc.gov.educ.pen.nominalroll.api.exception.EntityNotFoundException;
import ca.bc.gov.educ.pen.nominalroll.api.exception.NominalRollAPIRuntimeException;
import ca.bc.gov.educ.pen.nominalroll.api.helpers.NominalRollHelper;
import ca.bc.gov.educ.pen.nominalroll.api.mappers.v1.NominalRollStudentMapper;
import ca.bc.gov.educ.pen.nominalroll.api.messaging.MessagePublisher;
import ca.bc.gov.educ.pen.nominalroll.api.model.v1.FedProvCodeEntity;
import ca.bc.gov.educ.pen.nominalroll.api.model.v1.NominalRollPostedStudentEntity;
import ca.bc.gov.educ.pen.nominalroll.api.model.v1.NominalRollStudentEntity;
import ca.bc.gov.educ.pen.nominalroll.api.model.v1.NominalRollStudentValidationErrorEntity;
import ca.bc.gov.educ.pen.nominalroll.api.repository.v1.*;
import ca.bc.gov.educ.pen.nominalroll.api.rest.RestUtils;
import ca.bc.gov.educ.pen.nominalroll.api.struct.external.school.v1.FedProvSchoolCode;
import ca.bc.gov.educ.pen.nominalroll.api.struct.v1.*;
import ca.bc.gov.educ.pen.nominalroll.api.util.JsonUtil;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.jboss.threads.EnhancedQueueExecutor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;


@Component
@Service
@Slf4j
public class NominalRollService {
  private static final String STUDENT_ID_ATTRIBUTE = "nominalRollStudentID";
  private final MessagePublisher messagePublisher;
  private final RestUtils restUtils;
  private final NominalRollStudentRepository repository;
  private final NominalRollPostedStudentRepository postedStudentRepository;
  private final NominalRollStudentValidationErrorRepository nominalRollStudentValidationErrorRepository;
  private final FedProvCodeRepository fedProvCodeRepository;

  private Map<String, String> schoolCodeMap = new ConcurrentHashMap<>();

  private final NominalRollStudentRepositoryCustom nominalRollStudentRepositoryCustom;
  private final Executor paginatedQueryExecutor = new EnhancedQueueExecutor.Builder()
    .setThreadFactory(new ThreadFactoryBuilder().setNameFormat("async-pagination-query-executor-%d").build())
    .setCorePoolSize(2).setMaximumPoolSize(10).setKeepAliveTime(Duration.ofSeconds(60)).build();

  @Autowired
  public NominalRollService(final RestUtils restUtils, final MessagePublisher messagePublisher, final NominalRollStudentRepository repository, final NominalRollPostedStudentRepository postedStudentRepository,
                            final NominalRollStudentRepositoryCustom nominalRollStudentRepositoryCustom, final NominalRollStudentValidationErrorRepository nominalRollStudentValidationErrorRepository, final FedProvCodeRepository fedProvCodeRepository) {
    this.messagePublisher = messagePublisher;
    this.restUtils = restUtils;
    this.repository = repository;
    this.postedStudentRepository = postedStudentRepository;
    this.nominalRollStudentRepositoryCustom = nominalRollStudentRepositoryCustom;
    this.nominalRollStudentValidationErrorRepository = nominalRollStudentValidationErrorRepository;
    this.fedProvCodeRepository = fedProvCodeRepository;
  }

  public boolean isAllRecordsProcessed() {
    final long count = this.repository.countByStatus(NominalRollStudentStatus.LOADED.toString());
    return count < 1;
  }


  public boolean hasDuplicateRecords(final String processingYear) {
    final Long count = this.repository.countForDuplicateAssignedPENs(processingYear);
    return count != null && count > 1;
  }

  public List<NominalRollStudentEntity> getAllNominalRollStudents() {
    return this.repository.findAll();
  }

  public NominalRollStudentEntity getNominalRollStudentByID(final UUID nominalRollStudentID) {
    return this.repository.findById(nominalRollStudentID).orElseThrow(() -> new EntityNotFoundException(NominalRollStudentEntity.class, STUDENT_ID_ATTRIBUTE, nominalRollStudentID.toString()));
  }

  public List<NominalRollStudentCount> countAllNominalRollStudents(final String processingYear) {
    return this.repository.getCountByProcessingYear(processingYear);
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

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void deleteNominalRollStudentValidationErrors(final String nominalRollStudentID) {
    this.nominalRollStudentValidationErrorRepository.deleteNominalRollStudentValidationErrors(UUID.fromString(nominalRollStudentID));
  }

  //To save NominalRollStudent with ValidationErrors, query and save operation should be in the same transaction boundary.
  public NominalRollStudentEntity saveNominalRollStudentValidationErrors(final String nominalRollStudentID, final Map<String, String> errors, NominalRollStudentEntity entity) {
    if(entity == null) {
      val nomRollStudOptional = this.findByNominalRollStudentID(nominalRollStudentID);
      if (nomRollStudOptional.isPresent()) {
        entity = nomRollStudOptional.get();
      }else{
        throw new NominalRollAPIRuntimeException("Error while saving NominalRollStudent with ValidationErrors - entity was null");
      }
    }
    entity.getNominalRollStudentValidationErrors().addAll(NominalRollHelper.populateValidationErrors(errors, entity));
    entity.setStatus(NominalRollStudentStatus.ERROR.toString());
    return this.repository.save(entity);
  }

  public List<NominalRollPostedStudentEntity> findAllBySurnameAndGivenNamesAndBirthDateAndGender(final String surname, final String givenNames, final LocalDate birthDate, final String gender) {
    return this.postedStudentRepository.findAllBySurnameAndGivenNamesAndBirthDateAndGenderOrderByCreateDateDesc(surname, givenNames, birthDate, gender);
  }

  public List<NominalRollIDs> findAllNominalRollStudentIDs(final String processingYear, final List<String> statusCodes, final Map<String, String> searchCriteria) {
    return this.nominalRollStudentRepositoryCustom.getAllNominalRollStudentIDs(processingYear, statusCodes, searchCriteria);
  }

  public List<NominalRollStudentEntity> findAllByProcessingYear(final String processingYear) {
    return this.repository.findAllByProcessingYear(processingYear);
  }

  @Transactional
  public void savePostedStudents(final List<NominalRollPostedStudentEntity> postedStudentEntities) {
    this.postedStudentRepository.saveAll(postedStudentEntities);
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void savePostedStudentsAndIgnoredStudents(final String processingYear, final String updateUser, final String correlationID) {
    final Pair<LocalDateTime, LocalDateTime> firstAndLastDays = NominalRollHelper.getFirstAndLastDateTimesOfYear(processingYear);

    if(!this.postedStudentRepository.existsByProcessingYearBetween(firstAndLastDays.getLeft(), firstAndLastDays.getRight())) {
      val students = this.findAllByProcessingYear(processingYear);
      final List<NominalRollStudentEntity> ignoredStudents = new ArrayList<>();
      final List<NominalRollPostedStudentEntity> studentsToBePosted = new ArrayList<>();
      int index = 0;
      for (val student : students) {
        if (StringUtils.isBlank(student.getAssignedPEN())) {
          student.setStatus(NominalRollStudentStatus.IGNORED.toString());
          student.setUpdateUser(updateUser);
          student.setUpdateDate(LocalDateTime.now());
          ignoredStudents.add(student);
        } else {
          val postedStudentEntity = NominalRollStudentMapper.mapper.toPostedEntity(student);
          postedStudentEntity.setCreateUser(updateUser);
          postedStudentEntity.setCreateDate(LocalDateTime.now());
          postedStudentEntity.setUpdateUser(updateUser);
          postedStudentEntity.setUpdateDate(LocalDateTime.now());
          postedStudentEntity.setRecordNumber(++index);
          studentsToBePosted.add(postedStudentEntity);
        }
      }
      if (!ignoredStudents.isEmpty()) {
        log.debug("updating ignored nominal roll entities in transient table for transaction ID :: {}", correlationID);
        this.repository.saveAll(ignoredStudents);
      }
      if (!studentsToBePosted.isEmpty()) {
        this.savePostedStudents(studentsToBePosted);
      }
    } else {
      log.info("NominalRollPostedStudentEntities of processingYear {} already exist :: {} ", processingYear, correlationID);
    }
  }

  public List<NominalRollPostedStudentEntity> findPostedStudentsByProcessingYear(final String processingYear) {
    final Pair<LocalDateTime, LocalDateTime> firstAndLastDays = NominalRollHelper.getFirstAndLastDateTimesOfYear(processingYear);
    return this.postedStudentRepository.findAllByProcessingYearBetween(firstAndLastDays.getLeft(), firstAndLastDays.getRight());
  }

  public boolean hasPostedStudents(final String processingYear) {
    final Pair<LocalDateTime, LocalDateTime> firstAndLastDays = NominalRollHelper.getFirstAndLastDateTimesOfYear(processingYear);
    return this.postedStudentRepository.existsByProcessingYearBetween(firstAndLastDays.getLeft(), firstAndLastDays.getRight());
  }

  public List<NominalRollStudentValidationErrorEntity> getSchoolNumberValidationErrors(){
    return this.nominalRollStudentValidationErrorRepository.findAllByFieldName("School Number");
  }

  public void removeClosedSchoolsFedProvMappings() {
    val schools = restUtils.getSchools();
    val schoolCodes = this.fedProvCodeRepository.findAll();
    Set<String> closedSchools = new HashSet<>();
    for (val school: schools) {
      if (StringUtils.isNotBlank(school.getClosedDate()) && futureClosedDate(school.getClosedDate())) {
        closedSchools.add(school.getSchoolId());
      }
    }
    for (FedProvCodeEntity code : schoolCodes) {
      if (closedSchools.contains(code.getSchoolID())){
        fedProvCodeRepository.deleteAllBySchoolID(code.getSchoolID());
      }
    }
  }
  private boolean futureClosedDate(String closedDate) {
    try {
      DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
      LocalDate closed = LocalDate.parse(closedDate, formatter);
      if (closed.isBefore(LocalDate.now())) {
        return true;
      }
    } catch (DateTimeParseException e) {
      //Do nothing here
    }
    return false;
  }

  @Cacheable(CacheNames.FED_PROV_CODES)
  public Map<String, String> getFedProvSchoolCodes() {
    if (this.schoolCodeMap.isEmpty()) {
      List<FedProvCodeEntity> schoolCodes = fedProvCodeRepository.findAll();
      schoolCodeMap = schoolCodes.stream()
              .collect(Collectors.toMap(
                      FedProvCodeEntity::getFedBandCode,
                      entity -> restUtils.getSchoolBySchoolID(entity.getSchoolID().toString()).get().getMincode()
              ));
    }
    return schoolCodeMap;
  }

  public String getMincodeByFedBandCode(String fedBandCode) {
    Map<String, String> schoolCodeMap = getFedProvSchoolCodes();
    return schoolCodeMap.getOrDefault(fedBandCode, null);
  }
  @Transactional
  public void addFedProvSchoolCode(FedProvSchoolCode fedProvSchoolCode) {
    FedProvCodeEntity fedCodeEntity = new FedProvCodeEntity() ;
    fedCodeEntity.setFedBandCode(fedProvSchoolCode.getFederalCode());
    Optional<SchoolTombstone> currSchoolTombstone = restUtils.getSchoolByMincode(fedProvSchoolCode.getProvincialCode());
    SchoolTombstone currentSchool = currSchoolTombstone.orElseThrow(() ->
            new EntityNotFoundException(SchoolTombstone.class, "SchoolTombstone", fedProvSchoolCode.getProvincialCode()));
    fedCodeEntity.setCreateUser(fedProvSchoolCode.createUser);
    fedCodeEntity.setCreateDate(LocalDateTime.now());
    fedCodeEntity.setSchoolID(UUID.fromString(currentSchool.getSchoolId()));
    fedCodeEntity.setUpdateUser(fedProvSchoolCode.updateUser);
    fedCodeEntity.setUpdateDate(LocalDateTime.now());
    fedProvCodeRepository.save(fedCodeEntity);
  }
}
