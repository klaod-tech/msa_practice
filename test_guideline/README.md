# test_guideline: 교수님 코드 기준 장별 실습 문서

> 기준 코드: 교수님 저장소 [spring-microservice-boot4](https://github.com/NohYounghoon3651/spring-microservice-boot4) (Spring Boot 4.0.9 / Java 21 / Spring Cloud 2025.1.4 / Gradle 8.14.6)
> 예전 문서(우리 코드 기준, Lombok 등)는 [../SettingOption/](../SettingOption/)에 그대로 있다.

## 이 문서의 약속
- **N장 문서는 교수님 `Chapter(N-1)` 상태에서 시작해서, 문서대로 하면 교수님 `ChapterN`과 같은 코드가 된다.** 교재의 영어 주석만 빠진다.
- 장마다 빈 작업 폴더에서 문서 글자 그대로 따라 해 보고, 결과를 교수님 코드와 비교한 뒤 빌드와 테스트까지 돌려서 확인했다. 확인한 내용은 장별 README의 "검증" 칸에 있다.
- 파일 단계는 `code 경로` 코드 블록 → "붙여넣기 (새 파일)" 또는 "아래처럼 맞추기 (전체 파일)" 순서다. 바뀐 줄에는 `★ 추가` / `★ 변경 (전: ...)` / `★ 삭제`가 붙어 있다. md 화면에서 Ctrl+F로 `★`를 찾으면 바뀐 줄만 볼 수 있다.
- 모든 명령은 작업 폴더 `~/msa`에서 실행한다. 다른 폴더에서 하면 `~/msa`를 그 폴더로 바꿔 읽는다.

## 중간 장부터 시작하기
교수님 코드의 앞 장 결과를 작업 폴더에 복사하고 그 장 문서를 따라 한다. 예: 12장부터 하려면 교수님 `Chapter11`을 복사한다.
```bash
cd ~
git clone https://github.com/NohYounghoon3651/spring-microservice-boot4.git
mkdir -p ~/work
cp -r ~/spring-microservice-boot4/Chapter11/. ~/work/
cd ~/work
```
이미 받았다면 `git clone` 대신 `cd ~/spring-microservice-boot4 && git pull`.

## 장 목록

| 부 | 장 | 문서 | 시작 → 결과 | 상태 |
|---|---|---|---|---|
| - | 환경 | [../SettingOption/setting/](../SettingOption/setting/README.md) 01~05 | WSL, Docker, Java, Spring CLI, kubectl 등 | 설치 완료 |
| 1부 | 3 | [chapter3](chapter3/README.md) | 빈 폴더 → Chapter03/2-basic-rest-services | 검증 완료 |
| 1부 | 4 | [chapter4](chapter4/README.md) | Chapter03/2-basic-rest-services → Chapter04 | 검증 완료 |
| 1부 | 5 | [chapter5](chapter5/README.md) | Chapter04 → Chapter05 | 검증 완료 |
| 1부 | 6 | [chapter6](chapter6/README.md) | Chapter05 → Chapter06 | 검증 완료 |
| 1부 | 7 | [chapter7](chapter7/README.md) | Chapter06 → Chapter07 | 검증 완료 |
| 2부 | 8 | (코드 없음) | | |
| 2부 | 9 | [chapter9](chapter9/README.md) | Chapter07 → Chapter09 | 검증 완료 |
| 2부 | 10 | [chapter10](chapter10/README.md) | Chapter09 → Chapter10 | 검증 완료 |
| 2부 | 11 | [chapter11](chapter11/README.md) | Chapter10 → Chapter11 | 검증 완료 |
| 2부 | 12 | [chapter12](chapter12/README.md) | Chapter11 → Chapter12 | 검증 완료 |
| 2부 | 13 | [chapter13](chapter13/README.md) | Chapter12 → Chapter13 | 검증 완료 |
| 2부 | 14 | [chapter14](chapter14/README.md) | Chapter13 → Chapter14 | 검증 완료 |
| 3부 | 15 | [chapter15](chapter15/README.md) | Chapter14 → Chapter15 | 검증 완료 |
| 3부 | 16 | chapter16 | Chapter15 → Chapter16 | 작성 중 |
| 3부 | 17 | chapter17 | Chapter16 → Chapter17 | 예정 |
| 3부 | 18 | chapter18 | Chapter17 → Chapter18 | 예정 |
| 3부 | 19 | chapter19 | Chapter18 → Chapter19 | 예정 |
| 3부 | 20 | chapter20 | Chapter19 → Chapter20 | 예정 |
