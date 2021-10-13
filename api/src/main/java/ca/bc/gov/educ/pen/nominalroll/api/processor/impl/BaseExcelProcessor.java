package ca.bc.gov.educ.pen.nominalroll.api.processor.impl;

import ca.bc.gov.educ.pen.nominalroll.api.constants.HeaderNames;
import ca.bc.gov.educ.pen.nominalroll.api.exception.FileError;
import ca.bc.gov.educ.pen.nominalroll.api.exception.FileUnProcessableException;
import ca.bc.gov.educ.pen.nominalroll.api.exception.InvalidPayloadException;
import ca.bc.gov.educ.pen.nominalroll.api.exception.errors.ApiError;
import ca.bc.gov.educ.pen.nominalroll.api.processor.FileProcessor;
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
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.SignStyle;
import java.util.*;
import java.util.stream.Collectors;

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

  protected File getFile(final byte[] fileContents, final String code) throws URISyntaxException, IOException {
    val uri = Objects.requireNonNull(this.getClass().getClassLoader().getResource("application.properties")).toURI();
    final String mainPath = Paths.get(uri).toString().replaceAll("application.properties", "");
    final Path path = Files.createTempFile(Paths.get(mainPath), "nr-", code);
    Files.write(path, fileContents);
    final File outputFile = path.toFile();
    outputFile.deleteOnExit();
    return outputFile;
  }

  protected NominalRollFileProcessResponse processSheet(final Sheet sheet, final String correlationID) {
    final Map<Integer, String> headersMap = new HashMap<>();
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
        this.processEachColumn(correlationID, headersMap, rowNum, r, nominalRollStudent, cn);
      }
      this.populateRowData(correlationID, headersMap, nominalRollStudents, rowNum, nominalRollStudent);
    }
    return NominalRollFileProcessResponse.builder().headers(new ArrayList<>(headersMap.values())).nominalRollStudents(nominalRollStudents).build();
  }

  private void populateRowData(final String correlationID, final Map<Integer, String> headersMap, final List<NominalRollStudent> nominalRollStudents, final int rowNum, final NominalRollStudent nominalRollStudent) {
    if (rowNum == 0) {
      log.debug("Headers Map is populated as :: {}", headersMap);
      this.checkForValidHeaders(correlationID, headersMap);
    } else if (nominalRollStudent.getBirthDate() != null) {
      nominalRollStudents.add(nominalRollStudent);
    }
  }

  private void processEachColumn(final String correlationID, final Map<Integer, String> headersMap, final int rowNum, final Row r, final NominalRollStudent nominalRollStudent, final int cn) {
    if (rowNum == 0) {
      this.handleHeaderRow(r, cn, correlationID, headersMap);
    } else if (StringUtils.isNotBlank(headersMap.get(cn))) {
      final Cell cell = r.getCell(cn, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
      if (cell == null) {
        log.debug("empty cell at row :: {} and column :: {} for correlation :: {}", rowNum, cn, correlationID);
      } else {
        this.handleEachCell(rowNum, r, cn, correlationID, headersMap, nominalRollStudent);
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

  private void handleEachCell(final int rowNum, final Row r, final int cn, final String correlationID, final Map<Integer, String> headersMap, final NominalRollStudent nominalRollStudent) {
    final Cell cell = r.getCell(cn, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
    val headerNamesOptional = HeaderNames.fromString(headersMap.get(cn));
    if (headerNamesOptional.isPresent()) {
      val headerName = headerNamesOptional.get();
      val headerNames = headerName.getCode();
      switch (headerName) {
        case SCHOOL_DISTRICT_NUMBER:
          nominalRollStudent.setSchoolDistrictNumber(this.getCellValueString(cell, correlationID, rowNum, headerNames));
          break;
        case SCHOOL_NUMBER:
          nominalRollStudent.setSchoolNumber(this.getCellValueString(cell, correlationID, rowNum, headerNames));
          break;
        case SCHOOL_NAME:
          nominalRollStudent.setSchoolName(this.getCellValueString(cell, correlationID, rowNum, headerNames));
          break;
        case LEA_PROV:
          nominalRollStudent.setLeaProvincial(this.getCellValueString(cell, correlationID, rowNum, headerNames));
          break;
        case RECIPIENT_NUMBER:
          nominalRollStudent.setRecipientNumber(this.getCellValueString(cell, correlationID, rowNum, headerNames));
          break;
        case RECIPIENT_NAME:
          nominalRollStudent.setRecipientName(this.getCellValueString(cell, correlationID, rowNum, headerNames));
          break;
        case IDENTITY:
          nominalRollStudent.setIdentity(this.getCellValueString(cell, correlationID, rowNum, headerNames));
          break;
        case SURNAME:
          nominalRollStudent.setSurname(this.getCellValueString(cell, correlationID, rowNum, headerNames));
          break;
        case GIVEN_NAMES:
          nominalRollStudent.setGivenNames(this.getCellValueString(cell, correlationID, rowNum, headerNames));
          break;
        case INITIAL:
          nominalRollStudent.setInitial(this.getCellValueString(cell, correlationID, rowNum, headerNames));
          break;
        case GENDER:
          nominalRollStudent.setGender(this.getCellValueString(cell, correlationID, rowNum, headerNames));
          break;
        case BIRTH_DATE:
          nominalRollStudent.setBirthDate(this.getCellValueString(cell, correlationID, rowNum, headerNames));
          break;
        case GRADE:
          nominalRollStudent.setGrade(this.getCellValueString(cell, correlationID, rowNum, headerNames));
          break;
        case FTE:
          nominalRollStudent.setFte(this.getCellValueString(cell, correlationID, rowNum, headerNames));
          break;
        case BAND_OF_RESIDENCE:
          nominalRollStudent.setBandOfResidence(this.getCellValueString(cell, correlationID, rowNum, headerNames));
          break;
        default:
          log.debug("Header name from excel is :: {} is not present in configured headers.", headerNames);
          break;
      }
    } else {
      log.debug("Header :: '{}' is not configured.", headersMap.get(cn));
    }


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
