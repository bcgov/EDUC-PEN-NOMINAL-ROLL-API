package ca.bc.gov.educ.pen.nominalroll.api.controller.v1;

import ca.bc.gov.educ.pen.nominalroll.api.constants.FileTypes;
import ca.bc.gov.educ.pen.nominalroll.api.endpoint.v1.NominalRollApiEndpoint;
import ca.bc.gov.educ.pen.nominalroll.api.exception.EntityNotFoundException;
import ca.bc.gov.educ.pen.nominalroll.api.exception.FileError;
import ca.bc.gov.educ.pen.nominalroll.api.exception.FileUnProcessableException;
import ca.bc.gov.educ.pen.nominalroll.api.mappers.v1.NominalRollStudentMapper;
import ca.bc.gov.educ.pen.nominalroll.api.model.v1.NominalRollStudentEntity;
import ca.bc.gov.educ.pen.nominalroll.api.processor.FileProcessor;
import ca.bc.gov.educ.pen.nominalroll.api.service.v1.NominalRollService;
import ca.bc.gov.educ.pen.nominalroll.api.service.v1.NominalRollStudentSearchService;
import ca.bc.gov.educ.pen.nominalroll.api.struct.v1.FileUpload;
import ca.bc.gov.educ.pen.nominalroll.api.struct.v1.NominalRollFileProcessResponse;
import ca.bc.gov.educ.pen.nominalroll.api.struct.v1.NominalRollStudent;
import ca.bc.gov.educ.pen.nominalroll.api.util.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.poi.openxml4j.exceptions.OLE2NotOfficeXmlFileException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

import static ca.bc.gov.educ.pen.nominalroll.api.constants.FileTypes.XLS;
import static ca.bc.gov.educ.pen.nominalroll.api.constants.FileTypes.XLSX;

@RestController
@Slf4j
public class NominalRollApiController implements NominalRollApiEndpoint {
  private static final NominalRollStudentMapper mapper = NominalRollStudentMapper.mapper;
  private final Map<FileTypes, FileProcessor> fileProcessorsMap;
  private final NominalRollService service;
  private final NominalRollStudentSearchService searchService;

  public NominalRollApiController(final List<FileProcessor> fileProcessors, final NominalRollService service, final NominalRollStudentSearchService searchService) {
    this.fileProcessorsMap = fileProcessors.stream().collect(Collectors.toMap(FileProcessor::getFileType, Function.identity()));
    this.service = service;
    this.searchService = searchService;
  }

  @Override
  public ResponseEntity<NominalRollFileProcessResponse> processNominalRollFile(final FileUpload fileUpload, final String correlationID) {
    Optional<NominalRollFileProcessResponse> nominalRollFileProcessResponseOptional = Optional.empty();
    if (XLSX.getCode().equals(fileUpload.getFileExtension())) {
      try {
        nominalRollFileProcessResponseOptional = Optional.of(this.fileProcessorsMap.get(XLSX).processFile(Base64.getDecoder().decode(fileUpload.getFileContents()), correlationID));
      } catch (final OLE2NotOfficeXmlFileException ole2NotOfficeXmlFileException) {
        log.warn("OLE2NotOfficeXmlFileException during Nominal Roll file processing", ole2NotOfficeXmlFileException);
        throw new FileUnProcessableException(FileError.FILE_ENCRYPTED, correlationID);
      }
    } else if (XLS.getCode().equals(fileUpload.getFileExtension())) {
      nominalRollFileProcessResponseOptional = Optional.of(this.fileProcessorsMap.get(XLS).processFile(Base64.getDecoder().decode(fileUpload.getFileContents()), correlationID));
    }
    return nominalRollFileProcessResponseOptional.map(ResponseEntity::ok).orElse(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
  }

  @Override
  public ResponseEntity<Void> processNominalRollStudents(final List<NominalRollStudent> nominalRollStudents, final String correlationID) {
    val nomRollStudentEntities = nominalRollStudents.stream().map(NominalRollStudentMapper.mapper::toModel).collect(Collectors.toList());
    this.service.saveNominalRollStudents(nomRollStudentEntities, correlationID);
    return ResponseEntity.accepted().build();
  }

  @Override
  public ResponseEntity<Void> isBeingProcessed(final String processingYear) {
    if (this.service.countAllNominalRollStudents(processingYear) > 0) {
      return ResponseEntity.accepted().build();
    } else {
      return ResponseEntity.notFound().build();
    }
  }

  @Override
  public ResponseEntity<NominalRollStudent> getProcessingResultOfStudent(final UUID nomRollStudentID) {
    val nomRollStudent = this.service.getNominalRollStudentByID(nomRollStudentID);
    return ResponseEntity.ok(NominalRollStudentMapper.mapper.toStruct(nomRollStudent));
  }

  @Override
  public ResponseEntity<Void> deleteAll(final String processingYear) {
    this.service.deleteAllNominalRollStudents(processingYear);
    return ResponseEntity.noContent().build();
  }

  @Override
  public ResponseEntity<Boolean> checkForDuplicateNominalRollStudents(final String correlationID) {
    return ResponseEntity.ok(this.service.hasDuplicateRecords());
  }

  @Override
  @Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
  public CompletableFuture<Page<NominalRollStudent>> findAll(final Integer pageNumber, final Integer pageSize, final String sortCriteriaJson, final String searchCriteriaListJson) {
    final List<Sort.Order> sorts = new ArrayList<>();
    final Specification<NominalRollStudentEntity> studentSpecs = this.searchService.setSpecificationAndSortCriteria(sortCriteriaJson, searchCriteriaListJson, JsonUtil.mapper, sorts);
    return this.service.findAll(studentSpecs, pageNumber, pageSize, sorts).thenApplyAsync(studentEntities -> studentEntities.map(mapper::toStructure));
  }
}
