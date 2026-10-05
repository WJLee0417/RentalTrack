import axios from 'axios';
import type {
  InventoryRecommendation,
  OperatorAiDashboard,
  RecommendationStatus,
} from './types';

const api = axios.create({
  baseURL: '/api/v1/operator/ai',
  timeout: 10000,
});

export async function fetchOperatorDashboard(
  actorId: number,
): Promise<OperatorAiDashboard> {
  const response = await api.get<OperatorAiDashboard>('/dashboard', {
    headers: {
      'X-Actor-Id': actorId,
    },
  });

  return response.data;
}

export async function decideRecommendation(
  actorId: number,
  recommendationId: number,
  decision: Exclude<RecommendationStatus, 'OPEN'>,
): Promise<InventoryRecommendation> {
  const response = await api.patch<InventoryRecommendation>(
    `/recommendations/${recommendationId}/decision`,
    { decision },
    {
      headers: {
        'X-Actor-Id': actorId,
      },
    },
  );

  return response.data;
}
