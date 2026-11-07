export type Auth = {
  accessToken: string;
  memberId: number;
  name: string;
  role: string;
};
export type Item = {
  id: number;
  businessId: number;
  itemName: string;
  assetCode: string;
  categoryCd: string;
  itemStatusCd: string;
  requestable: boolean;
};
export type Business = { id: number; businessName: string };
export type Code = { codeGroupId: string; codeValue: string };
export type Rental = {
  legacySource?: { legacyId: number; originalStatus: string } | null;
  id: number;
  contractNo: string;
  itemId: number;
  contractStatusCd: string;
};
export type Photo = {
  legacySource?: { legacyId: number; originalViewpoint: string | null } | null;
  id: number;
  originalFileName: string;
  contentUrl: string;
  captureAngleCd: string;
  fileHash: string;
  analyses: {
    id: number;
    modelName: string;
    modelVersion: string;
    predictedDamageTypeCd: string;
    confidenceScore: number;
  }[];
};
export type Damage = {
  legacySource?: {
    legacyId: number;
    originalType: string;
    originalLocation: string;
  } | null;
  id: number;
  damageLocationCd: string;
  damageTypeCd: string;
  severityCd: string;
  damageDescription: string | null;
  evidence: { id: number; photoId: number }[];
};
export type Condition = {
  id: number;
  recordStageCd: string;
  recordRevisionNo: number;
  recordNote: string | null;
  confirmed: boolean;
  photos: Photo[];
  damages: Damage[];
};
export type Inspection = {
  legacySource?: {
    legacyId: number;
    originalResult: string;
    originalInspectedAt: string;
  } | null;
  id: number;
  intakeConditionId: number;
  returnConditionId: number;
  inspectionStatusCd: string;
  overallResultCd: string | null;
  assessments: {
    id: number;
    returnDamageId: number;
    intakeDamageId: number | null;
    assessmentResultCd: string;
    assessmentReason: string | null;
  }[];
};
export type Detail = {
  rental: Rental;
  conditions: Condition[];
  inspection: Inspection | null;
};
export async function api<T>(
  url: string,
  token?: string,
  method = "GET",
  body?: unknown,
): Promise<T> {
  const multipart = body instanceof FormData;
  const response = await fetch(url, {
    method,
    headers: {
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...(!multipart && body !== undefined
        ? { "Content-Type": "application/json" }
        : {}),
    },
    body:
      body === undefined ? undefined : multipart ? body : JSON.stringify(body),
  });
  if (!response.ok) {
    let message = `요청 실패 (${response.status})`;
    try {
      message = (await response.json()).message || message;
    } catch {}
    throw new Error(message);
  }
  return response.status === 204 ? (undefined as T) : response.json();
}
