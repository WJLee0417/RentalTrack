import { ArrowRight, ClipboardList } from "lucide-react";
import type { Item, Rental } from "./api";
import { EmptyState, MoreButton, SectionHeading, StatusBadge } from "./ui";
export default function RentalList({
  rentals,
  items,
  busy,
  compact = false,
  onSelect,
  onViewAll,
  onBrowse,
}: {
  rentals: Rental[];
  items: Item[];
  busy: boolean;
  compact?: boolean;
  onSelect: (id: number) => void;
  onViewAll: () => void;
  onBrowse: () => void;
}) {
  const shown = compact ? rentals.slice(0, 4) : rentals;
  return (
    <section className={`rental-panel ${compact ? "compact" : ""}`}>
      <SectionHeading
        title={compact ? "최근 대여" : "내 대여 이력"}
        count={rentals.length}
        action={
          compact && rentals.length > 0 ? (
            <MoreButton onClick={onViewAll}>전체 이력</MoreButton>
          ) : undefined
        }
      />
      {!shown.length ? (
        <EmptyState
          icon={ClipboardList}
          title="아직 대여 이력이 없습니다"
          description="상품을 신청하면 진행 상태와 기록을 여기서 확인할 수 있습니다."
          action={
            <button className="secondary" onClick={onBrowse}>
              상품 살펴보기
              <ArrowRight size={16} />
            </button>
          }
        />
      ) : (
        <div className="rental-table">
          <div className="rental-table-head">
            <span>계약 / 상품</span>
            <span>진행 상태</span>
            <span className="sr-only">계약 열기</span>
          </div>
          {shown.map((r) => (
            <button
              className="rental-row"
              key={r.id}
              disabled={busy}
              onClick={() => onSelect(r.id)}
              aria-label={`계약 #${r.id} ${items.find((i) => i.id === r.itemId)?.itemName || "대여 상품"} 상세 보기`}
            >
              <div className="rental-row-name">
                <span className="rental-id">
                  #{String(r.id).padStart(4, "0")}
                </span>
                <strong>
                  {items.find((i) => i.id === r.itemId)?.itemName ||
                    "대여 상품"}
                </strong>
                {!compact && (
                  <small>
                    {r.legacySource ? "이전 계약" : "대여 계약"} ·{" "}
                    {r.contractNo}
                  </small>
                )}
              </div>
              <StatusBadge status={r.contractStatusCd} />
              <ArrowRight size={17} className="row-arrow" aria-hidden="true" />
            </button>
          ))}
        </div>
      )}
    </section>
  );
}
