import { useEffect, useRef, type ReactNode } from "react";
import {
  ArrowUpRight,
  Boxes,
  ChevronRight,
  ClipboardList,
  LayoutDashboard,
  LogIn,
  LogOut,
  Menu,
  Route,
  Settings2,
  X,
} from "lucide-react";
import type { Auth } from "./api";
import type { View } from "./ui";
const pages = [
  { id: "overview" as View, label: "대여 현황", icon: LayoutDashboard },
  { id: "products" as View, label: "렌탈 상품", icon: Boxes },
  { id: "rentals" as View, label: "대여 이력", icon: ClipboardList },
  { id: "admin" as View, label: "운영 관리", icon: Settings2 },
];
const titles: Record<View, string> = {
  overview: "대여 현황",
  products: "렌탈 상품",
  rentals: "대여 이력",
  admin: "운영 관리",
  account: "계정",
  detail: "계약 상세",
};
const roles: Record<string, string> = {
  CUSTOMER: "일반회원",
  INSPECTOR: "검수담당자",
  ADMIN: "관리자",
};
export default function WorkspaceShell({
  view,
  auth,
  navigate,
  logout,
  children,
}: {
  view: View;
  auth: Auth | null;
  navigate: (v: View) => void;
  logout: () => void;
  children: ReactNode;
}) {
  const dialog = useRef<HTMLDialogElement>(null),
    visible = pages.filter((p) => p.id !== "admin" || auth?.role === "ADMIN");
  useEffect(() => {
    const media = window.matchMedia("(min-width:681px)");
    const close = () => {
      if (media.matches) dialog.current?.close();
    };
    media.addEventListener("change", close);
    return () => media.removeEventListener("change", close);
  }, []);
  const active = view === "detail" ? "rentals" : view;
  const go = (v: View) => {
    navigate(v);
    dialog.current?.close();
  };
  const brand = (
    <button
      className="brand"
      onClick={() => go("overview")}
      aria-label="RentalTrack 대여 현황"
    >
      <span className="brand-mark">
        <Route size={24} strokeWidth={1.6} aria-hidden="true" />
      </span>
      <span>
        RentalTrack<span className="brand-dot">.</span>
      </span>
    </button>
  );
  const navigation = (
    <nav aria-label="주 메뉴">
      <p className="nav-label">워크스페이스</p>
      {visible.map(({ id, label, icon: Icon }) => (
        <button
          key={id}
          className={`nav-item ${active === id ? "is-active" : ""}`}
          aria-current={active === id ? "page" : undefined}
          onClick={() => go(id)}
        >
          <Icon size={19} strokeWidth={1.6} aria-hidden="true" />
          <span>{label}</span>
          {active === id && <span className="nav-marker" aria-hidden="true" />}
        </button>
      ))}
    </nav>
  );
  const account = (
    <div className="sidebar-account">
      {auth ? (
        <>
          <span className="avatar">{auth.name.slice(0, 1)}</span>
          <div>
            <strong>{auth.name}</strong>
            <small>{roles[auth.role] || auth.role}</small>
          </div>
          <button
            className="icon-button"
            aria-label="로그아웃"
            onClick={() => {
              logout();
              dialog.current?.close();
            }}
          >
            <LogOut size={18} />
          </button>
        </>
      ) : (
        <button className="sidebar-login" onClick={() => go("account")}>
          <LogIn size={18} />
          <span>로그인</span>
          <ArrowUpRight size={16} />
        </button>
      )}
    </div>
  );
  return (
    <div className="workspace-shell">
      <a className="skip-link" href="#workspace-content">
        본문으로 이동
      </a>
      <aside className="sidebar">
        {brand}
        {navigation}
        <div className="sidebar-note">
          <span className="note-line" />
          <p>
            상품의 상태부터
            <br />
            계약의 마지막 기록까지.
          </p>
          <small>대여 · 기록 · 검수</small>
        </div>
        {account}
      </aside>
      <div className="workspace-body">
        <header className="workspace-topbar">
          <div className="breadcrumb">
            <button
              className="icon-button menu-button"
              onClick={() => dialog.current?.showModal()}
              aria-label="메뉴 열기"
            >
              <Menu size={21} />
            </button>
            <span className="breadcrumb-root">워크스페이스</span>
            <ChevronRight size={14} aria-hidden="true" />
            <strong>{titles[view]}</strong>
          </div>
          <div className="topbar-meta">
            <span className="today">
              {new Intl.DateTimeFormat("ko-KR", {
                timeZone: "Asia/Seoul",
                month: "long",
                day: "numeric",
                weekday: "long",
              }).format(new Date())}
            </span>
            <span className="topbar-divider" />
            {auth ? (
              <span className="topbar-role">{roles[auth.role]}</span>
            ) : (
              <button className="text-button" onClick={() => go("account")}>
                로그인
                <ArrowUpRight size={15} />
              </button>
            )}
          </div>
        </header>
        {children}
        <footer className="workspace-footer">
          <span>RentalTrack</span>
          <span>대여와 반납의 모든 기록</span>
        </footer>
      </div>
      <nav className="mobile-nav" aria-label="모바일 주 메뉴">
        {visible.map(({ id, label, icon: Icon }) => (
          <button
            key={id}
            aria-current={active === id ? "page" : undefined}
            onClick={() => go(id)}
          >
            <Icon size={20} strokeWidth={1.6} />
            <span>{label}</span>
          </button>
        ))}
      </nav>
      <dialog ref={dialog} className="mobile-menu" aria-label="워크스페이스 메뉴">
        <div className="mobile-menu-header">
          {brand}
          <button
            className="icon-button"
            onClick={() => dialog.current?.close()}
            aria-label="메뉴 닫기"
          >
            <X size={23} />
          </button>
        </div>
        {navigation}
        {account}
      </dialog>
    </div>
  );
}
