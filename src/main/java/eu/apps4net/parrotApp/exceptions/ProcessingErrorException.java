package eu.apps4net.parrotApp.exceptions;

/**
 * Thrown when a business-logic processing error occurs.
 * Maps to HTTP 400 via {@link GlobalExceptionHandler}.
 */
public class ProcessingErrorException extends RuntimeException {

	/**
	 * Constructs a new {@code ProcessingErrorException} with the given detail message.
	 *
	 * @param message the detail message
	 */
	public ProcessingErrorException(String message) {
		super(message);
	}
}
