# Microserviço Externo – Sistema de Controle de Bicicletário (SCB)

Este repositório contém o **microserviço externo (SCB-MSExterno)*** para integrar o sistema com serviços reais de **pagamento (Stripe)** e **envio de e-mails (Mailgun)**.

---

## 🔧 Visão Geral do Microserviço

O microserviço expõe uma API HTTP que centraliza operações relacionadas a
**cobranças** e **notificações por e-mail**, consumindo Stripe e Mailgun em
tempo de execução.

### 1. Integração com Stripe – API de Pagamento

Funcionalidades principais:

- **Criação de cobrança imediata**
    - Cria um `PaymentIntent` real na Stripe por meio de `StripeGat`.
    - Registra a cobrança em memória com status inicial `AGUARDANDO_PAGAMENTO`.

- **Fila de cobranças atrasadas**
    - Permite incluir cobranças em uma fila (em memória) para processamento posterior.
    - O serviço processa a fila e, para cada cobrança, cria e confirma um `PaymentIntent`
      na Stripe.

- **Processamento de pagamento**
    - Sucesso (`succeeded`) → status `PAGA`, grava `horaFinalizacao` e `gatewayID`.
    - Erros ou status como `requires_payment_method`, `requires_action` ou `canceled`
      → status `FALHA`/`FALHA_GATEWAY`, com registro de `horaFinalizacao`.

- **Validação de cartão de crédito**
    - Implementa o **algoritmo de Luhn** para validar o número do cartão antes de
      chamar o gateway.

### 2. Integração com Mailgun – API de E-mail

Funcionalidades principais:

- Recebe uma requisição de envio de e-mail com endereço e mensagem.
- Valida:
    - formato básico do e-mail,
    - domínio permitido (por exemplo, `gmail.com` e `hotmail.com`).
- Em caso de sucesso:
    - chama o gateway real `MailgunGat.enviarEmailSimples(...)`,
    - retorna um DTO `Email` com id gerado, destinatário, assunto e corpo.

Além disso, após o pagamento bem-sucedido de uma cobrança de fila, o serviço
pode enviar automaticamente um e-mail informando o sucesso da transação,
quando o identificador do “ciclista” é um e-mail válido.

---

## 🧪 Testes e Qualidade de Código

Os testes automatizados são **unitários** e focados na classe
`ExternoService`. Para não chamar Stripe e Mailgun durante os testes:

- `StripeGat` e `MailgunGat` são **mockados** com **Mockito**;
- apenas o comportamento dos métodos é simulado, enquanto a lógica do serviço
  (Estados das cobranças, regras de negócio, tratamento de exceções etc.)
  é exercitada de forma real.

São testados, entre outros cenários:

- Validação de e-mails e domínios permitidos.
- Criação e recuperação de cobranças (`criarCobranca`, `obterCobranca`).
- Processamento da fila de cobranças:
    - sem itens,
    - com um ou vários itens,
    - com cobranças que já estavam em falha e são reprocessadas.
- Mapeamento de status retornados pela Stripe para os estados internos:
    - `AGUARDANDO_PAGAMENTO`, `EM_FILA`, `PAGA`,
      `FALHA`, `FALHA_GATEWAY`.
- Tratamento de exceções lançadas pela Stripe tanto na criação quanto na
  confirmação de pagamentos.
- Lógica de validação de número de cartão via Luhn.

A qualidade do código é monitorada com **SonarCloud**, incluindo:

- cobertura de testes,
- code smells,
- e outras métricas de manutenibilidade.

---

## 🧱 Tecnologias Utilizadas

- **Java 17+**
- **Spring Boot**
- **Maven**
- **Stripe** (SDK oficial – pagamentos reais)
- **Mailgun** (envio de e-mails real)
- **JUnit 5** e **Mockito** (testes unitários)
- **SonarCloud** (análise de qualidade)
- **Render** (deploy em nuvem)

---

## 🚀 Como Executar Localmente

**Pré-requisitos:**

- Java 17+
- Maven 3+
- Conta e credenciais válidas de **Stripe** e **Mailgun**  
  (configuradas nas variáveis de ambiente ou em `application.properties`).

```bash
# compilar o projeto e rodar os testes
mvn clean verify

# subir a aplicação (porta padrão 8080)
mvn spring-boot:run
