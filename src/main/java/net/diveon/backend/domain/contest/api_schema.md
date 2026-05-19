# 개요
API 세부 명세서를 보관


# API 명세서 내역

### 대회 문제 기본
### 1. 대회 문제 목록 조회

'챌린지 탭'에서 대회에 등록된 문제 리스트를 보여줍니다.

- **Method / Endpoint**: `GET /api/contests/{contestId}/problems`
- **Authorization**: Bearer Token
- **Response (200 OK)**:

```json
{
  "status": 200,
  "message": "문제 목록 조회 성공",
  "data": {
    "problems": [
      {
        "id": "F-01",              // 화면 표시용 (예: 카테고리 이니셜+번호)
        "problemId": 105,          // DB의 실제 문제 PK (수정/삭제 시 필요)
        "title": "삭제된 파일 복구",
        "points": 100,
        "solvedCount": 85,
        "category": "Disk",
        "isSolved": true           // 현재 접속자가 이 문제를 풀었는지 여부
      },
      {
        "id": "F-02",
        "problemId": 106,
        "title": "메모리 덤프 분석",
        "points": 300,
        "solvedCount": 0,
        "category": "Memory",
        "isSolved": false
      }
    ]
  }
}
```

---

### 2. 문제 점수 설정 (관리자 전용)

관리자가 특정 문제의 배점을 수정할 때 호출합니다.

- **Method / Endpoint**: `PATCH /api/contests/{contestId}/problems/{problemId}/points`
- **Authorization**: Bearer Token (관리자 권한 확인 필수)
- **Request Body**:

```json
{
  "points": 500
}
```

- **Response (200 OK)**:

```json
{
  "status": 200,
  "message": "문제 배점이 수정되었습니다."
}
```


---

### 3. 문제 제거 (관리자)

- **Endpoint**: `DELETE /api/contests/{contestId}/problems/{problemId}`
- **Authorization**: Bearer Token (관리자 권한)

**Response (200 OK)**:

```json
{
  "status": 200,
  "message": "문제가 대회에서 제거되었습니다."
}
```

---

### 참가자 관리 탭 API (관리자 전용)

### 4. 참가자 목록 조회

- **Method / Endpoint**: `GET /api/contests/{contestId}/participants`
- **Authorization**: Bearer Token (관리자 권한)
- **Query Params**: `?page=1&size=20` (필요 시 검색어 `keyword` 추가 가능)
- **Response (200 OK)**:

```json
{
  "status": 200,
  "data": {
    "totalElements": 128,
    "totalPages": 7,
    "participants": [
      {
        "userId": 1001,
        "nickname": "Dankook_Hacker",
        "joinedAt": "2026.05.10T10:00:00",
        "score": 2850,
        "isBanned": false
      },
      {
        "userId": 1002,
        "nickname": "Bad_User_01",
        "joinedAt": "2026.05.12T11:20:00",
        "score": 100,
        "isBanned": true
      }
    ]
  }
}
```

### 5. 참가자 밴(실격) 및 해제 처리

하나의 API로 `isBanned` 값을 true/false로 넘겨 상태를 토글(Toggle)합니다.

- **Method / Endpoint**: `PATCH /api/contests/{contestId}/participants/{userId}/ban`
- **Authorization**: Bearer Token (관리자 권한)
- **Request Body**:

```json
{
  "isBanned": true  // 밴 할 때는 true, 해제할 때는 false
}
```

- **Response (200 OK)**:

```json
{
  "status": 200,
  "message": "해당 참가자의 상태가 변경되었습니다."
}
```