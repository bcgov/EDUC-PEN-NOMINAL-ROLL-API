package ca.bc.gov.educ.pen.nominalroll.api.service.v1;

import ca.bc.gov.educ.pen.nominalroll.api.exception.InvalidParameterException;
import ca.bc.gov.educ.pen.nominalroll.api.exception.NominalRollAPIRuntimeException;
import ca.bc.gov.educ.pen.nominalroll.api.filter.BaseFilterSpecs;
import ca.bc.gov.educ.pen.nominalroll.api.filter.FilterOperation;
import ca.bc.gov.educ.pen.nominalroll.api.struct.v1.*;
import ca.bc.gov.educ.pen.nominalroll.api.util.TransformUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

/**
 * The type search service.
 */
public abstract class SearchService<T> {
  private final BaseFilterSpecs<T> filterSpecs;
  private final Class<T> clazz;

  /**
   * Instantiates a new search service.
   *
   * @param filterSpecs the filter specs
   */
  protected SearchService(BaseFilterSpecs<T> filterSpecs, Class<T> clazz) {
    this.filterSpecs = filterSpecs;
    this.clazz = clazz;
  }

  /**
   * Gets specifications.
   *
   * @param specs the specs
   * @param i            the index
   * @param search       the search
   * @return the specifications
   */
  public Specification<T> getSpecifications(Specification<T> specs, int i, Search search) {
    if (i == 0) {
      specs = getEntitySpecification(search.getSearchCriteriaList());
    } else {
      if (search.getCondition() == Condition.AND) {
        specs = specs.and(getEntitySpecification(search.getSearchCriteriaList()));
      } else {
        specs = specs.or(getEntitySpecification(search.getSearchCriteriaList()));
      }
    }
    return specs;
  }

  private Specification<T> getEntitySpecification(List<SearchCriteria> criteriaList) {
    Specification<T> entitySpecs = null;
    if (!criteriaList.isEmpty()) {
      int i = 0;
      for (SearchCriteria criteria : criteriaList) {
        if (criteria.getKey() != null && criteria.getOperation() != null && criteria.getValueType() != null) {
          var criteriaValue = criteria.getValue();
          if(StringUtils.isNotBlank(criteria.getValue()) && TransformUtil.isUppercaseField(clazz, criteria.getKey())) {
            criteriaValue = criteriaValue.toUpperCase();
          }
          Specification<T> typeSpecification = getTypeSpecification(criteria.getKey(), criteria.getOperation(), criteriaValue, criteria.getValueType());
          entitySpecs = getSpecificationPerGroup(entitySpecs, i, criteria, typeSpecification);
          i++;
        } else {
          throw new InvalidParameterException("Search Criteria can not contain null values for key, value and operation type");
        }
      }
    }
    return entitySpecs;
  }

  /**
   * Gets specification per group.
   *
   * @param entitySpecification the entity specification
   * @param i                          the index
   * @param criteria                   the criteria
   * @param typeSpecification          the type specification
   * @return the specification per group
   */
  private Specification<T> getSpecificationPerGroup(Specification<T> entitySpecification, int i, SearchCriteria criteria, Specification<T> typeSpecification) {
    if (i == 0) {
      entitySpecification = Specification.where(typeSpecification);
    } else {
      if (criteria.getCondition() == Condition.AND) {
        entitySpecification = entitySpecification.and(typeSpecification);
      } else {
        entitySpecification = entitySpecification.or(typeSpecification);
      }
    }
    return entitySpecification;
  }

  private Specification<T> getTypeSpecification(String key, FilterOperation filterOperation, String value, ValueType valueType) {
    Specification<T> typeSpecification = null;
    switch (valueType) {
      case STRING:
        typeSpecification = filterSpecs.getStringTypeSpecification(key, value, filterOperation);
        break;
      case DATE_TIME:
        typeSpecification = filterSpecs.getDateTimeTypeSpecification(key, value, filterOperation);
        break;
      case LONG:
        typeSpecification = filterSpecs.getLongTypeSpecification(key, value, filterOperation);
        break;
      case INTEGER:
        typeSpecification = filterSpecs.getIntegerTypeSpecification(key, value, filterOperation);
        break;
      case DATE:
        typeSpecification = filterSpecs.getDateTypeSpecification(key, value, filterOperation);
        break;
      case UUID:
        typeSpecification = filterSpecs.getUUIDTypeSpecification(key, value, filterOperation);
        break;
      default:
        break;
    }
    return typeSpecification;
  }

  /**
   * Sets specification and sort criteria.
   *
   * @param sortCriteriaJson       the sort criteria json
   * @param searchCriteriaListJson the search criteria list json
   * @param objectMapper           the object mapper
   * @param sorts                  the sorts
   * @return the specification and sort criteria
   */
  public Specification<T> setSpecificationAndSortCriteria(String sortCriteriaJson, String searchCriteriaListJson, ObjectMapper objectMapper, List<Sort.Order> sorts) {
    Specification<T> specs = null;
    try {
      RequestUtil.getSortCriteria(sortCriteriaJson, objectMapper, sorts);
      if (StringUtils.isNotBlank(searchCriteriaListJson)) {
        List<Search> searches = objectMapper.readValue(searchCriteriaListJson, new TypeReference<>() {
        });
        int i = 0;
        for (var search : searches) {
          specs = getSpecifications(specs, i, search);
          i++;
        }
      }
    } catch (JsonProcessingException e) {
      throw new NominalRollAPIRuntimeException(e.getMessage());
    }
    return specs;
  }
}
