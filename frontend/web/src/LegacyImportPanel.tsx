import { useEffect, useState } from "react";
import { api } from "./api";
type Source = {
  members: {
    id: number;
    email: string;
    role: string;
    importedMemberId: number | null;
  }[];
  products: {
    id: number;
    itemName: string;
    assetCode: string;
    categoryCode: string;
    importedItemId: number | null;
  }[];
  inspections: {
    id: number;
    contractId: number;
    result: string;
    importedInspectionId: number | null;
  }[];
  damageLocations: { source: string }[];
  businesses: { id: number; businessNo: string; businessName: string }[];
  inspectors: { id: number; name: string; role: string }[];
  contractStates: Record<string, string>;
  itemStates: Record<string, string>;
};
type Issue = {
  code: string;
  entity: string;
  legacyId: number | null;
  message: string;
};
type Preview = {
  ready: boolean;
  planToken: string;
  sourceCounts: Record<string, number>;
  pendingCounts: Record<string, number>;
  alreadyMappedCounts: Record<string, number>;
  issues: Issue[];
  warnings: Issue[];
};
type Result = { status: string; runId: number; counts: Record<string, number> };
const base = "/api/v2/admin/legacy-import";
const locations = new Set([
  "FRONT",
  "BACK",
  "LEFT",
  "RIGHT",
  "TOP",
  "BOTTOM",
  "EDGE",
  "SCREEN",
  "LENS",
  "PORT",
  "OTHER",
]);
export default function LegacyImportPanel({
  token,
  onImported,
}: {
  token: string;
  onImported: () => Promise<void>;
}) {
  const [source, setSource] = useState<Source | null>(null),
    [text, setText] = useState(""),
    [preview, setPreview] = useState<Preview | null>(null),
    [result, setResult] = useState<Result | null>(null),
    [error, setError] = useState(""),
    [busy, setBusy] = useState(false);
  async function load() {
    const data = await api<Source>(base + "/source", token);
    setSource(data);
    setPreview(null);
    setText(
      JSON.stringify(
        {
          members: data.members
            .filter((m) => !m.importedMemberId)
            .map((m) => ({
              legacyMemberId: m.id,
              loginId: `legacy-${m.id}`,
              existingMemberId: null,
            })),
          products: data.products
            .filter((p) => !p.importedItemId)
            .map((p) => ({
              legacyProductId: p.id,
              businessId: null,
              categoryCd: null,
              assetCode: p.assetCode.length <= 40 ? p.assetCode : null,
            })),
          inspections: data.inspections
            .filter((i) => !i.importedInspectionId)
            .map((i) => ({
              legacyInspectionId: i.id,
              inspectorMemberId: null,
              legacyInspectorMemberId: null,
            })),
          damageLocations: data.damageLocations
            .filter((l) => !locations.has(l.source.trim().toUpperCase()))
            .map((l) => ({ source: l.source, target: null })),
        },
        null,
        2,
      ),
    );
  }
  useEffect(() => {
    void load().catch((e) => setError(e.message));
  }, [token]);
  async function run(action: () => Promise<void>) {
    setBusy(true);
    setError("");
    try {
      await action();
    } catch (e) {
      setError(e instanceof Error ? e.message : "요청 실패");
    } finally {
      setBusy(false);
    }
  }
  const rows = (values: Record<string, number>) => (
    <ul>
      {Object.entries(values).map(([name, count]) => (
        <li key={name}>
          {name}: {count}
        </li>
      ))}
    </ul>
  );
  const issues = (values: Issue[]) => (
    <ul>
      {values.map((i, n) => (
        <li key={n}>
          <strong>{i.code}</strong> · {i.entity}
          {i.legacyId !== null ? ` #${i.legacyId}` : ""} — {i.message}
        </li>
      ))}
    </ul>
  );
  return (
    <section className="legacy-import">
      <h2>기존 이력 이전</h2>
      <p>
        상품별 소유 사업자·분류와 검수 담당자를 지정한 뒤 사전 점검을
        실행하세요. 같은 이전 요청을 다시 보내도 이력을 중복 생성하지 않습니다.
      </p>
      {error && (
        <p role="alert" className="error">
          {error}
        </p>
      )}
      {source && (
        <>
          <p>
            기존 회원 {source.members.length}명 · 상품 {source.products.length}
            개 · 검수 {source.inspections.length}건
          </p>
          {!source.members.length && !source.products.length && (
            <p className="hint">아직 이전할 기존 회원·상품이 없습니다.</p>
          )}
          <details>
            <summary>대응표에 사용할 식별자·상태 규칙</summary>
            <h4>사업자</h4>
            {source.businesses.map((b) => (
              <p key={b.id}>
                #{b.id} · {b.businessName} · {b.businessNo}
              </p>
            ))}
            <h4>현재 검수 담당자</h4>
            {source.inspectors.map((i) => (
              <p key={i.id}>
                #{i.id} · {i.name} · {i.role}
              </p>
            ))}
            <h4>기존 회원</h4>
            {source.members.map((m) => (
              <p key={m.id}>
                #{m.id} · {m.email} · {m.role}
                {m.importedMemberId ? ` → 회원 #${m.importedMemberId}` : ""}
              </p>
            ))}
            <h4>기존 상품</h4>
            {source.products.map((p) => (
              <p key={p.id}>
                #{p.id} · {p.itemName} · {p.assetCode} · {p.categoryCode}
                {p.importedItemId ? ` → 상품 #${p.importedItemId}` : ""}
              </p>
            ))}
            <h4>기존 검수</h4>
            {source.inspections.map((i) => (
              <p key={i.id}>
                #{i.id} · 계약 #{i.contractId} · {i.result}
              </p>
            ))}
            <h4>계약 상태</h4>
            {Object.entries(source.contractStates).map(([old, next]) => (
              <p key={old}>
                {old} → {next}
              </p>
            ))}
          </details>
          <label>
            매핑 JSON
            <textarea
              className="mapping-json"
              value={text}
              disabled={busy}
              spellCheck={false}
              onChange={(e) => {
                setText(e.target.value);
                setPreview(null);
                setResult(null);
              }}
            />
          </label>
          <p className="hint">
            products.businessId에 실제 소유 사업자 ID, categoryCd에 CAMERA /
            COMPUTER / HOME_APPLIANCE / SPORTS 중 하나를 입력하세요.
            inspections에는 실제 inspectorMemberId 또는 legacyInspectorMemberId
            중 하나를 지정하세요. 기존 계정 연결은 members.existingMemberId로
            지정합니다.
          </p>
          <div className="rental-list">
            <button
              disabled={busy}
              onClick={() =>
                void run(async () => {
                  setPreview(
                    await api<Preview>(
                      base + "/preview",
                      token,
                      "POST",
                      JSON.parse(text),
                    ),
                  );
                  setResult(null);
                })
              }
            >
              사전 점검
            </button>
            <button
              disabled={busy || !preview?.ready}
              onClick={() =>
                void run(async () => {
                  if (!preview) return;
                  setResult(
                    await api<Result>(base + "/apply", token, "POST", {
                      manifest: JSON.parse(text),
                      planToken: preview.planToken,
                    }),
                  );
                  await onImported();
                  await load();
                })
              }
            >
              점검한 매핑으로 이전
            </button>
            <button
              className="secondary"
              disabled={busy}
              onClick={() => void run(load)}
            >
              원본 목록 다시 읽기
            </button>
          </div>
        </>
      )}
      {preview && (
        <article className="inspection">
          <h3>{preview.ready ? "이전 준비 완료" : "매핑 보완 필요"}</h3>
          <h4>이전할 레코드</h4>
          {rows(preview.pendingCounts)}
          {!!preview.issues.length && (
            <>
              <h4>해결할 항목</h4>
              {issues(preview.issues)}
            </>
          )}
          {!!preview.warnings.length && (
            <>
              <h4>확인 사항</h4>
              {issues(preview.warnings)}
            </>
          )}
        </article>
      )}
      {result && (
        <p role="status" className="notice">
          {result.status === "ALREADY_IMPORTED"
            ? "이미 반영한 이전입니다."
            : "이력을 이전했습니다."}{" "}
          · 실행 #{result.runId}
        </p>
      )}
    </section>
  );
}
