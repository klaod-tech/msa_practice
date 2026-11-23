# 05. 헬름 공통 차트 (common)

> 시작: 교수님 `Chapter15` 상태 / 결과: 교수님 `Chapter16`과 같은 코드(교재 영어 주석만 빠짐).
> 표기: `★ 추가` / `★ 변경 (전: ...)` / `★ 삭제` = 이전 장 대비 바뀌는 줄. md 화면에서 Ctrl+F로 `★`를 찾으면 바뀐 줄만 볼 수 있다.

모든 서비스가 같이 쓰는 템플릿. `_deployment.yaml`(디플로이먼트: 이미지, 환경 변수, 프로브, 리소스), `_service.yaml`(서비스), `_configmap_from_file.yaml`(config-repo 파일을 ConfigMap으로), `_secrets.yaml`(비밀값), `_helpers.tpl`(이름 규칙).

## 5-1. `kubernetes/helm/common/.helmignore` (새 파일)
```bash
mkdir -p kubernetes/helm/common
code kubernetes/helm/common/.helmignore
```
붙여넣기 (새 파일)
```
# Patterns to ignore when building packages.
# This supports shell glob matching, relative path matching, and
# negation (prefixed with !). Only one pattern per line.
.DS_Store
# Common VCS dirs
.git/
.gitignore
.bzr/
.bzrignore
.hg/
.hgignore
.svn/
# Common backup files
*.swp
*.bak
*.tmp
*~
# Various IDEs
.project
.idea/
*.tmproj
```

## 5-2. `kubernetes/helm/common/Chart.yaml` (새 파일)
```bash
code kubernetes/helm/common/Chart.yaml
```
붙여넣기 (새 파일)
```yaml
apiVersion: v2
name: common
type: library
description: A Helm library chart with shared definitions
version: 1.0.0
appVersion: "1.0"
```

## 5-3. `kubernetes/helm/common/templates/_configmap_from_file.yaml` (새 파일)
```bash
mkdir -p kubernetes/helm/common/templates
code kubernetes/helm/common/templates/_configmap_from_file.yaml
```
붙여넣기 (새 파일)
```yaml
{{- define "common.configmap_from_file" -}}
apiVersion: v1
kind: ConfigMap
metadata:
  name: {{ include "common.fullname" . }}
  labels:
    app.kubernetes.io/name: {{ include "common.name" . }}
    helm.sh/chart: {{ include "common.chart" . }}
    app.kubernetes.io/managed-by: {{ .Release.Service }}
data:
{{ (.Files.Glob "config-repo/*").AsConfig | indent 2 }}
{{- end -}}
```

## 5-4. `kubernetes/helm/common/templates/_deployment.yaml` (새 파일)
```bash
code kubernetes/helm/common/templates/_deployment.yaml
```
붙여넣기 (새 파일)
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
      labels:
        app.kubernetes.io/name: {{ include "common.name" . }}
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

## 5-5. `kubernetes/helm/common/templates/_helpers.tpl` (새 파일)
```bash
code kubernetes/helm/common/templates/_helpers.tpl
```
붙여넣기 (새 파일)
```
{{/*
Expand the name of the chart.
*/}}
{{- define "common.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Create a default fully qualified app name.
We truncate at 63 chars because some Kubernetes name fields are limited to this (by the DNS naming spec).
If release name contains chart name it will be used as a full name.
*/}}
{{- define "common.fullname" -}}
{{- if .Values.fullnameOverride }}
{{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- $name := default .Chart.Name .Values.nameOverride }}
{{- if contains $name .Release.Name }}
{{- .Release.Name | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- printf "%s-%s" .Release.Name $name | trunc 63 | trimSuffix "-" }}
{{- end }}
{{- end }}
{{- end }}

{{/*
Create chart name and version as used by the chart label.
*/}}
{{- define "common.chart" }}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}
```

## 5-6. `kubernetes/helm/common/templates/_secrets.yaml` (새 파일)
```bash
code kubernetes/helm/common/templates/_secrets.yaml
```
붙여넣기 (새 파일)
```yaml
{{- define "common.secrets" -}}
{{- range $secretName, $secretMap := .Values.secrets }}
apiVersion: v1
kind: Secret
metadata:
  name: {{ $secretName }}
  labels:
    app.kubernetes.io/name: {{ $secretName }}
    helm.sh/chart: {{ include "common.chart" $ }}
    app.kubernetes.io/managed-by: {{ $.Release.Service }}
type: Opaque
data:
{{- range $key, $val := $secretMap }}
  {{ $key }}: {{ $val | b64enc }}
{{- end }}
---
{{- end -}}
{{- end -}}
```

## 5-7. `kubernetes/helm/common/templates/_service.yaml` (새 파일)
```bash
code kubernetes/helm/common/templates/_service.yaml
```
붙여넣기 (새 파일)
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
  name: {{ include "common.fullname" . }}
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

## 5-8. `kubernetes/helm/common/values.yaml` (새 파일)
```bash
code kubernetes/helm/common/values.yaml
```
붙여넣기 (새 파일)
```yaml
replicaCount: 1

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
