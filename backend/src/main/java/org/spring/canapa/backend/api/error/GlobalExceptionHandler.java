package org.spring.canapa.backend.api.error;

import jakarta.servlet.http.HttpServletRequest;
import org.spring.canapa.backend.room.exception.InvalidRoomDataException;
import org.spring.canapa.backend.room.exception.InvalidRoomStatusTransitionException;
import org.spring.canapa.backend.room.exception.RoomActionNotAllowedException;
import org.spring.canapa.backend.room.exception.RoomJoinNotAllowedException;
import org.spring.canapa.backend.room.exception.RoomNotFoundException;
import org.spring.canapa.backend.room.exception.RoomParticipantAlreadyExistsException;
import org.spring.canapa.backend.user.exception.DuplicateUserEmailException;
import org.spring.canapa.backend.user.exception.InvalidUserDataException;
import org.spring.canapa.backend.user.exception.UserNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Instant;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(InvalidUserDataException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidUserData(
            InvalidUserDataException exception,
            HttpServletRequest request
    ) {
        return error(
                ApiErrorCode.INVALID_REQUEST,
                exception.getMessage(),
                HttpStatus.BAD_REQUEST,
                request
        );
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleUserNotFound(
            UserNotFoundException exception,
            HttpServletRequest request
    ) {
        return error(
                ApiErrorCode.USER_NOT_FOUND,
                exception.getMessage(),
                HttpStatus.NOT_FOUND,
                request
        );
    }

    @ExceptionHandler(DuplicateUserEmailException.class)
    public ResponseEntity<ApiErrorResponse> handleDuplicateUserData(
            DuplicateUserEmailException exception,
            HttpServletRequest request
    ) {
        return error(
                ApiErrorCode.DUPLICATE_USER_DATA,
                exception.getMessage(),
                HttpStatus.CONFLICT,
                request
        );
    }

    @ExceptionHandler(InvalidRoomDataException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidRoomData(
            InvalidRoomDataException exception,
            HttpServletRequest request
    ) {
        return error(
                ApiErrorCode.INVALID_REQUEST,
                exception.getMessage(),
                HttpStatus.BAD_REQUEST,
                request
        );
    }

    @ExceptionHandler(RoomNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleRoomNotFound(
            RoomNotFoundException exception,
            HttpServletRequest request
    ) {
        return error(
                ApiErrorCode.ROOM_NOT_FOUND,
                exception.getMessage(),
                HttpStatus.NOT_FOUND,
                request
        );
    }

    @ExceptionHandler(RoomParticipantAlreadyExistsException.class)
    public ResponseEntity<ApiErrorResponse> handleRoomParticipantAlreadyExists(
            RoomParticipantAlreadyExistsException exception,
            HttpServletRequest request
    ) {
        return error(
                ApiErrorCode.ROOM_PARTICIPANT_ALREADY_EXISTS,
                exception.getMessage(),
                HttpStatus.CONFLICT,
                request
        );
    }

    @ExceptionHandler(RoomJoinNotAllowedException.class)
    public ResponseEntity<ApiErrorResponse> handleRoomJoinNotAllowed(
            RoomJoinNotAllowedException exception,
            HttpServletRequest request
    ) {
        return error(
                ApiErrorCode.ROOM_JOIN_NOT_ALLOWED,
                exception.getMessage(),
                HttpStatus.CONFLICT,
                request
        );
    }

    @ExceptionHandler(InvalidRoomStatusTransitionException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidRoomStatusTransition(
            InvalidRoomStatusTransitionException exception,
            HttpServletRequest request
    ) {
        return error(
                ApiErrorCode.INVALID_ROOM_STATUS_TRANSITION,
                exception.getMessage(),
                HttpStatus.CONFLICT,
                request
        );
    }

    @ExceptionHandler(RoomActionNotAllowedException.class)
    public ResponseEntity<ApiErrorResponse> handleRoomActionNotAllowed(
            RoomActionNotAllowedException exception,
            HttpServletRequest request
    ) {
        return error(
                ApiErrorCode.ROOM_ACTION_NOT_ALLOWED,
                exception.getMessage(),
                HttpStatus.FORBIDDEN,
                request
        );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleUnreadableMessage(
            HttpMessageNotReadableException exception,
            HttpServletRequest request
    ) {
        return error(
                ApiErrorCode.INVALID_REQUEST,
                "Request body is missing or malformed",
                HttpStatus.BAD_REQUEST,
                request
        );
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException exception,
            HttpServletRequest request
    ) {
        return error(
                ApiErrorCode.INVALID_REQUEST,
                "Invalid value for " + exception.getName(),
                HttpStatus.BAD_REQUEST,
                request
        );
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ApiErrorResponse> handleMissingRequestHeader(
            MissingRequestHeaderException exception,
            HttpServletRequest request
    ) {
        return error(
                ApiErrorCode.INVALID_REQUEST,
                "Required request header is missing: " + exception.getHeaderName(),
                HttpStatus.BAD_REQUEST,
                request
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        return error(
                ApiErrorCode.INVALID_REQUEST,
                "Request validation failed",
                HttpStatus.BAD_REQUEST,
                request
        );
    }

    private ResponseEntity<ApiErrorResponse> error(
            ApiErrorCode code,
            String message,
            HttpStatus status,
            HttpServletRequest request
    ) {
        return ResponseEntity.status(status).body(new ApiErrorResponse(
                code,
                message,
                status.value(),
                request.getRequestURI(),
                Instant.now()
        ));
    }
}
