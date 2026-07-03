# Описание REST-методов

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

## Поля docs-класса метода

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

## Как определяется имя реального REST-метода

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
