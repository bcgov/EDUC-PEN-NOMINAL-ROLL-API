package ca.bc.gov.educ.pen.nominalroll.api.processor;

import ca.bc.gov.educ.pen.nominalroll.api.constants.FileTypes;
import ca.bc.gov.educ.pen.nominalroll.api.exception.FileUnProcessableException;
import ca.bc.gov.educ.pen.nominalroll.api.struct.v1.NominalRollFileProcessResponse;

public interface FileProcessor {
  NominalRollFileProcessResponse processFile(byte[] fileContents, String correlationID) throws FileUnProcessableException;

  FileTypes getFileType();
}
