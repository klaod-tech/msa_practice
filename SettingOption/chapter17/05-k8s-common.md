# 05. 헬름 공통 차트: ConfigMap, Ingress, 인증서 템플릿

> 시작: 16장까지 따라 한 상태 / 결과: 17장 코드(우리 Lombok 데이터 클래스 유지, 교재 영어 주석은 뺌).
> 표기: 한 줄은 줄 끝 `★ 추가` / `★ 변경 (전: ...)`, 여러 줄은 `★ 추가 시작 (N줄)` ~ `★ 추가 끝`, `★ 변경 시작` ~ `★ 변경 끝`(안의 `★ 전:`은 지울 옛 코드), `★ 삭제 시작` ~ `★ 삭제 끝`(안의 `★ 지움:` 줄을 지운다). `★` 줄은 안내용이라 치지 않아도 된다. Ctrl+F로 `★`를 찾으면 바뀐 곳만 볼 수 있다.

`_configmap_from_file.yaml`로 서비스 차트마다 자기 설정 파일을 ConfigMap으로 만들고, `_ingress.yaml`, `_issuer.yaml`로 Ingress와 cert-manager 발급자를 만든다.

## 5-1. `kubernetes/helm/common/templates/_ingress.yaml` (새 파일)
```bash
code kubernetes/helm/common/templates/_ingress.yaml
```
붙여넣기 (새 파일)
```yaml
{{- define "common.ingress" -}}
{{- $common := dict "Values" .Values.common -}}
{{- $noCommon := omit .Values "common" -}}
{{- $overrides := dict "Values" $noCommon -}}
{{- $noValues := omit . "Values" -}}
{{- with merge $noValues $overrides $common -}}
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: {{ include "common.fullname" . }}
  labels:
    app.kubernetes.io/name: {{ include "common.name" . }}
    helm.sh/chart: {{ include "common.chart" . }}
    app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- with .Values.ingress.annotations }}
  annotations:
{{ toYaml . | indent 4 }}
{{- end }}
spec:
  tls:
    - hosts:
        - {{ .Values.ingress.host | quote }}
      secretName: {{ .Values.ingress.tls.secretName }}
  rules:
    - host: {{ .Values.ingress.host | quote }}
      http:
        paths:
        {{- range .Values.ingress.paths }}
          - path: {{ .path }}
            pathType: Prefix
            backend:
              service:
                name: {{ .service }}
                port:
                  name: http

        {{- end }}
{{- end }}
{{- end -}}
```

## 5-2. `kubernetes/helm/common/templates/_issuer.yaml` (새 파일)
```bash
code kubernetes/helm/common/templates/_issuer.yaml
```
붙여넣기 (새 파일)
```yaml
{{- define "common.issuer" -}}
apiVersion: cert-manager.io/v1
kind: Issuer
metadata:
  name: selfsigned
spec:
  selfSigned: {}
{{- end -}}
```

## 5-3. `kubernetes/helm/common/values.yaml`
```bash
code kubernetes/helm/common/values.yaml
```
아래처럼 맞추기 (전체 파일)
```yaml
replicaCount: 1

# ★ 추가 시작 (5줄)
ingress:
  annotations:
    cert-manager.io/issuer: selfsigned
  tls:
    secretName: tls-certificate
# ★ 추가 끝

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
    port: 80
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
    port: 80
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

다음: [06-k8s-components.md](06-k8s-components.md)
