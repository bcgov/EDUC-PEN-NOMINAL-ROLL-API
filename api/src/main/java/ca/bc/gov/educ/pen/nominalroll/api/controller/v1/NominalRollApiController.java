package ca.bc.gov.educ.pen.nominalroll.api.controller.v1;

import ca.bc.gov.educ.pen.nominalroll.api.constants.FileTypes;
import ca.bc.gov.educ.pen.nominalroll.api.endpoint.v1.NominalRollApiEndpoint;
import ca.bc.gov.educ.pen.nominalroll.api.processor.FileProcessor;
import ca.bc.gov.educ.pen.nominalroll.api.struct.v1.FileUpload;
import ca.bc.gov.educ.pen.nominalroll.api.struct.v1.NominalRollFileProcessResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static ca.bc.gov.educ.pen.nominalroll.api.constants.FileTypes.XLS;
import static ca.bc.gov.educ.pen.nominalroll.api.constants.FileTypes.XLSX;

@RestController
@Slf4j
public class NominalRollApiController implements NominalRollApiEndpoint {
  private final Map<FileTypes, FileProcessor> fileProcessorsMap;

  public NominalRollApiController(final List<FileProcessor> fileProcessors) {
    this.fileProcessorsMap = fileProcessors.stream().collect(Collectors.toMap(FileProcessor::getFileType, Function.identity()));
  }

  @Override
  public ResponseEntity<NominalRollFileProcessResponse> processNominalRollFile(final FileUpload fileUpload, final String correlationID) {
    NominalRollFileProcessResponse nominalRollFileProcessResponse = null;
    if (XLSX.getCode().equals(fileUpload.getFileExtension())) {
      nominalRollFileProcessResponse = this.fileProcessorsMap.get(XLSX).processFile(Base64.getDecoder().decode(fileUpload.getFileContents()), correlationID);
    } else if (XLS.getCode().equals(fileUpload.getFileExtension())) {
      nominalRollFileProcessResponse = this.fileProcessorsMap.get(XLS).processFile(Base64.getDecoder().decode(fileUpload.getFileContents()), correlationID);
    }
    if (nominalRollFileProcessResponse != null) {
      return ResponseEntity.ok(nominalRollFileProcessResponse);
    }
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
  }
}
