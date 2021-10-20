package ca.bc.gov.educ.pen.nominalroll.api.processor.impl;

import ca.bc.gov.educ.pen.nominalroll.api.constants.HeaderNames;
import ca.bc.gov.educ.pen.nominalroll.api.exception.FileError;
import ca.bc.gov.educ.pen.nominalroll.api.exception.FileUnProcessableException;
import ca.bc.gov.educ.pen.nominalroll.api.exception.InvalidPayloadException;
import ca.bc.gov.educ.pen.nominalroll.api.exception.errors.ApiError;
import ca.bc.gov.educ.pen.nominalroll.api.helpers.NominalRollHelper;
import ca.bc.gov.educ.pen.nominalroll.api.mappers.LocalDateMapper;
import ca.bc.gov.educ.pen.nominalroll.api.processor.FileProcessor;
import ca.bc.gov.educ.pen.nominalroll.api.properties.ApplicationProperties;
import ca.bc.gov.educ.pen.nominalroll.api.struct.v1.NominalRollFileProcessResponse;
import ca.bc.gov.educ.pen.nominalroll.api.struct.v1.NominalRollStudent;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.springframework.http.HttpStatus;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.SignStyle;
import java.util.*;
import java.util.stream.Collectors;

import static ca.bc.gov.educ.pen.nominalroll.api.constants.HeaderNames.*;
import static java.time.temporal.ChronoField.*;

@Slf4j
public abstract class BaseExcelProcessor implements FileProcessor {
  private static final List<String> MANDATORY_HEADERS = Arrays.stream(HeaderNames.values()).map(HeaderNames::getCode).collect(Collectors.toList());
  private static final String STRING_TYPE = "String type :: {}";
  private static final String DATE_TYPE = "Date type :: {}";
  private static final String NUMBER_TYPE = "Number type :: {}";
  private static final String NUMERIC = "Numeric";
  private static final String NON_NUMERIC = "Non Numeric";
  private static final String DATE = "Date";
  private static final String NOT_A_DATE = "Not a Date";
  private static final String FORMULA_TYPE = "Formula type :: {} :: {}";
  private static final String BLANK_CELL = "blank cell";

  protected final ApplicationProperties applicationProperties;

  protected BaseExcelProcessor(final ApplicationProperties applicationProperties) {
    this.applicationProperties = applicationProperties;
  }

  protected File getFile(final byte[] fileContents, final String code) throws IOException {
    final Path path = Files.createTempFile(Paths.get("/temp"), "nr-", code);
    Files.write(path, fileContents);
    final File outputFile = path.toFile();
    outputFile.deleteOnExit();
    return outputFile;
  }

  protected NominalRollFileProcessResponse processSheet(final Sheet sheet, final String correlationID) {
    final Map<Integer, String> headersMap = new HashMap<>();
    final Map<HeaderNames, Integer> invalidValueCounterMap = new EnumMap<>(HeaderNames.class);
    final int rowEnd = sheet.getLastRowNum();
    final List<NominalRollStudent> nominalRollStudents = new ArrayList<>();
    for (int rowNum = 0; rowNum <= rowEnd; rowNum++) {
      final Row r = sheet.getRow(rowNum);
      if (rowNum == 0 && r == null) {
        throw new FileUnProcessableException(FileError.NO_HEADING, correlationID);
      } else if (r == null) {
        log.warn("empty row at :: {}", rowNum);
        continue;
      }
      final NominalRollStudent nominalRollStudent = NominalRollStudent.builder().build();
      final int lastColumn = r.getLastCellNum();
      for (int cn = 0; cn < lastColumn; cn++) {
        this.processEachColumn(correlationID, headersMap, rowNum, r, nominalRollStudent, cn, invalidValueCounterMap);
      }
      this.populateRowData(correlationID, headersMap, nominalRollStudents, rowNum, nominalRollStudent);
    }
    log.info("contains for invalid counter map is {}", invalidValueCounterMap);
    val isThresholdReached = invalidValueCounterMap.values().stream().filter(value -> value > this.applicationProperties.getNominalRollInvalidFieldThreshold()).findAny();
    if (isThresholdReached.isPresent()) {
      throw new FileUnProcessableException(FileError.FILE_THRESHOLD_CHECK_FAILED, correlationID);
    }
    return NominalRollFileProcessResponse.builder().headers(new ArrayList<>(headersMap.values())).nominalRollStudents(nominalRollStudents).build();
  }

  private void populateRowData(final String correlationID, final Map<Integer, String> headersMap, final List<NominalRollStudent> nominalRollStudents, final int rowNum, final NominalRollStudent nominalRollStudent) {
    if (rowNum == 0) {
      log.debug("Headers Map is populated as :: {}", headersMap);
      this.checkForValidHeaders(correlationID, headersMap);
    } else {
      nominalRollStudents.add(nominalRollStudent);
    }
  }

  private void processEachColumn(final String correlationID, final Map<Integer, String> headersMap, final int rowNum, final Row r, final NominalRollStudent nominalRollStudent, final int cn, final Map<HeaderNames, Integer> invalidValueCounterMap) {
    if (rowNum == 0) {
      this.handleHeaderRow(r, cn, correlationID, headersMap);
    } else if (StringUtils.isNotBlank(headersMap.get(cn))) {
      final Cell cell = r.getCell(cn, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
      if (cell == null) {
        log.debug("empty cell at row :: {} and column :: {} for correlation :: {}", rowNum, cn, correlationID);
      } else {
        this.handleEachCell(rowNum, r, cn, correlationID, headersMap, nominalRollStudent, invalidValueCounterMap);
      }
    }
  }

  private void checkForValidHeaders(final String correlationID, final Map<Integer, String> headersMap) {
    val headerNames = headersMap.values();
    for (val headerName : MANDATORY_HEADERS) {
      if (!headerNames.contains(headerName)) {
        throw new FileUnProcessableException(FileError.MISSING_MANDATORY_HEADER, correlationID, headerName);
      }
    }
  }


  private void handleHeaderRow(final Row r, final int cn, final String correlationID, final Map<Integer, String> headersMap) {
    final Cell cell = r.getCell(cn, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
    if (cell == null) {
      throw new FileUnProcessableException(FileError.BLANK_CELL_IN_HEADING_ROW, correlationID, String.valueOf(cn));
    }
    try {
      val headerNameFromFile = StringUtils.trim(cell.getStringCellValue());
      val headerOptional = HeaderNames.fromString(headerNameFromFile);
      if (headerOptional.isPresent()) {
        headersMap.put(cn, StringUtils.trim(cell.getStringCellValue()));
      }
    } catch (final Exception ex) {
      log.error("Header parsing exception", ex);
      throw new InvalidPayloadException(ApiError.builder().status(HttpStatus.BAD_REQUEST).message("Invalid or missing header.").build());
    }

  }

  private void handleEachCell(final int rowNum, final Row r, final int cn, final String correlationID, final Map<Integer, String> headersMap, final NominalRollStudent nominalRollStudent, final Map<HeaderNames, Integer> invalidValueCounterMap) {
    final Cell cell = r.getCell(cn, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
    val headerNamesOptional = HeaderNames.fromString(headersMap.get(cn));
    if (headerNamesOptional.isPresent()) {
      val headerName = headerNamesOptional.get();
      val headerNames = headerName.getCode();
      switch (headerName) {
        case SCHOOL_DISTRICT_NUMBER:
          this.setSchoolDistrictNumber(rowNum, correlationID, nominalRollStudent, invalidValueCounterMap, cell, headerNames);
          break;
        case SCHOOL_NUMBER:
          this.setSchoolNumber(rowNum, correlationID, nominalRollStudent, invalidValueCounterMap, cell, headerNames);
          break;
        case SCHOOL_NAME:
          this.setSchoolName(rowNum, correlationID, nominalRollStudent, invalidValueCounterMap, cell, headerNames);
          break;
        case LEA_PROV:
          this.setLeaProv(rowNum, correlationID, nominalRollStudent, invalidValueCounterMap, cell, headerNames);
          break;
        case RECIPIENT_NUMBER:
          this.setRecipientNumber(rowNum, correlationID, nominalRollStudent, invalidValueCounterMap, cell, headerNames);
          break;
        case RECIPIENT_NAME:
          this.setRecipientName(rowNum, correlationID, nominalRollStudent, cell, headerNames, invalidValueCounterMap);
          break;
        case SURNAME:
          this.setSurname(rowNum, correlationID, nominalRollStudent, cell, headerNames, invalidValueCounterMap);
          break;
        case GIVEN_NAMES:
          nominalRollStudent.setGivenNames(this.getCellValueString(cell, correlationID, rowNum, headerNames));
          break;
        case GENDER:
          this.setGender(rowNum, correlationID, nominalRollStudent, cell, headerNames, invalidValueCounterMap);
          break;
        case BIRTH_DATE:
          this.setBirthDate(rowNum, correlationID, nominalRollStudent, cell, headerNames, invalidValueCounterMap);
          break;
        case GRADE:
          this.setGrade(rowNum, correlationID, nominalRollStudent, cell, headerNames, invalidValueCounterMap);
          break;
        case FTE:
          this.setFTE(rowNum, correlationID, nominalRollStudent, cell, headerNames, invalidValueCounterMap);
          break;
        case BAND_OF_RESIDENCE:
          this.setBandOfResidence(rowNum, correlationID, nominalRollStudent, cell, headerNames, invalidValueCounterMap);
          break;
        default:
          log.debug("Header name from excel is :: {} is not present in configured headers.", headerNames);
          break;
      }
    } else {
      log.debug("Header :: '{}' is not configured.", headersMap.get(cn));
    }


  }

  private void setBandOfResidence(final int rowNum, final String correlationID, final NominalRollStudent nominalRollStudent, final Cell cell, final String headerNames, final Map<HeaderNames, Integer> invalidValueCounterMap) {
    val fieldValue = this.getCellValueString(cell, correlationID, rowNum, headerNames);
    if (StringUtils.isBlank(fieldValue)) {
      this.addToInvalidCounterMap(invalidValueCounterMap, BAND_OF_RESIDENCE);
    }
    nominalRollStudent.setBandOfResidence(this.getCellValueString(cell, correlationID, rowNum, headerNames));
  }

  private void setFTE(final int rowNum, final String correlationID, final NominalRollStudent nominalRollStudent, final Cell cell, final String headerNames, final Map<HeaderNames, Integer> invalidValueCounterMap) {
    val fieldValue = this.getCellValueString(cell, correlationID, rowNum, headerNames);
    if (StringUtils.isBlank(fieldValue)) {
      this.addToInvalidCounterMap(invalidValueCounterMap, FTE);
    }
    nominalRollStudent.setFte(this.getCellValueString(cell, correlationID, rowNum, headerNames));
  }

  private void setGrade(final int rowNum, final String correlationID, final NominalRollStudent nominalRollStudent, final Cell cell, final String headerNames, final Map<HeaderNames, Integer> invalidValueCounterMap) {
    val fieldValue = this.getCellValueString(cell, correlationID, rowNum, headerNames);
    if (StringUtils.isBlank(fieldValue) || !NominalRollHelper.isValidGradeCode(fieldValue)) {
      this.addToInvalidCounterMap(invalidValueCounterMap, GRADE);
    }
    nominalRollStudent.setGrade(this.getCellValueString(cell, correlationID, rowNum, headerNames));
  }

  private void setBirthDate(final int rowNum, final String correlationID, final NominalRollStudent nominalRollStudent, final Cell cell, final String headerNames, final Map<HeaderNames, Integer> invalidValueCounterMap) {
    val fieldValue = this.getCellValueString(cell, correlationID, rowNum, headerNames);
    if (StringUtils.isBlank(fieldValue)) {
      this.addToInvalidCounterMap(invalidValueCounterMap, BIRTH_DATE);
      nominalRollStudent.setBirthDate(fieldValue);
    } else {
      val birthDate = NominalRollHelper.getBirthDateFromString(fieldValue);
      if (birthDate.isPresent()) {
        nominalRollStudent.setBirthDate(new LocalDateMapper().map(birthDate.get()));
      } else {
        this.addToInvalidCounterMap(invalidValueCounterMap, BIRTH_DATE);
        nominalRollStudent.setBirthDate(null);
      }
    }

  }


  private void setGender(final int rowNum, final String correlationID, final NominalRollStudent nominalRollStudent, final Cell cell, final String headerNames, final Map<HeaderNames, Integer> invalidValueCounterMap) {
    val fieldValue = this.getCellValueString(cell, correlationID, rowNum, headerNames);
    if (StringUtils.isBlank(fieldValue) || !(fieldValue.trim().equalsIgnoreCase("M") || fieldValue.trim().equalsIgnoreCase("F"))) {
      this.addToInvalidCounterMap(invalidValueCounterMap, GENDER);
    }
    nominalRollStudent.setGender(fieldValue);
  }

  private void setSurname(final int rowNum, final String correlationID, final NominalRollStudent nominalRollStudent, final Cell cell, final String headerNames, final Map<HeaderNames, Integer> invalidValueCounterMap) {
    val fieldValue = this.getCellValueString(cell, correlationID, rowNum, headerNames);
    if (StringUtils.isBlank(fieldValue)) {
      this.addToInvalidCounterMap(invalidValueCounterMap, SURNAME);
    }
    nominalRollStudent.setSurname(fieldValue);
  }

  private void setRecipientName(final int rowNum, final String correlationID, final NominalRollStudent nominalRollStudent, final Cell cell, final String headerNames, final Map<HeaderNames, Integer> invalidValueCounterMap) {
    val fieldValue = this.getCellValueString(cell, correlationID, rowNum, headerNames);
    if (StringUtils.isBlank(fieldValue)) {
      this.addToInvalidCounterMap(invalidValueCounterMap, RECIPIENT_NAME);
    }
    nominalRollStudent.setRecipientName(fieldValue);
  }

  private void setRecipientNumber(final int rowNum, final String correlationID, final NominalRollStudent nominalRollStudent, final Map<HeaderNames, Integer> invalidValueCounterMap, final Cell cell, final String headerNames) {
    val recipientNum = this.getCellValueString(cell, correlationID, rowNum, headerNames);
    if (StringUtils.isBlank(recipientNum)) {
      this.addToInvalidCounterMap(invalidValueCounterMap, RECIPIENT_NUMBER);
    }
    nominalRollStudent.setRecipientNumber(recipientNum);
  }

  private void setLeaProv(final int rowNum, final String correlationID, final NominalRollStudent nominalRollStudent, final Map<HeaderNames, Integer> invalidValueCounterMap, final Cell cell, final String headerNames) {
    val leaProv = this.getCellValueString(cell, correlationID, rowNum, headerNames);
    if (StringUtils.isBlank(leaProv)) {
      this.addToInvalidCounterMap(invalidValueCounterMap, LEA_PROV);
    }
    nominalRollStudent.setLeaProvincial(leaProv);
  }

  private void setSchoolName(final int rowNum, final String correlationID, final NominalRollStudent nominalRollStudent, final Map<HeaderNames, Integer> invalidValueCounterMap, final Cell cell, final String headerNames) {
    val schoolName = this.getCellValueString(cell, correlationID, rowNum, headerNames);
    if (StringUtils.isBlank(schoolName)) {
      this.addToInvalidCounterMap(invalidValueCounterMap, SCHOOL_NAME);
    }
    nominalRollStudent.setSchoolName(schoolName);
  }

  private void setSchoolNumber(final int rowNum, final String correlationID, final NominalRollStudent nominalRollStudent, final Map<HeaderNames, Integer> invalidValueCounterMap, final Cell cell, final String headerNames) {
    val fieldValue = this.getCellValueString(cell, correlationID, rowNum, headerNames);
    if (StringUtils.isBlank(fieldValue)) {
      this.addToInvalidCounterMap(invalidValueCounterMap, SCHOOL_NUMBER);
    }
    nominalRollStudent.setSchoolNumber(fieldValue);
  }

  private void setSchoolDistrictNumber(final int rowNum, final String correlationID, final NominalRollStudent nominalRollStudent, final Map<HeaderNames, Integer> invalidValueCounterMap, final Cell cell, final String headerNames) {
    val fieldValue = this.getCellValueString(cell, correlationID, rowNum, headerNames).replaceAll("\\.\\d+$", "");
    if (StringUtils.isBlank(fieldValue)) {
      this.addToInvalidCounterMap(invalidValueCounterMap, SCHOOL_DISTRICT_NUMBER);
    }
    nominalRollStudent.setSchoolDistrictNumber(fieldValue); // if it is a number in Excel, poi adds `.0` at the end.
  }

  private void addToInvalidCounterMap(final Map<HeaderNames, Integer> invalidValueCounterMap, final HeaderNames headerName) {
    invalidValueCounterMap.computeIfPresent(headerName, (k, v) -> v + 1);
    invalidValueCounterMap.putIfAbsent(headerName, 1);
  }

  private Double getCellValueDouble(final Cell cell, final String correlationID, final int rowNum, final String headerName) {
    switch (cell.getCellType()) {
      case STRING:
        log.debug(STRING_TYPE, cell.getRichStringCellValue().getString());
        try {
          return Double.parseDouble(cell.getRichStringCellValue().getString());
        } catch (final NumberFormatException e) {
          throw new FileUnProcessableException(FileError.INVALID_VALUE_FOR_FIELD, correlationID, String.valueOf(rowNum), headerName, NUMERIC, NON_NUMERIC);
        }
      case NUMERIC:
        if (DateUtil.isCellDateFormatted(cell)) {
          log.debug(DATE_TYPE, cell.getDateCellValue());
          throw new FileUnProcessableException(FileError.INVALID_VALUE_FOR_FIELD, correlationID, String.valueOf(rowNum), headerName, NUMERIC, NON_NUMERIC);
        } else {
          log.debug(NUMBER_TYPE, cell.getNumericCellValue());
          return cell.getNumericCellValue();
        }
      case FORMULA:
        log.debug(FORMULA_TYPE, cell.getCellFormula(), cell.getNumericCellValue());
        return cell.getNumericCellValue();
      case BLANK:
        log.debug(BLANK_CELL);
        return 0.0;
      default:
        throw new FileUnProcessableException(FileError.INVALID_VALUE_FOR_FIELD, correlationID, String.valueOf(rowNum), headerName, NUMERIC, NON_NUMERIC);
    }
  }

  private LocalDate getCellValueLocalDate(final Cell cell, final String correlationID, final int rowNum, final String headerName) {
    // Alternatively, get the value and format it yourself
    val yyyyMMddFormatter = new DateTimeFormatterBuilder()
      .appendValue(YEAR, 4, 10, SignStyle.EXCEEDS_PAD)
      .appendValue(MONTH_OF_YEAR, 2)
      .appendValue(DAY_OF_MONTH, 2).toFormatter();
    String cellValue = "";
    switch (cell.getCellType()) {
      case STRING:
        cellValue = cell.getRichStringCellValue().getString();
        log.debug(STRING_TYPE, cellValue);
        try {
          return LocalDate.parse(cellValue, DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (final Exception e) {
          log.warn("could not parse with pattern yyyy-mm-dd date :: {}", cellValue);
        }
        try {

          cellValue = StringUtils.replace(cellValue, "\\.", "");
          cellValue = StringUtils.substring(cellValue, 0, 8);
          return LocalDate.parse(cellValue, yyyyMMddFormatter);
        } catch (final Exception e) {
          log.warn("could not parse with pattern yyyyMMdd, date :: {}", cellValue);
        }
        throw new FileUnProcessableException(FileError.INVALID_VALUE_FOR_FIELD, correlationID, String.valueOf(rowNum), headerName, DATE, NOT_A_DATE);
      case NUMERIC:
        if (DateUtil.isCellDateFormatted(cell)) {
          log.debug(DATE_TYPE, cell.getDateCellValue());
          return Instant.ofEpochMilli(cell.getDateCellValue().getTime())
            .atZone(ZoneId.systemDefault())
            .toLocalDate();
        } else {
          log.debug(NUMBER_TYPE, cell.getNumericCellValue());
          try {
            cellValue = String.valueOf(cell.getNumericCellValue());
            cellValue = StringUtils.replace(cellValue, ".", "");
            cellValue = StringUtils.substring(cellValue, 0, 8);
            return LocalDate.parse(cellValue, yyyyMMddFormatter);
          } catch (final Exception e) {
            log.warn("could not parse with pattern yyyyMMdd, date :: {}", cellValue);
          }
          throw new FileUnProcessableException(FileError.INVALID_VALUE_FOR_FIELD, correlationID, String.valueOf(rowNum), headerName, DATE, NOT_A_DATE);
        }
      default:
        throw new FileUnProcessableException(FileError.INVALID_VALUE_FOR_FIELD, correlationID, String.valueOf(rowNum), headerName, DATE, NOT_A_DATE);
    }
  }

  private String getCellValueString(final Cell cell, final String correlationID, final int rowNum, final String headerName) {
    switch (cell.getCellType()) {
      case STRING:
        log.debug(STRING_TYPE, cell.getRichStringCellValue().getString());
        return cell.getRichStringCellValue().getString();
      case NUMERIC:
        if (DateUtil.isCellDateFormatted(cell)) {
          val dateValue = cell.getDateCellValue();
          log.debug(DATE_TYPE, dateValue);
          return new SimpleDateFormat("yyyy-MM-dd").format(dateValue);
        }
        log.debug(NUMBER_TYPE, cell.getNumericCellValue());
        return String.valueOf(cell.getNumericCellValue());
      case BOOLEAN:
        log.debug("Boolean type :: {}", cell.getBooleanCellValue());
        return String.valueOf(cell.getBooleanCellValue());
      case FORMULA:
        log.debug(FORMULA_TYPE, cell.getCellFormula(), cell.getNumericCellValue());
        return String.valueOf(cell.getNumericCellValue());
      case BLANK:
        log.debug(BLANK_CELL);
        return "";
      default:
        log.debug("Default");
        return "";
    }
  }

  private Integer getCellValueInteger(final Cell cell, final String correlationID, final int rowNum, final String headerName) {
    // Alternatively, get the value and format it yourself
    switch (cell.getCellType()) {
      case STRING:
        log.debug(STRING_TYPE, cell.getRichStringCellValue().getString());
        try {
          return Integer.parseInt(cell.getRichStringCellValue().getString());
        } catch (final NumberFormatException e) {
          throw new FileUnProcessableException(FileError.INVALID_VALUE_FOR_FIELD, correlationID, String.valueOf(rowNum), headerName, NUMERIC, NON_NUMERIC);
        }
      case NUMERIC:
        if (DateUtil.isCellDateFormatted(cell)) {
          log.debug(DATE_TYPE, cell.getDateCellValue());
          throw new FileUnProcessableException(FileError.INVALID_VALUE_FOR_FIELD, correlationID, String.valueOf(rowNum), headerName, NUMERIC, NON_NUMERIC);
        } else {
          log.debug(NUMBER_TYPE, cell.getNumericCellValue());
          return (int) cell.getNumericCellValue();
        }
      case FORMULA:
        log.debug(FORMULA_TYPE, cell.getCellFormula(), cell.getNumericCellValue());
        return (int) cell.getNumericCellValue();
      case BLANK:
        log.info(BLANK_CELL);
        return 0;
      default:
        throw new FileUnProcessableException(FileError.INVALID_VALUE_FOR_FIELD, correlationID, String.valueOf(rowNum), headerName, NUMERIC, NON_NUMERIC);
    }
  }
}
