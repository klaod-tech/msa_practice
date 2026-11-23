# 09. 이스티오 설정, 시험용 파일

> 시작: 교수님 `Chapter17` 상태 / 결과: 교수님 `Chapter18`과 같은 코드(교재 영어 주석만 빠짐).
> 표기: `★ 추가` / `★ 변경 (전: ...)` / `★ 삭제` = 이전 장 대비 바뀌는 줄. md 화면에서 Ctrl+F로 `★`를 찾으면 바뀐 줄만 볼 수 있다.

`hands-on-namespace.yml`(사이드카 자동 주입 켠 네임스페이스), `istio-tracing.yml`·`istio-telemetry.yml`(이스티오 추적 설정), `resilience-tests/`(지연·오류 주입 VirtualService), `routing-tests/`(blue/green 비율 바꾸는 스크립트).

## 9-1. `kubernetes/hands-on-namespace.yml` (새 파일)
```bash
code kubernetes/hands-on-namespace.yml
```
붙여넣기 (새 파일)
```yaml
apiVersion: v1
kind: Namespace
metadata:
  name: hands-on
  labels:
    istio-injection: enabled
```

## 9-2. `kubernetes/istio-telemetry.yml` (새 파일)
```bash
code kubernetes/istio-telemetry.yml
```
붙여넣기 (새 파일)
```yaml
apiVersion: telemetry.istio.io/v1
kind: Telemetry
metadata:
  name: mesh-default
spec:
  tracing:
  - providers:
    - name: "zipkin"
    randomSamplingPercentage: 100.00
```

## 9-3. `kubernetes/istio-tracing.yml` (새 파일)
```bash
code kubernetes/istio-tracing.yml
```
붙여넣기 (새 파일)
```yaml
apiVersion: install.istio.io/v1alpha1
kind: IstioOperator
spec:
  meshConfig:
    enableTracing: true
    defaultConfig:
      tracing: {} # disable legacy MeshConfig tracing options
    extensionProviders:
    - name: "zipkin"
      zipkin:
        service: zipkin.istio-system.svc.cluster.local
        port: 9411
```

## 9-4. `kubernetes/resilience-tests/product-virtual-service-with-delay.yml` (새 파일)
```bash
mkdir -p kubernetes/resilience-tests
code kubernetes/resilience-tests/product-virtual-service-with-delay.yml
```
붙여넣기 (새 파일)
```yaml
apiVersion: networking.istio.io/v1beta1
kind: VirtualService
metadata:
  name: product
spec:
  hosts:
    - product
  http:
  - route:
    - destination:
        host: product
    fault:
      delay:
        fixedDelay: 3s
        percent: 100
```

## 9-5. `kubernetes/resilience-tests/product-virtual-service-with-faults.yml` (새 파일)
```bash
code kubernetes/resilience-tests/product-virtual-service-with-faults.yml
```
붙여넣기 (새 파일)
```yaml
apiVersion: networking.istio.io/v1beta1
kind: VirtualService
metadata:
  name: product
spec:
  hosts:
    - product
  http:
  - route:
    - destination:
        host: product
    fault:
      abort:
        httpStatus: 500
        percentage:
          value: 20
```

## 9-6. `kubernetes/routing-tests/split-traffic-between-old-and-new-services.bash` (새 파일)
```bash
mkdir -p kubernetes/routing-tests
code kubernetes/routing-tests/split-traffic-between-old-and-new-services.bash
```
붙여넣기 (새 파일)
```bash
#!/usr/bin/env bash

set -e

old_percentage=$1
new_percentage=$2

kubectl patch virtualservice product --type=json -p="[
  {\"op\": \"add\", \"path\": \"/spec/http/1/route/0/weight\", \"value\": ${old_percentage}},
  {\"op\": \"add\", \"path\": \"/spec/http/1/route/1/weight\", \"value\": ${new_percentage}}
]"

kubectl patch virtualservice recommendation --type=json -p="[
  {\"op\": \"add\", \"path\": \"/spec/http/1/route/0/weight\", \"value\": ${old_percentage}},
  {\"op\": \"add\", \"path\": \"/spec/http/1/route/1/weight\", \"value\": ${new_percentage}}
]"

kubectl patch virtualservice review --type=json -p="[
  {\"op\": \"add\", \"path\": \"/spec/http/1/route/0/weight\", \"value\": ${old_percentage}},
  {\"op\": \"add\", \"path\": \"/spec/http/1/route/1/weight\", \"value\": ${new_percentage}}
]"
```

실행할 수 있게 권한을 준다(붙여넣기로 만든 파일은 실행 권한이 없다).
<!-- run -->
```bash
chmod +x kubernetes/routing-tests/split-traffic-between-old-and-new-services.bash
```

다음: [10-build-compose-test.md](10-build-compose-test.md)
