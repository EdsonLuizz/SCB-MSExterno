# Testes de Unidade (Skeleton) — Externo-MS

Pacote de testes para o microsserviço Externo. Coloque os arquivos dentro do seu projeto em:
`src/test/java/com/example/externo/...` respeitando os pacotes.

Inclui:
- WebMvcTest do controller (simula HTTP e valida status 200/404/422)
- Testes do service (validação Luhn, fila, consulta)

Requisitos Maven (pom.xml):
- spring-boot-starter-test
- jacoco-maven-plugin (para cobertura no SonarCloud)

Execução:
```bash
mvn clean verify
```
