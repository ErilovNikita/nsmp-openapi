# OpenApiCollector

`OpenApiCollector` генерирует OpenAPI 3.0.4 JSON-документ для пакетного Groovy-модуля NSMP/Naumen Service Desk.

Документация описывается небольшими docs-классами:

- класс модуля помечается `@OpenApiDocs`;
- REST-методы помечаются `@OpenApiRoute`;
- описание модуля наследуется от `OpenApiModuleConfig`;
- описание метода наследуется от `OpenApiRouteConfig`;
- DTO-классы запроса и ответа используются для `parameters`, `requestBody`, `responses` и `components.schemas`.

Коллектор собирает `info`, `servers`, `paths`, `parameters`, `requestBody`, `responses`, `components.schemas` и `tags`, а на выходе возвращает pretty JSON или HTML-preview для Swagger UI.

## Что умеет модуль

- Находит описание модуля по аннотации `@OpenApiDocs`.
- Находит docs-классы методов по аннотации `@OpenApiRoute`.
- Строит URL методов NSMP вида `/exec?func=modules.<module>.<method>` и `/exec-post?func=modules.<module>.<method>`.
- Добавляет обязательные системные query-параметры `params`, `accessKey`, а для `POST` еще `raw`.
- Собирает query-параметры из DTO-класса.
- Собирает `requestBody` из DTO-класса тела запроса.
- Собирает DTO-схемы из моделей тела запроса и ответов.
- Поддерживает вложенные DTO и коллекции DTO.
- Поддерживает enum-значения через вложенные enum-классы вида `<fieldName>Enum`.
- Помечает обязательные поля по аннотации с именем `RequiredParam`.
- Добавляет типовые ответы `400`, `401`, `404`, `422`.
- Автоматически добавляет встроенные NSMP-ошибки авторизации `401` и `500`.
- Генерирует JSON для Swagger UI, Redoc, Postman или внутренней документации.
- Генерирует интерактивный HTML-preview через `preview()`.

## Быстрый старт

### 1. Импортируйте классы коллектора

```groovy
import ru.nerilov.openapi.OpenApiCollector
import ru.nerilov.openapi.OpenApiDocs
import ru.nerilov.openapi.OpenApiModuleConfig
import ru.nerilov.openapi.OpenApiRoute
import ru.nerilov.openapi.OpenApiRouteConfig
import ru.nerilov.openapi.DefaultResponse
import ru.nerilov.openapi.ResponseDoc
import ru.nerilov.openapi.Responses
```

Минимально обычно нужны `OpenApiCollector`, `OpenApiDocs`, `OpenApiModuleConfig`, `OpenApiRoute`, `OpenApiRouteConfig`, `DefaultResponse` и `Responses`.

### 2. Создайте описание модуля

Класс описания модуля помечается `@OpenApiDocs` и обычно наследуется от `OpenApiModuleConfig`.

```groovy
@OpenApiDocs
class Docs extends OpenApiModuleConfig {
    {
        name "zabbixController - Naumen Service Desk Package"
        description "Пакет для работы с внутренним и внешним инстансом Zabbix"
        version "2.0.1"
        author "Erilov.NA"
        changelog(
                "2026-06-23: Initial release",
                "2026-06-24: Migration to OpenApiCollector"
        )
    }
}
```

Если передать объект описания вторым аргументом в `OpenApiCollector`, аннотация не обязательна:

```groovy
new OpenApiCollector(this.getClass(), new Docs()).build()
```

Если второй аргумент не передан, коллектор ищет в текущем пакете класс с `@OpenApiDocs` и создает его через `newInstance()`.

Поля описания:

| Поле | DSL-метод | Тип | Значение по умолчанию | Для чего используется |
| --- | --- | --- | --- | --- |
| `name` | `name "..."` | `String` | имя модуля | `info.title` |
| `description` | `description "..."` | `String` | пустая строка | HTML-блок описания в `info.description` |
| `version` | `version "..."` | `String` | `1.0.0` | `info.version` |
| `author` | `author "..."` | `String` | пустая строка | `info.contact.name` |
| `changelog` | `changelog(...)` | `List<String>` | пустой список | HTML-список истории изменений в `info.description` |

### 3. Опишите DTO для query-параметров

DTO запроса - это обычный класс с приватными нестатическими полями. В Groovy свойства класса по умолчанию создают private fields, поэтому достаточно объявить их как обычные свойства.

```groovy
class QueryParamsDto {
    class GetDomains {
        String name
        String state
        Integer limit
        Boolean onlyActive
    }
}
```

Такой DTO превратится в query-параметры:

- `name`
- `state`
- `limit`
- `onlyActive`

Типы автоматически мапятся в OpenAPI-типы:

| Groovy/Java тип | OpenAPI type |
| --- | --- |
| `byte`, `short`, `int`, `Integer`, `long` | `integer` |
| `float`, `double`, `BigDecimal` | `number` |
| `boolean`, `Boolean` | `boolean` |
| `String`, `char`, `Character`, `Date`, `LocalDate`, `LocalDateTime` | `string` |
| `List`, `Set`, `Collection`, array | `array` |

### 4. Пометьте обязательные параметры

Коллектор не зависит от конкретного класса аннотации. Он смотрит только на имя аннотации: `RequiredParam`.

```groovy
import java.lang.annotation.ElementType
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import java.lang.annotation.Target

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@interface RequiredParam {}
```

Использование:

```groovy
class QueryParamsDto {
    class GetDomains {
        @RequiredParam
        String state

        Integer limit
    }
}
```

Поле `state` попадет в OpenAPI как `required: true`, а `limit` как `required: false`.

### 5. Опишите enum-ограничения для поля

Если у поля есть ограниченный набор значений, создайте вложенный enum рядом с DTO. Коллектор ищет enum по соглашению:

- `<fieldName>Enum`;
- регистр имени не важен.

```groovy
class QueryParamsDto {
    class GetDomains {
        String state

        enum StateEnum {
            active,
            closed,
            archived
        }
    }
}
```

В OpenAPI для `state` появится:

```yaml
schema:
  type: string
  enum:
    - active
    - closed
    - archived
```

Если enum-константы имеют метод или property `code`, коллектор использует `code`, а не `name()`:

```groovy
enum StateEnum {
    active("active"),
    closed("closed")

    final String code

    StateEnum(String code) {
        this.code = code
    }
}
```

### 6. Опишите DTO ответа

DTO ответа используется для `components.schemas` и `$ref` в responses.

```groovy
class ResponseModelDto {
    class Domain {
        String uuid
        String title
        String state
        Date createdAt
    }

    class ErrorMessage {
        Integer code
        String message
    }
}
```

Если DTO содержит другое DTO или коллекцию DTO, коллектор добавит все найденные схемы:

```groovy
class ResponseModelDto {
    class Owner {
        String uuid
        String title
    }

    class Domain {
        String uuid
        String title
        Owner owner
        List<Owner> reviewers
        Set<String> aliases
    }
}
```

Для `Set<T>` в OpenAPI дополнительно будет `uniqueItems: true`.

## Описание REST-метода

Каждый REST-метод описывается отдельным docs-классом с аннотацией `@OpenApiRoute`.

Рекомендуемый вариант - наследоваться от `OpenApiRouteConfig` и заполнять DSL в initializer-блоке:

```groovy
@OpenApiRoute
class GetDomainsDoc extends OpenApiRouteConfig {
    {
        summary "Метод для получения доменов"
        description "Метод получает список доменов с возможностью фильтрации"
        httpMethod "GET"
        tags "Find", "General"

        queryModel QueryParamsDto.GetDomains

        defaultResponse DefaultResponse.list(
                ResponseModelDto.Domain,
                "Список доменов успешно получен"
        )

        responses(
                Responses.validationError(
                        ResponseModelDto.ErrorMessage,
                        [code: 422, message: "Invalid value for parameter 'state'"]
                ),
                Responses.badRequest(
                        ResponseModelDto.ErrorMessage,
                        [code: 400, message: "Missing required parameter 'state'"]
                ),
                Responses.notFound(
                        ResponseModelDto.ErrorMessage,
                        [code: 404, message: "Objects not found"]
                )
        )
    }
}
```

Можно использовать и обычные поля, потому что коллектор читает свойства по имени:

```groovy
@OpenApiRoute
class GetDomainsDoc {
    String summary = "Метод для получения доменов"
    String description = "Метод получает список доменов с возможностью фильтрации"
    List<String> tags = ["Find", "General"]
    Class queryModel = QueryParamsDto.GetDomains
    DefaultResponse defaultResponse = DefaultResponse.list(
            ResponseModelDto.Domain,
            "Список доменов успешно получен"
    )
}
```

### Поля docs-класса метода

| Поле | DSL-метод | Тип | Значение по умолчанию | Что делает |
| --- | --- | --- | --- | --- |
| `summary` | `summary "..."` | `String` | пустая строка | Короткое описание операции |
| `description` | `description "..."` | `String` | пустая строка | Полное описание операции |
| `httpMethod` | `httpMethod "GET"` | `String` | `GET` | HTTP-метод операции. Для `POST` path будет `/exec-post?...` |
| `tags` | `tags "A", "B"` | `List<String>` | пустой список | OpenAPI tags операции |
| `queryModel` | `queryModel SomeDto` | `Class` | `null` | DTO query-параметров |
| `bodyModel` | `bodyModel SomeDto` | `Class` | `null` | DTO тела запроса |
| `defaultResponse` | `defaultResponse ...` | `DefaultResponse` | `null` | Основной успешный ответ |
| `responses` | `responses(...)` | `List<ResponseDoc>` | пустой список | Дополнительные ответы |

Дополнительно `OpenApiRouteConfig` поддерживает `response(...)`, чтобы добавлять ответы по одному:

```groovy
response Responses.badRequest(ResponseModelDto.ErrorMessage)
response Responses.validationError(ResponseModelDto.ErrorMessage)
```

### Как определяется имя реального REST-метода

Если в `@OpenApiRoute` не передано значение, имя метода вычисляется из имени docs-класса:

| Docs-класс | REST-метод |
| --- | --- |
| `GetDomainsDoc` | `getDomains` |
| `CreateIncidentDoc` | `createIncident` |
| `DocsDoc` | `docs` |

Пример:

```groovy
@OpenApiRoute
class GetDomainsDoc extends OpenApiRouteConfig {}
```

Будет сгенерирован path:

```text
/exec?func=modules.<moduleName>.getDomains
```

Если имя REST-метода отличается от имени docs-класса, укажите его явно:

```groovy
@OpenApiRoute("docs")
class OpenApiJsonDoc extends OpenApiRouteConfig {}
```

Будет сгенерирован path:

```text
/exec?func=modules.<moduleName>.docs
```

## Успешные ответы

Для основного ответа используйте `DefaultResponse`.

### Ответ объектом

```groovy
defaultResponse DefaultResponse.object(
        ResponseModelDto.Domain,
        "Домен успешно получен",
        [uuid: "domain-123", title: "example.com"]
)
```

OpenAPI response будет ссылаться на схему `Domain`.

### Ответ списком объектов

```groovy
defaultResponse DefaultResponse.list(
        ResponseModelDto.Domain,
        "Список доменов успешно получен"
)
```

OpenAPI response будет массивом:

```yaml
schema:
  type: array
  items:
    $ref: "#/components/schemas/Domain"
```

### Ответ без DTO-схемы

Если метод возвращает произвольный объект или документацию, можно передать `null`:

```groovy
defaultResponse DefaultResponse.object(
        null,
        "OpenAPI-документация успешно получена"
)
```

Тогда схема ответа будет:

```yaml
schema:
  type: object
```

## Дополнительные ответы

Для ошибок и альтернативных HTTP-статусов используйте `Responses`.

```groovy
responses(
        Responses.badRequest(ResponseModelDto.ErrorMessage),
        Responses.unauthorized(ResponseModelDto.ErrorMessage),
        Responses.notFound(ResponseModelDto.ErrorMessage),
        Responses.validationError(ResponseModelDto.ErrorMessage)
)
```

Доступные фабрики:

| Метод | HTTP-код | Description |
| --- | --- | --- |
| `Responses.badRequest(...)` | `400` | `Некорректный запрос` |
| `Responses.unauthorized(...)` | `401` | `Ошибка авторизации` |
| `Responses.notFound(...)` | `404` | `Объекты не найдены` |
| `Responses.validationError(...)` | `422` | `Некорректно заполнены параметры` |
| `Responses.error(...)` | любой | Передается вручную |

Можно передавать пример ответа:

```groovy
Responses.validationError(
        ResponseModelDto.ErrorMessage,
        [code: 422, message: "Invalid value 'deleted' for parameter 'state'"]
)
```

Можно создать произвольный ответ:

```groovy
Responses.error(
        409,
        "Конфликт состояния объекта",
        ResponseModelDto.ErrorMessage,
        [code: 409, message: "Object state conflict"]
)
```

Если схема не представлена DTO-классом, можно передать `schemaName`. Это используется, например, для встроенных текстовых ошибок:

```groovy
Responses.error(
        503,
        "Внешний сервис недоступен",
        null,
        "Service unavailable",
        "text/plain",
        "ExternalServiceError"
)
```

## POST-методы и requestBody

Для POST-метода укажите `httpMethod "POST"` и `bodyModel`.

```groovy
class RequestBodyDto {
    class CreateDomain {
        @RequiredParam
        String name

        String description
    }
}

@OpenApiRoute
class CreateDomainDoc extends OpenApiRouteConfig {
    {
        httpMethod "POST"
        summary "Создание домена"
        description "Создает домен с указанным именем"
        tags "Create"
        bodyModel RequestBodyDto.CreateDomain
        defaultResponse DefaultResponse.object(
                ResponseModelDto.Domain,
                "Домен успешно создан"
        )
        responses(
                Responses.badRequest(ResponseModelDto.ErrorMessage),
                Responses.validationError(ResponseModelDto.ErrorMessage)
        )
    }
}
```

Для `POST` коллектор добавит:

- path `/exec-post?func=modules.<moduleName>.createDomain`;
- базовые query-параметры `params`, `accessKey`, `raw`;
- `requestBody` с `$ref` на `CreateDomain`.

## Генерация OpenAPI JSON

Обычно в контроллере делают отдельный метод, который возвращает JSON.

```groovy
@SuppressWarnings('unused')
String getSwagger() {
    return new OpenApiCollector(this.getClass()).build()
}
```

При таком вызове коллектор автоматически найдет:

- описание модуля: первый класс в текущем пакете с `@OpenApiDocs`;
- docs-классы маршрутов: классы в текущем пакете с `@OpenApiRoute`, которые наследуются от `OpenApiRouteConfig`.

Если нужен строгий порядок маршрутов, передайте `routeDocs` явно:

```groovy
@SuppressWarnings('unused')
String getSwagger() {
    return new OpenApiCollector(
            this.getClass(),
            new Docs(),
            null,
            [
                    GetDomainsDoc,
                    CreateDomainDoc,
                    DocsDoc
            ]
    ).build()
}
```

Аргументы `OpenApiCollector`:

| Аргумент | Тип | Что передавать |
| --- | --- | --- |
| `controllerClass` | `Class` | Обычно `this.getClass()` |
| `moduleDoc` | `Object` | Объект описания модуля, например `new Docs()`. Если `null`, ищется `@OpenApiDocs` |
| `explicitSchemas` | `List<Class>` или `null` | Явный список DTO-схем. Обычно `null`, чтобы коллектор нашел схемы сам |
| `routeDocs` | `List<Class>` | Явный список docs-классов маршрутов |

Важно: автоматический поиск `routeDocs` берет только классы, которые одновременно помечены `@OpenApiRoute` и наследуются от `OpenApiRouteConfig`. Если передать `routeDocs` явно, достаточно аннотации `@OpenApiRoute`.

## Публикация метода документации

JSON можно вернуть из REST-метода.

```groovy
@OpenApiRoute("docs")
class DocsDoc extends OpenApiRouteConfig {
    {
        summary "Метод для получения OpenAPI-документации"
        description "Метод возвращает OpenAPI JSON-документацию текущего пакетного модуля"
        tags "General"
        defaultResponse DefaultResponse.object(
                null,
                "OpenAPI-документация успешно получена"
        )
    }
}

@SuppressWarnings('unused')
void docs() {
    RequestProcessor
            .create(request, response, user, prefs.copy().assertHttpMethod("GET"))
            .process { WebApiUtilities webUtils ->
                webUtils.setBodyAsString(getSwagger())
            }
}
```

После этого OpenAPI JSON будет доступен через REST-метод `docs`.

Для интерактивного просмотра можно вернуть HTML:

```groovy
@SuppressWarnings('unused')
String previewSwagger() {
    return new OpenApiCollector(this.getClass()).preview()
}
```

`preview()` встраивает сгенерированную спецификацию в Swagger UI.

## Полный пример

```groovy
package ru.nerilov.demo

import ru.nerilov.openapi.OpenApiCollector
import ru.nerilov.openapi.OpenApiDocs
import ru.nerilov.openapi.OpenApiModuleConfig
import ru.nerilov.openapi.OpenApiRoute
import ru.nerilov.openapi.OpenApiRouteConfig
import ru.nerilov.openapi.DefaultResponse
import ru.nerilov.openapi.ResponseDoc
import ru.nerilov.openapi.Responses

import java.lang.annotation.ElementType
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import java.lang.annotation.Target

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@interface RequiredParam {}

@OpenApiDocs
class Docs extends OpenApiModuleConfig {
    {
        name "demoController"
        description "Демонстрационный пакетный модуль"
        version "1.0.0"
        author "Team"
        changelog "2026-06-24: Initial release"
    }
}

class QueryParamsDto {
    class GetDomains {
        @RequiredParam
        String state

        Integer limit

        enum StateEnum {
            active,
            closed
        }
    }
}

class RequestBodyDto {
    class CreateDomain {
        @RequiredParam
        String name

        String description
    }
}

class ResponseModelDto {
    class Domain {
        String uuid
        String title
        String state
    }

    class ErrorMessage {
        Integer code
        String message
    }
}

@OpenApiRoute
class GetDomainsDoc extends OpenApiRouteConfig {
    {
        summary "Получение доменов"
        description "Возвращает список доменов с фильтрацией по состоянию"
        tags "Domains"
        queryModel QueryParamsDto.GetDomains
        defaultResponse DefaultResponse.list(
                ResponseModelDto.Domain,
                "Список доменов успешно получен"
        )
        responses(
                Responses.badRequest(ResponseModelDto.ErrorMessage),
                Responses.validationError(ResponseModelDto.ErrorMessage),
                Responses.notFound(ResponseModelDto.ErrorMessage)
        )
    }
}

@OpenApiRoute
class CreateDomainDoc extends OpenApiRouteConfig {
    {
        httpMethod "POST"
        summary "Создание домена"
        description "Создает новый домен"
        tags "Domains"
        bodyModel RequestBodyDto.CreateDomain
        defaultResponse DefaultResponse.object(
                ResponseModelDto.Domain,
                "Домен успешно создан"
        )
        responses(
                Responses.badRequest(ResponseModelDto.ErrorMessage),
                Responses.validationError(ResponseModelDto.ErrorMessage)
        )
    }
}

@OpenApiRoute("docs")
class DocsDoc extends OpenApiRouteConfig {
    {
        summary "OpenAPI-документация"
        description "Возвращает OpenAPI JSON текущего модуля"
        tags "General"
        defaultResponse DefaultResponse.object(null, "Документация успешно получена")
    }
}

@SuppressWarnings('unused')
String getSwagger() {
    return new OpenApiCollector(
            this.getClass(),
            new Docs(),
            null,
            [
                    GetDomainsDoc,
                    CreateDomainDoc,
                    DocsDoc
            ]
    ).build()
}
```

## Как коллектор собирает components.schemas
При автоматическом сборе коллектор берет DTO-схемы из нескольких мест:

- `bodyModel`;
- `defaultResponse.schema`;
- `responses[*].schema`;
- вложенные DTO-поля внутри найденных DTO;
- generic-тип коллекций `List<T>`, `Set<T>`, `Collection<T>`.

`queryModel` используется для генерации query-параметров и не добавляется в `components.schemas` как отдельная схема.

Если вы передали `explicitSchemas` в `OpenApiCollector`, автоматический сбор схем отключается и используется только этот список.

```groovy
new OpenApiCollector(
        this.getClass(),
        new Docs(),
        [
                RequestBodyDto.CreateDomain,
                ResponseModelDto.Domain,
                ResponseModelDto.ErrorMessage
        ],
        [
                GetDomainsDoc,
                DocsDoc
        ]
).build()
```

Обычно `explicitSchemas` лучше оставлять `null`, чтобы не забыть добавить новую DTO-схему вручную.

## Встроенные NSMP-ошибки
Коллектор автоматически добавляет в каждый метод две встроенные ошибки:

| HTTP-код | Content-Type | Схема | Description |
| --- | --- | --- | --- |
| `401` | `text/plain` | `EmptyAccessKey` | `Ключ доступа не найден **(Встроенная ошибка NSMP)**` |
| `500` | `text/plain` | `NoValidAccessKey` | `Ключ доступа не действительный **(Встроенная ошибка NSMP)**` |

Схемы `EmptyAccessKey` и `NoValidAccessKey` автоматически добавляются в `components.schemas`.

Эти ответы не нужно описывать руками в каждом docs-классе.

## Базовые query-параметры
В каждую операцию автоматически добавляются системные query-параметры NSMP:

| Параметр | Когда добавляется | Тип | Значение |
| --- | --- | --- | --- |
| `params` | всегда | `string` | пустая строка |
| `accessKey` | всегда | `string` | ключ доступа пользователя |
| `raw` | только для `POST` | `boolean` | `true` |

Их не нужно добавлять в `queryModel`.

## Servers
Коллектор пытается прочитать `api.web.baseUrl` и построить server вида:

```text
https://<host>/sd/services/rest
```

Описание server определяется по host:

| Признак host | Installation/Environment |
| --- | --- |
| содержит `support` | `Internal` |
| содержит `pss` или `partnersupport` | `DSO` |
| содержит `.dev.` или начинается с `dev.` | `Dev` |
| содержит `.staging.` или начинается с `staging.` | `Stage` |
| содержит `.test.` или начинается с `test.` | `Test` |
| начинается с `support.ocs`, содержит `.partnersupport.`, `.prod.` или начинается с `prod.` | `Prod` |

Если `api.web.baseUrl` недоступен или не содержит host, `servers` будет пустым списком.

## Правила именования
### Имя модуля
Обычно имя модуля берется из класса контроллера:

```groovy
this.getClass()
```

Последний сегмент имени класса становится `<moduleName>` в path:

```text
/exec?func=modules.<moduleName>.<methodName>
```

Для console/unknown script окружения используется fallback `ConsoleNSMP`.

### Имя схемы
Для вложенных DTO используется имя последнего класса:

```groovy
ResponseModelDto.Domain
```

В OpenAPI это будет:

```text
#/components/schemas/Domain
```

## Рекомендации по структуре
Хорошая структура контроллера:

```text
imports
аннотации RequiredParam / другие локальные аннотации
Docs с @OpenApiDocs
QueryParamsDto
RequestBodyDto
ResponseModelDto
docs-классы с @OpenApiRoute
реальные REST-методы
getSwagger/docs/preview
```

Практичные правила:

- Держите один docs-класс на один REST-метод.
- Называйте docs-класс как метод с суффиксом `Doc`: `GetDomainsDoc` -> `getDomains`.
- Если имя не совпадает, используйте `@OpenApiRoute("realMethodName")`.
- Наследуйте docs-классы методов от `OpenApiRouteConfig`, если хотите использовать автоматический поиск без явного `routeDocs`.
- Наследуйте описание модуля от `OpenApiModuleConfig`, чтобы использовать DSL-методы `name`, `description`, `version`, `author`, `changelog`.
- Всегда задавайте `summary`: это главная строка в Swagger UI.
- В `description` пишите поведение метода, ограничения и важные нюансы.
- Для группировки методов используйте `tags`.
- Для ошибок используйте `Responses.*`, а не ручные map.
- Для успешного ответа используйте `DefaultResponse.object()` или `DefaultResponse.list()`.
- Не добавляйте системные параметры `params`, `accessKey`, `raw` в DTO: коллектор делает это сам.
- Для enum-ограничений используйте вложенный enum `<fieldName>Enum`.
- Для обязательных параметров используйте аннотацию с именем `RequiredParam`.

## Частые ошибки
### Описание модуля не попало в документацию
Проверьте:

- класс описания помечен `@OpenApiDocs`;
- класс доступен в текущем package/classLoader;
- у класса есть публичный no-args constructor;
- или объект описания явно передан вторым аргументом в `OpenApiCollector`.

### Docs-класс метода не попал в документацию
Проверьте:

- на классе есть `@OpenApiRoute`;
- класс наследуется от `OpenApiRouteConfig`, если вы используете автоматический поиск;
- класс передан в `routeDocs`, если вы используете явный список;
- класс доступен в текущем scope контроллера;
- у класса есть публичный no-args constructor.

### Метод в OpenAPI получил неправильное имя
Если docs-класс называется не по правилу `<MethodName>Doc`, укажите имя явно:

```groovy
@OpenApiRoute("getDomains")
class DomainsDocumentation extends OpenApiRouteConfig {}
```

### Query-параметр не появился
Проверьте:

- `queryModel` указывает именно на `Class`, а не на объект;
- поле находится в DTO-классе;
- поле не `static`;
- DTO-класс не является enum, interface, annotation или abstract class.

### DTO-схема не появилась в components.schemas
Проверьте:

- DTO используется в `bodyModel`, `defaultResponse` или `responses`;
- DTO имеет хотя бы одно поле;
- DTO не находится в пакете `java.*` или `groovy.*`;
- если используется `explicitSchemas`, нужный DTO добавлен туда вручную.

Отдельный `queryModel` не появляется в `components.schemas`, потому что его поля раскрываются как query-параметры операции.

### Enum не появился в схеме параметра или поля
Проверьте имя вложенного enum:

```groovy
String state
enum stateEnum { active, closed }
```

или:

```groovy
String state
enum StateEnum { active, closed }
```

Оба варианта подходят, потому что поиск case-insensitive.

### POST не получил requestBody
Проверьте:

```groovy
httpMethod "POST"
bodyModel RequestBodyDto.CreateDomain
```

Если `bodyModel` не задан, `requestBody` не будет добавлен.

## Минимальный чеклист перед отправкой коллегам
- [ ] Есть описание модуля с `@OpenApiDocs` или объект описания явно передается в `OpenApiCollector`.
- [ ] У каждого REST-метода есть docs-класс с `@OpenApiRoute`.
- [ ] Docs-классы наследуются от `OpenApiRouteConfig`, если используется автоматический поиск.
- [ ] В docs-классе заполнены `summary`, `description`, `tags`.
- [ ] Для query-параметров задан `queryModel`.
- [ ] Для POST body задан `bodyModel`.
- [ ] Для успешного ответа задан `defaultResponse`.
- [ ] Ошибки описаны через `Responses.*`.
- [ ] DTO ошибок и ответов имеют поля.
- [ ] Обязательные поля помечены `@RequiredParam`.
- [ ] Enum-ограничения оформлены как `<fieldName>Enum`.
- [ ] Метод `getSwagger()` возвращает `new OpenApiCollector(...).build()`.
- [ ] Метод `docs()` или аналогичный отдает JSON наружу.

## Что получается на выходе
`build()` возвращает строку с pretty JSON:

```groovy
String json = new OpenApiCollector(
        this.getClass(),
        new Docs(),
        null,
        [GetDomainsDoc, DocsDoc]
).build()
```

`preview()` возвращает HTML-страницу со Swagger UI:

```groovy
String html = new OpenApiCollector(this.getClass()).preview()
```

JSON можно:
- открыть в Swagger UI;
- импортировать в Postman;
- передать фронтенду;
- положить в документацию;
- использовать для ревью контрактов API.