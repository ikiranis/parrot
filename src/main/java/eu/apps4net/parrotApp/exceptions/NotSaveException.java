package eu.apps4net.parrotApp.exceptions;

/**
 * Thrown when a persistence operation fails to save an entity.
 * Maps to HTTP 400 via {@link GlobalExceptionHandler}.
 */
public class NotSaveException extends RuntimeException {

	/**
	 * Constructs a new {@code NotSaveException} with the given detail message.
	 *
	 * @param message the detail message
	 */
	public NotSaveException(String message) {
		super(message);
	}
}
