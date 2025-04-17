
FROM maven:3.9.9-eclipse-temurin-21 AS builder

# Definir o diretório de trabalho
WORKDIR /app

# Copiar os arquivos de configuração do Maven
COPY pom.xml .

# Baixar as dependências (cache para acelerar builds futuras)
RUN mvn dependency:go-offline

# Copiar o código-fonte
COPY src ./src

# Compilar e empacotar a aplicação (ignorando testes para acelerar)
RUN mvn clean package -DskipTests

# Etapa 2: Criar a imagem final
FROM eclipse-temurin:21-jre

# Definir o diretório de trabalho
WORKDIR /app

# Copiar o JAR gerado da etapa de build
COPY --from=builder /app/target/Spring-batch-Training-0.0.1-SNAPSHOT.jar app.jar

# Expor a porta da aplicação (padrão do Spring Boot)
EXPOSE 8080

# Comando para executar a aplicação
ENTRYPOINT ["java", "-jar", "app.jar"]