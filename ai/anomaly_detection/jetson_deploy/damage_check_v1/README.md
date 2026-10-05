# Jetson 배포 패키지 — 우산 파손 판정 (`damage_check_v1`)

`ENV_SENSITIVITY_ANALYSIS.md` 조사(2026-07-29~30)로 확정된 최종 후보 모델
(`dmg-umbrella_yellow-dinov2-inscribed-raw-thr59-glare`)의 **배포용 산출물**이다.

## ⚠️ `.pt`/`.ckpt`가 아니라 ONNX인 이유 (`DESCRIPTION.md` §2 확정 사항)

PatchCore 학습 체크포인트(`.ckpt`)를 그대로 배포하면 Jetson에 `anomalib`·`lightning`·
`timm`·`scikit-image` 등 학습 프레임워크 전체를 설치해야 한다 — ARM64 휠이 없는 경우
소스 빌드로 시간이 들고, 학습·추론 측 버전을 계속 맞춰야 하는 족쇄가 생긴다. **ONNX로
내보내면 이 의존성이 통째로 사라진다.** 판정에 필요한 값(크롭 규칙·정규화·임계값·밴드)은
전부 `metadata.json`에 명시돼 있어, 추론 측은 ONNX + JSON만으로 학습 때와 동일한 판정을
재현한다(실측 검증: `ONNX ↔ torch` 수치 상대오차 1.71e-06, 학습 스크립트가 매 학습 직후
자동 확인).

## 내용물

| 파일 | 용도 | 의존성 |
| --- | --- | --- |
| `weights/onnx/model.onnx` | PatchCore 파손 판정 모델(128MB) — **배포 핵심** | ONNX Runtime |
| `metadata.json` | 판정 계약 — 크롭 방식·정규화·임계값(59.0)·밴드(0.45~0.55)·모델 버전 | 없음(순수 JSON) |
| `decision.py` | raw score → normScore → NORMAL/UNCERTAIN/DAMAGED 계산 | numpy만 |
| `crop.py` | bbox → 모델 입력 크롭·리사이즈·정규화 | numpy, opencv |
| `infer.py` | ①DETECT→②QUALITY→③DAMAGE_CHECK 3단계 통합 추론 스크립트 | 아래 참고 |
| `yolov8n.pt` | ① 우산 탐지용(COCO 사전학습, 파인튜닝 없음) | **ultralytics(+torch) 필요** |

`decision.py`·`crop.py`는 **numpy(+opencv)만 의존**하도록 의도적으로 분리돼 있다
(`DESCRIPTION.md`의 원칙 — anomalib 내부 구현을 몰라도 판정을 재현할 수 있어야 함).

## ⚠️ 아직 완전히 ONNX 전용은 아니다 — YOLO 탐지기 의존성 주의

`infer.py`의 ①DETECT 단계는 `ultralytics.YOLO(yolov8n.pt)`를 그대로 쓴다(`infer.py`
91번째 줄, `UmbrellaInspector.__init__` 안에서 지연 임포트). **`ultralytics` 패키지는
`torch`를 끌고 온다** — 즉 PatchCore(③)는 ONNX만으로 돌지만, 탐지(①)는 여전히 torch가
필요하다. `requirements.txt`(학습용)에도 `ultralytics`가 최상단에 있는 이유가 이것이다.
YOLO까지 ONNX로 내보내 이 의존성을 완전히 제거할지는 **팀 결정 필요**(`DESCRIPTION.md`
GATE-005 범위 밖, 이번 조사에서 다루지 않음) — 지금 이 패키지는 "PatchCore는 ONNX,
탐지는 아직 torch 필요"인 상태 그대로 가져온 것이다.

## ⚠️⚠️ ARM64(Jetson) 설치 시 반드시 읽을 것 — "환경이 안 맞는다" 문제의 실제 원인

**임베디드 파트 확인 결과 이 프로젝트의 Jetson은 JetPack 6.2.1(CUDA 12.6, TensorRT
10.3)이다.** 아래는 그 조합으로 확정된 값이다(2026-07-30, `pypi.jetson-ai-lab.io`
인덱스에서 실제 존재 확인).

**`pip install -r requirements_jetson.txt`를 그대로 실행하면 안 된다.** `torch`와
`onnxruntime-gpu`는 **PyPI에 aarch64(Jetson) 공식 빌드가 없다** — 그냥 설치하면 다음
증상 중 하나가 난다:
- `torch-X.X.X ... is not a supported wheel on this platform` 설치 에러
- 또는 설치는 되는데 CPU 전용 범용 ARM 빌드가 깔려 **TensorRT/CUDA 가속이 조용히
  빠진 채** 동작(에러 없이 그냥 느리거나, GPU 프로바이더 요청 시 실패)

**설치 순서(확정 버전)**:
```bash
python -m pip install torch==2.11.0 torchvision==0.26.0 \
  --index-url=https://pypi.jetson-ai-lab.io/jp6/cu126

python -m pip install onnxruntime-gpu==1.24.0 \
  --index-url=https://pypi.jetson-ai-lab.io/jp6/cu126

python -c "import onnxruntime, numpy; print(numpy.__version__)"   # 에러 없이 되는지 확인

pip install opencv-python numpy ultralytics
```
⚠️ `pypi.jetson-ai-lab.dev`(.dev 도메인)가 아니라 **`.io` 도메인**을 쓸 것 — `.dev`는
2026-07-30 확인 시점에 DNS 자체가 안 잡혔다(간헐적으로 다운되는 것으로 보임).

**⚠️ 학습 측(x86 PC, torch 2.6.0+cu124)과 Jetson 측(torch 2.11.0+cu126) CUDA 버전이
다른 건 정상이고 맞출 필요 없다** — 신경 써야 할 버전 정합은 오직 **"Jetson 안에서"
torch·onnxruntime·JetPack·TensorRT가 서로 맞는지**뿐이다. 이유:
- PatchCore(③)는 ONNX로 배포된다 — ONNX는 어떤 CUDA 버전으로 만들어졌는지 정보를
  안 갖는 이식성 있는 포맷이라, Jetson은 자기 자신의 onnxruntime+TensorRT 스택으로
  독립적으로 실행한다.
- YOLO(`yolov8n.pt`)도 Jetson에 **별도로 설치되는 Jetson 자체 torch**로 로드된다 —
  학습 측 torch와 전혀 다른 설치이고 버전을 맞출 이유가 없다. `.pt` 체크포인트는
  가중치 숫자일 뿐이라 CUDA 버전에 종속되지 않는다.

**모델 자체(연산자 호환성) 검증 결과(2026-07-30, 정적 분석)**: ONNX 그래프(opset 14,
IR 7)가 쓰는 연산자(`ArgMin`·`ArgMax`·`Erf`·`Range`·`TopK`·`Where`·`Equal`·
`ConstantOfShape`·`Resize` 등) 전부 [onnx-tensorrt 공식 문서](https://github.com/onnx/onnx-tensorrt/blob/main/docs/operators.md)
(TensorRT 최신 버전 기준, JetPack 6.2.1의 TensorRT 10.3과 같은 세대)에서 지원 확인.
`TopK`의 유일한 제약(`sorted=1`, `K<3840`)도 이 모델은 `K=9`라 해당 없음.

## Jetson에 설치해야 하는 것 (요약)

```
pip install opencv-python numpy ultralytics      # 나머지는 일반 PyPI로 충분
```

- **`anomalib`·`lightning`·`timm`·`scikit-image` 등 학습 프레임워크는 설치하지 않는다.**
- `torch`·`onnxruntime-gpu`는 위 "ARM64 설치 시 반드시 읽을 것" 절차를 따를 것 — 일반
  `pip install`이 아니다.
- `providers=["TensorrtExecutionProvider","CUDAExecutionProvider","CPUExecutionProvider"]`
  순으로 자동 폴백하도록 `infer.py`에 이미 구현돼 있다(`UmbrellaInspector.__init__`).

## 실행 예시

폴더 구조가 `infer.py`가 기대하는 경로(`<model_dir>/metadata.json`,
`<model_dir>/weights/onnx/model.onnx`)와 이미 일치하도록 맞춰뒀다 — **코드 수정 없이
이 폴더를 그대로 통째로 Jetson에 복사해서 실행하면 된다.**

```bash
# 라이브 카메라(카메라 인덱스는 Jetson 실제 연결 장치에 맞게)
python infer.py --source 0 --model_dir . --detector yolov8n.pt

# 정지 이미지/폴더로 배치 검증
python infer.py --source /path/to/images --model_dir . --detector yolov8n.pt --jsonl out.jsonl
```

## 판정 계약 요약 (`metadata.json`)

| 항목 | 값 |
| --- | --- |
| 모델 버전 | `dmg-umbrella_yellow-dinov2-inscribed-raw-thr59-glare` |
| 백본 | `vit_small_patch14_dinov2.lvd142m`(DINOv2, 자기지도학습) |
| 크롭 방식 | `inscribed`(내접 정사각, 배경 구조적 제거) + padding 0.05 |
| 입력 크기 | 252×252 |
| rawThreshold | 59.0 (수동 지정 — 배치 정상 최댓값과 파손 최솟값 사이 안전마진 내) |
| uncertainBand | [0.45, 0.55] |
| ONNX 정규화 적용 여부 | `apply=false`(학습 직후 실측으로 확정된 값 — 손으로 바꾸지 말 것) |

## 검증 상태 (2026-07-30 기준)

- 배치: AUROC 0.9994, 정상 95.8% NORMAL·4.2% UNCERTAIN·0% DAMAGED, 파손 92% DAMAGED
- 라이브(개발 PC 웹캠): 정상/파손 분리 확인, 각도 게이트(`max_aspect_ratio=1.15`)·
  글레어 데이터 보강 반영 완료 — 상세는 `ENV_SENSITIVITY_ANALYSIS.md` §3-17·§3-18
- **Jetson 실기(실제 TensorRT 변환 성공 여부·onnxruntime ARM64 빌드 설치·SSH 터널
  오버헤드 등)는 여전히 미검증** — `DESCRIPTION.md` "GATE-005에서 실기로 확인해야 할
  잔여 항목" 그대로 유효. 다만 ARM64 의존성 설치 방법·연산자 호환성은 위 "ARM64
  설치 시 반드시 읽을 것" 절에 정리된 대로 정적으로 확인 완료(2026-07-30).
