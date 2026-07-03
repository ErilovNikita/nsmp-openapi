# Частые ошибки

## Описание модуля не попало в документацию

Проверьте:

- класс описания помечен `@OpenApiDocs`;
- класс доступен в текущем package/classLoader;
- у класса есть публичный no-args constructor;
- или объект описания явно передан вторым аргументом в `OpenApiCollector`.

## Docs-класс метода не попал в документацию

Проверьте:

- на классе есть `@OpenApiRoute`;
- класс наследуется от `OpenApiRouteConfig`, если вы используете автоматический поиск;
- класс передан в `routeDocs`, если вы используете явный список;
- класс доступен в текущем scope контроллера;
- у класса есть публичный no-args constructor.

## Метод в OpenAPI получил неправильное имя

Если docs-класс называется не по правилу `<MethodName>Doc`, укажите имя явно:

```groovy
@OpenApiRoute("getDomains")
class DomainsDocumentation extends OpenApiRouteConfig {}
```

## Query-параметр не появился

Проверьте:

- `queryModel` указывает именно на `Class`, а не на объект;
- поле находится в DTO-классе;
- поле не `static`;
- DTO-класс не является enum, interface, annotation или abstract class.

## DTO-схема не появилась в components.schemas

Проверьте:

- DTO используется в `bodyModel`, `defaultResponse` или `responses`;
- DTO имеет хотя бы одно поле;
- DTO не находится в пакете `java.*` или `groovy.*`;
- если используется `explicitSchemas`, нужный DTO добавлен туда вручную.

Отдельный `queryModel` не появляется в `components.schemas`, потому что его поля раскрываются как query-параметры операции.

## Enum не появился в схеме параметра или поля

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

## POST не получил requestBody

Проверьте:

```groovy
httpMethod "POST"
bodyModel RequestBodyDto.CreateDomain
```

Если `bodyModel` не задан, `requestBody` не будет добавлен.

## Минимальный чеклист перед отправкой коллегам
- Есть описание модуля с `@OpenApiDocs` или объект описания явно передается в `OpenApiCollector`.
- У каждого REST-метода есть docs-класс с `@OpenApiRoute`.
- Docs-классы наследуются от `OpenApiRouteConfig`, если используется автоматический поиск.
- В docs-классе заполнены `summary`, `description`, `tags`.
- Для query-параметров задан `queryModel`.
- Для POST body задан `bodyModel`.
- Для успешного ответа задан `defaultResponse`.
- Ошибки описаны через `Responses.*`.
- DTO ошибок и ответов имеют поля.
- Обязательные поля помечены `@RequiredParam`.
- Enum-ограничения оформлены как `<fieldName>Enum`.
- Метод `getSwagger()` возвращает `new OpenApiCollector(...).build()`.
- Метод `docs()` или аналогичный отдает JSON наружу.
