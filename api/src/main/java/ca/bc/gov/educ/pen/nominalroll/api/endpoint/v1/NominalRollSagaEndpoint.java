package ca.bc.gov.educ.pen.nominalroll.api.endpoint.v1;

import ca.bc.gov.educ.pen.nominalroll.api.constants.v1.URL;
import ca.bc.gov.educ.pen.nominalroll.api.struct.v1.NominalRollPostSagaData;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * The interface Pen services saga endpoint.
 */
@RequestMapping(URL.BASE_URL)
public interface NominalRollSagaEndpoint {


  /**
   * Nominal Roll Post response entity.
   *
   * @param nominalRollPostSagaData the nominal roll post saga data
   * @return the response entity
   */
  @PostMapping("/post-saga")
  @PreAuthorize("hasAuthority('SCOPE_NOMINAL_ROLL')")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK.")})
  ResponseEntity<String> postNominalRoll(@Validated @RequestBody NominalRollPostSagaData nominalRollPostSagaData);

}
