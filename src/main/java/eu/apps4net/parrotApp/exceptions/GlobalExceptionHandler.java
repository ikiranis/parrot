package eu.apps4net.parrotApp.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.ZonedDateTime;

/**
 * Global exception handler that converts application exceptions into
 * consistent {@link ApiException} JSON error responses.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

	/**
	 * Handles {@link NotFoundException} and returns a 404 response.
	 *
	 * @param e the exception
	 * @return a 404 {@link ResponseEntity} with an {@link ApiException} body
	 */
	@ExceptionHandler(value = {NotFoundException.class})
	public ResponseEntity<ApiException> handleApiException(NotFoundException e) {
		ApiException apiException = new ApiException(
				404,
				e.getMessage(),
				e,
				HttpStatus.NOT_FOUND,
				ZonedDateTime.now()
		);

		return new ResponseEntity<>(apiException, HttpStatus.NOT_FOUND);
	}

	/**
	 * Handles {@link ProcessingErrorException} and returns a 400 response.
	 *
	 * @param e the exception
	 * @return a 400 {@link ResponseEntity} with an {@link ApiException} body
	 */
	@ExceptionHandler(value = {ProcessingErrorException.class})
	public ResponseEntity<ApiException> handleApiException(ProcessingErrorException e) {
		ApiException apiException = new ApiException(
				500,
				e.getMessage(),
				e,
				HttpStatus.BAD_REQUEST,
				ZonedDateTime.now()
		);

		return new ResponseEntity<>(apiException, HttpStatus.BAD_REQUEST);
	}

	/**
	 * Handles {@link NotSaveException} and returns a 400 response.
	 *
	 * @param e the exception
	 * @return a 400 {@link ResponseEntity} with an {@link ApiException} body
	 */
	@ExceptionHandler(value = {NotSaveException.class})
	public ResponseEntity<ApiException> handleApiException(NotSaveException e) {
		ApiException apiException = new ApiException(
				500,
				e.getMessage(),
				e,
				HttpStatus.BAD_REQUEST,
				ZonedDateTime.now()
		);

		return new ResponseEntity<>(apiException, HttpStatus.BAD_REQUEST);
	}
}
