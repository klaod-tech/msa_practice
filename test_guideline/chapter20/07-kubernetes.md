# 07. Grafana 대시보드

> 시작: 교수님 `Chapter19` 상태 / 결과: 교수님 `Chapter20`과 같은 코드(교재 영어 주석만 빠짐).
> 표기: `★ 추가` / `★ 변경 (전: ...)` / `★ 삭제` = 이전 장 대비 바뀌는 줄. md 화면에서 Ctrl+F로 `★`를 찾으면 바뀐 줄만 볼 수 있다.

`grafana/api-export-import/Hands-on-Dashboard.json`: Grafana에 가져올 대시보드.

## 7-1. `kubernetes/grafana/api-export-import/Hands-on-Dashboard.json` (새 파일)
```bash
mkdir -p kubernetes/grafana/api-export-import
code kubernetes/grafana/api-export-import/Hands-on-Dashboard.json
```
붙여넣기 (새 파일)
```json
{
  "meta": {
    "type": "db",
    "canSave": true,
    "canEdit": true,
    "canAdmin": true,
    "canStar": false,
    "canDelete": true,
    "slug": "hands-on-dashboard",
    "url": "/d/beo4o06vgsdmof/hands-on-dashboard",
    "expires": "0001-01-01T00:00:00Z",
    "created": "2025-06-06T09:51:56Z",
    "updated": "2025-06-06T09:51:56Z",
    "updatedBy": "Anonymous",
    "createdBy": "Anonymous",
    "version": 1,
    "hasAcl": false,
    "isFolder": false,
    "folderId": 0,
    "folderUid": "",
    "folderTitle": "General",
    "folderUrl": "",
    "provisioned": false,
    "provisionedExternalId": "",
    "annotationsPermissions": {
      "dashboard": {
        "canAdd": true,
        "canEdit": true,
        "canDelete": true
      },
      "organization": {
        "canAdd": true,
        "canEdit": true,
        "canDelete": true
      }
    }
  },
  "dashboard": {
    "annotations": {
      "list": [
        {
          "builtIn": 1,
          "datasource": {
            "type": "grafana",
            "uid": "-- Grafana --"
          },
          "enable": true,
          "hide": true,
          "iconColor": "rgba(0, 211, 255, 1)",
          "name": "Annotations & Alerts",
          "type": "dashboard"
        }
      ]
    },
    "editable": true,
    "fiscalYearStartMonth": 0,
    "graphTooltip": 0,
    "id": null,
    "links": [],
    "panels": [
      {
        "datasource": {
          "default": true,
          "type": "prometheus",
          "uid": "PBFA97CFB590B2093"
        },
        "fieldConfig": {
          "defaults": {
            "color": {
              "mode": "palette-classic"
            },
            "custom": {
              "axisBorderShow": false,
              "axisCenteredZero": false,
              "axisColorMode": "text",
              "axisLabel": "",
              "axisPlacement": "auto",
              "barAlignment": 0,
              "barWidthFactor": 0.6,
              "drawStyle": "line",
              "fillOpacity": 0,
              "gradientMode": "none",
              "hideFrom": {
                "legend": false,
                "tooltip": false,
                "viz": false
              },
              "insertNulls": false,
              "lineInterpolation": "linear",
              "lineWidth": 1,
              "pointSize": 5,
              "scaleDistribution": {
                "type": "linear"
              },
              "showPoints": "auto",
              "spanNulls": false,
              "stacking": {
                "group": "A",
                "mode": "none"
              },
              "thresholdsStyle": {
                "mode": "off"
              }
            },
            "mappings": [],
            "thresholds": {
              "mode": "absolute",
              "steps": [
                {
                  "color": "green",
                  "value": null
                },
                {
                  "color": "red",
                  "value": 80
                }
              ]
            }
          },
          "overrides": []
        },
        "gridPos": {
          "h": 8,
          "w": 12,
          "x": 0,
          "y": 0
        },
        "id": 2,
        "options": {
          "legend": {
            "calcs": [],
            "displayMode": "list",
            "placement": "bottom",
            "showLegend": true
          },
          "tooltip": {
            "mode": "multi",
            "sort": "none"
          }
        },
        "targets": [
          {
            "datasource": {
              "type": "prometheus",
              "uid": "PBFA97CFB590B2093"
            },
            "disableTextWrap": false,
            "editorMode": "builder",
            "expr": "rate(resilience4j_retry_calls_total[30s])",
            "fullMetaSearch": false,
            "includeNullMetadata": true,
            "instant": false,
            "legendFormat": "{{kind}}",
            "range": true,
            "refId": "A",
            "useBackend": false
          }
        ],
        "title": "Retry",
        "type": "timeseries"
      },
      {
        "datasource": {
          "default": true,
          "type": "prometheus",
          "uid": "PBFA97CFB590B2093"
        },
        "fieldConfig": {
          "defaults": {
            "color": {
              "mode": "palette-classic"
            },
            "custom": {
              "axisBorderShow": false,
              "axisCenteredZero": false,
              "axisColorMode": "text",
              "axisLabel": "",
              "axisPlacement": "auto",
              "barAlignment": 0,
              "barWidthFactor": 0.6,
              "drawStyle": "line",
              "fillOpacity": 0,
              "gradientMode": "none",
              "hideFrom": {
                "legend": false,
                "tooltip": false,
                "viz": false
              },
              "insertNulls": false,
              "lineInterpolation": "linear",
              "lineWidth": 1,
              "pointSize": 5,
              "scaleDistribution": {
                "type": "linear"
              },
              "showPoints": "auto",
              "spanNulls": false,
              "stacking": {
                "group": "A",
                "mode": "none"
              },
              "thresholdsStyle": {
                "mode": "off"
              }
            },
            "mappings": [],
            "thresholds": {
              "mode": "absolute",
              "steps": [
                {
                  "color": "green",
                  "value": null
                },
                {
                  "color": "red",
                  "value": 80
                }
              ]
            }
          },
          "overrides": []
        },
        "gridPos": {
          "h": 8,
          "w": 12,
          "x": 0,
          "y": 8
        },
        "id": 1,
        "options": {
          "legend": {
            "calcs": [],
            "displayMode": "list",
            "placement": "bottom",
            "showLegend": true
          },
          "tooltip": {
            "mode": "multi",
            "sort": "none"
          }
        },
        "targets": [
          {
            "datasource": {
              "type": "prometheus",
              "uid": "PBFA97CFB590B2093"
            },
            "disableTextWrap": false,
            "editorMode": "builder",
            "expr": "resilience4j_circuitbreaker_state{state=\"closed\"}",
            "fullMetaSearch": false,
            "includeNullMetadata": true,
            "instant": false,
            "legendFormat": "{{state}}",
            "range": true,
            "refId": "A",
            "useBackend": false
          },
          {
            "datasource": {
              "type": "prometheus",
              "uid": "PBFA97CFB590B2093"
            },
            "disableTextWrap": false,
            "editorMode": "builder",
            "expr": "resilience4j_circuitbreaker_state{state=\"open\"}",
            "fullMetaSearch": false,
            "hide": false,
            "includeNullMetadata": true,
            "instant": false,
            "legendFormat": "{{state}}",
            "range": true,
            "refId": "B",
            "useBackend": false
          },
          {
            "datasource": {
              "type": "prometheus",
              "uid": "PBFA97CFB590B2093"
            },
            "disableTextWrap": false,
            "editorMode": "builder",
            "expr": "resilience4j_circuitbreaker_state{state=\"half_open\"}",
            "fullMetaSearch": false,
            "hide": false,
            "includeNullMetadata": true,
            "instant": false,
            "legendFormat": "{{state}}",
            "range": true,
            "refId": "C",
            "useBackend": false
          }
        ],
        "title": "Circuit Breaker",
        "type": "timeseries"
      }
    ],
    "refresh": "5s",
    "schemaVersion": 39,
    "tags": [],
    "templating": {
      "list": []
    },
    "time": {
      "from": "now-5m",
      "to": "now"
    },
    "timepicker": {},
    "timezone": "browser",
    "title": "Hands-on Dashboard",
    "uid": "beo4o06vgsdmof",
    "version": 1,
    "weekStart": ""
  }
}
```

다음: [08-build-compose-test.md](08-build-compose-test.md)
