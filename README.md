# Codesy Platform Backend

Codesy is a modular monolith coding platform backend designed to handle secure code execution, user management, and problem submission. It is built with Spring Boot and leverages a modern, robust technology stack for high-performance and scalability.

## 🚀 Technology Stack

- **Java 21**
- **Spring Boot 3.3.5**
- **PostgreSQL 16** - Primary relational database
- **Redis 7** - Caching and rate limiting
- **Apache Kafka** - Event streaming and asynchronous execution (e.g., submission queuing)
- **Docker** - Secure sandbox environments for code execution
- **Flyway** - Database migrations
- **JWT (JSON Web Tokens)** - Authentication and authorization

## 🏗️ Features

- **Multi-language Code Execution**: Securely compile and execute code submissions in isolated Docker containers (Java, Python 3, C++17).
- **Asynchronous Processing**: Uses Kafka and the Outbox pattern for reliable, non-blocking code submission handling.
- **Robust Security**: Includes JWT-based authentication, rate limiting (IP and User-based), and guardrails against excessive resource usage.
- **API Documentation**: Integrated with Springdoc OpenAPI for interactive Swagger UI documentation.

## 🛠️ Prerequisites

To run the backend locally, you need the following installed:
- [Java 21](https://adoptium.net/temurin/releases/?version=21)
- [Maven](https://maven.apache.org/download.cgi)
- [Docker & Docker Compose](https://www.docker.com/products/docker-desktop/)

## 🏃 Getting Started

### 1. Start Infrastructure Services

The project uses Docker Compose to manage local infrastructure (PostgreSQL, Redis, Kafka) and the execution sandbox containers.

```bash
# Start the infrastructure and build the execution sandboxes
docker compose --profile sandboxes build
docker compose --profile sandboxes up -d
```

### 2. Configure the Application (Optional)

Default configurations are provided in `src/main/resources/application.yml`. By default, the application will connect to the locally running Docker containers. 

Flyway will automatically handle database migrations on application startup.

### 3. Run the Application

You can run the application using Maven:

```bash
# run the application
cd codesy-java
mvn spring-boot:run

# OR
./mvnw spring-boot:run
```

The server will start on port `8080` (default) or the port configured in your environment.

### 4. Access API Documentation

Once the application is running, you can access the Swagger UI for the REST API documentation at:
`http://localhost:8080/swagger-ui.html`

## 🐳 Sandbox Environments

The execution engine uses specific Docker profiles to run user code securely. The `docker-compose.yml` file contains the build definitions for these sandboxes:

- `sandbox-java`
- `sandbox-python`
- `sandbox-cpp`

These containers remain running in the background (`tail -f /dev/null`) and the application executes code within them via the Docker API.

## 🔒 Security & Rate Limiting

The platform implements guardrails to ensure stability:
- **Rate Limiting**: Configurable per-user and per-IP submission limits using Redis.
- **Resource Limits**: Sandbox execution has strict memory limits, compile timeouts, and run timeouts to prevent abuse.
- **Execution Queues**: Maximum in-flight and queued submissions are managed to protect the system under load.
