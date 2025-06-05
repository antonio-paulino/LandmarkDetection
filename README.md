# CN2425TF – Sistema Cloud-Native para Deteção de Landmarks em Imagens

Projeto final de Computação na Nuvem – ISEL, verão 2024/2025

---

## Descrição do Projeto

O CN2425TF é um sistema distribuído desenvolvido em Java, que permite a submissão, processamento e consulta de imagens para identificação automática de monumentos e locais famosos (landmarks). Utiliza vários serviços geridos da Google Cloud Platform (GCP): Cloud Storage, Firestore, Pub/Sub, Compute Engine e Vision API.

O sistema é modular, escalável e suporta elasticidade, sendo composto por cinco componentes principais:
- **client/**: Cliente gRPC para submissão e consulta de imagens/resultados.
- **cloudFunction/**: Cloud Function para integração.
- **contract/**: Contrato Protocol Buffers (.proto) para o serviço gRPC.
- **landmarksApp/**: Worker para processamento de imagens e integração com Vision API.
- **server/**: Servidor gRPC (fachada do sistema, orquestração de tarefas).
- **vmAdmin/**: Módulo de administração de máquinas virtuais.


---

## Estrutura do Projeto

```
client/           # Cliente gRPC
cloudFunction/    # Código cloudFunction
contract/         # Compiler .proto
landmarksApp/     # Worker para deteção de landmarks
server/           # Servidor gRPC
vmAdmin/          # VM Admin
README.md         # Este ficheiro

````

---

## Requisitos

- Java 21 ou superior
- Maven
- Conta Google Cloud Platform com:
    - APIs ativas: Cloud Storage, Firestore, Pub/Sub, Vision API, Static Maps API
    - Service Account com permissões e credenciais (ficheiro JSON)

---

## Configuração Inicial

1. **Criar recursos no GCP**:
    - Bucket Cloud Storage
    - Coleção Firestore
    - Tópico Pub/Sub

2. **Ativar APIs necessárias** no GCP.

3. **Compilar o contrato**:

   ```sh
   cd cloudFunction
   mvn package install
    ````

4. **Colocar o ficheiro de credenciais do Service Account** no diretório de cada componente, ou definir a variável de ambiente `GOOGLE_APPLICATION_CREDENTIALS` para apontar para esse ficheiro.

5. **Criar um ficheiro** .env com `GOOGLE_MAPS_API_KEY=`

---

## Compilação

Compilar módulos individualmente:

```sh
cd server
mvn package

cd ../LandmarksApp
mvn package

cd ../client
mvn package

cd ../vmAdmin
mvn package
```

---

## Execução dos Componentes

### 1. Iniciar o Worker (LandmarksApp)

```sh
cd LandmarksApp
java -jar target/LandmarksApp-1.0-jar-with-dependencies.jar
```

O worker subscreve ao Pub/Sub, processando novas imagens automaticamente.<br>
É necessário o ficheiro `.json` com as credenciais da Service Account.

---

### 2. Iniciar o Servidor gRPC

```sh
cd server
java -jar target/server-1.0-jar-with-dependencies.jar <port>
```

O servidor fica à escuta de pedidos gRPC dos clientes.<br>
É necessário:
* Ficheiro `.env` com a chave da API
* Ficheiro `.json` com as credenciais da Service Account

Se não for especificada, a porta padrão é `8000`.

---

### 3. Executar o Cliente

```sh
cd client
java -jar target/client-1.0-jar-with-dependencies.jar <server_address> <port>
```

O cliente conecta ao servidor gRPC e permite submeter imagens e consultar resultados.<br>
Se não for especificada, a porta padrão é `8000` e o endereço do servidor é escolhido aleatoriamente utilizando uma 
cloud functiom.

---





