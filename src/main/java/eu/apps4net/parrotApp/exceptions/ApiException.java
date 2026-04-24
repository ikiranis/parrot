package eu.apps4net.parrotApp.exceptions;

import org.springframework.http.HttpStatus;

import java.time.ZonedDateTime;

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
