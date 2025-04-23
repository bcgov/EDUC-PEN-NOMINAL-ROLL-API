package ca.bc.gov.educ.pen.nominalroll.api.rest;

import ca.bc.gov.educ.pen.nominalroll.api.constants.CacheNames;
import ca.bc.gov.educ.pen.nominalroll.api.mappers.LocalDateTimeMapper;
import ca.bc.gov.educ.pen.nominalroll.api.model.v1.PaginatedResponse;
import ca.bc.gov.educ.pen.nominalroll.api.properties.ApplicationProperties;
import ca.bc.gov.educ.pen.nominalroll.api.struct.external.sdc.v1.Collection;
import ca.bc.gov.educ.pen.nominalroll.api.struct.external.sdc.v1.SdcSchoolCollectionStudent;
import ca.bc.gov.educ.pen.nominalroll.api.struct.external.student.v1.GenderCode;
import ca.bc.gov.educ.pen.nominalroll.api.struct.external.student.v1.GradeCode;
import ca.bc.gov.educ.pen.nominalroll.api.struct.v1.District;
import ca.bc.gov.educ.pen.nominalroll.api.struct.v1.IndependentAuthority;
import ca.bc.gov.educ.pen.nominalroll.api.struct.v1.SchoolTombstone;
import ca.bc.gov.educ.pen.nominalroll.api.util.SearchCriteriaBuilder;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

  private final ReadWriteLock authorityLock = new ReentrantReadWriteLock();
  private final Map<String, SchoolTombstone> schoolMap = new ConcurrentHashMap<>();
  private final Map<String, District> districtMap = new ConcurrentHashMap<>();

  private final Map<String, IndependentAuthority> authorityMap = new ConcurrentHashMap<>();
  private final Map<String, SchoolTombstone> schoolMincodeMap = new ConcurrentHashMap<>();
  private final ApplicationProperties props;

  @Autowired
  private final ObjectMapper objectMapper;
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

  public RestUtils( @Autowired final ApplicationProperties props, final WebClient webClient, ObjectMapper objectMapper) {
    this.objectMapper=objectMapper;
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
    this.populateAuthorityMap();

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

  public void populateAuthorityMap() {
    val writeLock = this.authorityLock.writeLock();
    try {
      writeLock.lock();
      for (val authority : this.getAuthorities()) {
        this.authorityMap.put(authority.getIndependentAuthorityId(), authority);
      }
    } catch (Exception ex) {
      log.error("Unable to load map cache authorities {}", ex);
    } finally {
      writeLock.unlock();
    }
    log.info("Loaded  {} authorities to memory", this.authorityMap.values().size());
  }

  public Optional<SchoolTombstone> getSchoolBySchoolID(final String schoolId) {
    if (this.schoolMap.isEmpty()) {
      log.info("School map is empty reloading schools");
      this.populateSchoolMap();
    }
    return Optional.ofNullable(this.schoolMap.get(schoolId));
  }

  @Retryable(value = {Exception.class}, backoff = @Backoff(multiplier = 2, delay = 2000))
  @Cacheable(CacheNames.GENDER_CODES)
  public List<GenderCode> getActiveGenderCodes() {
    return Objects.requireNonNull(this.webClient.get()
      .uri(this.props.getStudentApiURL().concat("/gender-codes"))
      .header(CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
      .retrieve().bodyToFlux(GenderCode.class).buffer().blockLast()).stream().filter(this::filterGenderCodes).collect(Collectors.toList());
  }

  @CacheEvict(value = CacheNames.FED_PROV_CODES, allEntries = true)
  public void evictFedProvSchoolCodesCache() {}
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

  public List<IndependentAuthority> getAuthorities() {
    log.info("Calling Institute api to load authority to memory");
    return this.webClient.get()
            .uri(this.props.getInstituteApiURL() + "/authority")
            .header(CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .retrieve()
            .bodyToFlux(IndependentAuthority.class)
            .collectList()
            .block();
  }

  public Optional<IndependentAuthority> getAuthorityByAuthorityID(final String authorityID) {
    if (this.authorityMap.isEmpty()) {
      log.info("Authority map is empty reloading authorities");
      this.populateAuthorityMap();
    }
    return Optional.ofNullable(this.authorityMap.get(authorityID));
  }

  public Optional<District> getDistrictByDistrictID(final String districtID) {
    if (this.districtMap.isEmpty()) {
      log.info("District map is empty reloading schools");
      this.populateDistrictMap();
    }
    return Optional.ofNullable(this.districtMap.get(districtID));
  }

  public PaginatedResponse<Collection> getCollections(String processingYear) throws JsonProcessingException {
    List<Map<String, Object>> searchCriteriaList = SearchCriteriaBuilder.septemberCollectionsFromLastYear(processingYear);
    String searchJson = objectMapper.writeValueAsString(searchCriteriaList);
    String encodedSearchJson = URLEncoder.encode(searchJson, StandardCharsets.UTF_8);

    int pageNumber = 0;
    int pageSize = 50;

    try {
      String fullUrl = this.props.getSdcApiURL()
              + "/collection/paginated"
              + "?pageNumber=" + pageNumber
              + "&pageSize=" + pageSize
              + "&searchCriteriaList=" + encodedSearchJson;
      return webClient.get()
              .uri(fullUrl)
              .header(CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
              .retrieve()
              .bodyToMono(new ParameterizedTypeReference<PaginatedResponse<Collection>>() {
              })
              .block();
    } catch (Exception ex) {
      log.error("Error fetching schools on page {}", pageNumber, ex);
      return null;
    }
  }

  public List<SdcSchoolCollectionStudent> get1701DataForStudents(String collectionID, List<String> studentPens) throws JsonProcessingException {
    int maxPensPerBatch = 1500;
    int pageSize = 1500;

    ExecutorService executor = Executors.newFixedThreadPool(8); // Adjust thread pool size as needed
    List<CompletableFuture<List<SdcSchoolCollectionStudent>>> futures = new ArrayList<>();

    for (int i = 0; i < studentPens.size(); i += maxPensPerBatch) {
      int start = i;
      int end = Math.min(i + maxPensPerBatch, studentPens.size());
      List<String> batchPens = new ArrayList<>(studentPens.subList(start, end));

      CompletableFuture<List<SdcSchoolCollectionStudent>> future = CompletableFuture.supplyAsync(() -> {
        try {
          List<Map<String, Object>> searchCriteriaList = SearchCriteriaBuilder.byCollectionIdAndStudentPens(collectionID, batchPens);
          return fetchStudentsForBatch(pageSize, searchCriteriaList);
        } catch (Exception e) {
          log.error("Batch fetch failed", e);
          return Collections.emptyList();
        }
      }, executor);

      futures.add(future);
    }

    List<SdcSchoolCollectionStudent> allStudents = futures.stream()
            .map(CompletableFuture::join)
            .flatMap(List::stream)
            .collect(Collectors.toList());

    executor.shutdown();
    return allStudents;
  }

  private List<SdcSchoolCollectionStudent> fetchStudentsForBatch(int pageSize, List<Map<String, Object>> searchCriteriaList) throws JsonProcessingException {
    List<SdcSchoolCollectionStudent> students = new ArrayList<>();
    String searchJson = objectMapper.writeValueAsString(searchCriteriaList);
    String encodedSearchJson = URLEncoder.encode(searchJson, StandardCharsets.UTF_8);

    int pageNumber = 0;
    boolean hasNextPage = true;

    while (hasNextPage) {
      try {
        String fullUrl = this.props.getSdcApiURL()
                + "/sdcSchoolCollectionStudent/paginated"
                + "?pageNumber=" + pageNumber
                + "&pageSize=" + pageSize
                + "&sort=" // optional: add sort json or keep empty
                + "&searchCriteriaList=" + encodedSearchJson;

        PaginatedResponse<SdcSchoolCollectionStudent> response = webClient.get()
                .uri(fullUrl)
                .header(CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<PaginatedResponse<SdcSchoolCollectionStudent>>() {
                })
                .block();

        if (response != null && response.getContent() != null) {
          students.addAll(response.getContent());
          hasNextPage = response.getNumber() < response.getTotalPages() - 1;
          pageNumber++;
        } else {
          hasNextPage = false;
        }
      } catch (Exception ex) {
        log.error("Error fetching 1701 data for page {} of batch starting at PEN {}", pageNumber, ex);
        break;
      }
    }

    return students;
  }


  @Cacheable(CacheNames.DISTRICT_CODES)
  public List<String> districtCodes() {
    return this.getDistricts().stream().map(District::getDistrictNumber).filter(Objects::nonNull).collect(Collectors.toList());
  }

  public List<SdcSchoolCollectionStudent> get1701DataForStudentsWithFundingCode20(String collectionID) {
    try {
      List<Map<String, Object>> searchCriteriaFunding = SearchCriteriaBuilder.byCollectionIdAndFundingCode(collectionID);
      return fetchStudentsForBatch(15000, searchCriteriaFunding); // One big call (or paginate inside fetch)
    } catch (Exception e) {
      log.error("Fetching students with funding code 20 failed", e);
      return Collections.emptyList();
    }
  }

  public List<SdcSchoolCollectionStudent> getAll1701Students(String collectionID, List<String> studentPens) {
    ExecutorService executor = Executors.newFixedThreadPool(2); // Two threads: one for pens, one for fundingCode20

    CompletableFuture<List<SdcSchoolCollectionStudent>> fundingFuture = CompletableFuture.supplyAsync(() -> {
      long fundingStart = System.currentTimeMillis();
      try {
        return get1701DataForStudentsWithFundingCode20(collectionID);
      } finally {
        long fundingEnd = System.currentTimeMillis();
        System.out.println("fundingFuture took " + (fundingEnd - fundingStart) + " ms");
      }
    }, executor);

    CompletableFuture<List<SdcSchoolCollectionStudent>> pensFuture = CompletableFuture.supplyAsync(() -> {
      long penStart = System.currentTimeMillis();
      try {
        return get1701DataForStudents(collectionID, studentPens);
      } catch (JsonProcessingException e) {
        throw new RuntimeException(e);
      } finally {
        long penEnd = System.currentTimeMillis();
        System.out.println("pensFuture took " + (penEnd - penStart) + " ms");
      }
    }, executor);



    List<SdcSchoolCollectionStudent> combinedStudents = Stream.concat(
                    pensFuture.join().stream(),
                    fundingFuture.join().stream()
            )
            .collect(Collectors.collectingAndThen(
                    Collectors.toMap(
                            SdcSchoolCollectionStudent::getAssignedPen,   // Use PEN as unique key
                            student -> student,
                            (existing, duplicate) -> existing      // Keep the first one if duplicates
                    ),
                    map -> new ArrayList<>(map.values())
            ));

    executor.shutdown();
    return combinedStudents;
  }

}

