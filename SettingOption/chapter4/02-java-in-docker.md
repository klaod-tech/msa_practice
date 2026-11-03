# 02. 컨테이너 안 자바의 자원 제한 인식 (교재 95쪽~)

## 무엇을 확인하나
자바(JVM)는 시작할 때 "쓸 수 있는 CPU·메모리가 얼마지?"를 확인하고, 그 값으로 스레드 풀 크기와 힙 크기를 정한다.
컨테이너에 "CPU 3개만 써"라고 제한했는데 자바가 이를 모르고 호스트 전체를 기준으로 잡으면 성능이 오히려 나빠진다.
→ **최신 자바는 컨테이너 제한을 제대로 읽는다**는 걸 보여 주는 실험이다.

## 교재와 다르게 하는 것
- 이미지: 교재 `eclipse-temurin:24` → **`eclipse-temurin:21`** (우리 서비스가 Java 21. 결과는 같다)
- 결과 숫자: 교재는 CPU 8개, 이 PC는 **12개** (`docker info`의 `NCPU`). PC마다 다르다.

## 명령 풀이
```bash
echo 'Runtime.getRuntime().availableProcessors()' | docker run --rm -i eclipse-temurin:21 jshell -q
```
| 부분 | 뜻 |
|---|---|
| `echo '...'` | 자바 코드 한 줄을 문자열로 출력 |
| `\|` | 그 출력을 다음 명령의 입력으로 넘김 |
| `docker run --rm -i` | 컨테이너를 띄우고 입력을 받음. 키보드가 아니라 파이프로 입력하므로 `-t` 없이 `-i`만 |
| `eclipse-temurin:21` | Java 21 JDK가 들어 있는 이미지. 내 PC에 자바가 없어도 이미지 안의 자바로 실행된다 |
| `jshell -q` | 자바 코드를 한 줄씩 실행하는 도구. 받은 코드를 실행하고 끝남 |

## 1. CPU 제한
```bash
echo 'Runtime.getRuntime().availableProcessors()' | docker run --rm -i eclipse-temurin:21 jshell -q
echo 'Runtime.getRuntime().availableProcessors()' | docker run --rm -i --cpus=3 eclipse-temurin:21 jshell -q
```
기대 결과:
```
jshell> $1 ==> 12        ← 제한 없음
jshell> $1 ==> 3         ← --cpus=3
```
`$1 ==>`는 jshell이 첫 번째 결과를 보여 주는 형식이다.

### Docker Desktop에 컨테이너가 잠깐 보였다가 사라지는 이유 (정상)
**컨테이너는 안의 메인 프로그램이 끝나면 같이 끝난다.**
```
docker run → 컨테이너 생성, jshell 실행 (Docker Desktop에 나타남)
→ echo가 넘긴 한 줄 실행, 결과 출력
→ 더 들어올 입력이 없음 → jshell 종료 → 컨테이너 종료
→ --rm → 자동 삭제 (Docker Desktop에서 사라짐)
```
| | `docker run -it --rm ubuntu` (01) | `echo ... \| docker run --rm -i ... jshell -q` |
|---|---|---|
| 메인 프로그램 | bash | jshell |
| 입력 | 내 키보드 | echo가 넘긴 한 줄 |
| 언제 끝나나 | 내가 `exit`할 때까지 기다림 | 한 줄 처리하고 바로 끝남 |

## 2. 메모리(RAM) 제한
1번은 **CPU 코어 수** 제한(`--cpus`), 2번은 **메모리** 제한(`-m`)이다.
자바는 `-Xmx`를 따로 지정하지 않으면 **쓸 수 있는 메모리의 1/4을 최대 힙 크기(MaxHeapSize)**로 잡는다. 컨테이너에 메모리 제한을 걸면 그 1/4로 줄어드는지 본다.
```bash
docker run -it --rm eclipse-temurin:21 java -XX:+PrintFlagsFinal | grep "size_t MaxHeapSize"
docker run -it --rm -m=1024M eclipse-temurin:21 java -XX:+PrintFlagsFinal | grep "size_t MaxHeapSize"
docker run -it --rm -m=1024M eclipse-temurin:21 java -Xmx600m -XX:+PrintFlagsFinal -version | grep "size_t MaxHeapSize"
```
| 부분 | 뜻 |
|---|---|
| `-m=1024M` | 컨테이너가 쓸 수 있는 메모리를 1GB로 제한 (Docker 옵션) |
| `-XX:+PrintFlagsFinal` | JVM이 최종적으로 정한 설정값을 전부 출력 (Java 옵션) |
| `\| grep "size_t MaxHeapSize"` | 그중 최대 힙 크기 줄만 남김 |
| `-Xmx600m` | 최대 힙 크기를 600MB로 직접 지정 (Java 옵션) |

이 PC의 실제 결과 (eclipse-temurin:21):
| 명령 | MaxHeapSize | 의미 |
|---|---|---|
| 제한 없음 | `4171235328` (≈ 3,978MB) | Docker 메모리 약 15.5GB의 1/4 ≈ 4GB (교재: 4,188,012,544 — PC 메모리가 달라서 다름) |
| `-m=1024M` | `268435456` (= 256MB) | 1GB의 1/4 (교재와 같음) |
| `-m=1024M` + `-Xmx600m` | `629145600` (= 600MB) | 직접 지정한 값이 우선 (교재와 같음) |

### 제한을 "다시 풀어야" 하나? → 아니다
`--cpus=3`, `-m=1024M`은 **그 `docker run` 한 번으로 만든 컨테이너에만** 적용된다. PC나 Docker 설정이 바뀌는 게 아니고, `--rm`이라 실행이 끝나면 컨테이너째 사라진다. 다음에 옵션 없이 실행하면 다시 제한 없는 상태다.

### 왜 1GB 제한인데 256MB가 나오나
- `-m=1024M`은 **Docker**가 건 컨테이너 전체 메모리 상한, `MaxHeapSize`는 그중 **자바(JVM)**가 객체 저장 공간(힙)으로 쓸 최대치다.
- 자바는 **자기가 들어 있는 컨테이너가 쓸 수 있는 메모리의 1/4**을 힙으로 잡는다. 나머지는 메타스페이스, 스레드 스택, JIT 코드, OS 몫이다.
- 제한이 없을 때 기준은 **PC 실제 램(약 32GB)이 아니라 WSL2/Docker 몫(약 16GB)**이다. Docker Desktop은 WSL2 가상 머신 안에서 돌고, `.wslconfig`가 없으면 WSL2는 Windows 램의 절반 정도를 쓴다.
  ```
  PC 실제 램 32GB
   └─ WSL2/Docker 몫 16GB        ← 제한 없는 컨테이너가 보는 메모리 → 힙 ≈ 4GB
       └─ -m=1024M 컨테이너 1GB  ← 제한을 걸면 여기까지만 보임    → 힙 = 256MB
  ```

## 3. 메모리 부족 테스트 (교재 마무리)
1GB 제한 컨테이너(최대 힙 256MB)에서 jshell로 바이트 배열을 할당한다.
```bash
echo 'new byte[100_000_000]' | docker run -i --rm -m=1024M eclipse-temurin:21 jshell -q
echo 'new byte[500_000_000]' | docker run -i --rm -m=1024M eclipse-temurin:21 jshell -q
```
이 PC의 실제 결과 (eclipse-temurin:21):
```
jshell> $1 ==>                                                   ← 100MB: 성공 (256MB 안에 들어감)
jshell> |  Exception java.lang.OutOfMemoryError: Java heap space  ← 500MB: 실패 (256MB 초과)
|        at (#1:1)
```
- 100MB는 성공했지만, 0으로 채워진 1억 바이트를 다 출력할 수 없어서 `$1 ==>` 뒤가 비어 있다(교재 팁 박스 내용).
- 500MB는 최대 힙(256MB)보다 커서 즉시 `OutOfMemoryError`가 난다. → **자바가 컨테이너 메모리 제한을 따른다**는 증거다.
- 실행할 때마다 위에 `INFO: Created user preferences directory.` 같은 줄이 먼저 나올 수 있다. jshell이 설정 폴더를 처음 만들 때 찍는 안내라 무시한다.

다음: [03-dockerfile.md](03-dockerfile.md)
