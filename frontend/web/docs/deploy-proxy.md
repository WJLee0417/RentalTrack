# 배포본 API 프록시

`vercel.json` 의 `rewrites` 첫 줄이 왜 있는지 적어 둡니다. **그 파일에는 주석을 못 답니다** —
Vercel 이 스키마를 엄격히 검사해서 `_comment` 같은 임의 필드가 있으면 배포가 거부됩니다.

```json
{ "source": "/api/(.*)", "destination": "http://i15a201.p.ssafy.io:8080/api/$1" }
```

## 왜 프록시가 필요한가

배포본은 **HTTPS**(Vercel)인데 백엔드는 **평문 HTTP**(8080)입니다. 브라우저는 HTTPS 페이지가
HTTP 로 나가는 요청을 차단합니다(mixed content). `fetch` 는 예외를 허용하는 UI 조차 없어서
사용자가 무시할 수도 없습니다.

그래서 `VITE_API_BASE_URL` 을 절대 URL(`http://i15a201…:8080/…`)로 두는 방법은 **영원히
동작하지 않습니다.** 상대 경로(`/api/v1/admin`)로 두고 Vercel 이 서버 대 서버로 중계해야
합니다 — HTTP 구간이 브라우저 밖으로 나가면 정책이 적용되지 않습니다.

로컬 개발에서 `vite.config.ts` 의 `server.proxy` 가 하는 일과 같습니다. `server.proxy` 는
dev 서버 기능이라 배포본에는 없어서, 같은 역할을 `rewrites` 가 맡습니다.

## CORS 는 이걸로 안 풀립니다

프록시를 쓰면 **브라우저 입장에서는** 같은 출처라 CORS 가 안 걸립니다. 그런데 CORS 는 두
겹입니다.

| | 상태 |
| --- | --- |
| 브라우저가 응답 읽기를 막는 것 | 프록시로 해결됨 |
| **서버가 `Origin` 헤더를 보고 거절하는 것** | **해결 안 됨** |

브라우저는 같은 출처라도 POST·PATCH 에 `Origin` 을 붙이고, Vercel 이 그걸 그대로 백엔드에
전달합니다. Spring `CorsFilter` 는 브라우저 사정을 모르고 헤더만 보므로, 허용 목록에 없으면
403 입니다.

실측(EC2 직접 호출):

```
POST /api/v1/admin/auth/login   Origin 없음                → 200
POST /api/v1/admin/auth/login   Origin: …vercel.app        → 403 Invalid CORS request
POST /api/v1/admin/auth/login   Origin: http://localhost:5173 → 200
```

목록에 있는 출처는 통과하고 없는 출처만 막힙니다. 그래서 **배포 도메인을 백엔드 허용 목록에
넣어야** 합니다.

```yaml
# backend/docker-compose.yml — app(Spring) 서비스
ADMIN_CORS_ALLOWED_ORIGINS: "http://localhost:3000,http://localhost:5173,https://web-sigma-two-20.vercel.app"
```

`application.yaml` 은 이미 이 환경변수를 읽게 돼 있어(`${ADMIN_CORS_ALLOWED_ORIGINS:…}`)
코드 수정은 필요 없습니다. `docker-compose.yml` 에 값만 넣으면 됩니다.

## 배포 도메인이 바뀌면

`rewrites` 의 목적지와 백엔드 허용 목록 **둘 다** 고쳐야 합니다. 한쪽만 고치면 403 이거나
연결 실패입니다.

Vercel 미리보기 URL(`web-<해시>-…vercel.app`)은 배포마다 도메인이 달라서 허용 목록에 넣을
수 없고, Deployment Protection 때문에 `/api/*` 까지 SSO 로 막힙니다. **검증은 고정 주소
(`web-sigma-two-20.vercel.app`)로 하세요.**

## 나중에 없애려면

백엔드에 HTTPS 가 붙으면(nginx·Caddy·CloudFront 등) 이 rewrite 는 필요 없어집니다.
`VITE_API_BASE_URL` 을 `https://…/api/v1/admin` 으로 두고 이 규칙을 지우면 됩니다.
그때도 CORS 허용 목록은 그대로 필요합니다.
