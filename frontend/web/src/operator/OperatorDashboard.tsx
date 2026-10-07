import axios from 'axios';
import { FormEvent, useEffect, useMemo, useState } from 'react';
import {
  clearSession,
  loadSession,
  login,
  saveSession,
  type AuthSession,
} from '../auth/api';
import { decideRecommendation, fetchOperatorDashboard } from './api';
import type {
  InventoryRecommendation,
  OperatorAiDashboard,
  RecommendationStatus,
} from './types';

function formatNumber(value: number | null | undefined, digits = 1) {
  if (value == null || Number.isNaN(value)) return '-';
  return new Intl.NumberFormat('ko-KR', {
    maximumFractionDigits: digits,
  }).format(value);
}

function formatMetric(value: number | null | undefined) {
  if (value == null || Number.isNaN(value)) return '-';
  return value.toFixed(4);
}

function formatDate(value: string | null | undefined) {
  if (!value) return '-';
  return new Intl.DateTimeFormat('ko-KR', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  }).format(new Date(value));
}

function riskLabel(risk: InventoryRecommendation['riskLevel']) {
  if (risk === 'HIGH') return '높음';
  if (risk === 'MEDIUM') return '중간';
  return '낮음';
}

export default function OperatorDashboard() {
  const [session, setSession] = useState<AuthSession | null>(() => loadSession());
  const [email, setEmail] = useState(session?.email ?? '');
  const [password, setPassword] = useState('');
  const [dashboard, setDashboard] = useState<OperatorAiDashboard | null>(null);
  const [loading, setLoading] = useState(false);
  const [loginLoading, setLoginLoading] = useState(false);
  const [decisionId, setDecisionId] = useState<number | null>(null);
  const [error, setError] = useState<string | null>(null);

  const hasData = Boolean(dashboard?.latestModel);

  const handleUnauthorized = () => {
    clearSession();
    setSession(null);
    setDashboard(null);
  };

  const loadDashboard = async () => {
    if (!session) return;

    setLoading(true);
    setError(null);

    try {
      setDashboard(await fetchOperatorDashboard());
    } catch (requestError) {
      console.error(requestError);

      if (axios.isAxiosError(requestError) && requestError.response?.status === 401) {
        handleUnauthorized();
        setError('로그인 정보가 만료되었습니다. 다시 로그인해 주세요.');
      } else if (
        axios.isAxiosError(requestError)
        && requestError.response?.status === 403
      ) {
        setDashboard(null);
        setError('OPERATOR 권한이 필요한 화면입니다.');
      } else {
        setDashboard(null);
        setError('대시보드 데이터를 불러오지 못했습니다. Backend 상태를 확인해 주세요.');
      }
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (session) {
      void loadDashboard();
    }
  }, [session]);

  const handleLogin = async (event: FormEvent) => {
    event.preventDefault();
    setLoginLoading(true);
    setError(null);

    try {
      const nextSession = await login(email, password);
      saveSession(nextSession);
      setSession(nextSession);
      setPassword('');
    } catch (requestError) {
      console.error(requestError);
      setError('이메일 또는 비밀번호를 확인해 주세요.');
    } finally {
      setLoginLoading(false);
    }
  };

  const logout = () => {
    clearSession();
    setSession(null);
    setDashboard(null);
    setPassword('');
    setError(null);
  };

  const handleDecision = async (
    recommendationId: number,
    decision: Exclude<RecommendationStatus, 'OPEN'>,
  ) => {
    setDecisionId(recommendationId);
    setError(null);

    try {
      await decideRecommendation(recommendationId, decision);
      await loadDashboard();
    } catch (requestError) {
      console.error(requestError);
      if (axios.isAxiosError(requestError) && requestError.response?.status === 401) {
        handleUnauthorized();
        setError('로그인 정보가 만료되었습니다. 다시 로그인해 주세요.');
      } else {
        setError('운영자 판단을 저장하지 못했습니다. recommendation 상태를 확인해 주세요.');
      }
    } finally {
      setDecisionId(null);
    }
  };

  const bestMetric = useMemo(() => {
    if (!dashboard?.modelComparison.length) return null;

    return [...dashboard.modelComparison].sort((a, b) => {
      const aMae = a.mae ?? Number.POSITIVE_INFINITY;
      const bMae = b.mae ?? Number.POSITIVE_INFINITY;
      if (aMae !== bMae) return aMae - bMae;

      const aRmse = a.rmse ?? Number.POSITIVE_INFINITY;
      const bRmse = b.rmse ?? Number.POSITIVE_INFINITY;
      return aRmse - bRmse;
    })[0];
  }, [dashboard]);

  return (
    <main className="dashboard-page">
      <header className="dashboard-header">
        <div>
          <span className="eyebrow">RentalTrack · Operator</span>
          <h1>AI 운영 대시보드</h1>
          <p>
            지점별 수요 예측 결과와 재고 보충 추천을 확인하고 운영 판단을 기록합니다.
          </p>
        </div>

        {session ? (
          <div className="actor-panel">
            <span className="session-label">로그인 계정</span>
            <strong>{session.name}</strong>
            <span className="session-email">{session.email}</span>
            <div className="session-actions">
              <span className="badge badge-success">{session.role}</span>
              <button type="button" className="text-button" onClick={logout}>
                로그아웃
              </button>
            </div>
          </div>
        ) : (
          <form className="actor-panel login-panel" onSubmit={handleLogin}>
            <label htmlFor="login-email">운영자 로그인</label>
            <input
              id="login-email"
              type="email"
              autoComplete="username"
              value={email}
              onChange={(event) => setEmail(event.target.value)}
              placeholder="operator@example.com"
              required
            />
            <input
              id="login-password"
              type="password"
              autoComplete="current-password"
              value={password}
              onChange={(event) => setPassword(event.target.value)}
              placeholder="비밀번호"
              minLength={8}
              required
            />
            <button type="submit" className="secondary-button" disabled={loginLoading}>
              {loginLoading ? '로그인 중' : '로그인'}
            </button>
            <small>JWT access token으로 보호된 운영자 API에 접근합니다.</small>
          </form>
        )}
      </header>

      {error && <div className="error-banner">{error}</div>}

      {loading && !dashboard && (
        <section className="empty-panel">대시보드 데이터를 불러오는 중입니다.</section>
      )}

      {!loading && !session && (
        <section className="empty-panel">
          운영자 계정으로 로그인하면 예측 및 재고 추천 데이터를 조회할 수 있습니다.
        </section>
      )}

      {!loading && session && dashboard && !hasData && (
        <section className="empty-panel">
          아직 성공한 모델 실행 결과가 없습니다. Demand Forecast Experiment를 먼저 실행해 주세요.
        </section>
      )}

      {session && dashboard && hasData && (
        <>
          <section className="dashboard-meta">
            <span>Dataset · {dashboard.datasetVersion}</span>
            <span>갱신 · {formatDate(dashboard.generatedAt)}</span>
            <button
              type="button"
              className="text-button"
              onClick={() => void loadDashboard()}
              disabled={loading}
            >
              {loading ? '갱신 중' : '새로고침'}
            </button>
          </section>

          <section className="metric-grid" aria-label="운영 요약">
            <article className="metric-card">
              <span>향후 예측 수요</span>
              <strong>{formatNumber(dashboard.forecast.predictedDemand)}</strong>
              <small>{dashboard.forecast.seriesCount}개 지점·카테고리 시계열</small>
            </article>
            <article className="metric-card">
              <span>미처리 추천</span>
              <strong>{dashboard.recommendations.openCount}</strong>
              <small>운영자 판단 대기</small>
            </article>
            <article className="metric-card">
              <span>HIGH 위험</span>
              <strong>{dashboard.recommendations.highRiskOpenCount}</strong>
              <small>우선 검토 대상</small>
            </article>
            <article className="metric-card">
              <span>추가 배치 권장</span>
              <strong>
                {formatNumber(
                  dashboard.recommendations.recommendedAdditionalQuantity,
                  0,
                )}
              </strong>
              <small>OPEN 추천 기준 합계</small>
            </article>
          </section>

          <section className="dashboard-columns">
            <article className="panel">
              <div className="panel-heading">
                <div>
                  <span className="panel-kicker">Model</span>
                  <h2>예측 모델 비교</h2>
                </div>
                {bestMetric && (
                  <span className="badge badge-success">
                    MAE 우수 · {bestMetric.modelName}
                  </span>
                )}
              </div>

              <div className="model-table" role="table">
                <div className="model-row model-row-head" role="row">
                  <span>모델</span>
                  <span>MAE</span>
                  <span>RMSE</span>
                </div>
                {dashboard.modelComparison.map((metric) => (
                  <div className="model-row" role="row" key={metric.modelRunId}>
                    <span>
                      <strong>{metric.modelName}</strong>
                      <small>{metric.runType}</small>
                    </span>
                    <span>{formatMetric(metric.mae)}</span>
                    <span>{formatMetric(metric.rmse)}</span>
                  </div>
                ))}
              </div>

              {dashboard.latestModel && (
                <div className="latest-model">
                  <span>최근 성공 실행</span>
                  <strong>{dashboard.latestModel.modelName}</strong>
                  <small>
                    {dashboard.latestModel.runType} · 완료{' '}
                    {formatDate(dashboard.latestModel.completedAt)}
                  </small>
                </div>
              )}
            </article>

            <article className="panel">
              <div className="panel-heading">
                <div>
                  <span className="panel-kicker">Risk</span>
                  <h2>미처리 추천 분포</h2>
                </div>
              </div>

              <div className="risk-stack">
                <div className="risk-item">
                  <div><span className="risk-dot risk-high" /><span>HIGH</span></div>
                  <strong>{dashboard.recommendations.highRiskOpenCount}</strong>
                </div>
                <div className="risk-item">
                  <div><span className="risk-dot risk-medium" /><span>MEDIUM</span></div>
                  <strong>{dashboard.recommendations.mediumRiskOpenCount}</strong>
                </div>
                <div className="risk-item">
                  <div><span className="risk-dot risk-low" /><span>LOW</span></div>
                  <strong>{dashboard.recommendations.lowRiskOpenCount}</strong>
                </div>
              </div>
            </article>
          </section>

          <section className="panel recommendation-panel">
            <div className="panel-heading">
              <div>
                <span className="panel-kicker">Decision queue</span>
                <h2>우선 검토 추천</h2>
              </div>
              <span className="panel-note">위험도와 추가 필요 수량 기준 상위 5건</span>
            </div>

            {dashboard.priorityRecommendations.length === 0 ? (
              <div className="empty-inline">현재 처리할 OPEN 추천이 없습니다.</div>
            ) : (
              <div className="recommendation-list">
                {dashboard.priorityRecommendations.map((recommendation) => (
                  <article className="recommendation-item" key={recommendation.id}>
                    <div className="recommendation-main">
                      <div className="recommendation-title">
                        <span
                          className={`badge badge-${recommendation.riskLevel.toLowerCase()}`}
                        >
                          {riskLabel(recommendation.riskLevel)}
                        </span>
                        <strong>{recommendation.rentalLocationName}</strong>
                        <span>·</span>
                        <span>{recommendation.productCategoryName}</span>
                      </div>

                      <div className="recommendation-numbers">
                        <span>예측 <strong>{formatNumber(recommendation.predictedDemand)}</strong></span>
                        <span>가용 <strong>{recommendation.availableQuantity}</strong></span>
                        <span>
                          추가 권장 <strong>{recommendation.recommendedAdditionalQuantity}</strong>
                        </span>
                      </div>

                      <small>
                        {formatDate(recommendation.forecastStartAt)} ~{' '}
                        {formatDate(recommendation.forecastEndAt)}
                      </small>
                    </div>

                    <div className="decision-actions">
                      <button
                        type="button"
                        className="accept-button"
                        disabled={decisionId === recommendation.id}
                        onClick={() => void handleDecision(recommendation.id, 'ACCEPTED')}
                      >
                        승인
                      </button>
                      <button
                        type="button"
                        className="dismiss-button"
                        disabled={decisionId === recommendation.id}
                        onClick={() => void handleDecision(recommendation.id, 'DISMISSED')}
                      >
                        기각
                      </button>
                    </div>
                  </article>
                ))}
              </div>
            )}
          </section>
        </>
      )}
    </main>
  );
}
