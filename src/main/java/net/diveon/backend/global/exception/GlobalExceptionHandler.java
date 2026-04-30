package net.diveon.backend.global.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;


// 수정사항 2024.04.18 - 안상완, 코드가 들여쓰기가 왜인지는 모르겠는데 다른곳보다 조금씩 큰데,
// 그래서 반칸씩 떨어져 있던것을, 한칸씩 들여쓰기에 맞게 수정함.
@RestControllerAdvice
public class GlobalExceptionHandler {

        // 아이디/비밀번호 불일치 → 401
        @ExceptionHandler(InvalidCredentialsException.class)
        public ResponseEntity<ErrorResponse> handleInvalidCredentials(
                InvalidCredentialsException ex, HttpServletRequest request) {
                ErrorResponse body = new ErrorResponse(
                        "https://diveon.net/problems/invalid-credentials",
                        "Unauthorized",
                        401,
                        ex.getMessage(),
                        request.getRequestURI()
                );
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
        }

        // 유저 없음 → 404
        @ExceptionHandler(UserNotFoundException.class)
        public ResponseEntity<ErrorResponse> handleUserNotFound(
                UserNotFoundException ex, HttpServletRequest request) {
                ErrorResponse body = new ErrorResponse(
                        "https://diveon.net/problems/user-not-found",
                        "Not Found",
                        404,
                        ex.getMessage(),
                        request.getRequestURI()
                );
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
        }

        // 필수 파라미터 누락 → 400
        @ExceptionHandler(MethodArgumentNotValidException.class)
        public ResponseEntity<ErrorResponse> handleValidation(
                MethodArgumentNotValidException ex, HttpServletRequest request) {
                ErrorResponse body = new ErrorResponse(
                        "https://diveon.net/problems/bad-request",
                        "Bad Request",
                        400,
                        "필수 파라미터가 누락되었습니다.",
                        request.getRequestURI()
                );
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
        }

        // 409 Conflict: 서버의 현재 상태와 요청이 충돌할 때 사용합니다. (예: 중복된 아이디 가입)
        @ExceptionHandler(UserAlreadyExistException.class)
        public ResponseEntity<ErrorResponse> handleUserAlreadyExist(
                UserAlreadyExistException ex, HttpServletRequest request) {
                ErrorResponse body = new ErrorResponse(
                        "https://diveon.net/problems/conflict/user-already-exist",
                        "Conflict",
                        409,
                        ex.getMessage(),
                        request.getRequestURI()
                );
                return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
        }

        // 댓글을 찾을 수 없음 → 404
        @ExceptionHandler(CommentNotFoundException.class)
        public ResponseEntity<ErrorResponse> handleCommentNotFound(
                CommentNotFoundException ex, HttpServletRequest request) {
                ErrorResponse body = new ErrorResponse(
                        "https://diveon.net/problems/comment-not-found",
                        "Not Found",
                        404,
                        ex.getMessage(),
                        request.getRequestURI()
                );
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
        }

        // 문제를 찾을 수 없음 → 404
        @ExceptionHandler(ProblemNotFoundException.class)
        public ResponseEntity<ErrorResponse> handleProblemNotFound(
                ProblemNotFoundException ex, HttpServletRequest request) {
                ErrorResponse body = new ErrorResponse(
                        "https://diveon.net/problems/problem-not-found",
                        "Not Found",
                        404,
                        ex.getMessage(),
                        request.getRequestURI()
                );
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
        }

        // 유효하지 않은 문제 유형 → 400
        @ExceptionHandler(InvalidProblemTypeException.class)
        public ResponseEntity<ErrorResponse> handleInvalidProblemType(
                InvalidProblemTypeException ex, HttpServletRequest request) {
                ErrorResponse body = new ErrorResponse(
                        "https://diveon.net/problems/invalid-problem-type",
                        "Bad Request",
                        400,
                        ex.getMessage(),
                        request.getRequestURI()
                );
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
        }

        // 유효하지 않은 테스트케이스 → 422
        @ExceptionHandler(InvalidTestCaseException.class)
        public ResponseEntity<ErrorResponse> handleInvalidTestCase(
                InvalidTestCaseException ex, HttpServletRequest request) {
                ErrorResponse body = new ErrorResponse(
                        "https://diveon.net/problems/invalid-testcase",
                        "Unprocessable Entity",
                        422,
                        ex.getMessage(),
                        request.getRequestURI()
                );
                return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(body);
        }

        // 이미 실행 중인 VM 있음 → 409
        @ExceptionHandler(VmAlreadyExistsException.class)
        public ResponseEntity<ErrorResponse> handleVmAlreadyExists(
                VmAlreadyExistsException ex, HttpServletRequest request) {
                ErrorResponse body = new ErrorResponse(
                        "https://diveon.net/problems/conflict/vm-already-exists",
                        "Conflict",
                        409,
                        ex.getMessage(),
                        request.getRequestURI()
                );
                return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
        }

        // VM을 찾을 수 없음 → 404
        @ExceptionHandler(VmNotFoundException.class)
        public ResponseEntity<ErrorResponse> handleVmNotFound(
                VmNotFoundException ex, HttpServletRequest request) {
                ErrorResponse body = new ErrorResponse(
                        "https://diveon.net/problems/vm-not-found",
                        "Not Found",
                        404,
                        ex.getMessage(),
                        request.getRequestURI()
                );
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
        }

        // 본인 VM이 아닌 경우 → 403
        @ExceptionHandler(VmAccessDeniedException.class)
        public ResponseEntity<ErrorResponse> handleVmAccessDenied(
                VmAccessDeniedException ex, HttpServletRequest request) {
                ErrorResponse body = new ErrorResponse(
                        "https://diveon.net/problems/vm-access-denied",
                        "Forbidden",
                        403,
                        ex.getMessage(),
                        request.getRequestURI()
                );
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
        }

        // 이미지 준비 안 됨 → 400
        @ExceptionHandler(ImageNotReadyException.class)
        public ResponseEntity<ErrorResponse> handleImageNotReady(
                ImageNotReadyException ex, HttpServletRequest request) {
                ErrorResponse body = new ErrorResponse(
                        "https://diveon.net/problems/vm/image-not-ready",
                        "Bad Request",
                        400,
                        ex.getMessage(),
                        request.getRequestURI()
                );
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
        }
}
