package ca.bc.gov.educ.pen.nominalroll.api.rest;

import ca.bc.gov.educ.pen.nominalroll.api.constants.CacheNames;
import ca.bc.gov.educ.pen.nominalroll.api.mappers.LocalDateTimeMapper;
import ca.bc.gov.educ.pen.nominalroll.api.model.v1.FedProvCodeEntity;
import ca.bc.gov.educ.pen.nominalroll.api.properties.ApplicationProperties;
import ca.bc.gov.educ.pen.nominalroll.api.repository.v1.FedProvCodeRepository;
import ca.bc.gov.educ.pen.nominalroll.api.struct.external.school.v1.FedProvSchoolCode;
import ca.bc.gov.educ.pen.nominalroll.api.struct.external.student.v1.GenderCode;
import ca.bc.gov.educ.pen.nominalroll.api.struct.external.student.v1.GradeCode;
import ca.bc.gov.educ.pen.nominalroll.api.struct.v1.District;
import ca.bc.gov.educ.pen.nominalroll.api.struct.v1.SchoolTombstone;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.MediaType;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.lang.module.FindException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

/**
 * The type Rest utils.
 */
@Component
@Slf4j
public class RestUtils {
  /**
   * The constant CONTENT_TYPE.
   */
  public static final String CONTENT_TYPE = "Content-Type";
  private final ReadWriteLock schoolLock = new ReentrantReadWriteLock();

  private final ReadWriteLock districtLock = new ReentrantReadWriteLock();

  private final Map<String, SchoolTombstone> schoolMap = new ConcurrentHashMap<>();

  private final Map<String, District> districtMap = new ConcurrentHashMap<>();

  private final Map<String, SchoolTombstone> schoolMincodeMap = new ConcurrentHashMap<>();

  private final FedProvCodeRepository fedProvCodeRepository;


  private final ApplicationProperties props;
  private final Map<String, List<UUID>> independentAuthorityToSchoolIDMap = new ConcurrentHashMap<>();

  @Value("${initialization.background.enabled}")
  private Boolean isBackgroundInitializationEnabled;
  /**
   * The Web client.
   */
  private final WebClient webClient;

  @PostConstruct
  public void init() {
    if (this.isBackgroundInitializationEnabled != null && this.isBackgroundInitializationEnabled) {
      ApplicationProperties.bgTask.execute(this::initialize);
    }
  }

  public RestUtils(FedProvCodeRepository fedProvCodeRepository, @Autowired final ApplicationProperties props, final WebClient webClient) {
    this.fedProvCodeRepository = fedProvCodeRepository;
    this.props = props;
    this.webClient = webClient;
  }

  @Scheduled(cron = "${schedule.jobs.load.school.cron}")
  public void scheduled() {
    this.init();
  }
  private void initialize() {
    this.populateSchoolMap();
    this.populateSchoolMincodeMap();
    this.populateDistrictMap();

  }

  public void populateSchoolMap() {
    val writeLock = this.schoolLock.writeLock();
    try {
      writeLock.lock();
      for (val school : this.getSchools()) {
        this.schoolMap.put(school.getSchoolId(), school);
        if (StringUtils.isNotBlank(school.getIndependentAuthorityId())) {
          this.independentAuthorityToSchoolIDMap.computeIfAbsent(school.getIndependentAuthorityId(), k -> new ArrayList<>()).add(UUID.fromString(school.getSchoolId()));
        }
      }
    } catch (Exception ex) {
      log.error("Unable to load map cache school {}", ex);
    } finally {
      writeLock.unlock();
    }
    log.info("Loaded  {} schools to memory", this.schoolMap.values().size());
  }

  public void populateSchoolMincodeMap() {
    val writeLock = this.schoolLock.writeLock();
    try {
      writeLock.lock();
      for (val school : this.getSchools()) {
        this.schoolMincodeMap.put(school.getMincode(), school);
        if (StringUtils.isNotBlank(school.getIndependentAuthorityId())) {
          this.independentAuthorityToSchoolIDMap.computeIfAbsent(school.getIndependentAuthorityId(), k -> new ArrayList<>()).add(UUID.fromString(school.getSchoolId()));
        }
      }
    } catch (Exception ex) {
      log.error("Unable to load map cache school mincodes {}", ex);
    } finally {
      writeLock.unlock();
    }
    log.info("Loaded  {} school mincodes to memory", this.schoolMincodeMap.values().size());
  }


  public Optional<SchoolTombstone> getSchoolByMincode(final String mincode) {
    if (this.schoolMincodeMap.isEmpty()) {
      log.info("School mincode map is empty reloading schools");
      this.populateSchoolMincodeMap();
    }
    return Optional.ofNullable(this.schoolMincodeMap.get(mincode));
  }


  public Map<String, String> getFedProvSchoolCodes() {
    List<FedProvCodeEntity> schoolCodes = fedProvCodeRepository.findAll();
    return schoolCodes.stream().collect(Collectors.toMap(FedProvCodeEntity::getFedBandCode, FedProvCodeEntity::getMincode));
  }

  public void populateDistrictMap() {
    val writeLock = this.districtLock.writeLock();
    try {
      writeLock.lock();
      for (val district : this.getDistricts()) {
        this.districtMap.put(district.getDistrictId(), district);
      }
    } catch (Exception ex) {
      log.error("Unable to load map cache district {}", ex);
    } finally {
      writeLock.unlock();
    }
    log.info("Loaded  {} districts to memory", this.districtMap.values().size());
  }

  public boolean validateFedBandCode(final String fedBandCode){
    val schoolCodes = this.fedProvCodeRepository.findAll();
    if(schoolCodes.contains(fedBandCode)){
      return true;
    }
    return false;
  }
  public Optional<SchoolTombstone> getSchoolBySchoolNumber(final String schoolNumber) {
    if (this.schoolMap.isEmpty()) {
      log.info("School map is empty reloading schools");
      this.populateSchoolMap();
    }
    return Optional.ofNullable(this.schoolMap.get(schoolNumber));
  }

  @Retryable(value = {Exception.class}, backoff = @Backoff(multiplier = 2, delay = 2000))
  @Cacheable(CacheNames.GENDER_CODES)
  public List<GenderCode> getActiveGenderCodes() {
    return Objects.requireNonNull(this.webClient.get()
      .uri(this.props.getStudentApiURL().concat("/gender-codes"))
      .header(CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
      .retrieve().bodyToFlux(GenderCode.class).buffer().blockLast()).stream().filter(this::filterGenderCodes).collect(Collectors.toList());
  }


  @Retryable(value = {Exception.class}, backoff = @Backoff(multiplier = 2, delay = 2000))
  @Cacheable(CacheNames.GRADE_CODES)
  public List<GradeCode> getActiveGradeCodes() {
    return Objects.requireNonNull(this.webClient.get()
      .uri(this.props.getStudentApiURL().concat("/grade-codes"))
      .header(CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
      .retrieve().bodyToFlux(GradeCode.class).buffer().blockLast()).stream().filter(this::filterGradeCodes).collect(Collectors.toList());
  }

  private boolean filterGradeCodes(final GradeCode gradeCode) {
    val mapper = new LocalDateTimeMapper();
    val curTime = LocalDateTime.now();
    val effDate = mapper.map(gradeCode.getEffectiveDate());
    val expDate = mapper.map(gradeCode.getExpiryDate());
    return curTime.isAfter(effDate) && curTime.isBefore(expDate);
  }

  private boolean filterGenderCodes(final GenderCode genderCode) {
    val mapper = new LocalDateTimeMapper();
    val curTime = LocalDateTime.now();
    val effDate = mapper.map(genderCode.getEffectiveDate());
    val expDate = mapper.map(genderCode.getExpiryDate());
    return curTime.isAfter(effDate) && curTime.isBefore(expDate);
  }


  public List<SchoolTombstone> getSchools() {
    log.info("Calling Institute api to load schools to memory");
    return this.webClient.get()
            .uri(this.props.getInstituteApiURL() + "/school")
            .header(CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .retrieve()
            .bodyToFlux(SchoolTombstone.class)
            .collectList()
            .block();
  }

  public List<District> getDistricts() {
    log.info("Calling Institute api to load districts to memory");
    return this.webClient.get()
            .uri(this.props.getInstituteApiURL() + "/district")
            .header(CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .retrieve()
            .bodyToFlux(District.class)
            .collectList()
            .block();
  }
  @Cacheable(CacheNames.DISTRICT_CODES)
  public List<String> districtCodes() {
    return this.getDistricts().stream().map(District::getDistrictNumber).filter(Objects::nonNull).collect(Collectors.toList());
  }
}
