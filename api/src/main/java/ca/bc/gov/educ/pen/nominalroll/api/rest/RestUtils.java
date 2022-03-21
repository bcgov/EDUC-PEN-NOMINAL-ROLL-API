package ca.bc.gov.educ.pen.nominalroll.api.rest;

import ca.bc.gov.educ.pen.nominalroll.api.constants.CacheNames;
import ca.bc.gov.educ.pen.nominalroll.api.exception.NominalRollAPIRuntimeException;
import ca.bc.gov.educ.pen.nominalroll.api.mappers.LocalDateTimeMapper;
import ca.bc.gov.educ.pen.nominalroll.api.properties.ApplicationProperties;
import ca.bc.gov.educ.pen.nominalroll.api.struct.external.school.v1.FedProvSchoolCode;
import ca.bc.gov.educ.pen.nominalroll.api.struct.external.school.v1.School;
import ca.bc.gov.educ.pen.nominalroll.api.struct.external.student.v1.GenderCode;
import ca.bc.gov.educ.pen.nominalroll.api.struct.external.student.v1.GradeCode;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
  private final ApplicationProperties props;

  /**
   * The Web client.
   */
  private final WebClient webClient;

  /**
   * Instantiates a new Rest utils.
   *
   * @param props     the props
   * @param webClient the web client
   */
  public RestUtils(@Autowired final ApplicationProperties props, final WebClient webClient) {
    this.props = props;
    this.webClient = webClient;
  }


  @Retryable(value = {Exception.class}, backoff = @Backoff(multiplier = 2, delay = 2000))
  @Cacheable(CacheNames.FED_PROV_CODES)
  public Map<String, String> getFedProvSchoolCodes() {
    return Objects.requireNonNull(this.webClient.get()
      .uri(this.props.getSchoolApiURL().concat("/federal-province-codes"))
      .header(CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
      .retrieve().bodyToFlux(FedProvSchoolCode.class).buffer().blockLast()).stream().collect(Collectors.toMap(FedProvSchoolCode::getFederalCode, FedProvSchoolCode::getProvincialCode));
  }

  public void addFedProvSchoolCode(@NonNull final FedProvSchoolCode fedProvSchoolCode) {
    try {
      val response = this.webClient.post()
        .uri(this.props.getSchoolApiURL().concat("/federal-province-codes"))
        .header(CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        .body(Mono.just(fedProvSchoolCode), FedProvSchoolCode.class)
        .retrieve()
        .bodyToMono(FedProvSchoolCode.class)
        .block();
      if (response == null) {
        throw new NominalRollAPIRuntimeException("Error occurred while adding FedProvSchoolCode");
      }
    } catch (final WebClientResponseException e) {
      throw new NominalRollAPIRuntimeException("Error occurred while adding FedProvSchoolCode");
    }
  }

  @CacheEvict(value = CacheNames.FED_PROV_CODES, allEntries = true)
  public void evictFedProvSchoolCodesCache() {}

  @Retryable(value = {Exception.class}, backoff = @Backoff(multiplier = 2, delay = 2000))
  @Cacheable(CacheNames.GENDER_CODES)
  public List<GenderCode> getActiveGenderCodes() {
    return Objects.requireNonNull(this.webClient.get()
      .uri(this.props.getStudentApiURL().concat("/gender-codes"))
      .header(CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
      .retrieve().bodyToFlux(GenderCode.class).buffer().blockLast()).stream().filter(this::filterGenderCodes).collect(Collectors.toList());
  }

  @Retryable(value = {Exception.class}, backoff = @Backoff(multiplier = 2, delay = 2000))
  public void deleteFedProvCode(@NonNull final FedProvSchoolCode fedProvSchoolCode) {
    try {
    val response = webClient
              .method(HttpMethod.DELETE)
              .uri(this.props.getSchoolApiURL().concat("/federal-province-codes"))
              .body(BodyInserters.fromProducer(Mono.just(fedProvSchoolCode), FedProvSchoolCode.class))
              .retrieve()
              .toBodilessEntity().block();
    if (response == null) {
      throw new NominalRollAPIRuntimeException("Error occurred while deleting FedProvSchoolCode");
    }
  } catch (final WebClientResponseException e) {
    throw new NominalRollAPIRuntimeException("Error occurred while deleting FedProvSchoolCode");
  }
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


  @Retryable(value = {Exception.class}, backoff = @Backoff(multiplier = 2, delay = 2000))
  @Cacheable(CacheNames.SCHOOL_CODES)
  public List<School> getSchools() {
    return this.webClient.get()
      .uri(this.props.getSchoolApiURL())
      .header(CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
      .retrieve()
      .bodyToFlux(School.class)
      .collectList()
      .block();
  }

  @Cacheable(CacheNames.DISTRICT_CODES)
  public List<String> districtCodes() {
    return this.getSchools().stream().map(School::getDistNo).filter(Objects::nonNull).collect(Collectors.toList());
  }
}
