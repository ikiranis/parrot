package eu.apps4net.parrotApp.exceptions;

/**
 * Thrown when a requested resource cannot be found.
 * Maps to HTTP 404 via {@link GlobalExceptionHandler}.
 */
public class NotFoundException extends RuntimeException {

	/**
	 * Constructs a new {@code NotFoundException} with the given detail message.
	 *
	 * @param message the detail message
	 */
	public NotFoundException(String message) {
		super(message);
	}
}
