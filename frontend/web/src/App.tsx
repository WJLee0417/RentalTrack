import { useEffect, useState, type FormEvent, type ReactNode } from "react";
import {
  ArrowLeft,
  ArrowRight,
  ArrowUpRight,
  Box,
  Check,
  ClipboardCheck,
  ClipboardList,
  FileCheck2,
  Image as ImageIcon,
  Layers3,
  LockKeyhole,
  Plus,
  RefreshCw,
  Route,
  ShieldCheck,
} from "lucide-react";
import WorkspaceShell from "./WorkspaceShell";
import InventoryPanel from "./InventoryPanel";
import RentalList from "./RentalList";
import { EmptyState, MoreButton, StatusBadge, type View } from "./ui";
import { DEMO_MODE, demoIdentity, type DemoRole } from "./runtime";
import LegacyImportPanel from "./LegacyImportPanel";
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
  APPROVED: "승인",
  PICKED_UP: "인수 완료",
  IN_USE: "이용 중",
  REJECTED: "대여 거절",
  INSPECTING: "검수 중",
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
  const [failed, setFailed] = useState(false),
    [retry, setRetry] = useState(0);
  useEffect(() => {
    setFailed(false);
    setUrl("");
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
      .catch(() => {
        if (active) setFailed(true);
      });
    return () => {
      active = false;
      if (current) URL.revokeObjectURL(current);
    };
  }, [p.contentUrl, token, retry]);
  return (
    <figure>
      {!url && (
        <div className="photo-placeholder">
          <ImageIcon size={25} strokeWidth={1.4} />
          <span>
            {failed ? "사진을 불러오지 못했습니다" : "사진을 불러오는 중"}
          </span>
          {failed && (
            <button
              className="secondary"
              onClick={() => setRetry((v) => v + 1)}
            >
              다시 불러오기
            </button>
          )}
        </div>
      )}
      {url && (
        <img
          src={url}
          alt={`${label(p.captureAngleCd)} ${p.originalFileName}`}
        />
      )}
      <figcaption>
        #{p.id} · {label(p.captureAngleCd)} · {p.originalFileName}
        <small title={p.fileHash}>SHA-256 {p.fileHash.slice(0, 12)}…</small>
        {p.legacySource?.originalViewpoint &&
          p.legacySource.originalViewpoint !== p.captureAngleCd && (
            <small>기존 촬영 방향: {p.legacySource.originalViewpoint}</small>
          )}
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
  const [auth, setAuth] = useState<Auth | null>(() =>
      DEMO_MODE ? demoIdentity("CUSTOMER") : null,
    ),
    [items, setItems] = useState<Item[]>([]),
    [businesses, setBusinesses] = useState<Business[]>([]),
    [codes, setCodes] = useState<Code[]>([]),
    [rentals, setRentals] = useState<Rental[]>([]),
    [detail, setDetail] = useState<Detail | null>(null),
    [busy, setBusy] = useState(false),
    [error, setError] = useState(""),
    [notice, setNotice] = useState("");
  const [view, setView] = useState<View>("overview"),
    [adminTab, setAdminTab] = useState<"catalog" | "migration">("catalog"),
    [accountTab, setAccountTab] = useState<"login" | "register">("login"),
    [loaded, setLoaded] = useState(false);
  const navigate = (next: View) => {
    setNotice("");
    setError("");
    setView(
      next === "rentals" && !auth
        ? "account"
        : next === "admin" && auth?.role !== "ADMIN"
          ? "overview"
          : next,
    );
  };
  const openRental = (id: number) =>
    void run(async () => {
      setDetail(await api<Detail>(`/api/v2/rentals/${id}`, auth?.accessToken));
      setView("detail");
    });
  useEffect(() => {
    setView("overview");
  }, [auth?.memberId]);
  useEffect(() => {
    window.scrollTo(0, 0);
  }, [view]);
  useEffect(() => {
    if (detail?.rental.id) setView("detail");
  }, [detail?.rental.id]);
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
    setLoaded(true);
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
  async function run(action: () => Promise<unknown>, success = "") {
    setBusy(true);
    setError("");
    setNotice("");
    try {
      await action();
      setNotice(success);
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
          void run(
            () => action(f),
            title === "로그인"
              ? "로그인했습니다."
              : title === "새 회원"
                ? "회원가입을 완료했습니다."
                : title === "검수할 계약 조회"
                  ? ""
                  : "저장했습니다.",
          );
        }}
      >
        <div className="form-heading">
          <h4>{title}</h4>
          <span className="form-heading-line" />
        </div>
        <fieldset
          disabled={busy || (DEMO_MODE && title !== "검수할 계약 조회")}
        >
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
      <input
        name={name}
        type={type}
        required={required}
        autoComplete={
          name === "password"
            ? accountTab === "register"
              ? "new-password"
              : "current-password"
            : name === "loginId"
              ? "username"
              : name === "email"
                ? "email"
                : name === "name"
                  ? "name"
                  : undefined
        }
        step={type === "datetime-local" ? 1 : undefined}
      />
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
    !DEMO_MODE &&
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
          <StatusBadge
            status={c.confirmed ? "COMPLETED" : "DRAFT"}
            label={c.confirmed ? "확정" : "작성 중"}
          />
        </header>
        <p className="condition-note">
          {c.recordNote || "상태 설명이 없습니다."}
        </p>
        <div className="photos">
          {c.photos.map((p) => (
            <Preview key={p.id} p={p} token={token!} />
          ))}
        </div>
        {editable(c) && !c.confirmed && (
          <details className="condition-editor" open={c.photos.length === 0}>
            <summary>
              <Plus size={16} />
              기록 편집<span>설명 · 사진 · 손상 관찰</span>
            </summary>
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
          </details>
        )}
        {c.damages.map((d) => (
          <section className="damage" key={d.id}>
            <strong>
              손상 #{d.id} · {label(d.damageLocationCd)} /{" "}
              {label(d.damageTypeCd)} / {label(d.severityCd)}
            </strong>
            <p>{d.damageDescription}</p>
            {d.legacySource && (
              <small>
                기존 관찰 #{d.legacySource.legacyId} ·{" "}
                {d.legacySource.originalType} /{" "}
                {d.legacySource.originalLocation}
              </small>
            )}
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

  const currentConditions = detail
    ? ["INTAKE", "RETURN"].flatMap((stage) => {
        const fixed = inspection
          ? stage === "INTAKE"
            ? inspection.intakeConditionId
            : inspection.returnConditionId
          : undefined;
        const record = fixed
          ? detail.conditions.find((c) => c.id === fixed)
          : detail.conditions
              .filter((c) => c.recordStageCd === stage)
              .sort((a, b) => b.recordRevisionNo - a.recordRevisionNo)[0];
        return record ? [record] : [];
      })
    : [];
  const otherConditions =
    detail?.conditions.filter(
      (c) => !currentConditions.some((current) => current.id === c.id),
    ) || [];
  const pending = rentals.filter((item) =>
    ["REQUESTED", "RENTED", "RETURNED"].includes(item.contractStatusCd),
  ).length;
  const latestIntake = detail?.conditions
    .filter((c) => c.recordStageCd === "INTAKE")
    .sort((a, b) => b.recordRevisionNo - a.recordRevisionNo)[0];
  const progress =
    r?.contractStatusCd === "INSPECTED"
      ? 4
      : r?.contractStatusCd === "RETURNED"
        ? 3
        : r?.contractStatusCd === "RENTED"
          ? 2
          : r?.contractStatusCd === "REQUESTED"
            ? latestIntake?.confirmed
              ? 1
              : 0
            : -1;
  const heading = {
    overview: ["대여 현황", "상품과 계약의 현재 상태를 확인하세요."],
    products: ["렌탈 상품", "필요한 상품을 찾고 대여를 신청하세요."],
    rentals: ["대여 이력", "신청부터 반납 검수까지 계약별 기록을 확인하세요."],
    admin: ["운영 관리", "사업자와 상품을 등록하고 기존 이력을 연결하세요."],
    account: ["RentalTrack 계정", "대여 이력과 상태 기록을 계정에 보관하세요."],
    detail: ["계약 상세", "인수와 반납 기록을 한곳에서 비교하세요."],
  }[view];
  const requestItem = (itemId: number) =>
    void run(async () => {
      const next = await api<Rental>("/api/v2/rentals", token, "POST", {
        itemId,
      });
      await reload(next.id);
    }, "대여 신청을 접수했습니다.");
  const inventory = (compact = false) => (
    <InventoryPanel
      items={items}
      businesses={businesses}
      auth={auth}
      busy={busy}
      compact={compact}
      onRequest={requestItem}
      onViewAll={() => navigate("products")}
      onLogin={() => navigate("account")}
      onManage={() => navigate("admin")}
    />
  );
  const history = (compact = false) => (
    <RentalList
      rentals={rentals}
      items={items}
      busy={busy}
      compact={compact}
      onSelect={openRental}
      onViewAll={() => navigate("rentals")}
      onBrowse={() => navigate("products")}
    />
  );
  return (
    <WorkspaceShell
      view={view}
      auth={auth}
      navigate={navigate}
      logout={() => {
        setAuth(null);
        setDetail(null);
        setRentals([]);
        setNotice("");
        setView("overview");
      }}
    >
      <main
        id="workspace-content"
        className={"workspace-content view-" + view}
        aria-busy={busy}
      >
        {DEMO_MODE && (
          <section className="demo-banner" aria-label="공개 데모 안내">
            <div>
              <strong>공개 데모</strong>
              <p>샘플 데이터 · 기록 조회 전용</p>
            </div>
            <label>
              화면 역할
              <select
                aria-label="데모 역할"
                value={auth?.role || "GUEST"}
                onChange={(e) => {
                  setDetail(null);
                  setRentals([]);
                  setError("");
                  setNotice("");
                  setAuth(
                    e.target.value === "GUEST"
                      ? null
                      : demoIdentity(e.target.value as DemoRole),
                  );
                }}
              >
                <option value="GUEST">방문자</option>
                <option value="CUSTOMER">일반회원</option>
                <option value="INSPECTOR">검수담당자</option>
                <option value="ADMIN">관리자</option>
              </select>
            </label>
            {auth && (
              <div
                className="demo-shortcuts"
                role="group"
                aria-label="샘플 계약 바로가기"
              >
                <button
                  className="secondary"
                  disabled={busy}
                  onClick={() => openRental(1001)}
                >
                  대여 중 기록
                </button>
                <button
                  className="secondary"
                  disabled={busy}
                  onClick={() => openRental(1002)}
                >
                  반납 검수
                </button>
                <button
                  className="secondary"
                  disabled={busy}
                  onClick={() => openRental(1003)}
                >
                  완료된 기록
                </button>
              </div>
            )}
          </section>
        )}
        <div className="page-heading">
          <div>
            <p className="page-kicker">
              {view === "overview"
                ? "렌탈 워크스페이스"
                : view === "detail"
                  ? "계약별 기록"
                  : "RentalTrack"}
            </p>
            <h1>
              {heading[0]}
              <span className="heading-dot">.</span>
            </h1>
            <p className="page-description">{heading[1]}</p>
          </div>
          <div className="page-actions">
            <button
              className="icon-button refresh-button"
              disabled={busy}
              aria-label="목록 새로고침"
              onClick={() =>
                void run(() => reload(r?.id), "목록을 새로 불러왔습니다.")
              }
            >
              <RefreshCw size={18} />
            </button>
            {view !== "detail" && view !== "account" && view !== "admin" && (
              <button
                onClick={() => navigate(DEMO_MODE ? "rentals" : "products")}
              >
                <Plus size={17} />
                {DEMO_MODE ? "대여 이력" : "대여 신청"}
              </button>
            )}
          </div>
        </div>
        {error && (
          <div role="alert" className="error">
            <ShieldCheck size={19} />
            <div>
              <strong>요청을 처리하지 못했습니다</strong>
              <p>{error}</p>
            </div>
          </div>
        )}
        {notice && (
          <p role="status" className="notice">
            <Check size={17} />
            {notice}
          </p>
        )}
        {view === "overview" && (
          <>
            <section className="summary-strip" aria-label="대여 요약">
              {[
                {
                  label: "등록 상품",
                  value: items.length,
                  unit: "개",
                  icon: Box,
                  note: "전체 렌탈 상품",
                },
                {
                  label: "대여 가능",
                  value: items.filter((i) => i.requestable).length,
                  unit: "개",
                  icon: Layers3,
                  note: "지금 신청 가능한 상품",
                },
                {
                  label: "내 진행 계약",
                  value: pending,
                  unit: "건",
                  icon: ClipboardList,
                  note: "신청 · 대여 · 반납",
                },
                {
                  label: "내 완료 계약",
                  value: rentals.filter(
                    (i) => i.contractStatusCd === "INSPECTED",
                  ).length,
                  unit: "건",
                  icon: ClipboardCheck,
                  note: "최종 검수 완료",
                },
              ].map(({ label, value, unit, icon: Icon, note }) => (
                <div className="summary-item" key={label}>
                  <div className="summary-label">
                    {label}
                    <Icon size={18} strokeWidth={1.5} />
                  </div>
                  <div className="summary-value">
                    {loaded && (!label.startsWith("내") || auth)
                      ? String(value).padStart(2, "0")
                      : "—"}
                    <small>{unit}</small>
                  </div>
                  <p>
                    {!auth && label.startsWith("내") ? "로그인 후 확인" : note}
                  </p>
                </div>
              ))}
            </section>
            <div className="overview-grid">
              <div className="overview-inventory">
                {inventory(true)}
                <div className="workflow-note">
                  <span>
                    <Route size={21} />
                  </span>
                  <div>
                    <strong>계약 하나에, 필요한 기록을 모두</strong>
                    <p>상품 신청 → 인수 기록 → 이용·반납 → 최종 검수</p>
                  </div>
                  <MoreButton
                    onClick={() => navigate(auth ? "rentals" : "account")}
                  >
                    기록 확인
                  </MoreButton>
                </div>
              </div>
              <aside className="overview-aside">
                {auth ? (
                  history(true)
                ) : (
                  <section className="welcome-panel">
                    <span className="welcome-mark">
                      <LockKeyhole size={25} strokeWidth={1.5} />
                    </span>
                    <h2>내 기록을 이어가세요</h2>
                    <p>
                      로그인하면 대여 상태와 인수·반납 기록을 확인할 수
                      있습니다.
                    </p>
                    <button onClick={() => navigate("account")}>
                      로그인
                      <ArrowRight size={16} />
                    </button>
                    <button
                      className="text-button"
                      onClick={() => {
                        setAccountTab("register");
                        navigate("account");
                      }}
                    >
                      계정 만들기
                      <ArrowUpRight size={15} />
                    </button>
                  </section>
                )}
                <div className="process-panel">
                  <p className="panel-label">대여 기록 순서</p>
                  {[
                    {
                      icon: Box,
                      title: "상품 신청",
                      description: "개별 상품과 대여 계약 연결",
                    },
                    {
                      icon: ImageIcon,
                      title: "상태 기록",
                      description: "인수·반납 사진과 손상 관찰",
                    },
                    {
                      icon: FileCheck2,
                      title: "반납 검수",
                      description: "같은 계약의 기록 비교·판정",
                    },
                  ].map(({ icon: Icon, title, description }, i) => (
                    <div key={title}>
                      <span className="process-icon">
                        <Icon size={19} strokeWidth={1.5} />
                      </span>
                      <section>
                        <h3>{title}</h3>
                        <p>{description}</p>
                      </section>
                      <span className="process-index">0{i + 1}</span>
                    </div>
                  ))}
                </div>
              </aside>
            </div>
          </>
        )}
        {view === "products" && inventory()}
        {view === "rentals" && auth && (
          <>
            {staff && (
              <section className="contract-lookup">
                {form(
                  "검수할 계약 조회",
                  async (f) => {
                    setDetail(
                      await api<Detail>(
                        `/api/v2/rentals/${Number(value(f, "id"))}`,
                        token,
                      ),
                    );
                    setView("detail");
                  },
                  <>
                    {field("id", "계약 ID", "number")}
                    <p className="hint">검수할 계약 번호를 입력하세요.</p>
                  </>,
                  "계약 조회",
                )}
              </section>
            )}
            {history()}
          </>
        )}
        {view === "account" && !auth && DEMO_MODE && (
          <EmptyState
            title="체험할 화면을 선택하세요"
            description="샘플 회원이나 관리자로 대여 기록을 둘러볼 수 있습니다."
            action={
              <>
                <button onClick={() => setAuth(demoIdentity("CUSTOMER"))}>
                  일반회원 화면
                </button>
                <button
                  className="secondary"
                  onClick={() => setAuth(demoIdentity("ADMIN"))}
                >
                  관리자 화면
                </button>
              </>
            }
          />
        )}
        {view === "account" && !auth && !DEMO_MODE && (
          <section className="account-layout">
            <div className="account-copy">
              <span className="account-mark">
                <Route size={35} strokeWidth={1.3} />
              </span>
              <h2>
                대여의 시작부터
                <br />
                마지막 검수까지.
              </h2>
              <p>
                상품별 계약과 상태 기록을
                <br />
                한곳에서 관리하세요.
              </p>
              <div className="account-rule" />
              <div className="account-feature">
                <ShieldCheck size={18} />
                <span>내 계약에 연결된 기록과 사진</span>
              </div>
              <div className="account-feature">
                <ClipboardCheck size={18} />
                <span>차수별 상태와 검수 이력</span>
              </div>
            </div>
            <div className="account-card">
              <div className="account-tabs" role="group" aria-label="계정 작업">
                <button
                  aria-pressed={accountTab === "login"}
                  onClick={() => setAccountTab("login")}
                >
                  로그인
                </button>
                <button
                  aria-pressed={accountTab === "register"}
                  onClick={() => setAccountTab("register")}
                >
                  회원가입
                </button>
              </div>
              {accountTab === "login"
                ? form(
                    "로그인",
                    async (f) =>
                      setAuth(
                        await api<Auth>(
                          "/api/v1/auth/login",
                          undefined,
                          "POST",
                          {
                            loginId: value(f, "loginId"),
                            password: value(f, "password"),
                          },
                        ),
                      ),
                    <>
                      {field("loginId", "아이디")}
                      {field("password", "비밀번호", "password")}
                    </>,
                    "로그인",
                  )
                : form(
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
                    "계정 만들기",
                  )}
              <p className="account-footnote">
                상품은 로그인 없이 살펴볼 수 있습니다.
              </p>
            </div>
          </section>
        )}
        {view === "detail" && auth && r && detail && (
          <section className="detail">
            <button
              className="text-button back-link"
              onClick={() => navigate("rentals")}
            >
              <ArrowLeft size={16} />
              대여 이력으로
            </button>
            <header>
              <div>
                <p className="eyebrow">CONTRACT #{r.id}</p>
                <h2>
                  {items.find((i) => i.id === r.itemId)?.itemName ||
                    "대여 상품"}
                </h2>
                <small>{r.contractNo}</small>
                {r.legacySource && (
                  <small>
                    이전 이력 · 원본 #{r.legacySource.legacyId} ·{" "}
                    {label(r.legacySource.originalStatus)}
                  </small>
                )}
              </div>
              <span className="badge">{label(r.contractStatusCd)}</span>
            </header>
            <div className="rental-progress" aria-label="대여 진행 단계">
              {[
                "신청 접수",
                "인수 기록",
                "이용 시작",
                "반납 완료",
                "최종 검수",
              ].map((step, index) => (
                <div key={step} className={progress >= index ? "is-done" : ""}>
                  <span>
                    {progress >= index ? <Check size={14} /> : index + 1}
                  </span>
                  <strong>{step}</strong>
                </div>
              ))}
            </div>
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
                  disabled={busy || DEMO_MODE}
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
              {currentConditions.map(condition)}
            </div>
            {otherConditions.length > 0 && (
              <details className="revision-history">
                <summary>
                  다른 차수 보기 <span>{otherConditions.length}개 기록</span>
                </summary>
                <div className="condition-grid">
                  {otherConditions.map(condition)}
                </div>
              </details>
            )}
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
                {inspection.legacySource && (
                  <small>
                    기존 검수 #{inspection.legacySource.legacyId} ·{" "}
                    {inspection.legacySource.originalResult} ·{" "}
                    {inspection.legacySource.originalInspectedAt}
                  </small>
                )}
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
                    disabled={busy || DEMO_MODE}
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
                      disabled={busy || DEMO_MODE}
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

        {view === "admin" && auth?.role === "ADMIN" && (
          <div className="admin-tabs" role="group" aria-label="운영 관리 작업">
            <button
              aria-pressed={adminTab === "catalog"}
              onClick={() => setAdminTab("catalog")}
            >
              사업자 · 상품
            </button>
            <button
              aria-pressed={adminTab === "migration"}
              onClick={() => setAdminTab("migration")}
            >
              기존 이력 이전
            </button>
          </div>
        )}
        {view === "admin" &&
          adminTab === "catalog" &&
          auth?.role === "ADMIN" && (
            <section className="management-panel">
              <h2>사업자·개별 상품 등록</h2>
              {DEMO_MODE && (
                <p className="hint">
                  공개 데모에서는 등록 양식을 조회할 수 있습니다.
                </p>
              )}
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

        {view === "admin" &&
          adminTab === "migration" &&
          auth?.role === "ADMIN" &&
          (DEMO_MODE ? (
            <EmptyState
              title="공개 데모에는 이전할 이력이 없습니다"
              description="사업자·상품·회원·상태 매핑은 원본 자료를 연결한 환경에서 진행합니다."
            />
          ) : (
            <LegacyImportPanel
              token={auth.accessToken}
              onImported={() => reload(r?.id)}
            />
          ))}
      </main>
    </WorkspaceShell>
  );
}
