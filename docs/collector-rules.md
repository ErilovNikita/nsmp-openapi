# Правила коллектора

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
