import { test, expect } from "@playwright/test";

test("public demo loads under the repository path without an API server", async ({
  page,
}) => {
  const errors: string[] = [],
    apiCalls: string[] = [];
  page.on("pageerror", (e) => errors.push(e.message));
  page.on("request", (r) => {
    if (new URL(r.url()).pathname.startsWith("/api/")) apiCalls.push(r.url());
  });
  await page.goto("/RentalTrack/");
  await expect(
    page.getByRole("heading", { name: "업무용 노트북", exact: true }),
  ).toBeVisible();
  await expect(
    page.getByRole("region", { name: "공개 데모 안내" }),
  ).toContainText("기록 조회 전용");
  await page.getByRole("button", { name: "대여 중 기록", exact: true }).click();
  await expect(
    page.getByRole("heading", { name: "인수 기록 · 2차", exact: true }),
  ).toBeVisible();
  await page.getByText("다른 차수 보기", { exact: false }).click();
  await expect(
    page.getByRole("heading", { name: "인수 기록 · 1차", exact: true }),
  ).toBeVisible();
  await expect(
    page.getByRole("button", { name: "반납", exact: true }),
  ).toBeDisabled();
  await expect(page.locator(".condition-editor")).toHaveCount(0);
  await page.getByRole("button", { name: "반납 검수", exact: true }).click();
  await expect(
    page.getByRole("heading", { name: "검수 #3001 · 검수 중", exact: true }),
  ).toBeVisible();
  await expect(page.locator(".inspection")).toContainText("판정 보류");
  const pictures = page.locator(".condition-grid figure img");
  await expect(pictures).toHaveCount(2);
  await expect
    .poll(() =>
      pictures.evaluateAll((images) =>
        images.every(
          (img) =>
            (img as HTMLImageElement).complete &&
            (img as HTMLImageElement).naturalWidth > 0,
        ),
      ),
    )
    .toBe(true);
  await page.getByRole("combobox", { name: "데모 역할" }).selectOption("ADMIN");
  await page.getByRole("button", { name: "반납 검수", exact: true }).click();
  await expect(
    page.getByRole("button", { name: "검수 완료", exact: true }),
  ).toBeDisabled();
  const nav = page.locator(
    (page.viewportSize()?.width || 0) <= 680 ? ".mobile-nav" : ".sidebar nav",
  );
  await nav.getByRole("button", { name: "운영 관리", exact: true }).click();
  await expect(
    page.getByRole("heading", { name: "사업자·개별 상품 등록", exact: true }),
  ).toBeVisible();
  await expect(page.locator(".admin-grid input").first()).toBeDisabled();
  await page
    .getByRole("button", { name: "기존 이력 이전", exact: true })
    .click();
  await expect(
    page.getByRole("heading", {
      name: "공개 데모에는 이전할 이력이 없습니다",
      exact: true,
    }),
  ).toBeVisible();
  await page.getByRole("button", { name: "완료된 기록", exact: true }).click();
  await expect(page.locator(".inspection")).toContainText("신규 손상 없음");
  await expect(page.getByRole("alert")).toHaveCount(0);
  expect(
    await page.evaluate(
      () => document.documentElement.scrollWidth <= innerWidth,
    ),
  ).toBe(true);
  expect(errors).toEqual([]);
  expect(apiCalls).toEqual([]);
  await page.reload();
  await expect(page.getByRole("combobox", { name: "데모 역할" })).toHaveValue(
    "CUSTOMER",
  );
});

test("guest demo has role entry buttons instead of collecting credentials", async ({
  page,
}) => {
  await page.goto("/RentalTrack/");
  await page.getByRole("combobox", { name: "데모 역할" }).selectOption("GUEST");
  await page
    .locator(".topbar-meta")
    .getByRole("button", { name: "로그인", exact: true })
    .click();
  await expect(
    page.getByRole("heading", {
      name: "체험할 화면을 선택하세요",
      exact: true,
    }),
  ).toBeVisible();
  await expect(page.locator("input[type=password]")).toHaveCount(0);
  await page
    .getByRole("button", { name: "일반회원 화면", exact: true })
    .click();
  await expect(page.getByRole("combobox", { name: "데모 역할" })).toHaveValue(
    "CUSTOMER",
  );
});
