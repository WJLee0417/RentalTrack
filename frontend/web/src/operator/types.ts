export type RiskLevel = 'LOW' | 'MEDIUM' | 'HIGH';
export type RecommendationStatus = 'OPEN' | 'ACCEPTED' | 'DISMISSED';

export interface LatestModel {
  modelRunId: number;
  modelName: string;
  modelVersion: string;
  runType: string;
  mae: number | null;
  rmse: number | null;
  completedAt: string | null;
}

export interface ForecastSummary {
  seriesCount: number;
  predictedDemand: number;
}

export interface RecommendationSummary {
  openCount: number;
  highRiskOpenCount: number;
  mediumRiskOpenCount: number;
  lowRiskOpenCount: number;
  recommendedAdditionalQuantity: number;
}

export interface ModelMetric {
  modelRunId: number;
  modelName: string;
  runType: string;
  mae: number | null;
  rmse: number | null;
}

export interface InventoryRecommendation {
  id: number;
  demandForecastId: number;
  modelRunId: number;
  modelName: string;
  rentalLocationId: number;
  rentalLocationName: string;
  productCategoryId: number;
  productCategoryName: string;
  forecastStartAt: string;
  forecastEndAt: string;
  predictedDemand: number;
  availableQuantity: number;
  safetyStock: number;
  recommendedAdditionalQuantity: number;
  riskLevel: RiskLevel;
  reason: string | null;
  status: RecommendationStatus;
  generatedAt: string;
  decidedAt: string | null;
}

export interface OperatorAiDashboard {
  generatedAt: string;
  datasetVersion: string | null;
  latestModel: LatestModel | null;
  forecast: ForecastSummary;
  recommendations: RecommendationSummary;
  modelComparison: ModelMetric[];
  priorityRecommendations: InventoryRecommendation[];
}
