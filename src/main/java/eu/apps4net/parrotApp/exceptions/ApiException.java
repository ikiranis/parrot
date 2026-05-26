package eu.apps4net.parrotApp.exceptions;

import org.springframework.http.HttpStatus;

import java.time.ZonedDateTime;

/**
 * Immutable record representing an API error response body.
 * Returned by {@link GlobalExceptionHandler} for all handled exceptions.
 *
 * @param status        the numeric HTTP status code
 * @param message       a human-readable error description
 * @param throwable     the originating exception
 * @param httpStatus    the {@link HttpStatus} enum value
 * @param zonedDateTime the timestamp when the error occurred
 */
public record ApiException(
		int status,
		String message,
		Throwable throwable,
		HttpStatus httpStatus,
		ZonedDateTime zonedDateTime) {

	@Override
	public String toString() {
		return "ApiException{" +
				"status=" + status +
				", message='" + message + '\'' +
				", throwable=" + throwable +
				", httpStatus=" + httpStatus +
				", zonedDateTime=" + zonedDateTime +
				'}';
	}
}
