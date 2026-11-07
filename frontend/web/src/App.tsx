import { useEffect, useState, type FormEvent, type ReactNode } from "react";
import {
  api,
  type Auth,
  type Item,
  type Business,
  type Code,
  type Rental,
  type Detail,
  type Condition,
  type Photo,
  type Damage,
} from "./api";
const value = (f: HTMLFormElement, k: string) =>
  String(new FormData(f).get(k) || "").trim();
const dt = (s: string) => (s.length === 16 ? s + ":00" : s);
const names: Record<string, string> = {
  CUSTOMER: "일반회원",
  INSPECTOR: "검수담당자",
  ADMIN: "관리자",
  REQUESTED: "신청",
  RENTED: "대여 중",
  RETURNED: "반납",
  INSPECTED: "검수 완료",
  CANCELED: "취소",
  AVAILABLE: "대여 가능",
  INSPECTION: "점검 중",
  REPAIR: "수리 중",
  INACTIVE: "운영 중지",
  INTAKE: "인수",
  RETURN: "반납",
  DRAFT: "작성 중",
  IN_PROGRESS: "검수 중",
  COMPLETED: "완료",
  NO_NEW_DAMAGE: "신규 손상 없음",
  NEW_DAMAGE: "신규 손상 있음",
  EXISTING: "기존 손상",
  NEW: "신규 손상",
  HOLD: "판정 보류",
  CAMERA: "카메라",
  COMPUTER: "컴퓨터",
  HOME_APPLIANCE: "생활가전",
  SPORTS: "스포츠용품",
  FRONT: "정면",
  BACK: "후면",
  LEFT: "좌측",
  RIGHT: "우측",
  TOP: "상단",
  BOTTOM: "하단",
  EDGE: "가장자리",
  SCREEN: "화면",
  LENS: "렌즈",
  PORT: "연결단자",
  DETAIL: "상세",
  OTHER: "기타",
  SCRATCH: "긁힘",
  DENT: "찌그러짐",
  CRACK: "균열",
  BROKEN: "파손",
  STAIN: "얼룩",
  DISCOLORATION: "변색",
  MINOR: "경미",
  MODERATE: "보통",
  SEVERE: "심각",
  UNKNOWN: "판단 불가",
};
const label = (s: string) => names[s] || s;
function Preview({ p, token }: { p: Photo; token: string }) {
  const [url, setUrl] = useState("");
  useEffect(() => {
    let active = true,
      current = "";
    void fetch(p.contentUrl, { headers: { Authorization: `Bearer ${token}` } })
      .then((r) => {
        if (!r.ok) throw Error();
        return r.blob();
      })
      .then((b) => {
        current = URL.createObjectURL(b);
        if (active) setUrl(current);
        else URL.revokeObjectURL(current);
      })
      .catch(() => {});
    return () => {
      active = false;
      if (current) URL.revokeObjectURL(current);
    };
  }, [p.contentUrl, token]);
  return (
    <figure>
      {url && (
        <img
          src={url}
          alt={`${label(p.captureAngleCd)} ${p.originalFileName}`}
        />
      )}
      <figcaption>
        #{p.id} · {label(p.captureAngleCd)} · {p.originalFileName}
        <small title={p.fileHash}>SHA-256 {p.fileHash.slice(0, 12)}…</small>
      </figcaption>
      {p.analyses.map((a) => (
        <small key={a.id}>
          {a.modelName} {a.modelVersion} · {label(a.predictedDamageTypeCd)} ·{" "}
          {a.confidenceScore}
        </small>
      ))}
    </figure>
  );
}
export default function App() {
  const [auth, setAuth] = useState<Auth | null>(null),
    [items, setItems] = useState<Item[]>([]),
    [businesses, setBusinesses] = useState<Business[]>([]),
    [codes, setCodes] = useState<Code[]>([]),
    [rentals, setRentals] = useState<Rental[]>([]),
    [detail, setDetail] = useState<Detail | null>(null),
    [busy, setBusy] = useState(false),
    [error, setError] = useState(""),
    [notice, setNotice] = useState("");
  const token = auth?.accessToken,
    staff = !!auth && auth.role !== "CUSTOMER",
    r = detail?.rental,
    inspection = detail?.inspection;
  async function reload(id?: number) {
    const [a, b, c] = await Promise.all([
      api<Item[]>("/api/v2/catalog/items"),
      api<Business[]>("/api/v2/catalog/businesses"),
      api<Code[]>("/api/v2/catalog/codes"),
    ]);
    setItems(a);
    setBusinesses(b);
    setCodes(c);
    if (token) setRentals(await api<Rental[]>("/api/v2/rentals", token));
    if (token && id)
      setDetail(await api<Detail>(`/api/v2/rentals/${id}`, token));
  }
  useEffect(() => {
    void reload().catch((e) => setError(e.message));
  }, [token]);
  async function run(action: () => Promise<unknown>) {
    setBusy(true);
    setError("");
    setNotice("");
    try {
      await action();
      setNotice("반영했습니다.");
    } catch (e) {
      setError(e instanceof Error ? e.message : "처리 실패");
    } finally {
      setBusy(false);
    }
  }
  async function mutate(url: string, body?: unknown, method = "POST") {
    await api(url, token, method, body);
    await reload(r?.id);
  }
  const opts = (group: string) =>
    codes
      .filter((c) => c.codeGroupId === group)
      .map((c) => (
        <option value={c.codeValue} key={c.codeValue}>
          {label(c.codeValue)}
        </option>
      ));
  function form(
    title: string,
    action: (f: HTMLFormElement) => Promise<unknown>,
    children: ReactNode,
    button = "저장",
  ) {
    return (
      <form
        onSubmit={(e: FormEvent<HTMLFormElement>) => {
          e.preventDefault();
          const f = e.currentTarget;
          void run(() => action(f));
        }}
      >
        <h4>{title}</h4>
        <fieldset disabled={busy}>
          {children}
          <button>{button}</button>
        </fieldset>
      </form>
    );
  }
  const field = (
    name: string,
    title: string,
    type = "text",
    required = true,
  ) => (
    <label>
      {title}
      <input name={name} type={type} required={required} />
    </label>
  );
  const select = (
    name: string,
    title: string,
    group: string,
    initial?: string,
  ) => (
    <label>
      {title}
      <select name={name} defaultValue={initial}>
        {opts(group)}
      </select>
    </label>
  );
  const note = (
    name: string,
    title: string,
    initial?: string | null,
    required = false,
  ) => (
    <label>
      {title}
      <textarea
        name={name}
        maxLength={1000}
        defaultValue={initial || ""}
        required={required}
      />
    </label>
  );
  const editable = (c: Condition) =>
    !inspection &&
    r?.contractStatusCd ===
      (c.recordStageCd === "INTAKE" ? "REQUESTED" : "RETURNED");
  const damageFields = (d?: Damage) => (
    <>
      {select(
        "damageLocationCd",
        "위치",
        "DAMAGE_LOCATION",
        d?.damageLocationCd,
      )}
      {select("damageTypeCd", "유형", "DAMAGE_TYPE", d?.damageTypeCd)}
      {select("severityCd", "정도", "SEVERITY", d?.severityCd)}
      {note("damageDescription", "관찰 내용", d?.damageDescription)}
    </>
  );
  const damageBody = (f: HTMLFormElement) =>
    Object.fromEntries(
      [
        "damageLocationCd",
        "damageTypeCd",
        "severityCd",
        "damageDescription",
      ].map((k) => [k, value(f, k)]),
    );
  function condition(c: Condition) {
    return (
      <article key={c.id} className="condition">
        <header>
          <h3>
            {label(c.recordStageCd)} 기록 · {c.recordRevisionNo}차
          </h3>
          <span className="badge">{c.confirmed ? "확정" : "작성 중"}</span>
        </header>
        <p>{c.recordNote}</p>
        <div className="photos">
          {c.photos.map((p) => (
            <Preview key={p.id} p={p} token={token!} />
          ))}
        </div>
        {editable(c) && !c.confirmed && (
          <>
            {form(
              "상태 설명 수정",
              (f) =>
                mutate(
                  `/api/v2/conditions/${c.id}`,
                  { recordNote: value(f, "recordNote") },
                  "PATCH",
                ),
              note("recordNote", "상태 설명", c.recordNote),
            )}
            {form(
              "사진 추가",
              (f) =>
                mutate(`/api/v2/conditions/${c.id}/photos`, new FormData(f)),
              <>
                <label>
                  PNG / JPEG · 최대 10 MiB
                  <input
                    type="file"
                    name="file"
                    accept="image/png,image/jpeg"
                    required
                  />
                </label>
                {select("captureAngleCd", "촬영 방향", "CAPTURE_ANGLE")}
              </>,
              "업로드",
            )}
            {form(
              "손상 관찰 추가",
              (f) =>
                mutate(`/api/v2/conditions/${c.id}/damages`, damageBody(f)),
              damageFields(),
            )}
          </>
        )}
        {c.damages.map((d) => (
          <section className="damage" key={d.id}>
            <strong>
              손상 #{d.id} · {label(d.damageLocationCd)} /{" "}
              {label(d.damageTypeCd)} / {label(d.severityCd)}
            </strong>
            <p>{d.damageDescription}</p>
            <small>
              증거 사진:{" "}
              {d.evidence.map((e) => `#${e.photoId}`).join(", ") || "연결 없음"}
            </small>
            {editable(c) && !c.confirmed && (
              <details>
                <summary>관찰 수정·증거 연결</summary>
                {form(
                  "손상 수정",
                  (f) =>
                    mutate(`/api/v2/damages/${d.id}`, damageBody(f), "PATCH"),
                  damageFields(d),
                )}
                <button
                  className="secondary"
                  disabled={busy}
                  onClick={() =>
                    void run(() =>
                      mutate(`/api/v2/damages/${d.id}`, undefined, "DELETE"),
                    )
                  }
                >
                  이 차수의 손상 삭제
                </button>
                {form(
                  "증거 사진 연결",
                  (f) => {
                    const parts = ["x", "y", "width", "height"].map((k) =>
                      value(f, k),
                    );
                    if (parts.some(Boolean) && !parts.every(Boolean))
                      throw Error("영역 네 값을 모두 입력하세요.");
                    return mutate(`/api/v2/damages/${d.id}/photos`, {
                      photoId: Number(value(f, "photoId")),
                      box: parts.some(Boolean)
                        ? {
                            x: Number(parts[0]),
                            y: Number(parts[1]),
                            width: Number(parts[2]),
                            height: Number(parts[3]),
                          }
                        : null,
                    });
                  },
                  <>
                    <label>
                      사진
                      <select name="photoId" required>
                        <option value="">선택</option>
                        {c.photos.map((p) => (
                          <option value={p.id} key={p.id}>
                            #{p.id} · {p.originalFileName}
                          </option>
                        ))}
                      </select>
                    </label>
                    <p className="hint">
                      영역은 선택 입력입니다. 좌표·크기는 이미지 대비 0~1
                      비율입니다.
                    </p>
                    <div className="row">
                      {["x", "y", "width", "height"].map((k) => (
                        <label key={k}>
                          {k}
                          <input
                            type="number"
                            name={k}
                            min={0}
                            max={1}
                            step="0.0001"
                          />
                        </label>
                      ))}
                    </div>
                  </>,
                )}
              </details>
            )}
          </section>
        ))}
        {editable(c) && !c.confirmed && (
          <button
            disabled={busy || !c.photos.length}
            onClick={() =>
              void run(() => mutate(`/api/v2/conditions/${c.id}/confirm`))
            }
          >
            기록 확정
          </button>
        )}
        {editable(c) &&
          c.confirmed &&
          c.recordRevisionNo ===
            Math.max(
              ...detail!.conditions
                .filter((x) => x.recordStageCd === c.recordStageCd)
                .map((x) => x.recordRevisionNo),
            ) &&
          form(
            "다음 차수 작성",
            (f) =>
              mutate(`/api/v2/conditions/${c.id}/revisions`, {
                recordNote: value(f, "recordNote"),
              }),
            <>
              <p className="hint">
                현재 기록을 보존하고 사진·관찰·증거 연결을 복사합니다.
              </p>
              {note("recordNote", "새 상태 설명", c.recordNote)}
            </>,
            "새 차수 만들기",
          )}
      </article>
    );
  }
  return (
    <>
      <header className="top">
        <a href="/">RentalTrack</a>
        <span>대여 · 상태 기록 · 반납 검수</span>
        {auth && (
          <div>
            {auth.name} · {label(auth.role)}{" "}
            <button
              className="secondary"
              onClick={() => {
                setAuth(null);
                setDetail(null);
                setRentals([]);
              }}
            >
              로그아웃
            </button>
          </div>
        )}
      </header>
      <main aria-busy={busy}>
        <p className="eyebrow">RENTAL HISTORY</p>
        <h1>
          인수부터 반납 검수까지,
          <br />
          하나의 대여 이력으로.
        </h1>
        {error && (
          <p role="alert" className="error">
            {error}
          </p>
        )}
        {notice && (
          <p role="status" className="notice">
            {notice}
          </p>
        )}
        {!auth && (
          <div className="auth-grid">
            {form(
              "로그인",
              async (f) =>
                setAuth(
                  await api<Auth>("/api/v1/auth/login", undefined, "POST", {
                    loginId: value(f, "loginId"),
                    password: value(f, "password"),
                  }),
                ),
              <>
                {field("loginId", "아이디")}
                {field("password", "비밀번호", "password")}
              </>,
              "로그인",
            )}
            <details>
              <summary>회원가입</summary>
              {form(
                "새 회원",
                async (f) =>
                  setAuth(
                    await api<Auth>(
                      "/api/v1/auth/register",
                      undefined,
                      "POST",
                      {
                        loginId: value(f, "loginId"),
                        name: value(f, "name"),
                        email: value(f, "email"),
                        password: value(f, "password"),
                      },
                    ),
                  ),
                <>
                  {field("loginId", "아이디 (영문·숫자·._- 3~50자)")}
                  {field("name", "이름")}
                  {field("email", "이메일", "email")}
                  {field("password", "비밀번호 (10~72자)", "password")}
                </>,
                "가입",
              )}
            </details>
          </div>
        )}
        <section>
          <h2>렌탈 상품</h2>
          {!items.length && <p className="hint">등록된 상품이 없습니다.</p>}
          <div className="catalog">
            {items.map((i) => (
              <article key={i.id}>
                <p className="eyebrow">{label(i.categoryCd)}</p>
                <h3>{i.itemName}</h3>
                <p>
                  {businesses.find((b) => b.id === i.businessId)?.businessName}{" "}
                  · {i.assetCode}
                </p>
                <span className="badge">
                  {i.requestable
                    ? "대여 가능"
                    : i.itemStatusCd === "AVAILABLE"
                      ? "신청 중"
                      : label(i.itemStatusCd)}
                </span>
                {auth && (
                  <button
                    disabled={busy || !i.requestable}
                    onClick={() =>
                      void run(async () => {
                        const next = await api<Rental>(
                          "/api/v2/rentals",
                          token,
                          "POST",
                          { itemId: i.id },
                        );
                        await reload(next.id);
                      })
                    }
                  >
                    대여 신청
                  </button>
                )}
              </article>
            ))}
          </div>
        </section>
        {auth && (
          <section>
            <h2>내 대여 이력</h2>
            <div className="rental-list">
              {rentals.map((x) => (
                <button
                  className={x.id === r?.id ? "" : "secondary"}
                  disabled={busy}
                  key={x.id}
                  onClick={() =>
                    void run(async () =>
                      setDetail(
                        await api<Detail>(`/api/v2/rentals/${x.id}`, token),
                      ),
                    )
                  }
                >
                  #{x.id} · {label(x.contractStatusCd)}
                </button>
              ))}
            </div>
            {staff &&
              form(
                "검수할 계약 조회",
                async (f) =>
                  setDetail(
                    await api<Detail>(
                      `/api/v2/rentals/${Number(value(f, "id"))}`,
                      token,
                    ),
                  ),
                field("id", "계약 ID", "number"),
                "조회",
              )}
          </section>
        )}
        {auth && r && detail && (
          <section className="detail">
            <header>
              <div>
                <p className="eyebrow">CONTRACT #{r.id}</p>
                <h2>
                  {items.find((i) => i.id === r.itemId)?.itemName ||
                    "대여 상품"}
                </h2>
                <small>{r.contractNo}</small>
              </div>
              <span className="badge">{label(r.contractStatusCd)}</span>
            </header>
            {r.contractStatusCd === "REQUESTED" && (
              <div className="actions">
                {form(
                  "이용 시작",
                  (f) =>
                    mutate(`/api/v2/rentals/${r.id}/start`, {
                      rentalStartAt: dt(value(f, "start")),
                      rentalEndAt: dt(value(f, "end")),
                    }),
                  <>
                    <p className="hint">
                      최신 인수 기록을 확정한 뒤 시작할 수 있습니다.
                    </p>
                    {field("start", "시작 시각", "datetime-local")}
                    {field("end", "종료 예정", "datetime-local")}
                  </>,
                  "대여 시작",
                )}
                <button
                  className="secondary"
                  disabled={busy}
                  onClick={() =>
                    void run(() => mutate(`/api/v2/rentals/${r.id}/cancel`))
                  }
                >
                  신청 취소
                </button>
              </div>
            )}
            {r.contractStatusCd === "RENTED" &&
              form(
                "반납 처리",
                (f) =>
                  mutate(`/api/v2/rentals/${r.id}/return`, {
                    actualReturnAt: dt(value(f, "at")),
                  }),
                field("at", "실제 반납 시각", "datetime-local"),
                "반납",
              )}
            {!inspection &&
              ["REQUESTED", "RETURNED"].includes(r.contractStatusCd) &&
              !detail.conditions.some(
                (c) =>
                  c.recordStageCd ===
                  (r.contractStatusCd === "REQUESTED" ? "INTAKE" : "RETURN"),
              ) &&
              form(
                "상태 기록 생성",
                (f) =>
                  mutate(`/api/v2/rentals/${r.id}/conditions`, {
                    recordStageCd:
                      r.contractStatusCd === "REQUESTED" ? "INTAKE" : "RETURN",
                    recordNote: value(f, "note"),
                  }),
                note("note", "상태 설명"),
                "기록 만들기",
              )}
            <div className="condition-grid">
              {detail.conditions.map(condition)}
            </div>
            {staff &&
              r.contractStatusCd === "RETURNED" &&
              !inspection &&
              form(
                "반납 검수 생성",
                (f) =>
                  mutate(`/api/v2/rentals/${r.id}/inspection`, {
                    intakeConditionId: Number(value(f, "intake")),
                    returnConditionId: Number(value(f, "returned")),
                    inspectionNote: value(f, "note"),
                  }),
                <>
                  <p className="hint">
                    검수에 사용할 확정 기록을 고릅니다. 선택한 기준 기록은
                    고정됩니다.
                  </p>
                  {["INTAKE", "RETURN"].map((s) => (
                    <label key={s}>
                      {label(s)} 기록
                      <select
                        name={s === "INTAKE" ? "intake" : "returned"}
                        required
                      >
                        <option value="">선택</option>
                        {detail.conditions
                          .filter((c) => c.confirmed && c.recordStageCd === s)
                          .map((c) => (
                            <option key={c.id} value={c.id}>
                              {c.recordRevisionNo}차 · #{c.id}
                            </option>
                          ))}
                      </select>
                    </label>
                  ))}
                  {note("note", "검수 의견")}
                </>,
                "검수 생성",
              )}
            {inspection && (
              <article className="inspection">
                <h3>
                  검수 #{inspection.id} · {label(inspection.inspectionStatusCd)}
                </h3>
                <p>
                  고정 기록: 인수 #{inspection.intakeConditionId} → 반납 #
                  {inspection.returnConditionId}
                </p>
                {inspection.overallResultCd && (
                  <strong>{label(inspection.overallResultCd)}</strong>
                )}
                {inspection.assessments.map((a) => (
                  <p key={a.id}>
                    반납 손상 #{a.returnDamageId} ·{" "}
                    {label(a.assessmentResultCd)}
                    {a.intakeDamageId
                      ? ` → 인수 손상 #${a.intakeDamageId}`
                      : ""}{" "}
                    · {a.assessmentReason}
                  </p>
                ))}
                {staff && inspection.inspectionStatusCd === "DRAFT" && (
                  <button
                    disabled={busy}
                    onClick={() =>
                      void run(() =>
                        mutate(`/api/v2/inspections/${inspection.id}/begin`),
                      )
                    }
                  >
                    검수 시작
                  </button>
                )}
                {staff && inspection.inspectionStatusCd === "IN_PROGRESS" && (
                  <>
                    {form(
                      "손상 판정 등록·수정",
                      (f) =>
                        mutate(
                          `/api/v2/inspections/${inspection.id}/assessments`,
                          {
                            returnDamageId: Number(value(f, "returnDamage")),
                            intakeDamageId: value(f, "intakeDamage")
                              ? Number(value(f, "intakeDamage"))
                              : null,
                            assessmentResultCd: value(f, "result"),
                            assessmentReason: value(f, "reason"),
                          },
                          "PUT",
                        ),
                      <>
                        {["return", "intake"].map((s) => (
                          <label key={s}>
                            {s === "return"
                              ? "반납 손상"
                              : "인수 손상 (기존 손상일 때 선택)"}
                            <select
                              name={s + "Damage"}
                              required={s === "return"}
                            >
                              <option value="">선택</option>
                              {detail.conditions
                                .find(
                                  (c) =>
                                    c.id ===
                                    (s === "return"
                                      ? inspection.returnConditionId
                                      : inspection.intakeConditionId),
                                )
                                ?.damages.map((d) => (
                                  <option key={d.id} value={d.id}>
                                    #{d.id} · {label(d.damageTypeCd)} /{" "}
                                    {label(d.damageLocationCd)}
                                  </option>
                                ))}
                            </select>
                          </label>
                        ))}
                        {select("result", "판정", "ASSESSMENT_RESULT")}
                        {note("reason", "판정 사유", null, true)}
                      </>,
                    )}
                    <button
                      disabled={busy}
                      onClick={() =>
                        void run(() =>
                          mutate(
                            `/api/v2/inspections/${inspection.id}/complete`,
                          ),
                        )
                      }
                    >
                      검수 완료
                    </button>
                  </>
                )}
              </article>
            )}
          </section>
        )}
        {auth?.role === "ADMIN" && (
          <section>
            <h2>사업자·개별 상품 등록</h2>
            <div className="admin-grid">
              {form(
                "사업자 등록",
                (f) =>
                  mutate("/api/v2/businesses", {
                    businessNo: value(f, "no"),
                    businessName: value(f, "name"),
                    contactEmail: value(f, "email") || null,
                  }),
                <>
                  {field("no", "사업자등록번호 (숫자 10자리)")}
                  {field("name", "사업자명")}
                  {field("email", "담당 이메일", "email", false)}
                </>,
              )}
              {form(
                "개별 상품 등록",
                (f) =>
                  mutate("/api/v2/items", {
                    businessId: Number(value(f, "business")),
                    assetCode: value(f, "asset"),
                    itemName: value(f, "name"),
                    categoryCd: value(f, "category"),
                    description: value(f, "description"),
                  }),
                <>
                  <label>
                    사업자
                    <select name="business" required>
                      <option value="">선택</option>
                      {businesses.map((b) => (
                        <option key={b.id} value={b.id}>
                          {b.businessName}
                        </option>
                      ))}
                    </select>
                  </label>
                  {field("asset", "자산관리번호")}
                  {field("name", "상품명")}
                  {select("category", "분류", "CATEGORY")}
                  {note("description", "설명")}
                </>,
              )}
            </div>
          </section>
        )}
      </main>
      <footer>RentalTrack</footer>
    </>
  );
}
