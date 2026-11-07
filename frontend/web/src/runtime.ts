import type { Auth } from "./api";

export const DEMO_MODE = import.meta.env.VITE_DEMO_MODE === "true";
export type DemoRole = "CUSTOMER" | "INSPECTOR" | "ADMIN";
export function demoIdentity(role: DemoRole): Auth {
  const identities = {
    CUSTOMER: { memberId: 1, name: "샘플 회원" },
    INSPECTOR: { memberId: 2, name: "샘플 검수담당자" },
    ADMIN: { memberId: 3, name: "샘플 관리자" },
  };
  return { ...identities[role], role, accessToken: `demo-${role}` };
}
