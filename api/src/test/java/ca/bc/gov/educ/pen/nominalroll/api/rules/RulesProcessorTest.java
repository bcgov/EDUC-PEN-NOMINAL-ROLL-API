package ca.bc.gov.educ.pen.nominalroll.api.rules;

import ca.bc.gov.educ.pen.nominalroll.api.BaseNominalRollAPITest;
import ca.bc.gov.educ.pen.nominalroll.api.constants.GradeCodes;
import ca.bc.gov.educ.pen.nominalroll.api.constants.Headers;
import ca.bc.gov.educ.pen.nominalroll.api.mappers.v1.NominalRollStudentMapper;
import ca.bc.gov.educ.pen.nominalroll.api.service.v1.NominalRollService;
import ca.bc.gov.educ.pen.nominalroll.api.struct.external.student.v1.GenderCode;
import ca.bc.gov.educ.pen.nominalroll.api.struct.external.student.v1.GradeCode;
import ca.bc.gov.educ.pen.nominalroll.api.struct.v1.NominalRollStudent;
import lombok.val;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

public class RulesProcessorTest extends BaseNominalRollAPITest {

  @Autowired
  RulesProcessor processor;


  @Before
  public void before() {
    when(restUtils.getActiveGenderCodes()).thenReturn(List.of(GenderCode.builder().genderCode("F").build()));
    List<GradeCode> gradeCodes = new ArrayList<>();
    for (GradeCodes grade : GradeCodes.values()) {
      gradeCodes.add(GradeCode.builder().gradeCode(grade.getCode()).build());
    }
    when(restUtils.getActiveGradeCodes()).thenReturn(gradeCodes);
    when(restUtils.getFedProvSchoolCodes()).thenReturn(Map.of("102", "10200001"));
    when(restUtils.districtCodes()).thenReturn(List.of("102", "103", "021", "006"));
  }

  @Test
  public void testProcessRules_givenValidObject_shouldReturnEmptyMap() {
    NominalRollStudent student = getNominalRollStudent();
    val result = processor.processRules(NominalRollStudentMapper.mapper.toModel(student));
    assertThat(result).isEmpty();
  }

  @Test
  public void testProcessRules_givenInvalidSchoolDistNum_shouldReturnMapWithError() {
    NominalRollStudent student = getNominalRollStudent();
    student.setSchoolDistrictNumber("007");
    val result = processor.processRules(NominalRollStudentMapper.mapper.toModel(student));
    assertThat(result).isNotEmpty();
    assertThat(result.size()).isEqualTo(1);
    assertThat(result.get(Headers.SCHOOL_DISTRICT_NUMBER.getCode())).isNotBlank();
  }

  @Test
  public void testProcessRules_givenInvalidObject_shouldReturnMapWithError() {
    NominalRollStudent student = getNominalRollStudent();
    student.setSchoolDistrictNumber("007");
    student.setGender("A");
    student.setGrade("99");
    student.setSurname(null);
    student.setSchoolNumber("101");
    student.setLeaProvincial("A");
    student.setBirthDate("9999-12-31");
    val result = processor.processRules(NominalRollStudentMapper.mapper.toModel(student));
    assertThat(result).isNotEmpty();
    assertThat(result.size()).isEqualTo(6);
    assertThat(result.get(Headers.SCHOOL_DISTRICT_NUMBER.getCode())).isNotBlank();
    assertThat(result.get(Headers.SURNAME.getCode())).isNotBlank();
    assertThat(result.get(Headers.GENDER.getCode())).isNotBlank();
    assertThat(result.get(Headers.GRADE.getCode())).isNotBlank();
    assertThat(result.get(Headers.SCHOOL_NUMBER.getCode())).isNotBlank();
    assertThat(result.get(Headers.LEA_PROV.getCode())).isNotBlank();
  }

  private NominalRollStudent getNominalRollStudent() {
    return NominalRollStudent.builder().schoolNumber("102").leaProvincial("L").schoolName("school").schoolDistrictNumber("006").birthDate("2000-01-01").gender("F").grade("12").surname("surname").bandOfResidence("bor").fte("1.0").givenNames("givenNames").processingYear("2021").recipientName("recipientName").recipientNumber("recipientNumber").build();
  }
}
