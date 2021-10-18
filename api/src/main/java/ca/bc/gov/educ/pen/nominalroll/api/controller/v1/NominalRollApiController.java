package ca.bc.gov.educ.pen.nominalroll.api.controller.v1;

import ca.bc.gov.educ.pen.nominalroll.api.constants.FileTypes;
import ca.bc.gov.educ.pen.nominalroll.api.endpoint.v1.NominalRollApiEndpoint;
import ca.bc.gov.educ.pen.nominalroll.api.exception.EntityNotFoundException;
import ca.bc.gov.educ.pen.nominalroll.api.exception.FileError;
import ca.bc.gov.educ.pen.nominalroll.api.exception.FileUnProcessableException;
import ca.bc.gov.educ.pen.nominalroll.api.mappers.v1.NominalRollStudentMapper;
import ca.bc.gov.educ.pen.nominalroll.api.processor.FileProcessor;
import ca.bc.gov.educ.pen.nominalroll.api.service.v1.NominalRollService;
import ca.bc.gov.educ.pen.nominalroll.api.struct.v1.FileUpload;
import ca.bc.gov.educ.pen.nominalroll.api.struct.v1.NominalRollFileProcessResponse;
import ca.bc.gov.educ.pen.nominalroll.api.struct.v1.NominalRollStudent;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.poi.openxml4j.exceptions.OLE2NotOfficeXmlFileException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import static ca.bc.gov.educ.pen.nominalroll.api.constants.FileTypes.XLS;
import static ca.bc.gov.educ.pen.nominalroll.api.constants.FileTypes.XLSX;

@RestController
@Slf4j
public class NominalRollApiController implements NominalRollApiEndpoint {
  private final Map<FileTypes, FileProcessor> fileProcessorsMap;

  private final NominalRollService service;

  public NominalRollApiController(final List<FileProcessor> fileProcessors, final NominalRollService service) {
    this.fileProcessorsMap = fileProcessors.stream().collect(Collectors.toMap(FileProcessor::getFileType, Function.identity()));
    this.service = service;
  }

  @Override
  public ResponseEntity<NominalRollFileProcessResponse> processNominalRollFile(final FileUpload fileUpload, final String correlationID) {
    NominalRollFileProcessResponse nominalRollFileProcessResponse = null;
    if (XLSX.getCode().equals(fileUpload.getFileExtension())) {
      try {
        nominalRollFileProcessResponse = this.fileProcessorsMap.get(XLSX).processFile(Base64.getDecoder().decode(fileUpload.getFileContents()), correlationID);
      } catch (final OLE2NotOfficeXmlFileException ole2NotOfficeXmlFileException) {
        log.warn("OLE2NotOfficeXmlFileException during Nominal Roll file processing", ole2NotOfficeXmlFileException);
        throw new FileUnProcessableException(FileError.FILE_ENCRYPTED, correlationID);
      }
    } else if (XLS.getCode().equals(fileUpload.getFileExtension())) {
      nominalRollFileProcessResponse = this.fileProcessorsMap.get(XLS).processFile(Base64.getDecoder().decode(fileUpload.getFileContents()), correlationID);
    }
    if (nominalRollFileProcessResponse != null) {
      return ResponseEntity.ok(nominalRollFileProcessResponse);
    }
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
  }

  @Override
  public ResponseEntity<Void> processNominalRollStudents(final List<NominalRollStudent> nominalRollStudents, final String correlationID) {
    val nomRollStudentEntities = nominalRollStudents.stream().map(NominalRollStudentMapper.mapper::toModel).collect(Collectors.toList());
    this.service.saveNominalRollStudents(nomRollStudentEntities, correlationID);
    return ResponseEntity.accepted().build();
  }

  @Override
  public ResponseEntity<List<NominalRollStudent>> getAllProcessingResults() {
    if (this.service.isCurrentYearFileBeingProcessed()) {
      if (this.service.isAllRecordsProcessed()) {
        val students = this.service.getAllNominalRollStudents();
        val studentStructs = students.stream().map(NominalRollStudentMapper.mapper::toStruct).collect(Collectors.toList());
        return ResponseEntity.ok(studentStructs);
      } else {
        return ResponseEntity.accepted().build();
      }
    } else {
      return ResponseEntity.notFound().build();
    }
  }

  @Override
  public ResponseEntity<NominalRollStudent> getProcessingResultOfStudent(final UUID nomRollStudentID) {
    val nomRollStudent = this.service.getNominalRollStudentByID(nomRollStudentID).orElseThrow(EntityNotFoundException::new);
    return ResponseEntity.ok(NominalRollStudentMapper.mapper.toStruct(nomRollStudent));
  }

  @Override
  public ResponseEntity<Void> deleteAll() {
    this.service.deleteAllNominalRollStudents();
    return ResponseEntity.noContent().build();
  }

  @Override
  public ResponseEntity<Boolean> checkForDuplicateNominalRollStudents(String correlationID) {
    return ResponseEntity.ok(this.service.hasDuplicateRecords());
  }
}
