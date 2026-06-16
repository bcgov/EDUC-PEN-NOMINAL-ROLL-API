package ca.bc.gov.educ.pen.nominalroll.api.util;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
public class SearchCriteriaBuilderTest {

  @Test
  @SuppressWarnings("unchecked")
  public void byCollectionIdAndStudentPens_addsDeletedStatusExclusion() {
    List<Map<String, Object>> searchCriteria =
      SearchCriteriaBuilder.byCollectionIdAndStudentPens("collection-id", List.of("123456789"));

    assertThat(searchCriteria).hasSize(3);

    Map<String, Object> deletedStatusWrapper = searchCriteria.get(2);
    assertThat(deletedStatusWrapper.get("condition")).isEqualTo("AND");

    List<Map<String, Object>> deletedStatusCriteriaList =
      (List<Map<String, Object>>) deletedStatusWrapper.get("searchCriteriaList");
    assertThat(deletedStatusCriteriaList).singleElement().satisfies(criteria -> {
      assertThat(criteria.get("key")).isEqualTo("sdcSchoolCollectionStudentStatusCode");
      assertThat(criteria.get("value")).isEqualTo("DELETED");
      assertThat(criteria.get("operation")).isEqualTo("neq");
      assertThat(criteria.get("valueType")).isEqualTo("STRING");
    });
  }

  @Test
  @SuppressWarnings("unchecked")
  public void byCollectionIdAndFundingCode_addsDeletedStatusExclusion() {
    List<Map<String, Object>> searchCriteria =
      SearchCriteriaBuilder.byCollectionIdAndFundingCode("collection-id");

    assertThat(searchCriteria).hasSize(3);

    Map<String, Object> deletedStatusWrapper = searchCriteria.get(2);
    assertThat(deletedStatusWrapper.get("condition")).isEqualTo("AND");

    List<Map<String, Object>> deletedStatusCriteriaList =
      (List<Map<String, Object>>) deletedStatusWrapper.get("searchCriteriaList");
    assertThat(deletedStatusCriteriaList).singleElement().satisfies(criteria -> {
      assertThat(criteria.get("key")).isEqualTo("sdcSchoolCollectionStudentStatusCode");
      assertThat(criteria.get("value")).isEqualTo("DELETED");
      assertThat(criteria.get("operation")).isEqualTo("neq");
      assertThat(criteria.get("valueType")).isEqualTo("STRING");
    });
  }
}
