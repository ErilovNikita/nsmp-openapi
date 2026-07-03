# OpenApiCollector
> Генерирует OpenAPI документацию для пакетного Groovy-модуля Naumen Service Desk.

<p align="center">
  <img src="./docs/public/logo.png" width="512" height="512">
</p>

Документация описывается небольшими docs-классами:
- класс модуля помечается `@OpenApiDocs`;
- REST-методы помечаются `@OpenApiRoute`;
- описание модуля наследуется от `OpenApiModuleConfig`;
- описание метода наследуется от `OpenApiRouteConfig`;
- DTO-классы запроса и ответа используются для `parameters`, `requestBody`, `responses` и `components.schemas`.

Коллектор собирает `info`, `servers`, `paths`, `parameters`, `requestBody`, `responses`, `components.schemas` и `tags`, а на выходе возвращает pretty JSON или HTML-preview для Swagger UI.

## Документация
Полная документация перенесена в папку [`docs`](docs/index.md) и собирается через VitePress.

[Открыть документацию](https://erilovnikita.github.io/nsmp-openapi/)