# Полный пример

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
