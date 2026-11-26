# 06. 헬름 공통 차트: 이스티오 템플릿

> 시작: 17장까지 따라 한 상태 / 결과: 18장 코드(우리 Lombok 데이터 클래스 유지, 교재 영어 주석은 뺌).
> 표기: 한 줄은 줄 끝 `★ 추가` / `★ 변경 (전: ...)`, 여러 줄은 `★ 추가 시작 (N줄)` ~ `★ 추가 끝`, `★ 변경 시작` ~ `★ 변경 끝`(안의 `★ 전:`은 지울 옛 코드), `★ 삭제 시작` ~ `★ 삭제 끝`(안의 `★ 지움:` 줄을 지운다). `★` 줄은 안내용이라 치지 않아도 된다. Ctrl+F로 `★`를 찾으면 바뀐 곳만 볼 수 있다.

`_istio_base.yaml`(Gateway, VirtualService, 인증 정책), `_istio_dr_mutual_tls.yaml`(mTLS DestinationRule), `_istio_vs_green_blue_deploy.yaml`(`X-group` 헤더나 가중치로 blue/green 나누기).

## 6-1. `kubernetes/helm/common/templates/_deployment.yaml`
```bash
code kubernetes/helm/common/templates/_deployment.yaml
```
아래처럼 맞추기 (전체 파일)
```yaml
{{- define "common.deployment" -}}
{{- $common := dict "Values" .Values.common -}}
{{- $noCommon := omit .Values "common" -}}
{{- $overrides := dict "Values" $noCommon -}}
{{- $noValues := omit . "Values" -}}
{{- with merge $noValues $overrides $common -}}
apiVersion: apps/v1
kind: Deployment
metadata:
  name: {{ include "common.fullname" . }}
  labels:
    app.kubernetes.io/name: {{ include "common.name" . }}
    helm.sh/chart: {{ include "common.chart" . }}
    app.kubernetes.io/managed-by: {{ .Release.Service }}
spec:
  replicas: {{ .Values.replicaCount }}
  selector:
    matchLabels:
      app.kubernetes.io/name: {{ include "common.name" . }}
  template:
    metadata:
# ★ 추가 시작 (6줄)
{{- if .Values.annotations }}
{{- with .Values.annotations }}
      annotations:
{{ toYaml . | indent 8 }}
{{- end }}
{{- end }}
# ★ 추가 끝
      labels:
        app.kubernetes.io/name: {{ include "common.name" . }}
        # ★ 추가 시작 (2줄)
        app: {{ include "common.name" . }}
        version: {{ .Values.image.tag }}
        # ★ 추가 끝
    spec:
      containers:
        - name: {{ .Chart.Name }}
          image: "{{ .Values.image.repository }}/{{ .Values.image.name }}:{{ .Values.image.tag }}"
          imagePullPolicy: {{ .Values.image.pullPolicy }}
          {{- with .Values.image.args }}
          args:
            {{- toYaml . | nindent 12 }}
          {{- end }}
          {{- if .Values.env }}
          env:
          {{- range $key, $val := .Values.env }}
          - name: {{ $key }}
            value: {{ $val }}
          {{- end }}
          {{- end }}
          {{- if .Values.envFromSecretRefs }}
          envFrom:
          {{- range .Values.envFromSecretRefs }}
          - secretRef:
              name: {{ . }}
          {{- end }}
          {{- end }}
          {{- if .Values.livenessProbe_enabled }}
          livenessProbe:
{{ toYaml .Values.livenessProbe | indent 12 }}
          {{- end }}
          {{- if .Values.readinessProbe_enabled }}
          readinessProbe:
{{ toYaml .Values.readinessProbe | indent 12 }}
          {{- end }}
          ports:
{{ toYaml .Values.ports | indent 12 }}
          resources:
{{ toYaml .Values.resources | indent 12 }}
      {{- if .Values.configmap.enabled }}
          volumeMounts:
          - name: {{ include "common.fullname" . }}
            mountPath: {{ .Values.configmap.volumeMounts.mountPath }}
      volumes:
        - name: {{ include "common.fullname" . }}
          configMap:
            name: {{ include "common.fullname" . }}
      {{- end }}
{{- end -}}
{{- end -}}
```

## 6-2. `kubernetes/helm/common/templates/_istio_base.yaml` (새 파일)
```bash
code kubernetes/helm/common/templates/_istio_base.yaml
```
붙여넣기 (새 파일)
```yaml
{{- define "common.istio_base" -}}
apiVersion: security.istio.io/v1beta1
kind: AuthorizationPolicy
metadata:
  name: product-composite-require-jwt
spec:
  action: ALLOW
  rules:
  - {}
  selector:
    matchLabels:
      app.kubernetes.io/name: product-composite
---
apiVersion: security.istio.io/v1beta1
kind: PeerAuthentication
metadata:
  name: default
spec:
  mtls:
    mode: PERMISSIVE
---
apiVersion: security.istio.io/v1beta1
kind: RequestAuthentication
metadata:
  name: product-composite-request-authentication
spec:
  jwtRules:
  - forwardOriginalToken: true
    issuer: http://auth-server
    jwksUri: http://auth-server.hands-on.svc.cluster.local/oauth2/jwks
  selector:
    matchLabels:
      app.kubernetes.io/name: product-composite
---
apiVersion: networking.istio.io/v1beta1
kind: Gateway
metadata:
  name: hands-on-gw
spec:
  selector:
    istio: ingressgateway
  servers:
  - hosts:
    - minikube.me
    port:
      name: https
      number: 443
      protocol: HTTPS
    tls:
      credentialName: hands-on-certificate
      mode: SIMPLE
---
apiVersion: networking.istio.io/v1beta1
kind: VirtualService
metadata:
  name: hands-on-vs
spec:
  gateways:
  - hands-on-gw
  hosts:
  - minikube.me
  http:
  - match:
    - uri:
        prefix: /oauth2
    route:
    - destination:
        host: auth-server
  - match:
    - uri:
        prefix: /login
    route:
    - destination:
        host: auth-server
  - match:
    - uri:
        prefix: /error
    route:
    - destination:
        host: auth-server
  - match:
    - uri:
        prefix: /product-composite
    route:
    - destination:
        host: product-composite
        port:
          number: 80
  - match:
    - uri:
        prefix: /openapi
    route:
    - destination:
        host: product-composite
        port:
          number: 80
---
apiVersion: networking.istio.io/v1beta1
kind: Gateway
metadata:
  name: health-gateway
spec:
  selector:
    istio: ingressgateway
  servers:
  - hosts:
    - "health.minikube.me"
    port:
      number: 443
      name: https-health
      protocol: HTTPS
    tls:
      mode: SIMPLE
      credentialName: hands-on-certificate
---
apiVersion: networking.istio.io/v1beta1
kind: VirtualService
metadata:
  name: health-vs
spec:
  hosts:
  - "health.minikube.me"
  gateways:
  - health-gateway
  http:
  - route:
    - destination:
        host: product-composite
        port:
          number: 4004
{{- end -}}
```

## 6-3. `kubernetes/helm/common/templates/_istio_dr_mutual_tls.yaml` (새 파일)
```bash
code kubernetes/helm/common/templates/_istio_dr_mutual_tls.yaml
```
붙여넣기 (새 파일)
```yaml
{{- define "common.istio_dr_mutual_tls" -}}
{{- range $idx, $dr := .Values.destinationRules }}
apiVersion: networking.istio.io/v1beta1
kind: DestinationRule
metadata:
  name: {{ $dr.name }}
spec:
  host: {{ $dr.name }}
{{- if $dr.subsets }}
{{- with $dr.subsets }}
  subsets:
{{ toYaml . | indent 2 }}
{{- end }}
{{- end }}
  trafficPolicy:
    tls:
      mode: ISTIO_MUTUAL
---
{{- end -}}
{{- end -}}
```

## 6-4. `kubernetes/helm/common/templates/_istio_vs_green_blue_deploy.yaml` (새 파일)
```bash
code kubernetes/helm/common/templates/_istio_vs_green_blue_deploy.yaml
```
붙여넣기 (새 파일)
```yaml
{{- define "common.istio_vs_green_blue_deploy" -}}
{{- range $name := .Values.virtualServices }}
apiVersion: networking.istio.io/v1beta1
kind: VirtualService
metadata:
  name: {{ $name }}
spec:
  hosts:
  - {{ $name }}
  http:
  - match:
    - headers:
        X-group:
          exact: test
    route:
    - destination:
        host: {{ $name }}
        subset: new
  - route:
    - destination:
        host: {{ $name }}
        subset: old
      weight: 100
    - destination:
        host: {{ $name }}
        subset: new
      weight: 0
---
{{- end -}}
{{- end -}}
```

## 6-5. `kubernetes/helm/common/templates/_service.yaml`
```bash
code kubernetes/helm/common/templates/_service.yaml
```
아래처럼 맞추기 (전체 파일)
```yaml
{{- define "common.service" -}}
{{- $common := dict "Values" .Values.common -}}
{{- $noCommon := omit .Values "common" -}}
{{- $overrides := dict "Values" $noCommon -}}
{{- $noValues := omit . "Values" -}}
{{- with merge $noValues $overrides $common -}}
apiVersion: v1
kind: Service
metadata:
  name: {{ include "common.name" . }}  # ★ 변경 (전: name: {{ include "common.fullname" . }})
  labels:
    app.kubernetes.io/name: {{ include "common.name" . }}
    helm.sh/chart: {{ include "common.chart" . }}
    app.kubernetes.io/managed-by: {{ .Release.Service }}
spec:
  type: {{ .Values.service.type }}
  ports:
{{ toYaml .Values.service.ports | indent 4 }}
  selector:
    app.kubernetes.io/name: {{ include "common.name" . }}
{{- end -}}
{{- end -}}
```

## 6-6. `kubernetes/helm/common/values.yaml`
```bash
code kubernetes/helm/common/values.yaml
```
아래처럼 맞추기 (전체 파일)
```yaml
replicaCount: 1

ingress:
  annotations:
    cert-manager.io/issuer: selfsigned
  tls:
    secretName: tls-certificate

image:
  repository: hands-on
  tag: latest
  pullPolicy: Never
  args: []

ports:
  - name: http
    containerPort: 80
    protocol: TCP

livenessProbe_enabled: false
livenessProbe:
  httpGet:
    scheme: HTTP
    path: /actuator/health/liveness
    port: 4004  # ★ 변경 (전: port: 80)
  initialDelaySeconds: 10
  periodSeconds: 10
  timeoutSeconds: 2
  failureThreshold: 20
  successThreshold: 1

readinessProbe_enabled: false
readinessProbe:
  httpGet:
    scheme: HTTP
    path: /actuator/health/readiness
    port: 4004  # ★ 변경 (전: port: 80)
  initialDelaySeconds: 10
  periodSeconds: 10
  timeoutSeconds: 2
  failureThreshold: 3
  successThreshold: 1

configmap:
  enabled: false

resources:
  limits:
    memory: 400Mi

service:
  type: ClusterIP
  ports:
  - port: 80
    targetPort: http
    protocol: TCP
    name: http
```

## 6-7. 지울 파일
```bash
cd ~/msa
rm kubernetes/helm/common/templates/_ingress.yaml
rm kubernetes/helm/common/templates/_issuer.yaml
```

다음: [07-k8s-components.md](07-k8s-components.md)
