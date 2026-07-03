# DTO-модели

DTO-классы используются для `parameters`, `requestBody`, `responses` и `components.schemas`.

## Query DTO

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

## Типы полей

Типы автоматически мапятся в OpenAPI-типы:

| Groovy/Java тип | OpenAPI type |
| --- | --- |
| `byte`, `short`, `int`, `Integer`, `long` | `integer` |
| `float`, `double`, `BigDecimal` | `number` |
| `boolean`, `Boolean` | `boolean` |
| `String`, `char`, `Character`, `Date`, `LocalDate`, `LocalDateTime` | `string` |
| `List`, `Set`, `Collection`, array | `array` |

## Обязательные поля

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

## Enum-ограничения

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

## DTO ответа

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

## Как собираются components.schemas

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
