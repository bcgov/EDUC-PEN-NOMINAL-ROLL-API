package ca.bc.gov.educ.pen.nominalroll.api.helpers;

import ca.bc.gov.educ.pen.nominalroll.api.constants.GradeCodes;
import lombok.NonNull;
import org.apache.commons.lang3.StringUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.format.SignStyle;
import java.util.*;
import java.util.stream.Collectors;

import static java.time.temporal.ChronoField.*;

public final class NominalRollHelper {
  private static final Set<String> gradeCodes = Arrays.stream(GradeCodes.values()).map(GradeCodes::getCode).collect(Collectors.toSet());
  public static final Map<String, String> GRADE_CODE_MAP = new HashMap<>();
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

  private NominalRollHelper() {

  }

  static {
    GRADE_CODE_MAP.put("1", "01");
    GRADE_CODE_MAP.put("01", "01");
    GRADE_CODE_MAP.put("2", "02");
    GRADE_CODE_MAP.put("02", "02");
    GRADE_CODE_MAP.put("3", "03");
    GRADE_CODE_MAP.put("03", "03");
    GRADE_CODE_MAP.put("4", "04");
    GRADE_CODE_MAP.put("04", "04");
    GRADE_CODE_MAP.put("5", "05");
    GRADE_CODE_MAP.put("05", "05");
    GRADE_CODE_MAP.put("6", "06");
    GRADE_CODE_MAP.put("06", "06");
    GRADE_CODE_MAP.put("7", "07");
    GRADE_CODE_MAP.put("07", "07");
    GRADE_CODE_MAP.put("8", "08");
    GRADE_CODE_MAP.put("08", "08");
    GRADE_CODE_MAP.put("9", "09");
    GRADE_CODE_MAP.put("09", "09");
    GRADE_CODE_MAP.put("10", "10");
    GRADE_CODE_MAP.put("11", "11");
    GRADE_CODE_MAP.put("12", "12");
    GRADE_CODE_MAP.put("K", "KF");
    GRADE_CODE_MAP.put("KF", "KF");
    GRADE_CODE_MAP.put("UE", "EU");
    GRADE_CODE_MAP.put("EU", "EU");
    GRADE_CODE_MAP.put("US", "SU");
    GRADE_CODE_MAP.put("SU", "SU");
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
    return gradeCodes.contains(GRADE_CODE_MAP.get(StringUtils.upperCase(gradeCode)));
  }
}
