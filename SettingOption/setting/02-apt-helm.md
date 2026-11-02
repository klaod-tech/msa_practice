# 02. apt 기본 도구, Helm

## apt로 기본 도구 설치
```bash
sudo apt update
sudo apt install -y jq zip unzip siege
```
- `jq`: JSON을 보기 좋게 출력한다(`curl ... | jq .`).
- `zip`, `unzip`: SDKMAN 설치에 필요하다.
- `siege`: 부하 테스트 도구다.

## Helm 설치 (공식 스크립트)
```bash
curl -fsSL -o get_helm.sh https://raw.githubusercontent.com/helm/helm/main/scripts/get-helm-3
chmod 700 get_helm.sh
./get_helm.sh
helm version
rm get_helm.sh
```
apt 저장소 방식은 실패했다 → [troubleshooting.md](troubleshooting.md)

다음: [03-sdkman-java-spring.md](03-sdkman-java-spring.md)
