# 우산 파손검증 AI — 개발 PC 작업 환경

Jetson Orin Nano 온디바이스 추론(`DESCRIPTION.md`)을 위한 학습·검증 환경.
설계·판정 로직은 `DESCRIPTION.md`, 데이터 수집 계획은 `DATA_COLLECTION.md`.

## ⚠️ 데이터셋은 이미 크롭된 상태로 저장한다 (2026-07-24 확정)

`datasets/umbrella/train/good`, `test/good`의 이미지는 **원본 프레임이 아니라 YOLO 탐지 결과를
`precrop_dataset.py`로 미리 크롭한 결과물**이다. `DATA_COLLECTION.md §9`가 "얼굴이 프레임에
들어가면 안 된다"고 못박고 있는데, 실제 촬영은 카메라를 정면으로 보며 우산을 드는 방식이라
원본 풀프레임에 얼굴이 거의 항상 걸린다. 그래서 **원본을 저장하지 않고, 크롭된 우산 영역만
저장**하는 방식으로 이 문제를 구조적으로 해결했다(얼굴 검출기에 의존하지 않음 — 상세 경위는
`datasets/umbrella/manifest.csv`와 이 프로젝트의 대화 기록 참고).

- 크롭 padding은 표준값(0.05)이 아니라 **-0.20(더 세게 자름)**을 썼다 — 우산이 프레임을 꽉
  채우는 촬영 특성상 표준 padding으로는 크롭 모서리에 얼굴이 낄 수 있었다(실측으로 확인).
- **`train.py` 실행 시 반드시 `--crop_mode full`을 줄 것.** 이미 크롭된 이미지를 다시 YOLO에
  넣고 재크롭하면 이중 크롭이 되거나 탐지 자체가 실패한다.
- 원본 풀프레임(`datasets/umbrella/*.jpg`, `datasets/train_umbrella_0724.zip`)은 로컬에는
  남아있지만 `.gitignore`로 커밋 대상에서 제외했다. 절대 원본을 다시 train/good에 복사해
  넣지 말 것.
- 다음 촬영부터는 애초에 얼굴이 안 걸리게 찍을 필요 없다(카메라가 정면 얼굴을 보도록 설치되는
  게 확정됐으므로) — **이 크롭 파이프라인이 매 배치마다 자동으로 처리**하면 된다.
  `.venv\Scripts\python.exe precrop_dataset.py --source datasets/umbrella --test_ratio 0.15`

## 진행 상황 (2026-07-22 기준, 데이터 수집 상태는 위 배너 참고)

**촬영 시나리오(확정)**: 사용자가 우산을 들어 **우산함에 고정된 카메라**(성인 키보다 약간 높음)에
캐노피 외부 상단을 비춘다. 카메라만 고정이고 **피사체는 고정되지 않는다.**

| 항목 | 상태 |
| --- | --- |
| 촬영 시나리오 확정 | ✅ 손에 들고 제시 (거치 전제는 폐기) |
| 판정 로직 (크롭·임계값·밴드·재촬영·confidence) | ✅ `DESCRIPTION.md` 「판정 로직」 |
| 탐지 게이트 검증 도구 `check_umbrella.py` | ✅ 파일럿 측정 완료 (검출률 78%) |
| 학습 스크립트 `train.py` | ✅ 합성·정상전용·4개 크롭모드 완주 검증 |
| 추론 파이프라인 `infer.py` | ✅ 전 구간 검증 (학습 평가와 판정 일치) |
| 공유 모듈 `crop.py` / `decision.py` | ✅ 학습·추론 단일 출처 |
| ONNX 배포 경로 + JetPack 6 호환성 | ✅ 수치 동등성 6.2e-07, 정적 shape |
| 전처리 정합 자동 검증 | ✅ 학습 시 실측 확정 → `metadata.json` |
| **품질 게이트 수치 확정** | 🟢 **1차 실측 완료(2026-07-27)** — 기본값(블러60/면적0.05~0.85/신뢰도0.35) 유지로 확정, 반증 없음. 1인·1개체·1조명·18표본이라 최종 확정은 아님 — `EVAL_NOTES.md` §2-6 |
| **학습 데이터 촬영(구 3개체 혼입 데이터셋)** | ✅ 1차 배치 244장 확보(2026-07-24), 크롭 후 정상 207/37(train/test). **`datasets/umbrella`, 아래 데모 우산 트랙과 별개, 참고용으로만 유지** |
| **1차 학습·평가 (`dmg-20260724-0658`)** | 🔴 **2026-07-27 정식 재검수로 확정: 학습에 없는 신규 우산이면 NORMAL이 0/12 나옴(held-out 18.9%보다 훨씬 심각). 데모 우산 실물 확보→재학습 전까지 이 모델을 데모에 쓰면 안 됨. 상세·후속 행동은 `EVAL_NOTES.md` §2-7, §4** |
| **학습 데이터 촬영(신규 데모 우산 `umbrella_yellow`)** | ✅ train/good 261 · test/good 46 · test/defective(tear, 마스킹테이프) 36 확보 완료. 크롭 `bbox`(외접)+원본 백업. rib 샘플·실제 원단 파손 실물은 아직 미확보. 상세는 `DATA_COLLECTION.md` §3-2 |
| **재학습 결과(배치 평가 기준 최종 `dmg-umbrella_yellow-20260728i`)** | 🟡 배치는 AUROC 1.0000·정상 오탐 4.3%로 완료. **라이브 카메라 검증은 미해결**(원인 후보: `capture_umbrella.py` vs `infer.py` 캡처 방식 차이) — "AUROC 1.0=해결"로 오인 금지. 경위·재현 명령·다음 실험 3가지는 `DATA_COLLECTION.md` §3-2 |
| 크롭 방식 실데이터 확정 | ✅ `bbox`로 확정(합성 실험 당시 우세했던 `inscribed`는 테두리 파손을 구조적으로 놓쳐 폐기) |
| `imageRef` 저장소 · 서버 전송부 | ⏸ 백엔드 확정 필요 |

> **현재 블로커는 카메라 거치 높이·각도 확정 하나다.** 우산 거치 지그·측면 사광·단색 배경은
> 시나리오 정정으로 **더 이상 필요 없다** — 하드웨어 선행 조건이 크게 줄었다.
>
> **파손 우산은 블로커가 아니다.** PatchCore는 1-Class라 학습에 정상만 쓰고 임계값도 정상 분포
> 백분위로 뽑으므로, `test/defective`가 비어도 학습·ONNX 내보내기가 동작한다(검증 완료). 다만
> **파손 샘플이 없으면 실제 탐지 성능(recall/AUROC)을 측정할 방법이 없다** — 1차 평가에서
> 이게 그대로 문제가 됐다. 상세는 `EVAL_NOTES.md` 참고.

### 다음 세션 시작점

> 🔴 **2026-07-28 최신 상태**: 데모 우산 데이터셋·배치 평가·임계값 정책까지 완료
> (`dmg-umbrella_yellow-20260728i`, AUROC 1.0000·정상 오탐 4.3%). **라이브 카메라
> 검증은 미해결** — 원인 후보가 `capture_umbrella.py`(정지 촬영) vs `infer.py`(연속
> 라이브 캡처) 구조적 차이로 좁혀짐. **"AUROC 1.0=해결"로 오인 금지.** 다음 세션
> 실험 3가지(카메라 워밍업 시간·코드 정적 대조·라이브 프레임 저장 후 배치 재평가)와
> 전체 경위·재현 명령은 `DATA_COLLECTION.md` §3-2 참고(이 배너보다 최신·상세함).
> 그 외 남은 일: rib 샘플 시뮬레이션 방법 미정, 진짜 tear/rib 실물 확보되면 임계값
> 재검증 필수, `datasets/_diag_camera_check/` 정리.

<details>
<summary>1차 학습 이전의 계획 (참고용, 대부분 완료·대체됨)</summary>

**데이터 없이 지금 할 수 있는 것 (우선순위 순)**

1. **품질 게이트 임계값 튜닝** — `infer.py --source 0` (게이트 검증 모드)로 카메라 앞에서
   우산을 들고 `blurVar`·`areaRatio` 실측 분포를 보고 정한다. 현재 기본값
   (블러 60 / 점유율 0.05~0.85)은 **근거 없는 초기값**이다.
2. **탐지기 파인튜닝 판단** — 파일럿 촬영 시 `train.py`가 보고하는 *우산 미검출 비율*이
   곧 운영의 재촬영·`UNCERTAIN` 비율이다. 파일럿 30장이면 판단 가능.
3. `imageRef` 저장소·업로드 주체 결정 (백엔드와 협의)

**카메라 거치가 확정되면**

4. 파일럿 ~30장 → `train.py --smoke` 대신 실데이터로 완주 확인
5. 정상 본 촬영(300~400장) → 파손 촬영은 병렬 가능
6. `--crop_mode` 4가지 실데이터 비교 → 확정
7. GATE-005: Jetson 실기 (TensorRT 변환, 3종 모델 상주, 발열, 5초 타임아웃)

</details>

## 구성

| 항목 | 값 |
| --- | --- |
| Python | 3.10.11 (JetPack 6 / Jetson Python 3.10과 맞춤) |
| 가상환경 | `.venv/` (프로젝트 루트) |
| PyTorch | 2.6.0 + cu124 |
| Ultralytics | 8.3.x (YOLOv8) |
| anomalib | **2.5.1** (핀) |
| OpenCV | opencv-python 4.11.x |
| 가중치 | `yolov8n.pt` (COCO 80 클래스 사전학습) |

### 파일

| 파일 | 용도 |
| --- | --- |
| `train.py` | PatchCore 학습·평가·ONNX 내보내기 |
| `infer.py` | **추론 파이프라인** — ①탐지 → ②품질게이트 → ③파손판정 → 서버 payload |
| `decision.py` | **판정 계산식 (학습·추론 공유)**. numpy만 의존 — Jetson에서 그대로 사용 |
| `crop.py` | **크롭 로직 (학습·추론 공유)**. numpy·opencv만 의존 |
| `check_umbrella.py` | 우산 인식 각도 실시간 확인 (탐지 게이트 검증) |
| `detect_webcam.py` | COCO 80클래스 일반 웹캠 검출 (초기 배선 확인용) |
| `check_env.py` | 환경 점검 (버전·CUDA·카메라) |

> `decision.py`는 **torch·anomalib·opencv를 import하지 않는다.** Jetson에는 ONNX Runtime만
> 올리므로, 판정식이 무거운 의존성을 끌면 추론 측에서 못 쓴다. 학습(`train.py`)과 추론이
> **같은 함수를 쓰는 것이 요점** — 한쪽만 고치면 임계값이 조용히 어긋난다.

Python 본체는 `%LOCALAPPDATA%\Programs\Python\Python310\`에 사용자 범위로 설치돼 있다
(시스템 PATH를 건드리지 않으므로 항상 venv 경유로 실행한다).

## 실행

PowerShell 기준. 매번 activate 하지 않고 venv의 python을 직접 호출하는 방식을 권장한다.

```powershell
cd C:\repo\common_project\ai_train\anomaly_detection

# 1) 환경 점검 (카메라 창 없이 버전·CUDA·추론·카메라 오픈만 확인)
.\.venv\Scripts\python.exe check_env.py

# 2) 웹캠 실시간 검출 (COCO 80클래스 일반 확인)
.\.venv\Scripts\python.exe detect_webcam.py

# 3) top-down 우산 검출 신뢰도 실측 (GATE-005 선행)
.\.venv\Scripts\python.exe check_umbrella.py

# 4) 학습 파이프라인 배선 확인 (데이터 없이, 합성 데이터로)
.\.venv\Scripts\python.exe train.py --smoke
```

activate 방식이 편하면:

```powershell
.\.venv\Scripts\Activate.ps1
python detect_webcam.py
deactivate
```

> `Activate.ps1` 실행이 정책으로 막히면:
> `Set-ExecutionPolicy -Scope CurrentUser -ExecutionPolicy RemoteSigned`

## detect_webcam.py 옵션

| 옵션 | 기본값 | 설명 |
| --- | --- | --- |
| `--camera` | `0` | 카메라 인덱스. 노트북 내장/외장 웹캠이 함께 있으면 `1`도 시도 |
| `--model` | `yolov8n.pt` | 가중치 경로. 추후 우산 파손 모델로 교체 |
| `--imgsz` | `640` | 추론 입력 크기. 낮추면 빨라지고 정확도는 떨어짐 |
| `--conf` | `0.35` | 신뢰도 임계값 |
| `--device` | 자동 | `cpu`로 강제하면 Jetson 대비 하한 성능 감을 잡을 수 있음 |
| `--record` | 없음 | 결과 영상을 mp4로 저장 |

화면 좌상단에 **평균 추론 지연(ms) / FPS / 디바이스 / 검출 객체 수**가 표시된다.
종료는 `q` 또는 `ESC`.

## check_umbrella.py — 우산 인식 각도 실시간 확인

`DESCRIPTION.md §4 ①`의 **뷰 리스크**(COCO umbrella는 측면/사선 뷰 위주 → top-down 캐노피
과소표현)를 **우산을 직접 들고 각도를 돌려가며** 확인한다. COCO 사전학습을 그대로 게이트로
쓸 수 있는지, 파인튜닝이 필요한지를 이 결과로 판단한다.

```powershell
.\.venv\Scripts\python.exe check_umbrella.py
```

`detect_webcam.py`와 달리 **umbrella(COCO 25번) 하나만 추적**하고, 임계값을 일부러
`0.05`까지 낮춰 돌린다 — 기본값 0.25로는 "안 잡힘"만 보이고 *얼마나 아깝게* 놓치는지가
안 보이기 때문이다. 0.18에서 놓치는 것과 0.02에서 놓치는 건 판단이 완전히 다르다.

### 화면 구성

| 위치 | 내용 |
| --- | --- |
| 좌상단 | **GOOD**(≥0.25, 초록) / **WEAK**(검출됐지만 마진, 주황) / **MISS**(빨강) + 신뢰도 |
| 신뢰도 막대 | 0.25 기준선이 흰 눈금으로 표시 — 넘었는지 즉시 판별 |
| 좌하단 **추이 그래프** | 최근 약 180프레임의 신뢰도. **각도를 천천히 돌리며 이 선이 무너지는 지점을 보는 것이 핵심** |
| 우상단 | `hold`(0.25 연속 유지 시간) / `best`(최고기록) / `hit@0.25` / 지연·디바이스 |
| MISS일 때 | 우산 대신 **무엇으로 잡혔는지**(frisbee/bowl 등) 표시 |

순간값만 보면 손떨림에 묻혀 판단이 안 되므로, **추이 그래프와 `hold` 시간**으로 안정성을 본다.

### 조작·옵션

| 키 | 동작 |
| --- | --- |
| `s` | 스냅샷 저장(원본+주석 2장) |
| `r` | 추이 그래프·최고기록 리셋 — 다음 각도 조건으로 넘어갈 때 |
| `q`/`ESC` | 종료 후 요약 출력 |

| 옵션 | 기본값 | 설명 |
| --- | --- | --- |
| `--camera` | `0` | 카메라 인덱스. 내장/외장이 함께 있으면 `1`도 시도 |
| `--source` | 없음(웹캠) | 이미지 파일/폴더 경로. 지정 시 일괄 측정 모드 |
| `--conf` | `0.05` | 수집 임계값. 마진을 보려면 낮게 유지할 것 |
| `--no-mirror` | 꺼짐 | 좌우 반전 해제. 기본은 거울상(내장 캠 앞에서 각도 맞추기 편하도록) |
| `--imgsz` | `640` | 추론 입력 크기 |
| `--device` | 자동 | `cpu` 강제 가능 |
| `--outdir` | 자동 | 미지정 시 `runs/umbrella_check/<타임스탬프>/` |

### 산출물

`runs/umbrella_check/<타임스탬프>/`에 남는다:
- `measurements.csv` — 프레임별 신뢰도, bbox, **점유율(area_ratio)**, 오인 클래스, 지연
- `snapshots/` (라이브) 또는 `annotated/` (이미지 모드)

종료 시 임계값(0.25/0.35/0.50)별 검출률과 신뢰도 분포를 출력하지만, **라이브 측정의 비율은
어느 각도를 얼마나 오래 잡고 있었냐에 좌우되므로 그 자체를 근거로 삼지 말 것.** 판단 근거는
"어느 각도에서 무너졌는가"다. `area_ratio`는 파이프라인 **② 품질 게이트**의 점유율 임계값을
정할 때 참고한다.

## train.py — PatchCore 학습·평가·내보내기

`DESCRIPTION.md` 「판정 로직 (확정)」을 그대로 구현한다. 데이터가 없어도 `--smoke`로 배선 확인 가능.

```powershell
.\.venv\Scripts\python.exe train.py --smoke                    # 합성 데이터로 완주 확인
.\.venv\Scripts\python.exe train.py --data_path datasets\umbrella
.\.venv\Scripts\python.exe train.py --data_path datasets\umbrella --backbone resnet18 --coreset_ratio 0.05

# 크롭 방식 4가지 비교 (데이터 확보 후)
foreach ($m in "full","bbox","inscribed","mask_ellipse") {
  .\.venv\Scripts\python.exe train.py --data_path datasets\umbrella --crop_mode $m --keep_crops
}
```

| 옵션 | 기본값 | 설명 |
| --- | --- | --- |
| `--data_path` | `datasets/umbrella` | `train/good`, `test/good`, `test/defective`(선택) |
| `--crop_mode` | `inscribed` | `full`/`bbox`/`inscribed`/`mask_ellipse` — 아래 표 참고 |
| `--crop_padding` | `0.05` | bbox 정사각 확장 시 각 변 여백 비율 |
| `--detector` | `yolov8n.pt` | 크롭용 탐지기. **학습 이미지에도 추론과 같은 탐지기를 적용** |
| `--crop_conf` | `0.25` | 크롭용 탐지 신뢰도 임계값 |
| `--keep_crops` | 꺼짐 | 생성된 크롭 이미지를 남긴다 (눈으로 검수) |
| `--backbone` | `wide_resnet50_2` | Jetson 메모리가 빠듯하면 `resnet18` |
| `--coreset_ratio` | `0.1` | 추론이 느리면 `0.05`/`0.01`로 |
| `--percentile` | `95` | 정상 분포에서 임계값을 뽑을 백분위 |
| `--image_size` | `256` | 모델 입력 크기 |
| `--smoke` | 꺼짐 | 합성 데이터로 파이프라인만 검증 |
| `--no_export` | 꺼짐 | ONNX 내보내기 생략 |

### 산출물 (`weights/<modelVersion>/`)

| 파일 | 용도 |
| --- | --- |
| `weights/onnx/model.onnx` | **Jetson 배포 대상** |
| `metadata.json` | 추론 측 판정 계약 — ROI·정규화·`rawThreshold`·밴드·`modelVersion` |
| `scores.npz` | 원시 점수/라벨. **재학습 없이 임계값만 재조정**할 때 사용 |
| `lightning/.../model.ckpt` | 학습 측 보관용 |

### 크롭 방식 비교 (`--crop_mode`)

캐노피는 원형인데 crop은 사각형이라 **모서리에 배경이 들어온다.** PatchCore의 이미지 점수는
패치들의 **평균이 아니라 최댓값**이라, 모서리 패치 하나가 튀면 그대로 이미지 점수가 된다.
합성 실험에서 **배경만 바뀐 정상 우산이 파손 우산보다 높은 점수**를 냈다.

| 모드 | 설명 | 합성 실험 결과 |
| --- | --- | --- |
| `full` | 크롭 없음 (기준선) | — |
| `bbox` | 정사각 확장 bbox | ❌ 배경 영향이 파손의 **152%** |
| `inscribed` | **내접 정사각** (기본값) | ✅ 배경 영향 0, 파손 신호 **3배** |
| `mask_ellipse` | 내접 타원 밖을 0으로 | ❌ 경계 아티팩트로 파손 신호 **역전** |

> ⚠️ **위는 합성 이미지 실험이다.** 실데이터로 4개를 모두 돌려 재확정할 것 —
> `--crop_mode` 만 바꿔 반복 실행하면 된다.
>
> ⚠️ **`inscribed`의 대가**: 캐노피 면적의 약 **64%(2/π)** 만 덮는다. **바깥 테두리 36%를
> 버리므로** 테두리 파손(가장자리 찢어짐, 살 끝 휨)은 구조적으로 놓친다. 실제 파손이 테두리에
> 많으면 `bbox` + 배경 다양화 학습을 재검토할 것.

**학습 이미지에도 탐지기를 돌린다.** 운영에서는 탐지 bbox로 자른 영상이 PatchCore에 들어가므로,
학습만 다른 방식으로 자르면 두 분포가 어긋나 임계값이 무의미해진다. 크롭 결과는
`--keep_crops`로 남겨 **눈으로 검수**할 수 있다 — 탐지가 엉뚱한 걸 잡았는지는 수치보다 그림이 빠르다.

우산 미검출로 제외된 장수도 보고한다. **이 비율이 곧 운영의 재촬영·`UNCERTAIN` 비율**이므로,
높으면 탐지기 파인튜닝을 검토한다.

### 알아둘 것

- **임계값은 정상 데이터만으로** 뽑는다. 파손 표본(유형별 15~20장)에 F1을 맞추면 그 표본에
  과적합되므로, 파손은 Recall '확인'에만 쓰고 임계값을 되돌리지 않는다.
- 평가는 이진 통과율이 아니라 **3단 밴드(NORMAL/UNCERTAIN/DAMAGED)** 로 집계한다. 운영 지표 2개를
  함께 출력한다 — *정상인데 격리*(슬롯 3~5개라 시연이 막힘), *파손인데 통과*(다음 사용자에게 나감).
- 정상 표본이 백분위에 비해 적으면 경고한다(p95면 최소 20건).
- 파손 표본이 없으면 `scoreMax`를 추정하고 `metadata.json`에 `scoreMaxEstimated: true`를 남긴다
  → **`DAMAGED` 경계가 잠정**이라는 뜻. 파손 확보 후 재실행해 확정할 것.

## infer.py — 추론 파이프라인

①탐지 → ②품질게이트 → ③파손판정을 통합하고 서버 payload를 만든다. 재촬영 2회·5초 타임아웃
정책이 구현돼 있다.

```powershell
# 게이트 검증 모드 — 모델 없이 ①② 만. 지금 단계에서 품질 임계값 튜닝용
.\.venv\Scripts\python.exe infer.py --source 0
.\.venv\Scripts\python.exe infer.py --source pilot_shots\ --jsonl gate.jsonl

# 전체 파이프라인
.\.venv\Scripts\python.exe infer.py --source 0 --model_dir weights\dmg-20260722-0827
.\.venv\Scripts\python.exe infer.py --source shots\ --model_dir weights\... --jsonl out.jsonl
```

라이브 모드에서 **SPACE** = 반납 검수 1회 실행(재촬영·타임아웃 정책 적용), `q`/`ESC` = 종료.

| 옵션 | 기본값 | 설명 |
| --- | --- | --- |
| `--source` | `0` | 카메라 인덱스 / 이미지 파일 / 폴더 |
| `--model_dir` | 없음 | `weights/<modelVersion>`. **미지정 시 게이트 검증 모드** |
| `--detect_conf` | `0.25` | ① 탐지 임계값 |
| `--min_detect_conf` | `0.35` | ② 게이트: 최소 탐지 신뢰도 |
| `--min_blur` | `60.0` | ② 게이트: 최소 라플라시안 분산 (낮을수록 흐림) |
| `--min_area` / `--max_area` | `0.05` / `0.85` | ② 게이트: bbox 점유율 범위 |
| `--jsonl` | 없음 | 결과를 JSONL로 저장 |

> ⚠️ **품질 게이트 임계값은 전부 초기값이다.** 조명·카메라·거리가 바뀌면 특히 블러 임계값이
> 크게 달라진다. 파일럿 사진 폴더로 게이트 검증 모드를 돌려 출력되는 `blurVar`·`areaRatio`
> 분포를 보고 정할 것 — **데이터 없이 지금 할 수 있는 작업이다.**

### 전처리 정합 자동 검증

학습·추론 전처리 불일치는 **예외 없이 점수만 조용히 어긋나** 임계값을 무력화한다. 개발 중
실제로 **8배 어긋난 사례**가 있었다(학습 5.16 vs 추론 42.05).

그래서 `train.py`가 학습 직후 ONNX에 정규화 적용/미적용을 **모두 넣어 학습 점수와 맞는 쪽을
실측 확정**하고 `metadata.json`의 `input.normalization.apply`에 기록한다:

```
[전처리 정합 검증] 추론 전처리로 학습 점수가 재현되는가?
  학습 평균 5.1624 | ONNX(정규화O) 42.0506 | ONNX(정규화X) 5.1618
  ✅ 일치 — applyNormalize=False (상대오차 1.21e-04)
```

**이 값을 손으로 바꾸지 말 것.** ❌가 뜨면 그 모델은 배포하면 안 된다.

## 재현 방법 (다른 PC에서)

```powershell
winget install --id Python.Python.3.10 --scope user
& "$env:LOCALAPPDATA\Programs\Python\Python310\python.exe" -m venv .venv
.\.venv\Scripts\python.exe -m pip install --upgrade pip
.\.venv\Scripts\python.exe -m pip install torch torchvision --index-url https://download.pytorch.org/whl/cu124
.\.venv\Scripts\python.exe -m pip install -r requirements.txt

# ⚠️ 필수: anomalib이 딸려온 opencv-python-headless가 cv2.imshow를 죽인다 → 복구
.\.venv\Scripts\python.exe -m pip install --force-reinstall --no-deps "opencv-python==4.11.*"
.\.venv\Scripts\python.exe -c "import cv2; print(hasattr(cv2,'imshow'))"   # True 여야 함
```

## 주의

- **이 환경의 성능 수치는 Jetson 성능이 아니다.** dGPU(RTX)와 Jetson Orin Nano는 전력·메모리
  대역폭이 전혀 다르다. 지연·메모리 PoC는 반드시 Jetson 실기에서 다시 측정해야 한다.
- 얼굴 관련 데이터는 이 디렉터리에서 다루지 않는다. 얼굴 영상·임베딩은 Jetson 밖으로 나가지
  않는다는 프로젝트 원칙(`CLAUDE.md`)을 그대로 따른다.
- `.venv/`와 `yolov8n.pt`는 용량이 커서 커밋 대상이 아니다. `.gitignore` 참고.
