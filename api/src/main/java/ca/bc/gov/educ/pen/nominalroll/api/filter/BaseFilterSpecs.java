package ca.bc.gov.educ.pen.nominalroll.api.filter;

import org.springframework.data.jpa.domain.Specification;
import java.time.chrono.ChronoLocalDate;
import java.time.chrono.ChronoLocalDateTime;
import java.util.UUID;
import java.util.function.Function;

/**
 * this is the generic class to support all kind of filter specifications for different entities
 *
 * @param <R> the entity type.
 * @author Om
 */
public abstract class BaseFilterSpecs<R> {

  private final ca.bc.gov.educ.pen.nominalroll.api.filter.FilterSpecifications<R, ChronoLocalDate> dateFilterSpecifications;
  private final ca.bc.gov.educ.pen.nominalroll.api.filter.FilterSpecifications<R, ChronoLocalDateTime<?>> dateTimeFilterSpecifications;
  private final ca.bc.gov.educ.pen.nominalroll.api.filter.FilterSpecifications<R, Integer> integerFilterSpecifications;
  private final ca.bc.gov.educ.pen.nominalroll.api.filter.FilterSpecifications<R, String> stringFilterSpecifications;
  private final ca.bc.gov.educ.pen.nominalroll.api.filter.FilterSpecifications<R, Long> longFilterSpecifications;
  private final ca.bc.gov.educ.pen.nominalroll.api.filter.FilterSpecifications<R, UUID> uuidFilterSpecifications;
  private final ca.bc.gov.educ.pen.nominalroll.api.filter.Converters converters;

  /**
   * Instantiates a new Base filter specs.
   *
   * @param dateFilterSpecifications     the date filter specifications
   * @param dateTimeFilterSpecifications the date time filter specifications
   * @param integerFilterSpecifications  the integer filter specifications
   * @param stringFilterSpecifications   the string filter specifications
   * @param longFilterSpecifications     the long filter specifications
   * @param uuidFilterSpecifications     the uuid filter specifications
   * @param converters                   the converters
   */
  protected BaseFilterSpecs(ca.bc.gov.educ.pen.nominalroll.api.filter.FilterSpecifications<R, ChronoLocalDate> dateFilterSpecifications, ca.bc.gov.educ.pen.nominalroll.api.filter.FilterSpecifications<R, ChronoLocalDateTime<?>> dateTimeFilterSpecifications, ca.bc.gov.educ.pen.nominalroll.api.filter.FilterSpecifications<R, Integer> integerFilterSpecifications, ca.bc.gov.educ.pen.nominalroll.api.filter.FilterSpecifications<R, String> stringFilterSpecifications, ca.bc.gov.educ.pen.nominalroll.api.filter.FilterSpecifications<R, Long> longFilterSpecifications, ca.bc.gov.educ.pen.nominalroll.api.filter.FilterSpecifications<R, UUID> uuidFilterSpecifications, ca.bc.gov.educ.pen.nominalroll.api.filter.Converters converters) {
    this.dateFilterSpecifications = dateFilterSpecifications;
    this.dateTimeFilterSpecifications = dateTimeFilterSpecifications;
    this.integerFilterSpecifications = integerFilterSpecifications;
    this.stringFilterSpecifications = stringFilterSpecifications;
    this.longFilterSpecifications = longFilterSpecifications;
    this.uuidFilterSpecifications = uuidFilterSpecifications;
    this.converters = converters;
  }

  /**
   * Gets date type specification.
   *
   * @param fieldName       the field name
   * @param filterValue     the filter value
   * @param filterOperation the filter operation
   * @return the date type specification
   */
  public Specification<R> getDateTypeSpecification(String fieldName, String filterValue, ca.bc.gov.educ.pen.nominalroll.api.filter.FilterOperation filterOperation) {
    return getSpecification(fieldName, filterValue, filterOperation, converters.getFunction(ChronoLocalDate.class), dateFilterSpecifications);
  }

  /**
   * Gets date time type specification.
   *
   * @param fieldName       the field name
   * @param filterValue     the filter value
   * @param filterOperation the filter operation
   * @return the date time type specification
   */
  public Specification<R> getDateTimeTypeSpecification(String fieldName, String filterValue, ca.bc.gov.educ.pen.nominalroll.api.filter.FilterOperation filterOperation) {
    return getSpecification(fieldName, filterValue, filterOperation, converters.getFunction(ChronoLocalDateTime.class), dateTimeFilterSpecifications);
  }

  /**
   * Gets integer type specification.
   *
   * @param fieldName       the field name
   * @param filterValue     the filter value
   * @param filterOperation the filter operation
   * @return the integer type specification
   */
  public Specification<R> getIntegerTypeSpecification(String fieldName, String filterValue, ca.bc.gov.educ.pen.nominalroll.api.filter.FilterOperation filterOperation) {
    return getSpecification(fieldName, filterValue, filterOperation, converters.getFunction(Integer.class), integerFilterSpecifications);
  }

  /**
   * Gets long type specification.
   *
   * @param fieldName       the field name
   * @param filterValue     the filter value
   * @param filterOperation the filter operation
   * @return the long type specification
   */
  public Specification<R> getLongTypeSpecification(String fieldName, String filterValue, ca.bc.gov.educ.pen.nominalroll.api.filter.FilterOperation filterOperation) {
    return getSpecification(fieldName, filterValue, filterOperation, converters.getFunction(Long.class), longFilterSpecifications);
  }

  /**
   * Gets string type specification.
   *
   * @param fieldName       the field name
   * @param filterValue     the filter value
   * @param filterOperation the filter operation
   * @return the string type specification
   */
  public Specification<R> getStringTypeSpecification(String fieldName, String filterValue, ca.bc.gov.educ.pen.nominalroll.api.filter.FilterOperation filterOperation) {
    return getSpecification(fieldName, filterValue, filterOperation, converters.getFunction(String.class), stringFilterSpecifications);
  }

  /**
   * Gets uuid type specification.
   *
   * @param fieldName       the field name
   * @param filterValue     the filter value
   * @param filterOperation the filter operation
   * @return the uuid type specification
   */
  public Specification<R> getUUIDTypeSpecification(String fieldName, String filterValue, FilterOperation filterOperation) {
    return getSpecification(fieldName, filterValue, filterOperation, converters.getFunction(UUID.class), uuidFilterSpecifications);
  }

  private <T extends Comparable<T>> Specification<R> getSpecification(String fieldName,
                                                                      String filterValue,
                                                                      FilterOperation filterOperation,
                                                                      Function<String, T> converter,
                                                                      FilterSpecifications<R, T> specifications) {
    FilterCriteria<T> criteria = new FilterCriteria<>(fieldName, filterValue, filterOperation, converter);
    return specifications.getSpecification(criteria.getOperation()).apply(criteria);
  }
}
