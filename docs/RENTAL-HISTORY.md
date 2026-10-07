# User Rental History

## API

```text
GET /api/v1/members/{memberId}/rentals
```

Query parameters:

```text
status  optional
page    default 0
size    default 20, max 100
```

예:

```text
GET /api/v1/members/1/rentals?page=0&size=20
GET /api/v1/members/1/rentals?status=COMPLETED&page=0&size=10
```

## 정렬

대여 신청 시각 기준 최신순으로 반환합니다.

동일 시각에서는 `rental_contract.id DESC`를 보조 정렬 기준으로 사용합니다.

## 응답 항목

각 이력은 다음 정보를 포함합니다.

- 계약 식별자 / 계약 코드
- 상품 식별자 / 상품명
- 상품 카테고리
- 인수 지점
- 반납 지점
- 계약 상태
- 신청 / 인수 / 이용 시작 / 반납 / 완료 시각

전체 상세 계약 응답과 달리 이력 목록에 필요한 필드만 별도 DTO로 제공합니다.

## 페이지 응답

```text
content
page
size
totalElements
totalPages
hasNext
hasPrevious
```

회원이 존재하지 않으면 404, 잘못된 page/size/status는 400으로 처리합니다.
