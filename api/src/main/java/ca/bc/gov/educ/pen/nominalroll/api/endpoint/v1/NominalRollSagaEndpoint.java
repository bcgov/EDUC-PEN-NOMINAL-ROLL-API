package ca.bc.gov.educ.pen.nominalroll.api.endpoint.v1;

import ca.bc.gov.educ.pen.nominalroll.api.constants.v1.URL;
import ca.bc.gov.educ.pen.nominalroll.api.struct.v1.NominalRollPostSagaData;
import ca.bc.gov.educ.pen.nominalroll.api.struct.v1.Saga;
import ca.bc.gov.educ.pen.nominalroll.api.struct.v1.SagaEvent;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * The interface Pen services saga endpoint.
 */
@RequestMapping(URL.BASE_URL + URL.SAGA)
public interface NominalRollSagaEndpoint {


  /**
   * Nominal Roll Post response entity.
   *
   * @param nominalRollPostSagaData the nominal roll post saga data
   * @return the response entity
   */
  @PostMapping("/post-data")
  @PreAuthorize("hasAuthority('SCOPE_NOMINAL_ROLL_POST_DATA_SAGA')")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK."), @ApiResponse(responseCode = "409", description = "Conflict.")})
  @Transactional
  @Tag(name = "Endpoint to start post data saga.", description = "post data saga")
  @Schema(name = "NominalRollPostSagaData", implementation = NominalRollPostSagaData.class)
  ResponseEntity<String> postNominalRoll(@Validated @RequestBody NominalRollPostSagaData nominalRollPostSagaData);

  @GetMapping("/{sagaID}")
  @PreAuthorize("hasAuthority('SCOPE_NOMINAL_ROLL_READ_SAGA')")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK."), @ApiResponse(responseCode = "404", description = "Not Found.")})
  @Transactional(readOnly = true)
  @Tag(name = "Endpoint to retrieve saga by its ID (GUID).", description = "Endpoint to retrieve saga by its ID (GUID).")
  ResponseEntity<Saga> readSaga(@PathVariable UUID sagaID);

  /**
   * Find all Sagas for given search criteria.
   *
   * @param pageNumber             the page number
   * @param pageSize               the page size
   * @param sortCriteriaJson       the sort criteria json
   * @param searchCriteriaListJson the search list , the JSON string ( of Array or List of {@link ca.bc.gov.educ.pen.nominalroll.api.struct.v1.Search})
   * @return the completable future Page {@link Saga}
   */
  @GetMapping("/paginated")
  @PreAuthorize("hasAuthority('SCOPE_NOMINAL_ROLL_READ_SAGA')")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"), @ApiResponse(responseCode = "500", description = "INTERNAL SERVER ERROR.")})
  @Transactional(readOnly = true)
  @Tag(name = "Endpoint to support data table view in frontend, with sort, filter and pagination, for Sagas.", description = "This API endpoint exposes flexible way to query the entity by leveraging JPA specifications.")
  CompletableFuture<Page<Saga>> findAllSagas(@RequestParam(name = "pageNumber", defaultValue = "0") Integer pageNumber,
                                             @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize,
                                             @RequestParam(name = "sort", defaultValue = "") String sortCriteriaJson,
                                             @ArraySchema(schema = @Schema(name = "searchCriteriaList",
                                               description = "searchCriteriaList if provided should be a JSON string of Search Array",
                                               implementation = ca.bc.gov.educ.pen.nominalroll.api.struct.v1.Search.class))
                                             @RequestParam(name = "searchCriteriaList", required = false) String searchCriteriaListJson);

  @GetMapping("/{sagaId}/saga-events")
  @PreAuthorize("hasAuthority('SCOPE_NOMINAL_ROLL_READ_SAGA')")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK."), @ApiResponse(responseCode = "404", description = "Not Found.")})
  @Transactional(readOnly = true)
  @Tag(name = "Endpoint to retrieve all saga events by its ID (GUID).", description = "Endpoint to retrieve all saga events by its ID (GUID).")
  ResponseEntity<List<SagaEvent>> getSagaEventsBySagaID(@PathVariable UUID sagaId);

  @PutMapping("/{sagaId}")
  @PreAuthorize("hasAuthority('SCOPE_NOMINAL_ROLL_WRITE_SAGA')")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK."), @ApiResponse(responseCode = "404", description = "Not Found."), @ApiResponse(responseCode = "409", description = "Conflict.")})
  @Transactional
  @Tag(name = "Endpoint to update saga by its ID.", description = "Endpoint to update saga by its ID.")
  ResponseEntity<Saga> updateSaga(@RequestBody Saga saga, @PathVariable UUID sagaId);

}
