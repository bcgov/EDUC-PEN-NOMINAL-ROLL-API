package ca.bc.gov.educ.pen.nominalroll.api.constants;

import lombok.Getter;

public enum FileTypes {
  XLSX("xlsx"),
  XLS("xls"),
  CSV("csv");

  @Getter
  private final String code;

  FileTypes(final String fileType) {
    this.code = fileType;
  }
}
