# Spring Cloud Config Demo

A small, two-application demonstration of **Spring Cloud Config**. The project runs a Config Server that reads configuration from a separate Git repository, and a payment-service client that loads its settings from that server at startup.

The external configuration repository is [config-repository](https://github.com/varunbansal946/config-repository.git).

## What this project demonstrates

- Keeping application code and environment configuration in separate repositories.
- Serving Git-backed configuration through Spring Cloud Config Server.
- Applying shared, application-specific, and profile-specific configuration.
- Importing remote configuration into a Spring Boot application with `spring.config.import`.

## Architecture

```text
payment-client (payment-service, dev profile)
        |
        | requests http://localhost:8888/payment-service/dev
        v
Config Server (port 8888)
        |
        | clones/reads the main branch
        v
config-repository
  ├── application.yml
  ├── payment-service.yml
  └── payment-service-dev.yml
```

The client receives an effective configuration made from these files:

1. `application.yml` — shared configuration for all clients.
2. `payment-service.yml` — configuration for the application named `payment-service`.
3. `payment-service-dev.yml` — values for the active `dev` profile; these override matching keys from the non-profile file.

For this example, the `dev` profile makes the payment service use the sandbox gateway, a 10-second timeout, and INR.

## Repository layout

```text
spring-cloud-config-demo/
├── configserver/                 # Git-backed Spring Cloud Config Server
│   └── src/main/resources/application.yaml
└── payment-client/               # Client application named payment-service
    ├── src/main/resources/application.yaml
    └── src/main/java/.../PaymentConfigController.java
```

Each folder is an independent Maven project; there is no parent Maven build at the repository root.

## Prerequisites

- JDK 21 or newer (the projects are compiled for Java 21).
- Internet access to GitHub. The Config Server is configured to clone the configuration repository at startup.
- Git, if you are cloning the repositories yourself.

The Maven Wrapper (`mvnw`) is included, so a separate Maven installation is not required.

## Run the example

Clone both repositories (the Config Server is already configured to use the public GitHub URL shown above):

```bash
git clone https://github.com/varunbansal946/spring-cloud-config-demo.git
git clone https://github.com/varunbansal946/config-repository.git
```

The second clone is useful for viewing or changing configuration locally; the running server reads the GitHub repository configured in `configserver/src/main/resources/application.yaml`.

### 1. Start the Config Server

In one terminal:

```bash
cd spring-cloud-config-demo/configserver
./mvnw spring-boot:run
```

Wait until the server is listening on port `8888`. Confirm that it can resolve the client configuration:

```bash
curl http://localhost:8888/payment-service/dev
```

The response should identify the `payment-service` application, the `dev` profile, and the Git-backed property sources.

### 2. Start the payment client

In a second terminal:

```bash
cd spring-cloud-config-demo/payment-client
./mvnw spring-boot:run
```

The client configuration declares:

```yaml
spring:
  application:
    name: payment-service
  profiles:
    active: dev
  config:
    import: configserver:http://localhost:8888
```

Because the import is not marked `optional:`, the client requires the Config Server to be available at startup.

### 3. Verify the resolved values

```bash
curl http://localhost:8080/payment/config
```

Expected response:

```json
{
  "gatewayUrl": "https://sandbox.payment-provider.com",
  "requestTimeout": 10000,
  "currency": "INR",
  "loggingLevel": "INFO"
}
```

The first three values come from `payment-service-dev.yml`; `loggingLevel` comes from the shared `application.yml`.

## How it works

### Config Server

`ConfigserverApplication` is annotated with `@EnableConfigServer`. Its configuration points Spring Cloud Config Server at:

```yaml
spring.cloud.config.server.git.uri: https://github.com/varunbansal946/config-repository.git
spring.cloud.config.server.git.default-label: main
spring.cloud.config.server.git.clone-on-start: true
```

`clone-on-start: true` means the server validates and clones the Git repository during startup. If GitHub is unavailable or the repository cannot be accessed, the server will not start successfully.

### Payment client

The client has the application name `payment-service` and starts with the `dev` profile. Spring Cloud Config uses those values to request `/payment-service/dev` from the server. `PaymentConfigController` injects the resulting `payment.gateway-url`, `payment.request-timeout`, `payment.currency`, and `logging.level.root` properties and exposes them at `/payment/config`.

## Test and build

Run the Config Server test from its own application directory:

```bash
cd configserver
./mvnw test
```

The Config Server context test initializes the Git-backed repository. It therefore needs outbound access to GitHub; an offline environment can cause that test to fail while trying to clone the configuration repository. Start the Config Server first before running the payment client's context test, because the client has a required Config Server import:

```bash
cd payment-client
./mvnw test
```

To build a runnable JAR without running tests:

```bash
./mvnw clean package -DskipTests
```

Run the generated JAR from the relevant `target` directory, or use `./mvnw spring-boot:run` as shown above.

## Changing configuration

Edit and commit a file in the [configuration repository](https://github.com/varunbansal946/config-repository.git), then restart the payment client to load the new values. This demo does not configure Spring Cloud Bus or a refresh-scoped endpoint on the client, so it does not provide automatic runtime refresh.

## Related repository

- **Configuration repository:** [config-repository](https://github.com/varunbansal946/config-repository.git) — contains the external YAML property sources served by this project.
