import { useMemo, useState } from "react";
import {
  ArrowRight,
  Camera,
  Check,
  House,
  Laptop,
  Search,
  SlidersHorizontal,
  Volleyball,
} from "lucide-react";
import type { Auth, Business, Item } from "./api";
import { EmptyState, MoreButton, SectionHeading, StatusBadge } from "./ui";
const categories = [
  { value: "ALL", label: "전체 상품" },
  { value: "CAMERA", label: "카메라" },
  { value: "COMPUTER", label: "컴퓨터" },
  { value: "HOME_APPLIANCE", label: "생활가전" },
  { value: "SPORTS", label: "스포츠용품" },
];
const icons = {
  CAMERA: Camera,
  COMPUTER: Laptop,
  HOME_APPLIANCE: House,
  SPORTS: Volleyball,
};
export default function InventoryPanel({
  items,
  businesses,
  auth,
  busy,
  compact = false,
  onRequest,
  onViewAll,
  onLogin,
  onManage,
}: {
  items: Item[];
  businesses: Business[];
  auth: Auth | null;
  busy: boolean;
  compact?: boolean;
  onRequest: (id: number) => void;
  onViewAll: () => void;
  onLogin: () => void;
  onManage: () => void;
}) {
  const [query, setQuery] = useState(""),
    [category, setCategory] = useState("ALL"),
    [available, setAvailable] = useState(false);
  const filtered = useMemo(
    () =>
      items.filter(
        (i) =>
          (category === "ALL" || i.categoryCd === category) &&
          (!available || i.requestable) &&
          `${i.itemName} ${i.assetCode} ${businesses.find((b) => b.id === i.businessId)?.businessName || ""}`
            .toLocaleLowerCase()
            .includes(query.toLocaleLowerCase().trim()),
      ),
    [items, businesses, query, category, available],
  );
  const shown = compact ? filtered.slice(0, 4) : filtered;
  return (
    <section className={`inventory-panel ${compact ? "compact" : ""}`}>
      <SectionHeading
        title="렌탈 상품"
        count={items.length}
        action={
          compact ? (
            <MoreButton onClick={onViewAll}>전체 상품 보기</MoreButton>
          ) : undefined
        }
      />
      {!compact && (
        <div className="inventory-controls">
          <label className="search-field">
            <Search size={18} aria-hidden="true" />
            <span className="sr-only">상품 검색</span>
            <input
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              placeholder="상품명, 자산번호, 사업자로 검색"
            />
          </label>
          <button
            className={`filter-button ${available ? "is-on" : ""}`}
            aria-pressed={available}
            onClick={() => setAvailable(!available)}
          >
            {available ? <Check size={16} /> : <SlidersHorizontal size={16} />}
            대여 가능만
          </button>
        </div>
      )}
      {!compact && (
        <div className="category-tabs" role="group" aria-label="상품 분류">
          {categories.map((c) => (
            <button
              key={c.value}
              aria-pressed={category === c.value}
              onClick={() => setCategory(c.value)}
            >
              {c.label}
              {c.value === "ALL" && <span>{items.length}</span>}
            </button>
          ))}
        </div>
      )}
      {!shown.length ? (
        <EmptyState
          title={
            items.length ? "검색 결과가 없습니다" : "등록된 상품이 없습니다"
          }
          description={
            items.length
              ? "검색어나 분류를 변경해 보세요."
              : "사업자와 상품을 등록하면 이곳에서 대여할 수 있습니다."
          }
          action={
            items.length ? (
              <button
                className="secondary"
                onClick={() => {
                  setQuery("");
                  setCategory("ALL");
                  setAvailable(false);
                }}
              >
                필터 초기화
              </button>
            ) : auth?.role === "ADMIN" ? (
              <button className="secondary" onClick={onManage}>
                상품 등록하기
                <ArrowRight size={16} />
              </button>
            ) : undefined
          }
        />
      ) : (
        <div className="catalog">
          {shown.map((item) => {
            const Icon = icons[item.categoryCd as keyof typeof icons] || House;
            const status = item.requestable
              ? "AVAILABLE"
              : item.itemStatusCd === "AVAILABLE"
                ? "REQUESTED"
                : item.itemStatusCd;
            return (
              <article className="product-card" key={item.id}>
                <div className="product-card-top">
                  <span
                    className={`product-glyph glyph-${item.categoryCd.toLowerCase()}`}
                  >
                    <Icon size={26} strokeWidth={1.4} aria-hidden="true" />
                  </span>
                  <StatusBadge
                    status={status}
                    label={status === "REQUESTED" ? "신청 중" : undefined}
                  />
                </div>
                <div className="product-info">
                  <span className="product-category">
                    {categories.find((c) => c.value === item.categoryCd)
                      ?.label || item.categoryCd}
                  </span>
                  <h3>{item.itemName}</h3>
                  <p>
                    {businesses.find((b) => b.id === item.businessId)
                      ?.businessName || "등록 사업자"}
                  </p>
                </div>
                <div className="product-card-bottom">
                  <span className="asset-code">{item.assetCode}</span>
                  <button
                    className="secondary product-action"
                    disabled={busy || !item.requestable}
                    onClick={() => (auth ? onRequest(item.id) : onLogin())}
                  >
                    {auth ? "대여 신청" : "로그인 후 신청"}
                    <ArrowRight size={15} aria-hidden="true" />
                  </button>
                </div>
              </article>
            );
          })}
        </div>
      )}
      {!compact && shown.length > 0 && (
        <p className="list-footnote">
          전체 {items.length}개 중 {filtered.length}개 상품
        </p>
      )}
    </section>
  );
}
