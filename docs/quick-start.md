# Быстрый старт

Эта страница показывает минимальный порядок подключения `OpenApiCollector`. Детальные правила вынесены в отдельные разделы:

- [Описание модуля](module-doc.md)
- [DTO-модели](dto-models.md)
- [REST-ручки](routes.md)
- [Ответы и requestBody](responses.md)
- [Генерация OpenAPI JSON](generation.md)

## 1. Импортируйте классы коллектора

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

## 2. Опишите модуль

Создайте docs-класс модуля с `@OpenApiDocs` и `OpenApiModuleConfig`.

```groovy
@OpenApiDocs
class Docs extends OpenApiModuleConfig {
    {
        name "demoController"
        description "Демонстрационный пакетный модуль"
        version "1.0.0"
        author "Team"
    }
}
```

## 3. Опишите DTO

DTO используются для query-параметров, тела запроса, ответов и `components.schemas`.

```groovy
class QueryParamsDto {
    class GetDomains {
        @RequiredParam
        String state

        Integer limit
    }
}

class ResponseModelDto {
    class Domain {
        String uuid
        String title
        String state
    }
}
```

## 4. Опишите REST-ручку

Каждая ручка описывается отдельным docs-классом с `@OpenApiRoute`.

```groovy
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
    }
}
```

## 5. Сгенерируйте OpenAPI JSON

```groovy
@SuppressWarnings('unused')
String getSwagger() {
    return new OpenApiCollector(this.getClass()).build()
}
```

Если нужен интерактивный HTML-preview для Swagger UI:

```groovy
@SuppressWarnings('unused')
String previewSwagger() {
    return new OpenApiCollector(this.getClass()).preview()
}
```
