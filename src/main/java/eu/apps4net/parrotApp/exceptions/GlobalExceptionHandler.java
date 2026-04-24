package eu.apps4net.parrotApp.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.ZonedDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {

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
