# 05. 설치 버전 확인, 교재 소스 내려받기

## 설치 버전 한 번에 확인
```bash
git version ; docker version -f json | jq -r .Client.Version ; java -version 2>&1 | grep "openjdk version" ; curl --version | grep "curl" ; jq --version ; spring --version ; siege --version 2>&1 | grep SIEGE ; helm version --short ; kubectl version --client ; minikube version | grep "minikube" ; istioctl version --remote=false
```
결과가 [README.md](README.md)의 설치 결과 요약표와 같은지 확인한다.

## 교재 소스코드 내려받기
```bash
echo 'export BOOK_HOME=~/spring-microservice' >> ~/.bashrc
source ~/.bashrc
echo $BOOK_HOME

git clone https://github.com/wikibook/spring-microservice.git $BOOK_HOME
cd $BOOK_HOME
ls
code .
```
- `$BOOK_HOME`은 이후 모든 장에서 교재 소스 경로로 쓴다. 예: `$BOOK_HOME/Chapter03/2-basic-rest-services`
- 교재 소스는 **Java 24 / Spring Boot 3.5.0** 기준이다. 실습 레포(`~/msa`)로 가져올 때는 버전을 맞춰서 바꾼다.

이제 [06-create-projects.md](06-create-projects.md)로 넘어가 3장 기본 골격을 만든다.
