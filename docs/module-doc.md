# Описание модуля

Описание модуля задает верхнеуровневые поля OpenAPI-документа: название, описание, версию, автора и changelog.

## Класс Docs

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

## Поля описания

| Поле | DSL-метод | Тип | Значение по умолчанию | Для чего используется |
| --- | --- | --- | --- | --- |
| `name` | `name "..."` | `String` | имя модуля | `info.title` |
| `description` | `description "..."` | `String` | пустая строка | HTML-блок описания в `info.description` |
| `version` | `version "..."` | `String` | `1.0.0` | `info.version` |
| `author` | `author "..."` | `String` | пустая строка | `info.contact.name` |
| `changelog` | `changelog(...)` | `List<String>` | пустой список | HTML-список истории изменений в `info.description` |

## Имя модуля в paths

Обычно имя модуля берется из класса контроллера:

```groovy
this.getClass()
```

Последний сегмент имени класса становится `<moduleName>` в path:

```text
/exec?func=modules.<moduleName>.<methodName>
```

Для console/unknown script окружения используется fallback `ConsoleNSMP`.

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
