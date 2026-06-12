# openapi-test-framework

[![CI](https://github.com/Jonnydevp/openapi-test-framework/actions/workflows/ci.yml/badge.svg)](https://github.com/Jonnydevp/openapi-test-framework/actions/workflows/ci.yml)

Фреймворк для **автоматической генерации тестов REST API** на основе OpenAPI-спецификации.

Курсовая работа. Инструмент анализирует OpenAPI/Swagger-спецификацию и автоматически
порождает набор тестов трех видов, а также выявляет расхождения между документацией
и фактическим поведением API:

- **smoke** - happy-path вызов каждого эндпоинта (ожидание 2xx);
- **контрактные** - соответствие статуса, content-type, схемы тела и заголовков спецификации;
- **fuzz (property-based)** - генерация входных данных из JSON-схем (Kotest property), свойство
  "сервис не отвечает 5xx на валидном входе"

## Стек

Kotlin 2 · JUnit 5 (`@TestFactory`) · REST Assured · Kotest (property) · swagger-parser ·
networknt json-schema-validator · KotlinPoet (кодогенерация `.kt`) · WireMock (демо) ·
Allure (отчеты) · GitHub Actions (CI). JVM target 17.

## Два режима генерации

- **runtime** - тесты строятся и исполняются "на лету" как JUnit 5 `DynamicTest` (без файлов);
- **codegen** - фреймворк эмитит готовые читаемые `.kt`-файлы тестов (KotlinPoet).

```kotlin
val framework = ApiTestFramework.fromResource("openapi/petstore.yaml")

// runtime: отдать из @TestFactory
@TestFactory fun api() = framework.runtime("http://localhost:8080").allTests()

// codegen: выпустить .kt
framework.emit(Path.of("build/generated-tests"))
```

## Архитектура (модуль `core`)

| Пакет         | Назначение                                                                 |
|---------------|----------------------------------------------------------------------------|
| `spec`        | `SpecLoader` (swagger-parser, `resolveFully`) + внутренняя модель + `SpecMapper` |
| `data`        | `SchemaDataGenerator` (валидный пример) и `SchemaArb` (Kotest `Arb` для fuzz) |
| `execution`   | `ApiClient` (обертка REST Assured), `RequestDataFactory`                    |
| `generator`   | `SmokeGen` / `ContractGen` / `FuzzGen`, runtime `DynamicTestFactory`, codegen `KotlinPoetEmitter` |
| `validation`  | `SchemaValidator`, `ContractValidator`, `MismatchReport` - детектор расхождений док↔поведение |

| Модуль  | Назначение                                                          |
|---------|--------------------------------------------------------------------|
| `core`  | сам фреймворк                                                      |
| `demo`  | эталонная спека Petstore, WireMock-мок, e2e-прогон, Allure          |

## Сборка и запуск

```bash
./gradlew build                      # компиляция + юнит-тесты core + e2e demo
./gradlew :demo:test                 # только e2e против WireMock-мока
./gradlew allureReport               # сгенерировать Allure-отчет (demo/build/reports/allure-report)
./gradlew allureServe                # открыть Allure-отчет в браузере
```

## Детектор расхождений

Демо намеренно содержит «сломанный» стаб (`GET /pets/{petId}` без обязательного поля `name`).
`ContractValidator` ловит это как расхождение `BODY_SCHEMA`, что демонстрируется в
`MismatchDetectionDemoTest`. Категории: `UNDECLARED_STATUS`, `CONTENT_TYPE`, `BODY_SCHEMA`,
`MISSING_HEADER`.

## CI

GitHub Actions (`.github/workflows/ci.yml`): сборка, e2e против WireMock внутри JVM (без внешней
сети), генерация Allure-отчета и публикация артефактов (`allure-report`, `test-reports`).
