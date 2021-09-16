package ca.bc.gov.educ.pen.nominalroll.api.exception;

/**
 * The type Pen reg api runtime exception.
 */
public class NominalRollAPIRuntimeException extends RuntimeException {

  /**
   * The constant serialVersionUID.
   */
  private static final long serialVersionUID = 5241655513745148898L;

  /**
   * Instantiates a new Pen reg api runtime exception.
   *
   * @param message the message
   */
  public NominalRollAPIRuntimeException(String message) {
		super(message);
	}

  public NominalRollAPIRuntimeException(Throwable exception) {
    super(exception);
  }

}
