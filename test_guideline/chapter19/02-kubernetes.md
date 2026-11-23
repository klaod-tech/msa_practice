# 02. Fluentd (kubernetes/efk)

> 시작: 교수님 `Chapter18` 상태 / 결과: 교수님 `Chapter19`과 같은 코드(교재 영어 주석만 빠짐).
> 표기: `★ 추가` / `★ 변경 (전: ...)` / `★ 삭제` = 이전 장 대비 바뀌는 줄. md 화면에서 Ctrl+F로 `★`를 찾으면 바뀐 줄만 볼 수 있다.

`Dockerfile`: 공식 Fluentd 이미지에 Elasticsearch 플러그인을 더한 이미지. `fluentd-ds.yml`: 노드마다 Fluentd를 띄우는 데몬셋. `fluentd-hands-on-configmap.yml`: 스프링 부트 로그 형식을 파싱하는 Fluentd 설정. `kibana-api-exported-objects.ndjson`: Kibana에 가져올 검색·대시보드.

## 2-1. `kubernetes/efk/fluentd-ds.yml` (새 파일)
```bash
mkdir -p kubernetes/efk
code kubernetes/efk/fluentd-ds.yml
```
붙여넣기 (새 파일)
```yaml
apiVersion: apps/v1
kind: DaemonSet
metadata:
  name: fluentd
  namespace: kube-system
  labels:
    app: fluentd
    version: v1
spec:
  selector:
    matchLabels:
        app: fluentd
        version: v1
  template:
    metadata:
      labels:
        app: fluentd
        version: v1
    spec:
      tolerations:
      - key: node-role.kubernetes.io/master
        effect: NoSchedule
      containers:
      - name: fluentd
        image: hands-on/fluentd:v1
        env:
          - name:  FLUENT_ELASTICSEARCH_HOST
            value: "elasticsearch.logging"
          - name:  FLUENT_ELASTICSEARCH_PORT
            value: "9200"
          - name: FLUENT_ELASTICSEARCH_SCHEME
            value: "http"
          - name: FLUENT_ELASTICSEARCH_SED_DISABLE
            value: "true"
        resources:
          limits:
            cpu: 500m
            memory: 500Mi
          requests:
            cpu: 100m
            memory: 200Mi
        volumeMounts:
        - name: varlog
          mountPath: /var/log
        - name: varlibdockercontainers
          mountPath: /var/lib/docker/containers
          readOnly: true
        - name: journal
          mountPath: /var/log/journal
          readOnly: true
        - name: fluentd-extra-config
          mountPath: /fluentd/etc/conf.d
      volumes:
      - name: varlog
        hostPath:
          path: /var/log
      - name: varlibdockercontainers
        hostPath:
          path: /var/lib/docker/containers
      - name: journal
        hostPath:
          path: /run/log/journal
      - name: fluentd-extra-config
        configMap:
          name: "fluentd-hands-on-config"
      terminationGracePeriodSeconds: 30
```

## 2-2. `kubernetes/efk/fluentd-hands-on-configmap.yml` (새 파일)
```bash
code kubernetes/efk/fluentd-hands-on-configmap.yml
```
붙여넣기 (새 파일)
```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: fluentd-hands-on-config
  namespace: kube-system
data:
  fluentd-hands-on.conf: |

    <match kubernetes.**istio**>
      @type rewrite_tag_filter
      <rule>
        key log
        pattern ^(.*)$
        tag istio.${tag}
      </rule>
    </match>

    <match kubernetes.**hands-on**>
      @type rewrite_tag_filter
      <rule>
        key log
        pattern ^(.*)$
        tag spring-boot.${tag}
      </rule>
    </match>

    <match spring-boot.**>
      @type rewrite_tag_filter
      <rule>
        key log
        pattern /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}\.\d{3}([-+]\d{2}:\d{2}|Z).*/
        tag parse.${tag}
      </rule>
      # Get rid of Reactor debug info:
      #
      #   Suppressed: reactor.core.publisher.FluxOnAssembly$OnAssemblyException:
      # Error has been observed at the following site(s):
      #   *__checkpoint ⇢ Handler se.magnus.microservices.core.product.services.ProductServiceImpl#getProduct(HttpHeaders, int, int, int) [DispatcherHandler]
      #   *__checkpoint ⇢ org.springframework.web.filter.reactive.ServerHttpObservationFilter [DefaultWebFilterChain]
      #   *__checkpoint ⇢ HTTP GET "/product/1?faultPercent=100" [ExceptionHandlingWebHandler]
      # Original Stack Trace:
      <rule>
        key log
        pattern /^\s+Suppressed:.*$/
        tag skip.${tag}
      </rule>
      <rule>
        key log
        pattern /^Error has been observed at the following site.*/
        tag skip.${tag}
      </rule>
      <rule>
        key log
        pattern /^\s+\*__checkpoint.*/
        tag skip.${tag}
      </rule>
      <rule>
        key log
        pattern /^Original Stack Trace:.*/
        tag skip.${tag}
      </rule>

      <rule>
        key log
        pattern /^.*/
        tag check.exception.${tag}
      </rule>
    </match>

    <match skip.spring-boot.**>
      @type null
    </match>

    <match check.exception.spring-boot.**>
      @type detect_exceptions
      languages java
      remove_tag_prefix check
      message log
      multiline_flush_interval 5
    </match>

    <filter parse.spring-boot.**>
      @type parser
      key_name log
      time_key time
      time_format %Y-%m-%dT%H:%M:%S.%N
      reserve_data true
      # Sample log messages:
      # 2025-03-12T20:03:53.168+01:00  INFO 1 --- [     parallel-3] [product-composite,67d1e8a919c841e2c7a0fc30e2585f78,0522b62d4c2f4998] s.m.m.c.p.s.ProductCompositeServiceImpl  : Will get composite product info for product.id=1
      # 2025-03-12T20:03:53.168Z  INFO 1 --- [     parallel-3] [product-composite,67d1e8a919c841e2c7a0fc30e2585f78,0522b62d4c2f4998] s.m.m.c.p.s.ProductCompositeServiceImpl  : Will get composite product info for product.id=1
      format /^(?<time>\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}\.\d{3}([-+]\d{2}:\d{2}|Z))\s+(?<spring.level>[^\s]+)\s+(?<spring.pid>\d+)\s+---\s+\[\s*(?<spring.thread>[^\]]+)\]\s+(\[(?<spring.service>[^,]*),(?<spring.trace>[^,]*),(?<spring.span>[^\]]*)]*\])\s+(?<spring.class>[^\s]+)\s*:\s+(?<log>.*)$/
    </filter>
```

## 2-3. `kubernetes/efk/kibana-api-exported-objects.ndjson` (새 파일)
```bash
code kubernetes/efk/kibana-api-exported-objects.ndjson
```
붙여넣기 (새 파일)
```
{"attributes":{"fields":"[{\"name\":\"@timestamp\",\"type\":\"date\",\"esTypes\":[\"date\"],\"count\":0,\"scripted\":false,\"searchable\":true,\"aggregatable\":true,\"readFromDocValues\":true},{\"name\":\"BOOT_ID\",\"type\":\"string\",\"esTypes\":[\"text\"],\"count\":0,\"scripted\":false,\"searchable\":true,\"aggregatable\":false,\"readFromDocValues\":false},{\"name\":\"BOOT_ID.keyword\",\"type\":\"string\",\"esTypes\":[\"keyword\"],\"count\":0,\"scripted\":false,\"searchable\":true,\"aggregatable\":true,\"readFromDocValues\":true,\"parent\":\"BOOT_ID\",\"subType\":\"multi\"},{\"name\":\"CAP_EFFECTIVE\",\"type\":\"string\",\"esTypes\":[\"text\"],\"count\":0,\"scripted\":false,\"searchable\":true,\"aggregatable\":false,\"readFromDocValues\":false},{\"name\":\"CAP_EFFECTIVE.keyword\",\"type\":\"string\",\"esTypes\":[\"keyword\"],\"count\":0,\"scripted\":false,\"searchable\":true,\"aggregatable\":true,\"readFromDocValues\":true,\"parent\":\"CAP_EFFECTIVE\",\"subType\":\"multi\"},{\"name\":\"CMDLINE\",\"type\":\"string\",\"esTypes\":[\"text\"],\"count\":0,\"scripted\":false,\"searchable\":true,\"aggregatable\":false,\"readFromDocValues\":false},{\"name\":\"CMDLINE.keyword\",\"type\":\"string\",\"esTypes\":[\"keyword\"],\"count\":0,\"scripted\":false,\"searchable\":true,\"aggregatable\":true,\"readFromDocValues\":true,\"parent\":\"CMDLINE\",\"subType\":\"multi\"},{\"name\":\"COMM\",\"type\":\"string\",\"esTypes\":[\"text\"],\"count\":0,\"scripted\":false,\"searchable\":true,\"aggregatable\":false,\"readFromDocValues\":false},{\"name\":\"COMM.keyword\",\"type\":\"string\",\"esTypes\":[\"keyword\"],\"count\":0,\"scripted\":false,\"searchable\":true,\"aggregatable\":true,\"readFromDocValues\":true,\"parent\":\"COMM\",\"subType\":\"multi\"},{\"name\":\"EXE\",\"type\":\"string\",\"esTypes\":[\"text\"],\"count\":0,\"scripted\":false,\"searchable\":true,\"aggregatable\":false,\"readFromDocValues\":false},{\"name\":\"EXE.keyword\",\"type\":\"string\",\"esTypes\":[\"keyword\"],\"count\":0,\"scripted\":false,\"searchable\":true,\"aggregatable\":true,\"readFromDocValues\":true,\"parent\":\"EXE\",\"subType\":\"multi\"},{\"name\":\"GID\",\"type\":\"string\",\"esTypes\":[\"text\"],\"count\":0,\"scripted\":false,\"searchable\":true,\"aggregatable\":false,\"readFromDocValues\":false},{\"name\":\"GID.keyword\",\"type\":\"string\",\"esTypes\":[\"keyword\"],\"count\":0,\"scripted\":false,\"searchable\":true,\"aggregatable\":true,\"readFromDocValues\":true,\"parent\":\"GID\",\"subType\":\"multi\"},{\"name\":\"HOSTNAME\",\"type\":\"string\",\"esTypes\":[\"text\"],\"count\":0,\"scripted\":false,\"searchable\":true,\"aggregatable\":false,\"readFromDocValues\":false},{\"name\":\"HOSTNAME.keyword\",\"type\":\"string\",\"esTypes\":[\"keyword\"],\"count\":0,\"scripted\":false,\"searchable\":true,\"aggregatable\":true,\"readFromDocValues\":true,\"parent\":\"HOSTNAME\",\"subType\":\"multi\"},{\"name\":\"MACHINE_ID\",\"type\":\"string\",\"esTypes\":[\"text\"],\"count\":0,\"scripted\":false,\"searchable\":true,\"aggregatable\":false,\"readFromDocValues\":false},{\"name\":\"MACHINE_ID.keyword\",\"type\":\"string\",\"esTypes\":[\"keyword\"],\"count\":0,\"scripted\":false,\"searchable\":true,\"aggregatable\":true,\"readFromDocValues\":true,\"parent\":\"MACHINE_ID\",\"subType\":\"multi\"},{\"name\":\"MESSAGE\",\"type\":\"string\",\"esTypes\":[\"text\"],\"count\":0,\"scripted\":false,\"searchable\":true,\"aggregatable\":false,\"readFromDocValues\":false},{\"name\":\"MESSAGE.keyword\",\"type\":\"string\",\"esTypes\":[\"keyword\"],\"count\":0,\"scripted\":false,\"searchable\":true,\"aggregatable\":true,\"readFromDocValues\":true,\"parent\":\"MESSAGE\",\"subType\":\"multi\"},{\"name\":\"PID\",\"type\":\"string\",\"esTypes\":[\"text\"],\"count\":0,\"scripted\":false,\"searchable\":true,\"aggregatable\":false,\"readFromDocValues\":false},{\"name\":\"PID.keyword\",\"type\":\"string\",\"esTypes\":[\"keyword\"],\"count\":0,\"scripted\":false,\"searchable\":true,\"aggregatable\":true,\"readFromDocValues\":true,\"parent\":\"PID\",\"subType\":\"multi\"},{\"name\":\"PRIORITY\",\"type\":\"string\",\"esTypes\":[\"text\"],\"count\":0,\"scripted\":false,\"searchable\":true,\"aggregatable\":false,\"readFromDocValues\":false},{\"name\":\"PRIORITY.keyword\",\"type\":\"string\",\"esTypes\":[\"keyword\"],\"count\":0,\"scripted\":false,\"searchable\":true,\"aggregatable\":true,\"readFromDocValues\":true,\"parent\":\"PRIORITY\",\"subType\":\"multi\"},{\"name\":\"STREAM_ID\",\"type\":\"string\",\"esTypes\":[\"text\"],\"count\":0,\"scripted\":false,\"searchable\":true,\"aggregatable\":false,\"readFromDocValues\":false},{\"name\":\"STREAM_ID.keyword\",\"type\":\"string\",\"esTypes\":[\"keyword\"],\"count\":0,\"scripted\":false,\"searchable\":true,\"aggregatable\":true,\"readFromDocValues\":true,\"parent\":\"STREAM_ID\",\"subType\":\"multi\"},{\"name\":\"SYSLOG_FACILITY\",\"type\":\"string\",\"esTypes\":[\"text\"],\"count\":0,\"scripted\":false,\"searchable\":true,\"aggregatable\":false,\"readFromDocValues\":false},{\"name\":\"SYSLOG_FACILITY.keyword\",\"type\":\"string\",\"esTypes\":[\"keyword\"],\"count\":0,\"scripted\":false,\"searchable\":true,\"aggregatable\":true,\"readFromDocValues\":true,\"parent\":\"SYSLOG_FACILITY\",\"subType\":\"multi\"},{\"name\":\"SYSLOG_IDENTIFIER\",\"type\":\"string\",\"esTypes\":[\"text\"],\"count\":0,\"scripted\":false,\"searchable\":true,\"aggregatable\":false,\"readFromDocValues\":false},{\"name\":\"SYSLOG_IDENTIFIER.keyword\",\"type\":\"string\",\"esTypes\":[\"keyword\"],\"count\":0,\"scripted\":false,\"searchable\":true,\"aggregatable\":true,\"readFromDocValues\":true,\"parent\":\"SYSLOG_IDENTIFIER\",\"subType\":\"multi\"},{\"name\":\"SYSTEMD_CGROUP\",\"type\":\"string\",\"esTypes\":[\"text\"],\"count\":0,\"scripted\":false,\"searchable\":true,\"aggregatable\":false,\"readFromDocValues\":false},{\"name\":\"SYSTEMD_CGROUP.keyword\",\"type\":\"string\",\"esTypes\":[\"keyword\"],\"count\":0,\"scripted\":false,\"searchable\":true,\"aggregatable\":true,\"readFromDocValues\":true,\"parent\":\"SYSTEMD_CGROUP\",\"subType\":\"multi\"},{\"name\":\"SYSTEMD_INVOCATION_ID\",\"type\":\"string\",\"esTypes\":[\"text\"],\"count\":0,\"scripted\":false,\"searchable\":true,\"aggregatable\":false,\"readFromDocValues\":false},{\"name\":\"SYSTEMD_INVOCATION_ID.keyword\",\"type\":\"string\",\"esTypes\":[\"keyword\"],\"count\":0,\"scripted\":false,\"searchable\":true,\"aggregatable\":true,\"readFromDocValues\":true,\"parent\":\"SYSTEMD_INVOCATION_ID\",\"subType\":\"multi\"},{\"name\":\"SYSTEMD_SLICE\",\"type\":\"string\",\"esTypes\":[\"text\"],\"count\":0,\"scripted\":false,\"searchable\":true,\"aggregatable\":false,\"readFromDocValues\":false},{\"name\":\"SYSTEMD_SLICE.keyword\",\"type\":\"string\",\"esTypes\":[\"keyword\"],\"count\":0,\"scripted\":false,\"searchable\":true,\"aggregatable\":true,\"readFromDocValues\":true,\"parent\":\"SYSTEMD_SLICE\",\"subType\":\"multi\"},{\"name\":\"SYSTEMD_UNIT\",\"type\":\"string\",\"esTypes\":[\"text\"],\"count\":0,\"scripted\":false,\"searchable\":true,\"aggregatable\":false,\"readFromDocValues\":false},{\"name\":\"SYSTEMD_UNIT.keyword\",\"type\":\"string\",\"esTypes\":[\"keyword\"],\"count\":0,\"scripted\":false,\"searchable\":true,\"aggregatable\":true,\"readFromDocValues\":true,\"parent\":\"SYSTEMD_UNIT\",\"subType\":\"multi\"},{\"name\":\"TRANSPORT\",\"type\":\"string\",\"esTypes\":[\"text\"],\"count\":0,\"scripted\":false,\"searchable\":true,\"aggregatable\":false,\"readFromDocValues\":false},{\"name\":\"TRANSPORT.keyword\",\"type\":\"string\",\"esTypes\":[\"keyword\"],\"count\":0,\"scripted\":false,\"searchable\":true,\"aggregatable\":true,\"readFromDocValues\":true,\"parent\":\"TRANSPORT\",\"subType\":\"multi\"},{\"name\":\"UID\",\"type\":\"string\",\"esTypes\":[\"text\"],\"count\":0,\"scripted\":false,\"searchable\":true,\"aggregatable\":false,\"readFromDocValues\":false},{\"name\":\"UID.keyword\",\"type\":\"string\",\"esTypes\":[\"keyword\"],\"count\":0,\"scripted\":false,\"searchable\":true,\"aggregatable\":true,\"readFromDocValues\":true,\"parent\":\"UID\",\"subType\":\"multi\"},{\"name\":\"_id\",\"type\":\"string\",\"esTypes\":[\"_id\"],\"count\":0,\"scripted\":false,\"searchable\":true,\"aggregatable\":true,\"readFromDocValues\":false},{\"name\":\"_index\",\"type\":\"string\",\"esTypes\":[\"_index\"],\"count\":0,\"scripted\":false,\"searchable\":true,\"aggregatable\":true,\"readFromDocValues\":false},{\"name\":\"_score\",\"type\":\"number\",\"count\":0,\"scripted\":false,\"searchable\":false,\"aggregatable\":false,\"readFromDocValues\":false},{\"name\":\"_source\",\"type\":\"_source\",\"esTypes\":[\"_source\"],\"count\":0,\"scripted\":false,\"searchable\":false,\"aggregatable\":false,\"readFromDocValues\":false},{\"name\":\"_type\",\"type\":\"string\",\"esTypes\":[\"_type\"],\"count\":0,\"scripted\":false,\"searchable\":true,\"aggregatable\":true,\"readFromDocValues\":false},{\"name\":\"docker.container_id\",\"type\":\"string\",\"esTypes\":[\"text\"],\"count\":0,\"scripted\":false,\"searchable\":true,\"aggregatable\":false,\"readFromDocValues\":false},{\"name\":\"docker.container_id.keyword\",\"type\":\"string\",\"esTypes\":[\"keyword\"],\"count\":0,\"scripted\":false,\"searchable\":true,\"aggregatable\":true,\"readFromDocValues\":true,\"parent\":\"docker.container_id\",\"subType\":\"multi\"},{\"name\":\"kubernetes.container_image\",\"type\":\"string\",\"esTypes\":[\"text\"],\"count\":2,\"scripted\":false,\"searchable\":true,\"aggregatable\":false,\"readFromDocValues\":false},{\"name\":\"kubernetes.container_image.keyword\",\"type\":\"string\",\"esTypes\":[\"keyword\"],\"count\":0,\"scripted\":false,\"searchable\":true,\"aggregatable\":true,\"readFromDocValues\":true,\"parent\":\"kubernetes.container_image\",\"subType\":\"multi\"},{\"name\":\"kubernetes.container_image_id\",\"type\":\"string\",\"esTypes\":[\"text\"],\"count\":0,\"scripted\":false,\"searchable\":true,\"aggregatable\":false,\"readFromDocValues\":false},{\"name\":\"kubernetes.container_image_id.keyword\",\"type\":\"string\",\"esTypes\":[\"keyword\"],\"count\":0,\"scripted\":false,\"searchable\":true,\"aggregatable\":true,\"readFromDocValues\":true,\"parent\":\"kubernetes.container_image_id\",\"subType\":\"multi\"},{\"name\":\"kubernetes.container_name\",\"type\":\"string\",\"esTypes\":[\"text\"],\"count\":1,\"scripted\":false,\"searchable\":true,\"aggregatable\":false,\"readFromDocValues\":false},{\"name\":\"kubernetes.container_name.keyword\",\"type\":\"string\",\"esTypes\":[\"keyword\"],\"count\":0,\"scripted\":false,\"searchable\":true,\"aggregatable\":true,\"readFromDocValues\":true,\"parent\":\"kubernetes.container_name\",\"subType\":\"multi\"},{\"name\":\"kubernetes.host\",\"type\":\"string\",\"esTypes\":[\"text\"],\"count\":0,\"scripted\":false,\"searchable\":true,\"aggregatable\":false,\"readFromDocValues\":false},{\"name\":\"kubernetes.host.keyword\",\"type\":\"string\",\"esTypes\":[\"keyword\"],\"count\":0,\"scripted\":false,\"searchable\":true,\"aggregatable\":true,\"readFromDocValues\":true,\"parent\":\"kubernetes.host\",\"subType\":\"multi\"},{\"name\":\"kubernetes.labels.addonmanager_kubernetes_io/mode\",\"type\":\"string\",\"esTypes\":[\"text\"],\"count\":0,\"scripted\":false,\"searchable\":true,\"aggregatable\":false,\"readFromDocValues\":false},{\"name\":\"kubernetes.labels.addonmanager_kubernetes_io/mode.keyword\",\"type\":\"string\",\"esTypes\":[\"keyword\"],\"count\":0,\"scripted\":false,\"searchable\":true,\"aggregatable\":true,\"readFromDocValues\":true,\"parent\":\"kubernetes.labels.addonmanager_kubernetes_io/mode\",\"subType\":\"multi\"},{\"name\":\"kubernetes.labels.app\",\"type\":\"string\",\"esTypes\":[\"text\"],\"count\":0,\"scripted\":false,\"searchable\":true,\"aggregatable\":false,\"readFromDocValues\":false},{\"name\":\"kubernetes.labels.app.keyword\",\"type\":\"string\",\"esTypes\":[\"keyword\"],\"count\":0,\"scripted\":false,\"searchable\":true,\"aggregatable\":true,\"readFromDocValues\":true,\"parent\":\"kubernetes.labels.app\",\"subType\":\"multi\"},{\"name\":\"kubernetes.labels.app_kubernetes_io/name\",\"type\":\"string\",\"esTypes\":[\"text\"],\"count\":0,\"scripted\":false,\"searchable\":true,\"aggregatable\":false,\"readFromDocValues\":false},{\"name\":\"kubernetes.labels.app_kubernetes_io/name.keyword\",\"type\":\"string\",\"esTypes\":[\"keyword\"],\"count\":0,\"scripted\":false,\"searchable\":true,\"aggregatable\":true,\"readFromDocValues\":true,\"parent\":\"kubernetes.labels.app_kubernetes_io/name\",\"subType\":\"multi\"},{\"name\":\"kubernetes.labels.app_kubernetes_io/part-of\",\"type\":\"string\",\"esTypes\":[\"text\"],\"count\":0,\"scripted\":false,\"searchable\":true,\"aggregatable\":false,\"readFromDocValues\":false},{\"name\":\"kubernetes.labels.app_kubernetes_io/part-of.keyword\",\"type\":\"string\",\"esTypes\":[\"keyword\"],\"count\":0,\"scripted\":false,\"searchable\":true,\"aggregatable\":true,\"readFromDocValues\":true,\"parent\":\"kubernetes.labels.app_kubernetes_io/part-of\",\"subType\":\"multi\"},{\"name\":\"kubernetes.labels.component\",\"type\":\"string\",\"esTypes\":[\"text\"],\"count\":0,\"scripted\":false,\"searchable\":true,\"aggregatable\":false,\"readFromDocValues\":false},{\"name\":\"kubernetes.labels.component.keyword\",\"type\":\"string\",\"esTypes\":[\"keyword\"],\"count\":0,\"scripted\":false,\"searchable\":true,\"aggregatable\":true,\"readFromDocValues\":true,\"parent\":\"kubernetes.labels.component\",\"subType\":\"multi\"},{\"name\":\"kubernetes.labels.controller-revision-hash\",\"type\":\"string\",\"esTypes\":[\"text\"],\"count\":0,\"scripted\":false,\"searchable\":true,\"aggregatable\":false,\"readFromDocValues\":false},{\"name\":\"kubernetes.labels.controller-revision-hash.keyword\",\"type\":\"string\",\"esTypes\":[\"keyword\"],\"count\":0,\"scripted\":false,\"searchable\":true,\"aggregatable\":true,\"readFromDocValues\":true,\"parent\":\"kubernetes.labels.controller-revision-hash\",\"subType\":\"multi\"},{\"name\":\"kubernetes.labels.k8s-app\",\"type\":\"string\",\"esTypes\":[\"text\"],\"count\":0,\"scripted\":false,\"searchable\":true,\"aggregatable\":false,\"readFromDocValues\":false},{\"name\":\"kubernetes.labels.k8s-app.keyword\",\"type\":\"string\",\"esTypes\":[\"keyword\"],\"count\":0,\"scripted\":false,\"searchable\":true,\"aggregatable\":true,\"readFromDocValues\":true,\"parent\":\"kubernetes.labels.k8s-app\",\"subType\":\"multi\"},{\"name\":\"kubernetes.labels.kubernetes_io/minikube-addons\",\"type\":\"string\",\"esTypes\":[\"text\"],\"count\":0,\"scripted\":false,\"searchable\":true,\"aggregatable\":false,\"readFromDocValues\":false},{\"name\":\"kubernetes.labels.kubernetes_io/minikube-addons.keyword\",\"type\":\"string\",\"esTypes\":[\"keyword\"],\"count\":0,\"scripted\":false,\"searchable\":true,\"aggregatable\":true,\"readFromDocValues\":true,\"parent\":\"kubernetes.labels.kubernetes_io/minikube-addons\",\"subType\":\"multi\"},{\"name\":\"kubernetes.labels.pod-template-generation\",\"type\":\"string\",\"esTypes\":[\"text\"],\"count\":0,\"scripted\":false,\"searchable\":true,\"aggregatable\":false,\"readFromDocValues\":false},{\"name\":\"kubernetes.labels.pod-template-generation.keyword\",\"type\":\"string\",\"esTypes\":[\"keyword\"],\"count\":0,\"scripted\":false,\"searchable\":true,\"aggregatable\":true,\"readFromDocValues\":true,\"parent\":\"kubernetes.labels.pod-template-generation\",\"subType\":\"multi\"},{\"name\":\"kubernetes.labels.pod-template-hash\",\"type\":\"string\",\"esTypes\":[\"text\"],\"count\":0,\"scripted\":false,\"searchable\":true,\"aggregatable\":false,\"readFromDocValues\":false},{\"name\":\"kubernetes.labels.pod-template-hash.keyword\",\"type\":\"string\",\"esTypes\":[\"keyword\"],\"count\":0,\"scripted\":false,\"searchable\":true,\"aggregatable\":true,\"readFromDocValues\":true,\"parent\":\"kubernetes.labels.pod-template-hash\",\"subType\":\"multi\"},{\"name\":\"kubernetes.labels.run\",\"type\":\"string\",\"esTypes\":[\"text\"],\"count\":0,\"scripted\":false,\"searchable\":true,\"aggregatable\":false,\"readFromDocValues\":false},{\"name\":\"kubernetes.labels.run.keyword\",\"type\":\"string\",\"esTypes\":[\"keyword\"],\"count\":0,\"scripted\":false,\"searchable\":true,\"aggregatable\":true,\"readFromDocValues\":true,\"parent\":\"kubernetes.labels.run\",\"subType\":\"multi\"},{\"name\":\"kubernetes.labels.tier\",\"type\":\"string\",\"esTypes\":[\"text\"],\"count\":0,\"scripted\":false,\"searchable\":true,\"aggregatable\":false,\"readFromDocValues\":false},{\"name\":\"kubernetes.labels.tier.keyword\",\"type\":\"string\",\"esTypes\":[\"keyword\"],\"count\":0,\"scripted\":false,\"searchable\":true,\"aggregatable\":true,\"readFromDocValues\":true,\"parent\":\"kubernetes.labels.tier\",\"subType\":\"multi\"},{\"name\":\"kubernetes.labels.version\",\"type\":\"string\",\"esTypes\":[\"text\"],\"count\":0,\"scripted\":false,\"searchable\":true,\"aggregatable\":false,\"readFromDocValues\":false},{\"name\":\"kubernetes.labels.version.keyword\",\"type\":\"string\",\"esTypes\":[\"keyword\"],\"count\":0,\"scripted\":false,\"searchable\":true,\"aggregatable\":true,\"readFromDocValues\":true,\"parent\":\"kubernetes.labels.version\",\"subType\":\"multi\"},{\"name\":\"kubernetes.master_url\",\"type\":\"string\",\"esTypes\":[\"text\"],\"count\":0,\"scripted\":false,\"searchable\":true,\"aggregatable\":false,\"readFromDocValues\":false},{\"name\":\"kubernetes.master_url.keyword\",\"type\":\"string\",\"esTypes\":[\"keyword\"],\"count\":0,\"scripted\":false,\"searchable\":true,\"aggregatable\":true,\"readFromDocValues\":true,\"parent\":\"kubernetes.master_url\",\"subType\":\"multi\"},{\"name\":\"kubernetes.namespace_id\",\"type\":\"string\",\"esTypes\":[\"text\"],\"count\":0,\"scripted\":false,\"searchable\":true,\"aggregatable\":false,\"readFromDocValues\":false},{\"name\":\"kubernetes.namespace_id.keyword\",\"type\":\"string\",\"esTypes\":[\"keyword\"],\"count\":0,\"scripted\":false,\"searchable\":true,\"aggregatable\":true,\"readFromDocValues\":true,\"parent\":\"kubernetes.namespace_id\",\"subType\":\"multi\"},{\"name\":\"kubernetes.namespace_labels.addonmanager_kubernetes_io/mode\",\"type\":\"string\",\"esTypes\":[\"text\"],\"count\":0,\"scripted\":false,\"searchable\":true,\"aggregatable\":false,\"readFromDocValues\":false},{\"name\":\"kubernetes.namespace_labels.addonmanager_kubernetes_io/mode.keyword\",\"type\":\"string\",\"esTypes\":[\"keyword\"],\"count\":0,\"scripted\":false,\"searchable\":true,\"aggregatable\":true,\"readFromDocValues\":true,\"parent\":\"kubernetes.namespace_labels.addonmanager_kubernetes_io/mode\",\"subType\":\"multi\"},{\"name\":\"kubernetes.namespace_labels.kubernetes_io/minikube-addons\",\"type\":\"string\",\"esTypes\":[\"text\"],\"count\":0,\"scripted\":false,\"searchable\":true,\"aggregatable\":false,\"readFromDocValues\":false},{\"name\":\"kubernetes.namespace_labels.kubernetes_io/minikube-addons.keyword\",\"type\":\"string\",\"esTypes\":[\"keyword\"],\"count\":0,\"scripted\":false,\"searchable\":true,\"aggregatable\":true,\"readFromDocValues\":true,\"parent\":\"kubernetes.namespace_labels.kubernetes_io/minikube-addons\",\"subType\":\"multi\"},{\"name\":\"kubernetes.namespace_name\",\"type\":\"string\",\"esTypes\":[\"text\"],\"count\":3,\"scripted\":false,\"searchable\":true,\"aggregatable\":false,\"readFromDocValues\":false},{\"name\":\"kubernetes.namespace_name.keyword\",\"type\":\"string\",\"esTypes\":[\"keyword\"],\"count\":0,\"scripted\":false,\"searchable\":true,\"aggregatable\":true,\"readFromDocValues\":true,\"parent\":\"kubernetes.namespace_name\",\"subType\":\"multi\"},{\"name\":\"kubernetes.pod_id\",\"type\":\"string\",\"esTypes\":[\"text\"],\"count\":0,\"scripted\":false,\"searchable\":true,\"aggregatable\":false,\"readFromDocValues\":false},{\"name\":\"kubernetes.pod_id.keyword\",\"type\":\"string\",\"esTypes\":[\"keyword\"],\"count\":0,\"scripted\":false,\"searchable\":true,\"aggregatable\":true,\"readFromDocValues\":true,\"parent\":\"kubernetes.pod_id\",\"subType\":\"multi\"},{\"name\":\"kubernetes.pod_name\",\"type\":\"string\",\"esTypes\":[\"text\"],\"count\":0,\"scripted\":false,\"searchable\":true,\"aggregatable\":false,\"readFromDocValues\":false},{\"name\":\"kubernetes.pod_name.keyword\",\"type\":\"string\",\"esTypes\":[\"keyword\"],\"count\":0,\"scripted\":false,\"searchable\":true,\"aggregatable\":true,\"readFromDocValues\":true,\"parent\":\"kubernetes.pod_name\",\"subType\":\"multi\"},{\"name\":\"log\",\"type\":\"string\",\"esTypes\":[\"text\"],\"count\":1,\"scripted\":false,\"searchable\":true,\"aggregatable\":false,\"readFromDocValues\":false},{\"name\":\"log.keyword\",\"type\":\"string\",\"esTypes\":[\"keyword\"],\"count\":0,\"scripted\":false,\"searchable\":true,\"aggregatable\":true,\"readFromDocValues\":true,\"parent\":\"log\",\"subType\":\"multi\"},{\"name\":\"spring.class\",\"type\":\"string\",\"esTypes\":[\"text\"],\"count\":0,\"scripted\":false,\"searchable\":true,\"aggregatable\":false,\"readFromDocValues\":false},{\"name\":\"spring.class.keyword\",\"type\":\"string\",\"esTypes\":[\"keyword\"],\"count\":0,\"scripted\":false,\"searchable\":true,\"aggregatable\":true,\"readFromDocValues\":true,\"parent\":\"spring.class\",\"subType\":\"multi\"},{\"name\":\"spring.level\",\"type\":\"string\",\"esTypes\":[\"text\"],\"count\":1,\"scripted\":false,\"searchable\":true,\"aggregatable\":false,\"readFromDocValues\":false},{\"name\":\"spring.level.keyword\",\"type\":\"string\",\"esTypes\":[\"keyword\"],\"count\":0,\"scripted\":false,\"searchable\":true,\"aggregatable\":true,\"readFromDocValues\":true,\"parent\":\"spring.level\",\"subType\":\"multi\"},{\"name\":\"spring.pid\",\"type\":\"string\",\"esTypes\":[\"text\"],\"count\":0,\"scripted\":false,\"searchable\":true,\"aggregatable\":false,\"readFromDocValues\":false},{\"name\":\"spring.pid.keyword\",\"type\":\"string\",\"esTypes\":[\"keyword\"],\"count\":0,\"scripted\":false,\"searchable\":true,\"aggregatable\":true,\"readFromDocValues\":true,\"parent\":\"spring.pid\",\"subType\":\"multi\"},{\"name\":\"spring.service\",\"type\":\"string\",\"esTypes\":[\"text\"],\"count\":0,\"scripted\":false,\"searchable\":true,\"aggregatable\":false,\"readFromDocValues\":false},{\"name\":\"spring.service.keyword\",\"type\":\"string\",\"esTypes\":[\"keyword\"],\"count\":0,\"scripted\":false,\"searchable\":true,\"aggregatable\":true,\"readFromDocValues\":true,\"parent\":\"spring.service\",\"subType\":\"multi\"},{\"name\":\"spring.span\",\"type\":\"string\",\"esTypes\":[\"text\"],\"count\":0,\"scripted\":false,\"searchable\":true,\"aggregatable\":false,\"readFromDocValues\":false},{\"name\":\"spring.span.keyword\",\"type\":\"string\",\"esTypes\":[\"keyword\"],\"count\":0,\"scripted\":false,\"searchable\":true,\"aggregatable\":true,\"readFromDocValues\":true,\"parent\":\"spring.span\",\"subType\":\"multi\"},{\"name\":\"spring.thread\",\"type\":\"string\",\"esTypes\":[\"text\"],\"count\":0,\"scripted\":false,\"searchable\":true,\"aggregatable\":false,\"readFromDocValues\":false},{\"name\":\"spring.thread.keyword\",\"type\":\"string\",\"esTypes\":[\"keyword\"],\"count\":0,\"scripted\":false,\"searchable\":true,\"aggregatable\":true,\"readFromDocValues\":true,\"parent\":\"spring.thread\",\"subType\":\"multi\"},{\"name\":\"spring.trace\",\"type\":\"string\",\"esTypes\":[\"text\"],\"count\":1,\"scripted\":false,\"searchable\":true,\"aggregatable\":false,\"readFromDocValues\":false},{\"name\":\"spring.trace.keyword\",\"type\":\"string\",\"esTypes\":[\"keyword\"],\"count\":0,\"scripted\":false,\"searchable\":true,\"aggregatable\":true,\"readFromDocValues\":true,\"parent\":\"spring.trace\",\"subType\":\"multi\"},{\"name\":\"stream\",\"type\":\"string\",\"esTypes\":[\"text\"],\"count\":0,\"scripted\":false,\"searchable\":true,\"aggregatable\":false,\"readFromDocValues\":false},{\"name\":\"stream.keyword\",\"type\":\"string\",\"esTypes\":[\"keyword\"],\"count\":0,\"scripted\":false,\"searchable\":true,\"aggregatable\":true,\"readFromDocValues\":true,\"parent\":\"stream\",\"subType\":\"multi\"},{\"name\":\"tag\",\"type\":\"string\",\"esTypes\":[\"text\"],\"count\":0,\"scripted\":false,\"searchable\":true,\"aggregatable\":false,\"readFromDocValues\":false},{\"name\":\"tag.keyword\",\"type\":\"string\",\"esTypes\":[\"keyword\"],\"count\":0,\"scripted\":false,\"searchable\":true,\"aggregatable\":true,\"readFromDocValues\":true,\"parent\":\"tag\",\"subType\":\"multi\"}]","timeFieldName":"@timestamp","title":"logstash-*"},"id":"e1c8e4a0-26ef-11ea-9178-29b953d1e9df","migrationVersion":{"index-pattern":"6.5.0"},"references":[],"type":"index-pattern","updated_at":"2020-02-09T05:38:57.617Z","version":"WzEsMV0="}
{"attributes":{"columns":["spring.level","kubernetes.namespace_name","kubernetes.container_name","spring.trace","log"],"description":"","hits":0,"kibanaSavedObjectMeta":{"searchSourceJSON":"{\"highlightAll\":true,\"version\":true,\"query\":{\"query\":\"\",\"language\":\"kuery\"},\"filter\":[{\"meta\":{\"negate\":false,\"type\":\"phrase\",\"key\":\"kubernetes.namespace_name\",\"value\":\"hands-on\",\"params\":{\"query\":\"hands-on\"},\"disabled\":false,\"alias\":null,\"indexRefName\":\"kibanaSavedObjectMeta.searchSourceJSON.filter[0].meta.index\"},\"query\":{\"match\":{\"kubernetes.namespace_name\":{\"query\":\"hands-on\",\"type\":\"phrase\"}}},\"$state\":{\"store\":\"appState\"}}],\"indexRefName\":\"kibanaSavedObjectMeta.searchSourceJSON.index\"}"},"sort":["@timestamp","desc"],"title":"discover-hands-on-log-records","version":1},"id":"8bac8870-26f3-11ea-9178-29b953d1e9df","migrationVersion":{"search":"7.0.0"},"references":[{"id":"e1c8e4a0-26ef-11ea-9178-29b953d1e9df","name":"kibanaSavedObjectMeta.searchSourceJSON.index","type":"index-pattern"},{"id":"e1c8e4a0-26ef-11ea-9178-29b953d1e9df","name":"kibanaSavedObjectMeta.searchSourceJSON.filter[0].meta.index","type":"index-pattern"}],"type":"search","updated_at":"2020-02-09T05:38:57.617Z","version":"WzIsMV0="}
{"attributes":{"description":"","kibanaSavedObjectMeta":{"searchSourceJSON":"{\"query\":{\"query\":\"\",\"language\":\"kuery\"},\"filter\":[],\"indexRefName\":\"kibanaSavedObjectMeta.searchSourceJSON.index\"}"},"title":"analyze-log-records","uiStateJSON":"{}","version":1,"visState":"{\"title\":\"analyze-log-records\",\"type\":\"pie\",\"params\":{\"type\":\"pie\",\"addTooltip\":true,\"addLegend\":true,\"legendPosition\":\"right\",\"isDonut\":true,\"labels\":{\"show\":false,\"values\":true,\"last_level\":true,\"truncate\":100},\"dimensions\":{\"metric\":{\"accessor\":1,\"format\":{\"id\":\"number\"},\"params\":{},\"aggType\":\"count\"},\"buckets\":[{\"accessor\":0,\"format\":{\"id\":\"terms\",\"params\":{\"id\":\"string\",\"otherBucketLabel\":\"Other\",\"missingBucketLabel\":\"Missing\"}},\"params\":{},\"aggType\":\"terms\"},{\"accessor\":2,\"format\":{\"id\":\"terms\",\"params\":{\"id\":\"string\",\"otherBucketLabel\":\"Other\",\"missingBucketLabel\":\"Missing\"}},\"params\":{},\"aggType\":\"terms\"}]}},\"aggs\":[{\"id\":\"1\",\"enabled\":true,\"type\":\"count\",\"schema\":\"metric\",\"params\":{}},{\"id\":\"2\",\"enabled\":true,\"type\":\"terms\",\"schema\":\"segment\",\"params\":{\"field\":\"kubernetes.namespace_name.keyword\",\"orderBy\":\"1\",\"order\":\"desc\",\"size\":10,\"otherBucket\":true,\"otherBucketLabel\":\"Other\",\"missingBucket\":true,\"missingBucketLabel\":\"Missing\"}},{\"id\":\"3\",\"enabled\":true,\"type\":\"terms\",\"schema\":\"segment\",\"params\":{\"field\":\"kubernetes.container_name.keyword\",\"orderBy\":\"1\",\"order\":\"desc\",\"size\":10,\"otherBucket\":true,\"otherBucketLabel\":\"Other\",\"missingBucket\":true,\"missingBucketLabel\":\"Missing\"}},{\"id\":\"4\",\"enabled\":true,\"type\":\"terms\",\"schema\":\"segment\",\"params\":{\"field\":\"SYSLOG_IDENTIFIER.keyword\",\"orderBy\":\"1\",\"order\":\"desc\",\"size\":5,\"otherBucket\":true,\"otherBucketLabel\":\"Other\",\"missingBucket\":true,\"missingBucketLabel\":\"Missing\"}}]}"},"id":"78eb3590-e802-11e9-95ec-dffc1c724d01","migrationVersion":{"visualization":"7.3.0"},"references":[{"id":"e1c8e4a0-26ef-11ea-9178-29b953d1e9df","name":"kibanaSavedObjectMeta.searchSourceJSON.index","type":"index-pattern"}],"type":"visualization","updated_at":"2020-02-09T05:38:57.617Z","version":"WzMsMV0="}
```

## 2-4. `kubernetes/efk/Dockerfile` (새 파일)
```bash
code kubernetes/efk/Dockerfile
```
붙여넣기 (새 파일)
```dockerfile
FROM fluent/fluentd-kubernetes-daemonset:v1.4.2-debian-elasticsearch-1.1

RUN gem install fluent-plugin-detect-exceptions -v 0.0.12 \
 && gem sources --clear-all \
 && rm -rf /var/lib/apt/lists/* \
           /home/fluent/.gem/ruby/2.3.0/cache/*.gem
```

## 확인 1: 도커 컴포즈 (18장과 같음)
```bash
cd ~/msa
./gradlew build
docker compose build
HOST=localhost PORT=8443 HEALTH_URL=https://localhost:8443 USE_K8S=false ./test-em-all.bash start stop
```
마지막 줄 `End, all tests OK`.

## 확인 2: 18장 상태의 클러스터 준비
18장의 확인 2~4(minikube 12GB, cert-manager, 이스티오와 관찰 도구, `hands-on` 네임스페이스에 마이크로서비스 배포)가 된 상태에서 시작한다. 이미 18장 것이 설치돼 있으면 이 장의 차트로 바꿔 설치한다.
```bash
eval $(minikube docker-env)
docker compose build
for f in kubernetes/helm/components/*; do helm dep up $f; done
for f in kubernetes/helm/environments/*; do helm dep up $f; done
helm upgrade --install hands-on-dev-env kubernetes/helm/environments/dev-env -n hands-on
kubectl wait --timeout=900s --for=condition=ready pod --all -n hands-on
```

## 확인 3: EFK 설치
```bash
eval $(minikube docker-env)
docker pull docker.elastic.co/elasticsearch/elasticsearch:7.17.28
docker pull docker.elastic.co/kibana/kibana:7.17.28
helm install logging-hands-on-add-on kubernetes/helm/environments/logging -n logging --create-namespace --wait
docker build -f kubernetes/efk/Dockerfile -t hands-on/fluentd:v1 kubernetes/efk/
kubectl apply -f kubernetes/efk/fluentd-hands-on-configmap.yml
kubectl apply -f kubernetes/efk/fluentd-ds.yml
kubectl wait --timeout=300s --for=condition=ready pod -l app=fluentd -n kube-system
```
- Elasticsearch·Kibana 이미지는 크다(1GB 가까이). 먼저 받아 두면 `helm install --wait`가 시간 초과로 실패하지 않는다.
- Fluentd 이미지는 minikube 안에서 직접 만든다(`hands-on/fluentd:v1`). 데몬셋은 `kube-system` 네임스페이스에 뜬다.

## 확인 4: 로그 만들고 확인하기 (교수님 안내와 다름: WSL)
이스티오 입구를 port-forward로 연결한다(18장과 같음).
```bash
kubectl port-forward -n istio-system service/istio-ingressgateway 8443:443
```
**새 터미널**에서 테스트를 돌려 로그를 만든다:
```bash
cd ~/msa
PORT=8443 HEALTH_URL=https://health.minikube.me:8443 ./test-em-all.bash
```
마지막 줄 `End, all tests OK`. 1~2분 뒤 Elasticsearch에 로그가 쌓였는지 본다:
```bash
curl -sk "https://elasticsearch.minikube.me:8443/_cat/indices?v" | grep logstash
curl -sk "https://elasticsearch.minikube.me:8443/logstash-*/_count?q=kubernetes.container_name:product-composite" | jq .count
curl -sk -o /dev/null -w "%{http_code}\n" https://kibana.minikube.me:8443/
```
- 첫째: `logstash-날짜` 인덱스가 보인다.
- 둘째: product-composite 로그 줄 수(0보다 크다).
- 셋째: Kibana `302`(첫 화면 주소로 넘겨 주는 응답이라 정상). 검증 때 인덱스 하나에 로그 약 1,300줄, 그중 product-composite 약 570줄이 쌓였다. 브라우저로 보려면 https://kibana.minikube.me:8443 (Windows 브라우저는 18장 troubleshooting의 Windows hosts 설정이 필요).

## 끝내기
```bash
kubectl delete -f kubernetes/efk/fluentd-ds.yml
helm uninstall logging-hands-on-add-on -n logging
eval $(minikube docker-env -u)
minikube stop
```
