package ca.bc.gov.educ.pen.nominalroll.api.processor.impl;

import ca.bc.gov.educ.pen.nominalroll.api.constants.ColumnType;
import ca.bc.gov.educ.pen.nominalroll.api.constants.Headers;
import ca.bc.gov.educ.pen.nominalroll.api.exception.FileError;
import ca.bc.gov.educ.pen.nominalroll.api.exception.FileUnProcessableException;
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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

import static ca.bc.gov.educ.pen.nominalroll.api.constants.Headers.*;

@Slf4j
public abstract class BaseExcelProcessor implements FileProcessor {
  private static final List<String> MANDATORY_HEADERS = Arrays.stream(Headers.values()).map(Headers::getCode).collect(Collectors.toList());
  private static final String STRING_TYPE = "String type :: {}";
  private static final String DATE_TYPE = "Date type :: {}";
  private static final String NUMBER_TYPE = "Number type :: {}";
  protected final ApplicationProperties applicationProperties;

  protected BaseExcelProcessor(final ApplicationProperties applicationProperties) {
    this.applicationProperties = applicationProperties;
  }

  protected File getFile(final byte[] fileContents, final String code) throws IOException {
    final Path path = Files.createTempFile(Paths.get(applicationProperties.getFolderBasePath()), "nr-", code);
    Files.write(path, fileContents);
    final File outputFile = path.toFile();
    outputFile.deleteOnExit();
    return outputFile;
  }

  protected NominalRollFileProcessResponse processSheet(final Sheet sheet, final String correlationID) {
    final Map<Integer, String> headersMap = new HashMap<>();
    final Map<Headers, Integer> invalidValueCounterMap = new EnumMap<>(Headers.class);
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
    val isThresholdReachedList = invalidValueCounterMap.entrySet().stream()
      .filter(ivCounter -> ivCounter.getValue() > this.applicationProperties.getNominalRollInvalidFieldThreshold())
      .map(value -> value.getKey().toString())
      .collect(Collectors.toList());
    if (isThresholdReachedList.size() > 0) {
      throw new FileUnProcessableException(FileError.FILE_THRESHOLD_CHECK_FAILED, correlationID, "File threshold failed with field(s): " + String.join(",", isThresholdReachedList));
    }
    return NominalRollFileProcessResponse.builder().headers(new ArrayList<>(headersMap.values())).nominalRollStudents(nominalRollStudents).build();
  }

  private void populateRowData(final String correlationID, final Map<Integer, String> headersMap, final List<NominalRollStudent> nominalRollStudents, final int rowNum, final NominalRollStudent nominalRollStudent) {
    if (rowNum == 0) {
      log.debug("Headers Map is populated as :: {}", headersMap);
      this.checkForValidHeaders(correlationID, headersMap);
    } else {
      if (nominalRollStudent != null && !nominalRollStudent.isEmpty()) {
        nominalRollStudents.add(nominalRollStudent);
      }
    }
  }

  private void processEachColumn(final String correlationID, final Map<Integer, String> headersMap, final int rowNum, final Row r, final NominalRollStudent nominalRollStudent, final int cn, final Map<Headers, Integer> invalidValueCounterMap) {
    if (rowNum == 0) {
      this.handleHeaderRow(r, cn, correlationID, headersMap);
    } else if (StringUtils.isNotBlank(headersMap.get(cn))) {
      this.handleEachCell(r, cn, headersMap, nominalRollStudent, invalidValueCounterMap);
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
    val headerNameFromFile = StringUtils.trim(cell.getStringCellValue());
    val headerOptional = Headers.fromString(headerNameFromFile);
    headerOptional.ifPresent(header -> headersMap.put(cn, StringUtils.trim(header.getCode())));
  }

  private void handleEachCell(final Row r, final int cn, final Map<Integer, String> headersMap, final NominalRollStudent nominalRollStudent, final Map<Headers, Integer> invalidValueCounterMap) {
    final Cell cell = r.getCell(cn, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
    val headerNamesOptional = Headers.fromString(headersMap.get(cn));
    if (headerNamesOptional.isPresent()) {
      final Headers header = headerNamesOptional.get();
      switch (header) {
        case SCHOOL_DISTRICT_NUMBER:
          this.setSchoolDistrictNumber(nominalRollStudent, invalidValueCounterMap, cell, header.getType());
          break;
        case SCHOOL_NUMBER:
          this.setSchoolNumber(nominalRollStudent, invalidValueCounterMap, cell, header.getType());
          break;
        case SCHOOL_NAME:
          this.setSchoolName(nominalRollStudent, invalidValueCounterMap, cell, header.getType());
          break;
        case LEA_PROV:
          this.setLeaProv(nominalRollStudent, invalidValueCounterMap, cell, header.getType());
          break;
        case RECIPIENT_NUMBER:
          this.setRecipientNumber(nominalRollStudent, invalidValueCounterMap, cell, header.getType());
          break;
        case RECIPIENT_NAME:
          this.setRecipientName(nominalRollStudent, cell, invalidValueCounterMap, header.getType());
          break;
        case SURNAME:
          this.setSurname(nominalRollStudent, cell, invalidValueCounterMap, header.getType());
          break;
        case GIVEN_NAMES:
          nominalRollStudent.setGivenNames(this.getCellValueString(cell, header.getType()));
          break;
        case GENDER:
          this.setGender(nominalRollStudent, cell, invalidValueCounterMap, header.getType());
          break;
        case BIRTH_DATE:
          this.setBirthDate(nominalRollStudent, cell, invalidValueCounterMap, header.getType());
          break;
        case GRADE:
          this.setGrade(nominalRollStudent, cell, invalidValueCounterMap, header.getType());
          break;
        case FTE:
          this.setFTE(nominalRollStudent, cell, invalidValueCounterMap, header.getType());
          break;
        case BAND_OF_RESIDENCE:
          this.setBandOfResidence(nominalRollStudent, cell, invalidValueCounterMap, header.getType());
          break;
      }
    } else {
      log.debug("Header :: '{}' is not configured.", headersMap.get(cn));
    }


  }

  private void setBandOfResidence(final NominalRollStudent nominalRollStudent, final Cell cell, final Map<Headers, Integer> invalidValueCounterMap, final ColumnType columnType) {
    val fieldValue = this.getCellValueString(cell, columnType);
    if (StringUtils.isBlank(fieldValue)) {
      this.addToInvalidCounterMap(invalidValueCounterMap, BAND_OF_RESIDENCE);
    }
    nominalRollStudent.setBandOfResidence(this.getCellValueString(cell, columnType));
  }

  private void setFTE(final NominalRollStudent nominalRollStudent, final Cell cell, final Map<Headers, Integer> invalidValueCounterMap, final ColumnType columnType) {
    val fieldValue = this.getCellValueString(cell, columnType);
    if (StringUtils.isBlank(fieldValue)) {
      this.addToInvalidCounterMap(invalidValueCounterMap, FTE);
    }
    nominalRollStudent.setFte(this.getCellValueString(cell, columnType));
  }

  private void setGrade(final NominalRollStudent nominalRollStudent, final Cell cell, final Map<Headers, Integer> invalidValueCounterMap, final ColumnType columnType) {
    val fieldValue = this.getCellValueString(cell, columnType);
    if (StringUtils.isBlank(fieldValue) || !NominalRollHelper.isValidGradeCode(fieldValue)) {
      this.addToInvalidCounterMap(invalidValueCounterMap, GRADE);
    }
    nominalRollStudent.setGrade(this.getCellValueString(cell, columnType));
  }

  private void setBirthDate(final NominalRollStudent nominalRollStudent, final Cell cell, final Map<Headers, Integer> invalidValueCounterMap, final ColumnType columnType) {
    val fieldValue = this.getCellValueString(cell, columnType);
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

  private void setGender(final NominalRollStudent nominalRollStudent, final Cell cell, final Map<Headers, Integer> invalidValueCounterMap, final ColumnType columnType) {
    val fieldValue = this.getCellValueString(cell, columnType);
    if (StringUtils.isBlank(fieldValue) || !(fieldValue.trim().equalsIgnoreCase("M") || fieldValue.trim().equalsIgnoreCase("X") || fieldValue.trim().equalsIgnoreCase("F"))) {
      this.addToInvalidCounterMap(invalidValueCounterMap, GENDER);
    }
    nominalRollStudent.setGender(fieldValue);
  }

  private void setSurname(final NominalRollStudent nominalRollStudent, final Cell cell, final Map<Headers, Integer> invalidValueCounterMap, final ColumnType columnType) {
    val fieldValue = this.getCellValueString(cell, columnType);
    if (StringUtils.isBlank(fieldValue)) {
      this.addToInvalidCounterMap(invalidValueCounterMap, SURNAME);
    }
    nominalRollStudent.setSurname(fieldValue);
  }

  private void setRecipientName(final NominalRollStudent nominalRollStudent, final Cell cell, final Map<Headers, Integer> invalidValueCounterMap, final ColumnType columnType) {
    val fieldValue = this.getCellValueString(cell, columnType);
    if (StringUtils.isBlank(fieldValue)) {
      this.addToInvalidCounterMap(invalidValueCounterMap, RECIPIENT_NAME);
    }
    nominalRollStudent.setRecipientName(fieldValue);
  }

  private void setRecipientNumber(final NominalRollStudent nominalRollStudent, final Map<Headers, Integer> invalidValueCounterMap, final Cell cell, final ColumnType columnType) {
    val recipientNum = this.getCellValueString(cell, columnType);
    if (StringUtils.isBlank(recipientNum)) {
      this.addToInvalidCounterMap(invalidValueCounterMap, RECIPIENT_NUMBER);
    }
    nominalRollStudent.setRecipientNumber(recipientNum);
  }

  private void setLeaProv(final NominalRollStudent nominalRollStudent, final Map<Headers, Integer> invalidValueCounterMap, final Cell cell, final ColumnType columnType) {
    val leaProv = this.getCellValueString(cell, columnType);
    if (StringUtils.isBlank(leaProv)) {
      this.addToInvalidCounterMap(invalidValueCounterMap, LEA_PROV);
    }
    nominalRollStudent.setLeaProvincial(leaProv);
  }

  private void setSchoolName(final NominalRollStudent nominalRollStudent, final Map<Headers, Integer> invalidValueCounterMap, final Cell cell, final ColumnType columnType) {
    val schoolName = this.getCellValueString(cell, columnType);
    if (StringUtils.isBlank(schoolName)) {
      this.addToInvalidCounterMap(invalidValueCounterMap, SCHOOL_NAME);
    }
    nominalRollStudent.setSchoolName(schoolName);
  }

  private void setSchoolNumber(final NominalRollStudent nominalRollStudent, final Map<Headers, Integer> invalidValueCounterMap, final Cell cell, final ColumnType columnType) {
    val fieldValue = this.getCellValueString(cell, columnType);
    if (StringUtils.isBlank(fieldValue)) {
      this.addToInvalidCounterMap(invalidValueCounterMap, SCHOOL_NUMBER);
    }
    nominalRollStudent.setSchoolNumber(fieldValue);
  }

  private void setSchoolDistrictNumber(final NominalRollStudent nominalRollStudent, final Map<Headers, Integer> invalidValueCounterMap, final Cell cell, final ColumnType columnType) {
    val fieldValue = this.getCellValueString(cell, columnType) != null ? this.getCellValueString(cell, columnType).replaceAll("\\.\\d+$", "") : null;
    if (StringUtils.isBlank(fieldValue)) {
      this.addToInvalidCounterMap(invalidValueCounterMap, SCHOOL_DISTRICT_NUMBER);
    }
    nominalRollStudent.setSchoolDistrictNumber(fieldValue); // if it is a number in Excel, poi adds `.0` at the end.
  }

  private void addToInvalidCounterMap(final Map<Headers, Integer> invalidValueCounterMap, final Headers headerName) {
    invalidValueCounterMap.computeIfPresent(headerName, (k, v) -> v + 1);
    invalidValueCounterMap.putIfAbsent(headerName, 1);
  }

  private String getCellValueString(final Cell cell,final ColumnType columnType) {
    if (cell == null) {
      return null;
    }
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

        if(columnType.equals(ColumnType.DOUBLE)) {
          return String.valueOf(cell.getNumericCellValue());
        }else{
          Double value = cell.getNumericCellValue();
          return String.valueOf(value.intValue());
        }
      default:
        log.debug("Default");
        return "";
    }
  }

}
