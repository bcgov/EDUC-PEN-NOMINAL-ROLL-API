package ca.bc.gov.educ.pen.nominalroll.api.processor.impl;

import ca.bc.gov.educ.pen.nominalroll.api.constants.FileTypes;
import ca.bc.gov.educ.pen.nominalroll.api.exception.FileUnProcessableException;
import ca.bc.gov.educ.pen.nominalroll.api.exception.NominalRollAPIRuntimeException;
import ca.bc.gov.educ.pen.nominalroll.api.struct.v1.NominalRollFileProcessResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

@Slf4j
@Service
public class ExcelFileProcessor extends BaseExcelProcessor {

  @Override
  public NominalRollFileProcessResponse processFile(final byte[] fileContents, final String correlationID) throws FileUnProcessableException {
    try {
      final File outputFile = this.getFile(fileContents, FileTypes.XLS.getCode());
      try (final POIFSFileSystem fs = new POIFSFileSystem(outputFile)) {
        try (final HSSFWorkbook wb = new HSSFWorkbook(fs.getRoot(), true)) {
          return this.processSheet(wb.getSheetAt(0), correlationID);
        }
      }
    } catch (final IOException | URISyntaxException e) {
      log.error("exception", e);
      throw new NominalRollAPIRuntimeException(e.getMessage());
    }
  }

  @Override
  public FileTypes getFileType() {
    return FileTypes.XLS;
  }
}
