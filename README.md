# 🧑‍💻 service-processor

O `service-processor` é um microsserviço responsável por processar arquivos enviados para o SQS. Ele realiza a compactação (formato `.zip`) de arquivos armazenados no S3 e publica o resultado de volta no S3. Após o processamento, o serviço envia uma notificação via SQS para informar que o processamento foi concluído.

---

## ⚙️ Tecnologias utilizadas

- ✅ Java 21
- ✅ Spring WebFlux
- ✅ Kubernetes
- ✅ Terraform
- ✅ AWS
- ✅ Lombok
- ✅ SQS
- ✅ Testes Unitários (JUnit + Mockito)
- ✅ Maven

---

## 🚀 Funcionalidades

- ☁️ Baixa arquivos do Amazon S3.
- 🗜️ Compacta o(s) arquivo(s) em formato `.zip`.
- 📤 Faz o upload do arquivo `.zip` para o S3.
- 📬 Envia mensagem para uma fila SQS notificando outro serviço sobre a finalização do processamento.

---

## 📦 Como executar localmente

### Pré-requisitos

- Java 21
- Maven 3.8+
- Docker (opcional para execução em container)

### Executar com Maven

```bash
./mvnw spring-boot:run 
