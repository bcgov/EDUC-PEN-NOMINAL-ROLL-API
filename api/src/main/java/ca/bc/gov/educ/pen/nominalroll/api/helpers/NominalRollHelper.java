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
  public static final Map<String, String> gradeCodeMap = new HashMap<>();
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

  public static boolean isValidDate(final String date, final DateTimeFormatter formatter) {
    try {
      LocalDate.parse(date, formatter);
      return true;
    } catch (final DateTimeParseException dateTimeParseException3) {
      return false;
    }
  }

  public static boolean isValidGradeCode(@NonNull final String gradeCode) {
    return gradeCodes.contains(gradeCodeMap.get(StringUtils.upperCase(gradeCode)));
  }
}
