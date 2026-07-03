# Генерация OpenAPI JSON

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
