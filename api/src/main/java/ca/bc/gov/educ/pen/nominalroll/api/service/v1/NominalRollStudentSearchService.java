package ca.bc.gov.educ.pen.nominalroll.api.service.v1;

import ca.bc.gov.educ.pen.nominalroll.api.exception.InvalidParameterException;
import ca.bc.gov.educ.pen.nominalroll.api.exception.NominalRollAPIRuntimeException;
import ca.bc.gov.educ.pen.nominalroll.api.filter.FilterOperation;
import ca.bc.gov.educ.pen.nominalroll.api.filter.NominalRollStudentFilterSpecs;
import ca.bc.gov.educ.pen.nominalroll.api.model.v1.NominalRollStudentEntity;
import ca.bc.gov.educ.pen.nominalroll.api.struct.v1.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * The type Student search service.
 */
@Service
public class NominalRollStudentSearchService {
  private final NominalRollStudentFilterSpecs nominalRollStudentFilterSpecs;

  /**
   * Instantiates a new Student search service.
   *
   * @param nominalRollStudentFilterSpecs the student filter specs
   */
  public NominalRollStudentSearchService(NominalRollStudentFilterSpecs nominalRollStudentFilterSpecs) {
    this.nominalRollStudentFilterSpecs = nominalRollStudentFilterSpecs;
  }

  /**
   * Gets specifications.
   *
   * @param studentSpecs the pen reg batch specs
   * @param i            the
   * @param search       the search
   * @return the specifications
   */
  public Specification<NominalRollStudentEntity> getSpecifications(Specification<NominalRollStudentEntity> studentSpecs, int i, Search search) {
    if (i == 0) {
      studentSpecs = getStudentEntitySpecification(search.getSearchCriteriaList());
    } else {
      if (search.getCondition() == Condition.AND) {
        studentSpecs = studentSpecs.and(getStudentEntitySpecification(search.getSearchCriteriaList()));
      } else {
        studentSpecs = studentSpecs.or(getStudentEntitySpecification(search.getSearchCriteriaList()));
      }
    }
    return studentSpecs;
  }

  private Specification<NominalRollStudentEntity> getStudentEntitySpecification(List<SearchCriteria> criteriaList) {
    Specification<NominalRollStudentEntity> studentSpecs = null;
    if (!criteriaList.isEmpty()) {
      int i = 0;
      for (SearchCriteria criteria : criteriaList) {
        if (criteria.getKey() != null && criteria.getOperation() != null && criteria.getValueType() != null) {
          var criteriaValue = criteria.getValue();
          if(StringUtils.isNotBlank(criteria.getValue())) {
            criteriaValue = criteriaValue.toUpperCase();
          }
          Specification<NominalRollStudentEntity> typeSpecification = getTypeSpecification(criteria.getKey(), criteria.getOperation(), criteriaValue, criteria.getValueType());
          studentSpecs = getSpecificationPerGroup(studentSpecs, i, criteria, typeSpecification);
          i++;
        } else {
          throw new InvalidParameterException("Search Criteria can not contain null values for key, value and operation type");
        }
      }
    }
    return studentSpecs;
  }

  /**
   * Gets specification per group.
   *
   * @param studentEntitySpecification the pen request batch entity specification
   * @param i                          the
   * @param criteria                   the criteria
   * @param typeSpecification          the type specification
   * @return the specification per group
   */
  private Specification<NominalRollStudentEntity> getSpecificationPerGroup(Specification<NominalRollStudentEntity> studentEntitySpecification, int i, SearchCriteria criteria, Specification<NominalRollStudentEntity> typeSpecification) {
    if (i == 0) {
      studentEntitySpecification = Specification.where(typeSpecification);
    } else {
      if (criteria.getCondition() == Condition.AND) {
        studentEntitySpecification = studentEntitySpecification.and(typeSpecification);
      } else {
        studentEntitySpecification = studentEntitySpecification.or(typeSpecification);
      }
    }
    return studentEntitySpecification;
  }

  private Specification<NominalRollStudentEntity> getTypeSpecification(String key, FilterOperation filterOperation, String value, ValueType valueType) {
    Specification<NominalRollStudentEntity> studentEntitySpecification = null;
    switch (valueType) {
      case STRING:
        studentEntitySpecification = nominalRollStudentFilterSpecs.getStringTypeSpecification(key, value, filterOperation);
        break;
      case DATE_TIME:
        studentEntitySpecification = nominalRollStudentFilterSpecs.getDateTimeTypeSpecification(key, value, filterOperation);
        break;
      case LONG:
        studentEntitySpecification = nominalRollStudentFilterSpecs.getLongTypeSpecification(key, value, filterOperation);
        break;
      case INTEGER:
        studentEntitySpecification = nominalRollStudentFilterSpecs.getIntegerTypeSpecification(key, value, filterOperation);
        break;
      case DATE:
        studentEntitySpecification = nominalRollStudentFilterSpecs.getDateTypeSpecification(key, value, filterOperation);
        break;
      case UUID:
        studentEntitySpecification = nominalRollStudentFilterSpecs.getUUIDTypeSpecification(key, value, filterOperation);
        break;
      default:
        break;
    }
    return studentEntitySpecification;
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
  public Specification<NominalRollStudentEntity> setSpecificationAndSortCriteria(String sortCriteriaJson, String searchCriteriaListJson, ObjectMapper objectMapper, List<Sort.Order> sorts) {
    Specification<NominalRollStudentEntity> studentSpecs = null;
    try {
      RequestUtil.getSortCriteria(sortCriteriaJson, objectMapper, sorts);
      if (StringUtils.isNotBlank(searchCriteriaListJson)) {
        List<Search> searches = objectMapper.readValue(searchCriteriaListJson, new TypeReference<>() {
        });
        int i = 0;
        for (var search : searches) {
          studentSpecs = getSpecifications(studentSpecs, i, search);
          i++;
        }
      }
    } catch (JsonProcessingException e) {
      throw new NominalRollAPIRuntimeException(e.getMessage());
    }
    return studentSpecs;
  }
}
