# 00. 배경: 로그를 한곳에 모으기 (EFK), 18장과 무엇이 다른가

> 코드 없이 읽기만 하는 문서.

## 1. 18장까지
- 로그는 파드마다 따로 있다(`kubectl logs`). 파드가 지워지면 로그도 사라지고, 여러 서비스의 로그를 traceId로 모아 보려면 하나씩 열어야 한다.

## 2. 19장이 끝나면
```
 노드마다 Fluentd(데몬셋) ─ 컨테이너 로그 파일을 읽어 ─▶ Elasticsearch(저장·검색) ─▶ Kibana(화면)
                                                                                  https://kibana.minikube.me
```
| 구성 요소 | 하는 일 |
|---|---|
| **E**lasticsearch | 로그를 저장하고 검색한다 |
| **F**luentd | 노드마다 하나씩 떠서(데몬셋) 컨테이너 로그를 읽고, 스프링 로그 형식을 파싱해 Elasticsearch로 보낸다 |
| **K**ibana | 로그 검색·시각화 화면. traceId로 한 요청의 로그를 서비스 상관없이 모아 본다 |

## 3. 18장과 달라지는 점
| 항목 | 18장 | 19장 |
|---|---|---|
| 마이크로서비스 코드 | - | 변경 없음 |
| 새 파일 | - | `kubernetes/efk/`(Fluentd 이미지 Dockerfile, 데몬셋, 설정 ConfigMap, Kibana 내보내기 파일), `logging` 환경 차트(Elasticsearch, Kibana) |

## 4. 용어
| 용어 | 뜻 |
|---|---|
| 데몬셋(DaemonSet) | 노드마다 파드를 하나씩 띄우는 쿠버네티스 객체 |
| 인덱스 | Elasticsearch의 데이터 묶음(로그는 날짜별 인덱스) |
| 인덱스 패턴 / 데이터 뷰 | Kibana가 어떤 인덱스들을 볼지 정한 것 |
