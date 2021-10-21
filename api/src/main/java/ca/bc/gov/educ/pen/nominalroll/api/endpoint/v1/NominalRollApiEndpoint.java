package ca.bc.gov.educ.pen.nominalroll.api.endpoint.v1;

import ca.bc.gov.educ.pen.nominalroll.api.constants.v1.URL;
import ca.bc.gov.educ.pen.nominalroll.api.struct.v1.FileUpload;
import ca.bc.gov.educ.pen.nominalroll.api.struct.v1.NominalRollFileProcessResponse;
import ca.bc.gov.educ.pen.nominalroll.api.struct.v1.NominalRollStudent;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;


@RequestMapping(URL.BASE_URL)
@OpenAPIDefinition(info = @Info(title = "API for Pen Registry.", description = "This API is related to processing of nominal roll.", version = "1"), security = {@SecurityRequirement(name = "OAUTH2", scopes = {"READ_NOMINAL_ROLL", "PROCESS_NOMINAL_ROLL"})})
public interface NominalRollApiEndpoint {

  @PostMapping
  @PreAuthorize("hasAuthority('SCOPE_NOMINAL_ROLL')")
  @ApiResponses(value = {@ApiResponse(responseCode = "201", description = "CREATED"), @ApiResponse(responseCode = "400", description = "BAD REQUEST")})
  @Transactional
  @Tag(name = "Endpoint to Upload an excel file and convert to json structure.", description = "Endpoint to Upload an excel file and convert to json structure")
  @Schema(name = "FileUpload", implementation = FileUpload.class)
  ResponseEntity<NominalRollFileProcessResponse> processNominalRollFile(@Validated @RequestBody FileUpload fileUpload, @RequestHeader(name = "correlationID") String correlationID);


  @PostMapping(URL.PROCESSING)
  @PreAuthorize("hasAuthority('SCOPE_NOMINAL_ROLL')")
  @ApiResponses(value = {@ApiResponse(responseCode = "202", description = "ACCEPTED")})
  @Transactional
  @Tag(name = "Endpoint to start processing of nominal roll students", description = "Endpoint to start processing of nominal roll students")
  @Schema(name = "NominalRollStudent", implementation = NominalRollStudent.class)
  ResponseEntity<Void> processNominalRollStudents(@Validated @RequestBody List<NominalRollStudent> nominalRollStudents, @RequestHeader(name = "correlationID") String correlationID);

  @GetMapping
  @PreAuthorize("hasAuthority('SCOPE_NOMINAL_ROLL')")
  @ApiResponses(value = {@ApiResponse(responseCode = "202", description = "ACCEPTED"), @ApiResponse(responseCode = "200", description = "OK"), @ApiResponse(responseCode = "404", description = "NOT FOUND")})
  @Transactional(readOnly = true)
  @Tag(name = "Endpoint to check if provided processing year nominal roll is already in progress", description = "Endpoint to check if provided processing year nominal roll is already in progress")
  ResponseEntity<Void> isBeingProcessed(@RequestParam(name = "processingYear") String processingYear);

  @GetMapping(URL.NOM_ROLL_STUDENT_ID)
  @PreAuthorize("hasAuthority('SCOPE_NOMINAL_ROLL')")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"), @ApiResponse(responseCode = "404", description = "NOT FOUND")})
  @Transactional(readOnly = true)
  @Tag(name = "Endpoint to get the  details of individual record in nominal roll", description = "Endpoint to get the  details of individual record in nominal roll")
  ResponseEntity<NominalRollStudent> getProcessingResultOfStudent(@PathVariable UUID nomRollStudentID);

  @DeleteMapping
  @PreAuthorize("hasAuthority('SCOPE_NOMINAL_ROLL')")
  @ApiResponses(value = {@ApiResponse(responseCode = "204", description = "NO CONTENT")})
  @Transactional
  @Tag(name = "Endpoint to Delete the entire data set from transient table for the processing year provided", description = "Endpoint to Delete the entire data set from transient table for the processing year provided")
  ResponseEntity<Void> deleteAll(@RequestParam(name = "processingYear") String processingYear);

  @GetMapping(URL.DUPLICATES)
  @PreAuthorize("hasAuthority('SCOPE_NOMINAL_ROLL')")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK")})
  @Transactional(readOnly = true)
  @Tag(name = "Endpoint to check for duplicate nominal roll students", description = "Endpoint to check for duplicate nominal roll students")
  ResponseEntity<Boolean> checkForDuplicateNominalRollStudents(@RequestHeader(name = "correlationID") String correlationID);

  /**
   * Find all completable future.
   *
   * @param pageNumber             the page number
   * @param pageSize               the page size
   * @param sortCriteriaJson       the sort criteria json
   * @param searchCriteriaListJson the search criteria list json
   * @return the completable future
   */
  @GetMapping(URL.PAGINATED)
  @Async
  @PreAuthorize("hasAuthority('SCOPE_NOMINAL_ROLL')")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"), @ApiResponse(responseCode = "500", description = "INTERNAL SERVER ERROR.")})
  @Transactional(readOnly = true)
  @Tag(name = "Endpoint to support data table view in frontend, with sort, filter and pagination.", description = "This API endpoint exposes flexible way to query the entity by leveraging JPA specifications.")
  CompletableFuture<Page<NominalRollStudent>> findAll(@RequestParam(name = "pageNumber", defaultValue = "0") Integer pageNumber,
                                                      @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize,
                                                      @RequestParam(name = "sort", defaultValue = "") String sortCriteriaJson,
                                                      @RequestParam(name = "searchCriteriaList", required = false) String searchCriteriaListJson);
  @PostMapping(URL.VALIDATE)
  @PreAuthorize("hasAuthority('SCOPE_NOMINAL_ROLL')")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK")})
  @Transactional
  @Tag(name = "Endpoint to validate the given nominal roll student", description = "Endpoint to validate the given nominal roll student")
  @Schema(name = "NominalRollStudent", implementation = NominalRollStudent.class)
  ResponseEntity<NominalRollStudent> validateNomRollStudent(NominalRollStudent nominalRollStudent);
}
