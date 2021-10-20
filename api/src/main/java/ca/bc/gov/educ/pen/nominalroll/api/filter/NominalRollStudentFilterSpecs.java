package ca.bc.gov.educ.pen.nominalroll.api.filter;

import ca.bc.gov.educ.pen.nominalroll.api.model.v1.NominalRollStudentEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.chrono.ChronoLocalDate;
import java.time.chrono.ChronoLocalDateTime;
import java.util.UUID;

/**
 * The type Student filter specs.
 */
@Service
@Slf4j
public class NominalRollStudentFilterSpecs extends BaseFilterSpecs<NominalRollStudentEntity> {

  /**
   * Instantiates a new Student filter specs.
   *
   * @param dateFilterSpecifications     the date filter specifications
   * @param dateTimeFilterSpecifications the date time filter specifications
   * @param integerFilterSpecifications  the integer filter specifications
   * @param stringFilterSpecifications   the string filter specifications
   * @param longFilterSpecifications     the long filter specifications
   * @param uuidFilterSpecifications     the uuid filter specifications
   * @param converters                   the converters
   */
  public NominalRollStudentFilterSpecs(FilterSpecifications<NominalRollStudentEntity, ChronoLocalDate> dateFilterSpecifications, FilterSpecifications<NominalRollStudentEntity, ChronoLocalDateTime<?>> dateTimeFilterSpecifications, FilterSpecifications<NominalRollStudentEntity, Integer> integerFilterSpecifications, FilterSpecifications<NominalRollStudentEntity, String> stringFilterSpecifications, FilterSpecifications<NominalRollStudentEntity, Long> longFilterSpecifications, FilterSpecifications<NominalRollStudentEntity, UUID> uuidFilterSpecifications, Converters converters) {
    super(dateFilterSpecifications, dateTimeFilterSpecifications, integerFilterSpecifications, stringFilterSpecifications, longFilterSpecifications, uuidFilterSpecifications, converters);
  }
}
