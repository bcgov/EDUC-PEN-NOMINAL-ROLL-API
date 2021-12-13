package ca.bc.gov.educ.pen.nominalroll.api.helpers;

import ca.bc.gov.educ.pen.nominalroll.api.model.v1.NominalRollPostedStudentEntity;
import ca.bc.gov.educ.pen.nominalroll.api.model.v1.NominalRollStudentEntity;
import ca.bc.gov.educ.pen.nominalroll.api.struct.v1.NominalRollStudent;
import lombok.val;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
public class NominalRollHelperTest {

  @Test
  public void getBirthDateFromString() {
    Optional<LocalDate> birthDate = NominalRollHelper.getBirthDateFromString("2000-01-01");
    assertThat(birthDate).isPresent();
    birthDate = NominalRollHelper.getBirthDateFromString("2000-01-01T00:00:00");
    assertThat(birthDate).isEmpty();
    birthDate = NominalRollHelper.getBirthDateFromString("2000/01/01");
    assertThat(birthDate).isPresent();
    birthDate = NominalRollHelper.getBirthDateFromString("20000101");
    assertThat(birthDate).isPresent();
  }

  @Test
  public void isValidDate() {
    boolean isValidDate = NominalRollHelper.isValidDate("20000101");
    assertThat(isValidDate).isFalse();
    isValidDate = NominalRollHelper.isValidDate("2000-01-01");
    assertThat(isValidDate).isTrue();
  }

  @Test
  public void isValidGradeCode() {
    boolean isValidGradeCode = NominalRollHelper.isValidGradeCode("20000101");
    assertThat(isValidGradeCode).isFalse();
    isValidGradeCode = NominalRollHelper.isValidGradeCode("01");
    assertThat(isValidGradeCode).isTrue();
  }

  @Test
  public void populateValidationErrors() {
    final NominalRollStudentEntity nominalRollStudentEntity = new NominalRollStudentEntity();
    assertThat(nominalRollStudentEntity.getNominalRollStudentValidationErrors()).isEmpty();
    final Map<String, String> validationErrors = new HashMap<>();
    validationErrors.put("GENDER", "Invalid Gender Code");
    val valErrors = NominalRollHelper.populateValidationErrors(validationErrors, nominalRollStudentEntity);
    assertThat(valErrors).isNotEmpty();
  }

  @Test
  public void findMatchingPEN() {
    final List<NominalRollPostedStudentEntity> postedStudentEntityList = new ArrayList<>();
    final NominalRollStudent nominalRollStudent = new NominalRollStudent();
    nominalRollStudent.setBirthDate("2000-01-01");
    val matchingPen = NominalRollHelper.findMatchingPEN(nominalRollStudent, postedStudentEntityList);
    assertThat(matchingPen).isEmpty();
    final NominalRollPostedStudentEntity postedStudentEntity = new NominalRollPostedStudentEntity();
    postedStudentEntity.setBirthDate(LocalDate.parse("2000-01-01"));
    postedStudentEntity.setAssignedPEN("123456789");
    postedStudentEntity.setStatus("PROCESSED");
    postedStudentEntity.setAgreementType("P");
    nominalRollStudent.setLeaProvincial("P");
    nominalRollStudent.setRecipientNumber("1");
    postedStudentEntity.setFederalBandCode("1");
    postedStudentEntity.setFederalRecipientBandName("test");
    nominalRollStudent.setRecipientName("test");

    postedStudentEntity.setSurname("test");
    nominalRollStudent.setSurname("test");

    postedStudentEntity.setGivenNames("test");
    nominalRollStudent.setGivenNames("test");

    postedStudentEntity.setGender("M");
    nominalRollStudent.setGender("M");

    postedStudentEntity.setFte(BigDecimal.valueOf(1.5));
    nominalRollStudent.setFte("1.5");

    postedStudentEntity.setBandOfResidence("test");
    nominalRollStudent.setBandOfResidence("test");

    postedStudentEntityList.add(postedStudentEntity);
    val matchingPen2 = NominalRollHelper.findMatchingPEN(nominalRollStudent, postedStudentEntityList);
    assertThat(matchingPen2).isPresent();
    assertThat(matchingPen2.get()).isEqualTo("123456789");
  }

  @Test
  public void getGradeCodeMap() {
    assertThat(NominalRollHelper.getGradeCodeMap()).isNotEmpty();
    assertThat(NominalRollHelper.getGradeCodeMap()).hasSize(27);
  }
}
