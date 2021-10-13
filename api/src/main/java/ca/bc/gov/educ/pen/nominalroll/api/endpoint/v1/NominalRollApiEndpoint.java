package ca.bc.gov.educ.pen.nominalroll.api.endpoint.v1;

import ca.bc.gov.educ.pen.nominalroll.api.constants.v1.URL;
import ca.bc.gov.educ.pen.nominalroll.api.struct.v1.FileUpload;
import ca.bc.gov.educ.pen.nominalroll.api.struct.v1.NominalRollFileProcessResponse;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static org.springframework.http.HttpStatus.CREATED;


@RequestMapping(URL.BASE_URL)
@OpenAPIDefinition(info = @Info(title = "API for Pen Registry.", description = "This API is related to processing of nominal roll.", version = "1"), security = {@SecurityRequirement(name = "OAUTH2", scopes = {"READ_NOMINAL_ROLL", "PROCESS_NOMINAL_ROLL"})})
public interface NominalRollApiEndpoint {

  @PostMapping
  @PreAuthorize("hasAuthority('SCOPE_NOMINAL_ROLL')")
  @ApiResponses(value = {@ApiResponse(responseCode = "201", description = "CREATED"), @ApiResponse(responseCode = "400", description = "BAD REQUEST")})
  @ResponseStatus(CREATED)
  @Transactional
  @Tag(name = "Endpoint to create Pen Request Batch Entity.", description = "Endpoint to upload a file to process nominal roll.")
  @Schema(name = "FileUpload", implementation = FileUpload.class)
  ResponseEntity<NominalRollFileProcessResponse> processNominalRollFile(@Validated @RequestBody FileUpload fileUpload, @RequestHeader(name = "correlationID") String correlationID);
}
