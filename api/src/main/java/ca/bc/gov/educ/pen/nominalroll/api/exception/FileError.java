package ca.bc.gov.educ.pen.nominalroll.api.exception;

import lombok.Getter;

/**
 * The enum File error.
 *
 * @author OM
 */
public enum FileError {
  FILE_ENCRYPTED("File is password protected"),

  NO_HEADING("Heading row is missing"),

  BLANK_CELL_IN_HEADING_ROW("Heading row has a blank cell at column $?"),

  MISSING_MANDATORY_HEADER("Missing required header $?"),

  INVALID_VALUE_FOR_FIELD("Invalid value provided at row $? and field $?. Expected is $? and got $?");

  @Getter
  private final String message;

  /**
   * Instantiates a new File error.
   *
   * @param message the message
   */
  FileError(final String message) {
    this.message = message;
  }
}
