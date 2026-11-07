import type {
  Business,
  Code,
  Condition,
  Damage,
  Detail,
  Item,
  Photo,
} from "./api";

const businesses: Business[] = [{ id: 1, businessName: "샘플 렌탈 장비실" }];
const items: Item[] = [
  {
    id: 1,
    businessId: 1,
    itemName: "업무용 노트북",
    assetCode: "DEMO-PC-001",
    categoryCd: "COMPUTER",
    itemStatusCd: "RENTED",
    requestable: false,
  },
  {
    id: 2,
    businessId: 1,
    itemName: "미러리스 카메라",
    assetCode: "DEMO-CAM-002",
    categoryCd: "CAMERA",
    itemStatusCd: "INSPECTION",
    requestable: false,
  },
  {
    id: 3,
    businessId: 1,
    itemName: "공기청정기",
    assetCode: "DEMO-HM-003",
    categoryCd: "HOME_APPLIANCE",
    itemStatusCd: "AVAILABLE",
    requestable: true,
  },
  {
    id: 4,
    businessId: 1,
    itemName: "실내 자전거",
    assetCode: "DEMO-SP-004",
    categoryCd: "SPORTS",
    itemStatusCd: "AVAILABLE",
    requestable: true,
  },
];
const groups: Record<string, string[]> = {
  CATEGORY: ["CAMERA", "COMPUTER", "HOME_APPLIANCE", "SPORTS"],
  DAMAGE_LOCATION: [
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
  ],
  DAMAGE_TYPE: [
    "SCRATCH",
    "DENT",
    "CRACK",
    "BROKEN",
    "STAIN",
    "DISCOLORATION",
    "OTHER",
  ],
  SEVERITY: ["MINOR", "MODERATE", "SEVERE", "UNKNOWN"],
  CAPTURE_ANGLE: [
    "FRONT",
    "BACK",
    "LEFT",
    "RIGHT",
    "TOP",
    "BOTTOM",
    "DETAIL",
    "OTHER",
  ],
  ASSESSMENT_RESULT: ["EXISTING", "NEW", "HOLD"],
};
const codes: Code[] = Object.entries(groups).flatMap(([codeGroupId, values]) =>
  values.map((codeValue) => ({ codeGroupId, codeValue })),
);
// These hashes are generated from the committed sample SVG bytes, not from a real inspection.
import hashes from "./demo-image-hashes.json";
const photo = (id: number, kind: keyof typeof hashes): Photo => ({
  id,
  originalFileName: `샘플-${kind}-도식.svg`,
  contentUrl: `${import.meta.env.BASE_URL}demo/${kind}.svg`,
  captureAngleCd: "FRONT",
  fileHash: hashes[kind],
  analyses: [],
});
const damage = (id: number, photoId: number, type = "SCRATCH"): Damage => ({
  id,
  damageLocationCd: type === "DENT" ? "EDGE" : "FRONT",
  damageTypeCd: type,
  severityCd: "MINOR",
  damageDescription:
    type === "DENT"
      ? "샘플: 반납 기록에서 확인한 모서리 눌림"
      : "샘플: 전면의 작은 긁힘",
  evidence: [{ id, photoId }],
});
const record = (
  id: number,
  stage: string,
  revision: number,
  kind: keyof typeof hashes,
  note: string,
  damages: Damage[] = [],
): Condition => ({
  id,
  recordStageCd: stage,
  recordRevisionNo: revision,
  recordNote: note,
  confirmed: true,
  photos: [photo(id, kind)],
  damages,
});
const details: Detail[] = [
  {
    rental: {
      id: 1001,
      contractNo: "DEMO-1001",
      itemId: 1,
      contractStatusCd: "RENTED",
    },
    conditions: [
      record(1101, "INTAKE", 1, "laptop", "샘플: 인수 전 외관과 화면 확인"),
      record(
        1102,
        "INTAKE",
        2,
        "laptop",
        "샘플: 인수 기록을 보존하고 2차 기록으로 확인",
      ),
    ],
    inspection: null,
  },
  {
    rental: {
      id: 1002,
      contractNo: "DEMO-1002",
      itemId: 2,
      contractStatusCd: "RETURNED",
    },
    conditions: [
      record(
        1201,
        "INTAKE",
        1,
        "camera-intake",
        "샘플: 전면 긁힘을 기록하고 인수",
        [damage(2201, 1201)],
      ),
      record(
        1202,
        "RETURN",
        1,
        "camera-return",
        "샘플: 기존 긁힘과 반납 시 모서리 상태 비교",
        [damage(2202, 1202), damage(2203, 1202, "DENT")],
      ),
    ],
    inspection: {
      id: 3001,
      intakeConditionId: 1201,
      returnConditionId: 1202,
      inspectionStatusCd: "IN_PROGRESS",
      overallResultCd: null,
      assessments: [
        {
          id: 4001,
          returnDamageId: 2202,
          intakeDamageId: 2201,
          assessmentResultCd: "EXISTING",
          assessmentReason: "샘플: 인수 기록의 전면 긁힘과 위치 비교",
        },
        {
          id: 4002,
          returnDamageId: 2203,
          intakeDamageId: null,
          assessmentResultCd: "HOLD",
          assessmentReason: "샘플: 모서리 눌림 추가 확인 중",
        },
      ],
    },
  },
  {
    rental: {
      id: 1003,
      contractNo: "DEMO-1003",
      itemId: 3,
      contractStatusCd: "INSPECTED",
    },
    conditions: [
      record(1301, "INTAKE", 1, "appliance", "샘플: 외관과 흡입구 상태 확인"),
      record(1302, "RETURN", 1, "appliance", "샘플: 인수 당시 상태와 비교"),
    ],
    inspection: {
      id: 3002,
      intakeConditionId: 1301,
      returnConditionId: 1302,
      inspectionStatusCd: "COMPLETED",
      overallResultCd: "NO_NEW_DAMAGE",
      assessments: [],
    },
  },
];

/** Read-only, browser-local transport. This does not authenticate against the Spring API. */
export async function demoApi<T>(
  url: string,
  token: string | undefined,
  method: string,
): Promise<T> {
  if (method !== "GET")
    throw Error("공개 데모에서는 샘플 기록 조회만 제공합니다.");
  let result: unknown;
  if (url === "/api/v2/catalog/items") result = items;
  else if (url === "/api/v2/catalog/businesses") result = businesses;
  else if (url === "/api/v2/catalog/codes") result = codes;
  else {
    if (!token?.startsWith("demo-")) throw Error("샘플 역할을 선택하세요.");
    if (url === "/api/v2/rentals")
      result = token === "demo-CUSTOMER" ? details.map((d) => d.rental) : [];
    else if (/^\/api\/v2\/rentals\/\d+$/.test(url)) {
      result = details.find(
        (d) => d.rental.id === Number(url.split("/").pop()),
      );
      if (!result)
        throw Error("샘플 계약 번호 1001·1002·1003 중 하나를 선택하세요.");
    } else throw Error("공개 데모에서 제공하지 않는 조회입니다.");
  }
  return structuredClone(result) as T;
}
