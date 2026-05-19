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

        // 문제 삭제 권한 없음 → 403
        @ExceptionHandler(ProblemDeletePermisionDeny.class)
        public ResponseEntity<ErrorResponse> handleProblemDeletePermisionDeny(
                ProblemDeletePermisionDeny ex, HttpServletRequest request) {
                ErrorResponse body = new ErrorResponse(
                        "https://diveon.net/problems/problem-delete-permision-deny",
                        "Forbidden",
                        403,
                        ex.getMessage(),
                        request.getRequestURI()
                );
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
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

        // 제출 없음 → 404
        @ExceptionHandler(SubmissionNotFoundException.class)
        public ResponseEntity<ErrorResponse> handleSubmissionNotFound(
                SubmissionNotFoundException ex, HttpServletRequest request) {
                ErrorResponse body = new ErrorResponse(
                        "https://diveon.net/problems/submission-not-found",
                        "Not Found",
                        404,
                        ex.getMessage(),
                        request.getRequestURI()
                );
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
        }

        // 본인 제출 아님 → 403
        @ExceptionHandler(SubmissionAccessDeniedException.class)
        public ResponseEntity<ErrorResponse> handleSubmissionAccessDenied(
                SubmissionAccessDeniedException ex, HttpServletRequest request) {
                ErrorResponse body = new ErrorResponse(
                        "https://diveon.net/problems/submission-access-denied",
                        "Forbidden",
                        403,
                        ex.getMessage(),
                        request.getRequestURI()
                );
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
        }

        // 그룹 없음 → 404
        @ExceptionHandler(GroupNotFoundException.class)
        public ResponseEntity<ErrorResponse> handleGroupNotFound(
                GroupNotFoundException ex, HttpServletRequest request) {
                ErrorResponse body = new ErrorResponse(
                        "https://diveon.net/problems/group-not-found",
                        "Not Found",
                        404,
                        ex.getMessage(),
                        request.getRequestURI()
                );
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
        }

        // 그룹 유저 없음 → 404
        @ExceptionHandler(GroupUserNotFoundException.class)
        public ResponseEntity<ErrorResponse> handleGroupUserNotFound(
                GroupUserNotFoundException ex, HttpServletRequest request) {
                ErrorResponse body = new ErrorResponse(
                        "https://diveon.net/problems/group-user-not-found",
                        "Not Found",
                        404,
                        ex.getMessage(),
                        request.getRequestURI()
                );
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
        }

        // 그룹장 탈퇴 불가 → 403
        @ExceptionHandler(GroupLeaderCannotLeaveException.class)
        public ResponseEntity<ErrorResponse> handleGroupLeaderCannotLeave(
                GroupLeaderCannotLeaveException ex, HttpServletRequest request) {
                ErrorResponse body = new ErrorResponse(
                        "https://diveon.net/problems/group-leader-cannot-leave",
                        "Forbidden",
                        403,
                        ex.getMessage(),
                        request.getRequestURI()
                );
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
        }

        // 그룹장 권한 없음 → 403
        @ExceptionHandler(GroupLeaderPermissionDeniedException.class)
        public ResponseEntity<ErrorResponse> handleGroupLeaderPermissionDenied(
                GroupLeaderPermissionDeniedException ex, HttpServletRequest request) {
                ErrorResponse body = new ErrorResponse(
                        "https://diveon.net/problems/group-leader-permission-denied",
                        "Forbidden",
                        403,
                        ex.getMessage(),
                        request.getRequestURI()
                );
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
        }

        // 가입 신청 철회 불가 → 409
        @ExceptionHandler(GroupAssignRequestNotPendingException.class)
        public ResponseEntity<ErrorResponse> handleGroupAssignRequestNotPending(
                GroupAssignRequestNotPendingException ex, HttpServletRequest request) {
                ErrorResponse body = new ErrorResponse(
                        "https://diveon.net/problems/conflict/group-assign-request-not-pending",
                        "Conflict",
                        409,
                        ex.getMessage(),
                        request.getRequestURI()
                );
                return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
        }

        // 그룹 최대 인원 초과 → 409
        @ExceptionHandler(GroupCapacityExceededException.class)
        public ResponseEntity<ErrorResponse> handleGroupCapacityExceeded(
                GroupCapacityExceededException ex, HttpServletRequest request) {
                ErrorResponse body = new ErrorResponse(
                        "https://diveon.net/problems/conflict/group-capacity-exceeded",
                        "Conflict",
                        409,
                        ex.getMessage(),
                        request.getRequestURI()
                );
                return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
        }

        // 이미 그룹에 추가된 문제 → 409
        @ExceptionHandler(GroupProblemAlreadyExistsException.class)
        public ResponseEntity<ErrorResponse> handleGroupProblemAlreadyExists(
                GroupProblemAlreadyExistsException ex, HttpServletRequest request) {
                ErrorResponse body = new ErrorResponse(
                        "https://diveon.net/problems/conflict/group-problem-already-exists",
                        "Conflict",
                        409,
                        ex.getMessage(),
                        request.getRequestURI()
                );
                return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
        }

        // 그룹 멤버 아님 → 403
        @ExceptionHandler(GroupAccessDeniedException.class)
        public ResponseEntity<ErrorResponse> handleGroupAccessDenied(
                GroupAccessDeniedException ex, HttpServletRequest request) {
                ErrorResponse body = new ErrorResponse(
                        "https://diveon.net/problems/group-access-denied",
                        "Forbidden",
                        403,
                        ex.getMessage(),
                        request.getRequestURI()
                );
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
        }

        // 채점 미완료 → 409
        @ExceptionHandler(SubmissionNotCompletedException.class)
        public ResponseEntity<ErrorResponse> handleSubmissionNotCompleted(
                SubmissionNotCompletedException ex, HttpServletRequest request) {
                ErrorResponse body = new ErrorResponse(
                        "https://diveon.net/problems/submission-not-completed",
                        "Conflict",
                        409,
                        ex.getMessage(),
                        request.getRequestURI()
                );
                return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
        }

        // 대회 없음 → 404
        @ExceptionHandler(ContestNotFoundException.class)
        public ResponseEntity<ErrorResponse> handleContestNotFound(
                ContestNotFoundException ex, HttpServletRequest request) {
                ErrorResponse body = new ErrorResponse(
                        "https://diveon.net/problems/contest-not-found",
                        "Not Found",
                        404,
                        ex.getMessage(),
                        request.getRequestURI()
                );
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
        }

        // 대회 권한 없음 → 403
        @ExceptionHandler(ContestAccessDeniedException.class)
        public ResponseEntity<ErrorResponse> handleContestAccessDenied(
                ContestAccessDeniedException ex, HttpServletRequest request) {
                ErrorResponse body = new ErrorResponse(
                        "https://diveon.net/problems/contest-access-denied",
                        "Forbidden",
                        403,
                        ex.getMessage(),
                        request.getRequestURI()
                );
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
        }

        // 대회 문제 없음 → 404
        @ExceptionHandler(ContestProblemNotFoundException.class)
        public ResponseEntity<ErrorResponse> handleContestProblemNotFound(
                ContestProblemNotFoundException ex, HttpServletRequest request) {
                ErrorResponse body = new ErrorResponse(
                        "https://diveon.net/problems/contest-problem-not-found",
                        "Not Found",
                        404,
                        ex.getMessage(),
                        request.getRequestURI()
                );
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
        }

        // 대회 참가자 없음 → 404
        @ExceptionHandler(ContestParticipantNotFoundException.class)
        public ResponseEntity<ErrorResponse> handleContestParticipantNotFound(
                ContestParticipantNotFoundException ex, HttpServletRequest request) {
                ErrorResponse body = new ErrorResponse(
                        "https://diveon.net/problems/contest-participant-not-found",
                        "Not Found",
                        404,
                        ex.getMessage(),
                        request.getRequestURI()
                );
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
        }

        // 이미 참가한 대회 → 409
        @ExceptionHandler(ContestAlreadyParticipatedException.class)
        public ResponseEntity<ErrorResponse> handleContestAlreadyParticipated(
                ContestAlreadyParticipatedException ex, HttpServletRequest request) {
                ErrorResponse body = new ErrorResponse(
                        "https://diveon.net/problems/conflict/contest-already-participated",
                        "Conflict",
                        409,
                        ex.getMessage(),
                        request.getRequestURI()
                );
                return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
        }

        // 진행 중인 대회 아님 → 409
        @ExceptionHandler(ContestNotOngoingException.class)
        public ResponseEntity<ErrorResponse> handleContestNotOngoing(
                ContestNotOngoingException ex, HttpServletRequest request) {
                ErrorResponse body = new ErrorResponse(
                        "https://diveon.net/problems/conflict/contest-not-ongoing",
                        "Conflict",
                        409,
                        ex.getMessage(),
                        request.getRequestURI()
                );
                return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
        }

        // Q&A 없음 → 404
        @ExceptionHandler(ContestQnaNotFoundException.class)
        public ResponseEntity<ErrorResponse> handleContestQnaNotFound(
                ContestQnaNotFoundException ex, HttpServletRequest request) {
                ErrorResponse body = new ErrorResponse(
                        "https://diveon.net/problems/contest-qna-not-found",
                        "Not Found",
                        404,
                        ex.getMessage(),
                        request.getRequestURI()
                );
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
        }

        // 공지사항 없음 → 404
        @ExceptionHandler(ContestNoticeNotFoundException.class)
        public ResponseEntity<ErrorResponse> handleContestNoticeNotFound(
                ContestNoticeNotFoundException ex, HttpServletRequest request) {
                ErrorResponse body = new ErrorResponse(
                        "https://diveon.net/problems/contest-notice-not-found",
                        "Not Found",
                        404,
                        ex.getMessage(),
                        request.getRequestURI()
                );
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
        }

        // 이미 시작된 대회 참가/취소 불가 → 409
        @ExceptionHandler(ContestAlreadyStartedException.class)
        public ResponseEntity<ErrorResponse> handleContestAlreadyStarted(
                ContestAlreadyStartedException ex, HttpServletRequest request) {
                ErrorResponse body = new ErrorResponse(
                        "https://diveon.net/problems/conflict/contest-already-started",
                        "Conflict",
                        409,
                        ex.getMessage(),
                        request.getRequestURI()
                );
                return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
        }
}
