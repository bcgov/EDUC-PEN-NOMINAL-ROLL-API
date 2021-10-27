package ca.bc.gov.educ.pen.nominalroll.api.helpers;

import ca.bc.gov.educ.pen.nominalroll.api.constants.GradeCodes;
import ca.bc.gov.educ.pen.nominalroll.api.model.v1.NominalRollPostedStudentEntity;
import ca.bc.gov.educ.pen.nominalroll.api.model.v1.NominalRollStudentEntity;
import ca.bc.gov.educ.pen.nominalroll.api.model.v1.NominalRollStudentValidationError;
import ca.bc.gov.educ.pen.nominalroll.api.properties.ApplicationProperties;
import ca.bc.gov.educ.pen.nominalroll.api.struct.v1.NominalRollStudent;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import lombok.Getter;
import lombok.NonNull;
import org.apache.commons.lang3.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.format.SignStyle;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static java.time.temporal.ChronoField.*;

public final class NominalRollHelper {
  public static final DateTimeFormatter YYYY_MM_DD_SLASH_FORMATTER = new DateTimeFormatterBuilder()
    .appendValue(YEAR, 4, 10, SignStyle.EXCEEDS_PAD)
    .appendLiteral("/")
    .appendValue(MONTH_OF_YEAR, 2)
    .appendLiteral("/")
    .appendValue(DAY_OF_MONTH, 2).toFormatter();
  public static final DateTimeFormatter YYYY_MM_DD_FORMATTER = new DateTimeFormatterBuilder()
    .appendValue(YEAR, 4, 10, SignStyle.EXCEEDS_PAD)
    .appendValue(MONTH_OF_YEAR, 2)
    .appendValue(DAY_OF_MONTH, 2).toFormatter();
  @Getter
  private static final Map<String, String> gradeCodeMap = new HashMap<>();
  @Getter
  private static final ListMultimap<String, String> agreementTypeMap = ArrayListMultimap.create();

  private static final Set<String> gradeCodes = Arrays.stream(GradeCodes.values()).map(GradeCodes::getCode).collect(Collectors.toSet());

  static {
    gradeCodeMap.put("1", "01");
    gradeCodeMap.put("01", "01");
    gradeCodeMap.put("2", "02");
    gradeCodeMap.put("02", "02");
    gradeCodeMap.put("3", "03");
    gradeCodeMap.put("03", "03");
    gradeCodeMap.put("4", "04");
    gradeCodeMap.put("04", "04");
    gradeCodeMap.put("5", "05");
    gradeCodeMap.put("05", "05");
    gradeCodeMap.put("6", "06");
    gradeCodeMap.put("06", "06");
    gradeCodeMap.put("7", "07");
    gradeCodeMap.put("07", "07");
    gradeCodeMap.put("8", "08");
    gradeCodeMap.put("08", "08");
    gradeCodeMap.put("9", "09");
    gradeCodeMap.put("09", "09");
    gradeCodeMap.put("10", "10");
    gradeCodeMap.put("11", "11");
    gradeCodeMap.put("12", "12");
    gradeCodeMap.put("K", "KF");
    gradeCodeMap.put("KF", "KF");
    gradeCodeMap.put("UE", "EU");
    gradeCodeMap.put("EU", "EU");
    gradeCodeMap.put("US", "SU");
    gradeCodeMap.put("SU", "SU");
    agreementTypeMap.put("L", "L");
    agreementTypeMap.put("LEA", "L");
    agreementTypeMap.put("Local Ed. Agreement", "L");
    agreementTypeMap.put("Local Education Agreement", "L");
    agreementTypeMap.put("P", "P");
    agreementTypeMap.put("Provincial", "P");
    agreementTypeMap.put("Prov", "P");

  }

  private NominalRollHelper() {

  }

  public static Optional<LocalDate> getBirthDateFromString(final String birthDate) {
    try {
      return Optional.of(LocalDate.parse(birthDate)); // yyyy-MM-dd
    } catch (final DateTimeParseException dateTimeParseException) {
      try {
        return Optional.of(LocalDate.parse(birthDate, YYYY_MM_DD_SLASH_FORMATTER));// yyyy/MM/dd
      } catch (final DateTimeParseException dateTimeParseException2) {
        try {
          return Optional.of(LocalDate.parse(birthDate, YYYY_MM_DD_FORMATTER));// yyyyMMdd
        } catch (final DateTimeParseException dateTimeParseException3) {
          return Optional.empty();
        }
      }
    }
  }

  /**
   * @param date the string date to be validated.
   * @return true if it is yyyy-MM-dd format false otherwise.
   */
  public static boolean isValidDate(final String date) {
    try {
      LocalDate.parse(date);
      return true;
    } catch (final DateTimeParseException dateTimeParseException3) {
      return false;
    }
  }


  public static boolean isValidGradeCode(@NonNull final String gradeCode) {
    return gradeCodes.contains(gradeCodeMap.get(StringUtils.upperCase(gradeCode)));
  }


  public static Set<NominalRollStudentValidationError> populateValidationErrors(final Map<String, String> errors, final NominalRollStudentEntity nominalRollStudentEntity) {
    final Set<NominalRollStudentValidationError> validationErrors = new HashSet<>();
    errors.forEach((k, v) -> {
      final NominalRollStudentValidationError error = new NominalRollStudentValidationError();
      error.setFieldName(k);
      error.setFieldError(v);
      error.setNominalRollStudent(nominalRollStudentEntity);
      error.setCreateDate(LocalDateTime.now());
      error.setUpdateDate(LocalDateTime.now());
      error.setCreateUser(ApplicationProperties.API_NAME);
      error.setUpdateUser(ApplicationProperties.API_NAME);
      validationErrors.add(error);
    });
    return validationErrors;
  }

  public static Optional<String> findMatchingPEN(final NominalRollStudent nominalRollStudent, final List<NominalRollPostedStudentEntity> nomRollPostedStudents) {
    return nomRollPostedStudents.stream().filter(findExactMatch(nominalRollStudent)).map(NominalRollPostedStudentEntity::getAssignedPEN).findFirst();
  }

  private static Predicate<NominalRollPostedStudentEntity> findExactMatch(final NominalRollStudent nominalRollStudent) {
    return nomRollPostedStudent -> StringUtils.equalsIgnoreCase(nomRollPostedStudent.getAgreementType(), NominalRollHelper.getAgreementTypeMap().get(nominalRollStudent.getLeaProvincial()).get(0))
      && StringUtils.equalsIgnoreCase(nomRollPostedStudent.getFederalBandCode(), getFederalBandCode(nominalRollStudent.getRecipientNumber()))
      && StringUtils.equalsIgnoreCase(nomRollPostedStudent.getFederalRecipientName(), nominalRollStudent.getRecipientName())
      && StringUtils.equalsIgnoreCase(nomRollPostedStudent.getFederalSchoolNumber(), nominalRollStudent.getSchoolNumber()) && StringUtils.equalsIgnoreCase(nomRollPostedStudent.getFederalSchoolName(), nominalRollStudent.getSchoolName())
      && StringUtils.equalsIgnoreCase(nomRollPostedStudent.getFederalSchoolBoard(), nominalRollStudent.getSchoolDistrictNumber())
      && StringUtils.equalsIgnoreCase(nomRollPostedStudent.getSurname(), nominalRollStudent.getSurname())
      && StringUtils.equalsIgnoreCase(nomRollPostedStudent.getGivenNames(), nominalRollStudent.getGivenNames())
      && StringUtils.equalsIgnoreCase(nomRollPostedStudent.getGender(), nominalRollStudent.getGender())
      && StringUtils.equalsIgnoreCase(nomRollPostedStudent.getGrade(), nominalRollStudent.getGrade())
      && StringUtils.equalsIgnoreCase(nomRollPostedStudent.getBirthDate().format(DateTimeFormatter.ISO_LOCAL_DATE), nominalRollStudent.getBirthDate())
      && StringUtils.equalsIgnoreCase(String.valueOf(nomRollPostedStudent.getFte().doubleValue()), nominalRollStudent.getFte())
      && StringUtils.equalsIgnoreCase(nomRollPostedStudent.getBandOfResidence(), nominalRollStudent.getBandOfResidence());
  }

  public static String getFederalBandCode(final String federalBandCode) {
    if (StringUtils.isBlank(federalBandCode)) {
      return "";
    } else {
      return federalBandCode.replaceFirst("^0+(?!$)", "");
    }
  }
}
