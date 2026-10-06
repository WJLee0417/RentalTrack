import axios from 'axios';
import { loadSession } from '../auth/api';
import type {
  InventoryRecommendation,
  OperatorAiDashboard,
  RecommendationStatus,
} from './types';

const api = axios.create({
  baseURL: '/api/v1/operator/ai',
  timeout: 10000,
});

api.interceptors.request.use((config) => {
  const session = loadSession();
  if (session?.accessToken) {
    config.headers.Authorization = `Bearer ${session.accessToken}`;
  }
  return config;
});

export async function fetchOperatorDashboard(): Promise<OperatorAiDashboard> {
  const response = await api.get<OperatorAiDashboard>('/dashboard');
  return response.data;
}

export async function decideRecommendation(
  recommendationId: number,
  decision: Exclude<RecommendationStatus, 'OPEN'>,
): Promise<InventoryRecommendation> {
  const response = await api.patch<InventoryRecommendation>(
    `/recommendations/${recommendationId}/decision`,
    { decision },
  );

  return response.data;
}
