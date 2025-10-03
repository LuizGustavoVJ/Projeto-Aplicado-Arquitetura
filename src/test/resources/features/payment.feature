# language: pt
Funcionalidade: Processamento de Pagamentos
  Como um lojista
  Eu quero processar pagamentos com cartão de crédito
  Para receber pagamentos dos meus clientes

  Contexto:
    Dado que o sistema está configurado
    E que eu tenho uma API Key válida

  Cenário: Autorizar pagamento com sucesso
    Dado que eu tenho um cartão de crédito válido
    Quando eu envio uma requisição de autorização de R$ 100,00
    Então o pagamento deve ser autorizado
    E eu devo receber um ID de transação
    E o status deve ser "AUTHORIZED"

  Cenário: Capturar pagamento autorizado
    Dado que eu tenho um pagamento autorizado
    Quando eu envio uma requisição de captura
    Então o pagamento deve ser capturado
    E o status deve ser "CAPTURED"

  Cenário: Cancelar pagamento capturado
    Dado que eu tenho um pagamento capturado
    Quando eu envio uma requisição de cancelamento
    Então o pagamento deve ser cancelado
    E o status deve ser "VOIDED"

  Cenário: Rejeitar pagamento com cartão inválido
    Dado que eu tenho um cartão de crédito inválido
    Quando eu envio uma requisição de autorização
    Então o pagamento deve ser rejeitado
    E eu devo receber uma mensagem de erro

  Cenário: Validar campos obrigatórios
    Quando eu envio uma requisição sem o valor
    Então eu devo receber um erro de validação
    E o status HTTP deve ser 400

  Cenário: Listar transações com filtros
    Dado que existem transações no sistema
    Quando eu consulto transações com status "CAPTURED"
    Então eu devo receber uma lista de transações
    E todas devem ter status "CAPTURED"

  Esquema do Cenário: Processar pagamentos com diferentes valores
    Dado que eu tenho um cartão de crédito válido
    Quando eu envio uma requisição de autorização de R$ <valor>
    Então o pagamento deve ser <resultado>

    Exemplos:
      | valor  | resultado   |
      | 10,00  | autorizado  |
      | 100,00 | autorizado  |
      | 1000,00| autorizado  |
      | 0,00   | rejeitado   |
      | -10,00 | rejeitado   |

  Esquema do Cenário: Testar diferentes bandeiras de cartão
    Dado que eu tenho um cartão <bandeira>
    Quando eu envio uma requisição de autorização
    Então o pagamento deve ser autorizado
    E a bandeira deve ser identificada como <bandeira>

    Exemplos:
      | bandeira   |
      | Visa       |
      | Mastercard |
      | Elo        |
      | Amex       |
