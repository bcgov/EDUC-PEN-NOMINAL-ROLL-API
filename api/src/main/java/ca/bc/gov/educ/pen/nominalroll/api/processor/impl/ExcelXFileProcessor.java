package ca.bc.gov.educ.pen.nominalroll.api.processor.impl;

import ca.bc.gov.educ.pen.nominalroll.api.constants.FileTypes;
import ca.bc.gov.educ.pen.nominalroll.api.exception.FileUnProcessableException;
import ca.bc.gov.educ.pen.nominalroll.api.exception.NominalRollAPIRuntimeException;
import ca.bc.gov.educ.pen.nominalroll.api.struct.v1.NominalRollFileProcessResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

@Slf4j
@Service
public class ExcelXFileProcessor extends BaseExcelProcessor {

  @Override
  public NominalRollFileProcessResponse processFile(final byte[] fileContents, final String correlationID) throws FileUnProcessableException {
    try {
      final File outputFile = this.getFile(fileContents, FileTypes.XLSX.getCode());
      try (final OPCPackage pkg = OPCPackage.open(outputFile)) {
        try (final XSSFWorkbook wb = new XSSFWorkbook(pkg)) {
          return this.processSheet(wb.getSheetAt(0), correlationID);
        }
      }
    } catch (final IOException | InvalidFormatException | URISyntaxException e) {
      log.error("exception", e);
      throw new NominalRollAPIRuntimeException(e.getMessage());
    }
  }


  @Override
  public FileTypes getFileType() {
    return FileTypes.XLSX;
  }
}
