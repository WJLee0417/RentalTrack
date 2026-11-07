import { ArrowUpRight, Inbox, type LucideIcon } from "lucide-react";
import type { ReactNode } from "react";
export type View =
  "overview" | "products" | "rentals" | "admin" | "account" | "detail";
export const statuses: Record<string, string> = {
  REQUESTED: "신청 접수",
  RENTED: "대여 중",
  RETURNED: "반납 완료",
  INSPECTED: "검수 완료",
  CANCELED: "취소",
  AVAILABLE: "대여 가능",
  INSPECTION: "점검 중",
  REPAIR: "수리 중",
  INACTIVE: "운영 중지",
  DRAFT: "작성 중",
  IN_PROGRESS: "검수 중",
  COMPLETED: "완료",
};
export function StatusBadge({
  status,
  label,
}: {
  status: string;
  label?: string;
}) {
  return (
    <span className={`status-badge status-${status.toLowerCase()}`}>
      <span aria-hidden="true" />
      {label || statuses[status] || status}
    </span>
  );
}
export function EmptyState({
  title,
  description,
  action,
  icon: Icon = Inbox,
}: {
  title: string;
  description: string;
  action?: ReactNode;
  icon?: LucideIcon;
}) {
  return (
    <div className="empty-state">
      <span className="empty-icon">
        <Icon size={25} strokeWidth={1.4} aria-hidden="true" />
      </span>
      <h3>{title}</h3>
      <p>{description}</p>
      {action}
    </div>
  );
}
export function SectionHeading({
  title,
  count,
  action,
}: {
  title: string;
  count?: number;
  action?: ReactNode;
}) {
  return (
    <div className="section-heading">
      <h2>
        {title}
        {count !== undefined && <span className="count-label">{count}</span>}
      </h2>
      {action}
    </div>
  );
}
export function MoreButton({
  onClick,
  children,
}: {
  onClick: () => void;
  children: ReactNode;
}) {
  return (
    <button className="text-button" onClick={onClick}>
      {children}
      <ArrowUpRight size={16} aria-hidden="true" />
    </button>
  );
}
