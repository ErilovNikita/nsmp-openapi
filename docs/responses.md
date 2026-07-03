# Ответы и requestBody

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
