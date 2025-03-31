package ca.bc.gov.educ.pen.nominalroll.api.controller.v1;

import ca.bc.gov.educ.pen.nominalroll.api.constants.FileTypes;
import ca.bc.gov.educ.pen.nominalroll.api.constants.v1.NominalRollStudentStatus;
import ca.bc.gov.educ.pen.nominalroll.api.endpoint.v1.NominalRollApiEndpoint;
import ca.bc.gov.educ.pen.nominalroll.api.exception.FileError;
import ca.bc.gov.educ.pen.nominalroll.api.exception.FileUnProcessableException;
import ca.bc.gov.educ.pen.nominalroll.api.mappers.v1.NominalRollStudentMapper;
import ca.bc.gov.educ.pen.nominalroll.api.model.v1.NominalRollStudentEntity;
import ca.bc.gov.educ.pen.nominalroll.api.processor.FileProcessor;
import ca.bc.gov.educ.pen.nominalroll.api.repository.v1.NominalRollStudentValidationErrorRepository;
import ca.bc.gov.educ.pen.nominalroll.api.rest.RestUtils;
import ca.bc.gov.educ.pen.nominalroll.api.rules.RulesProcessor;
import ca.bc.gov.educ.pen.nominalroll.api.service.v1.NominalRollService;
import ca.bc.gov.educ.pen.nominalroll.api.service.v1.NominalRollStudentSearchService;
import ca.bc.gov.educ.pen.nominalroll.api.struct.external.school.v1.FedProvSchoolCode;
import ca.bc.gov.educ.pen.nominalroll.api.struct.v1.*;
import ca.bc.gov.educ.pen.nominalroll.api.util.JsonUtil;
import ca.bc.gov.educ.pen.nominalroll.api.util.TransformUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.openxml4j.exceptions.OLE2NotOfficeXmlFileException;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
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
  private final RulesProcessor rulesProcessor;
  private final RestUtils restUtils;

  public NominalRollApiController(final List<FileProcessor> fileProcessors, final NominalRollService service, final NominalRollStudentSearchService searchService, final RulesProcessor rulesProcessor, final RestUtils restUtils) {
    this.fileProcessorsMap = fileProcessors.stream().collect(Collectors.toMap(FileProcessor::getFileType, Function.identity()));
    this.service = service;
    this.searchService = searchService;
    this.rulesProcessor = rulesProcessor;
    this.restUtils = restUtils;
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

  @SneakyThrows
  @Override
  public ResponseEntity<Void> processNominalRollStudents(final List<NominalRollStudent> nominalRollStudents, final String correlationID) {
    val nomRollStudentEntities = nominalRollStudents.stream().map(NominalRollStudentMapper.mapper::toModel).map(TransformUtil::uppercaseFields).collect(Collectors.toList());
    service.removeClosedSchoolsFedProvMappings();
    this.service.saveNominalRollStudents(nomRollStudentEntities, correlationID);
    return ResponseEntity.accepted().build();
  }

  @Override
  public ResponseEntity<List<NominalRollStudentCount>> isBeingProcessed(final String processingYear) {
    return ResponseEntity.ok(this.service.countAllNominalRollStudents(processingYear));
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
  public ResponseEntity<Boolean> checkForDuplicateNominalRollStudents(String processingYear, final String correlationID) {
    if(StringUtils.isBlank(processingYear)) {
      processingYear = Integer.toString(LocalDateTime.now().getYear());
    }
    return ResponseEntity.ok(this.service.hasDuplicateRecords(processingYear));
  }

  @Override
  public CompletableFuture<Page<NominalRollStudent>> findAll(final Integer pageNumber, final Integer pageSize, final String sortCriteriaJson, final String searchCriteriaListJson) {
    final List<Sort.Order> sorts = new ArrayList<>();
    final Specification<NominalRollStudentEntity> studentSpecs = this.searchService.setSpecificationAndSortCriteria(sortCriteriaJson, searchCriteriaListJson, JsonUtil.mapper, sorts);
    return this.service.findAll(studentSpecs, pageNumber, pageSize, sorts).thenApplyAsync(studentEntities -> studentEntities.map(mapper::toStruct));
  }

  @Override
  public ResponseEntity<NominalRollStudent> validateNomRollStudent(final NominalRollStudent nominalRollStudent) {
    this.restUtils.evictFedProvSchoolCodesCache();
    val errorsMap = this.rulesProcessor.processRules(NominalRollStudentMapper.mapper.toModel(nominalRollStudent));
    nominalRollStudent.setValidationErrors(errorsMap);
    return ResponseEntity.ok(nominalRollStudent);
  }

  @Override
  public ResponseEntity<NominalRollStudent> updateNominalRollStudent(final UUID nomRollStudentID, final NominalRollStudent nominalRollStudent) {
    NominalRollStudentEntity dbEntity = this.service.getNominalRollStudentByID(nomRollStudentID);
    val entity = NominalRollStudentMapper.mapper.toModel(nominalRollStudent);
    if(StringUtils.isNotEmpty(nominalRollStudent.getStatus()) && !nominalRollStudent.getStatus().equals(NominalRollStudentStatus.IGNORED.toString())) {
      this.restUtils.evictFedProvSchoolCodesCache();
      var errorsMap = this.rulesProcessor.processRules(entity);
      if (errorsMap.isEmpty()) {
        BeanUtils.copyProperties(entity, dbEntity, "createDate", "createUser", "nominalRollStudentID", "nominalRollStudentValidationErrors");
        // no validation errors so remove existing ones.
        dbEntity.getNominalRollStudentValidationErrors().clear();
        return ResponseEntity.ok(NominalRollStudentMapper.mapper.toStruct(this.service.updateNominalRollStudent(dbEntity)));
      } else if (nominalRollStudent.getStatus().equals(NominalRollStudentStatus.ERROR.toString())) {
        dbEntity.getNominalRollStudentValidationErrors().clear();
        dbEntity = this.service.saveNominalRollStudentValidationErrors(dbEntity.getNominalRollStudentID().toString(), errorsMap, dbEntity);
        return ResponseEntity.ok(NominalRollStudentMapper.mapper.toStruct(dbEntity));
      } else {
        nominalRollStudent.setValidationErrors(errorsMap);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(nominalRollStudent);
      }
    }else{
      dbEntity.setStatus(NominalRollStudentStatus.IGNORED.toString());
      return ResponseEntity.ok(NominalRollStudentMapper.mapper.toStruct(this.service.updateNominalRollStudent(dbEntity)));
    }
  }

  @Override
  public ResponseEntity<List<NominalRollIDs>> findAllNominalRollStudentIDs(final String processingYear, final List<String> statusCodes, final String searchCriteriaListJson) {
    val errorCode = statusCodes.stream().filter(statusCode -> NominalRollStudentStatus.valueOfCode(statusCode) == null).findFirst();
    if (errorCode.isPresent()) {
      log.error("Invalid nominal roll student status code provided :: " + errorCode);
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
    }

    Map<String, String> searchCriteria = null;
    try {
      if (StringUtils.isNotBlank(searchCriteriaListJson)) {
        searchCriteria = JsonUtil.mapper.readValue(searchCriteriaListJson, new TypeReference<>() {
        });
      }
    } catch (JsonProcessingException e) {
      log.error("Invalid nominal roll student searchCriteria provided :: " + searchCriteriaListJson);
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
    }
    return ResponseEntity.ok(this.service.findAllNominalRollStudentIDs(processingYear, statusCodes, searchCriteria));
    //.stream().map(UUID::toString).collect(Collectors.toList())
  }

  @Override
  public ResponseEntity<Boolean> checkForNominalRollPostedStudents(String processingYear) {
    return ResponseEntity.ok(this.service.hasPostedStudents(processingYear));
  }

  @Override
  public ResponseEntity<Void> addFedProvSchoolCode(FedProvSchoolCode fedProvSchoolCode) {
    this.service.addFedProvSchoolCode(fedProvSchoolCode);
    this.restUtils.evictFedProvSchoolCodesCache();
    var validationErrorEntities = this.service.getSchoolNumberValidationErrors();
    if (!validationErrorEntities.isEmpty()) {
      for(val validationErrorEntity : validationErrorEntities) {
        val studentEntity = validationErrorEntity.getNominalRollStudent();
        studentEntity.getNominalRollStudentValidationErrors().clear();
        var errorsMap = this.rulesProcessor.processRules(studentEntity);
        if (!errorsMap.isEmpty()) {
          this.service.saveNominalRollStudentValidationErrors(studentEntity.getNominalRollStudentID().toString(), errorsMap, studentEntity);
        }else{
          studentEntity.setStatus(NominalRollStudentStatus.FIXABLE.toString());
          this.service.updateNominalRollStudent(studentEntity);
        }
      }
    }
    return ResponseEntity.ok().build();
  }

}
