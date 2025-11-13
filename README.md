# Microserviço Externo

Este microserviço é um componente de mock para o **Sistema de Controle de Bicicletário (SCB)**.

---

## Contexto Acadêmico

Este sistema está sendo desenvolvido como parte da disciplina **Engenharia de Software II** do curso de **Sistemas de Informação na UNIRIO**, no período **2025.2**.

## Funcionalidades Principais

O serviço simula as APIs externas listadas abaixo:

* **API de Pagamento:** Simula a validação de cartões de crédito e a realização de cobranças.
* **API de E-mail:** Simula o recebimento de uma requisição de envio de e-mail e retorna sempre seu status.

## Deploy

O deploy deste microserviço é feito de forma contínua e está hospedado na plataforma Render.

## ⛳ Links Rápidos

[SonarCloud]([https://sonarcloud.io/api/project_badges/measure?project=EdsonLuizz_SCB-MSExterno&metric=alert_status)](https://sonarcloud.io/project/overview?id=EdsonLuizz_SCB-MSExterno](https://sonarcloud.io/project/overview?id=EdsonLuizz_SCB-MSExterno&authuser=1))•

[Repositório]([https://github.com/EdsonLuizz/SCB-MSExterno](https://github.com/EdsonLuizz/SCB-MSExterno)) •

[Deploy (Render)]([https://scb-msexterno.onrender.com/](https://scb-msexterno.onrender.com)) • 

[Postman (raw)]([https://raw.githubusercontent.com/EdsonLuizz/SCB-MSExterno/refs/heads/main/postman_collection.json](https://raw.githubusercontent.com/EdsonLuizz/SCB-MSExterno/refs/heads/main/postman_collection.json?authuser=1))•

> Versão entregue: 1.0

---

## Como Executar Localmente

**Pré-requisitos**: Java 17+ e Maven

```bash
# compilar e rodar testes
mvn clean verify

# subir a aplicação (porta padrão 8080)
mvn spring-boot:run
