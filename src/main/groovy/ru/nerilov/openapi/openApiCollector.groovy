package ru.nerilov.openapi

/*! UTF8 */

import ru.naumen.core.server.script.api.injection.InjectApi

import groovy.json.JsonBuilder
import groovy.json.StringEscapeUtils

import java.lang.annotation.ElementType
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import java.lang.annotation.Target
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.lang.reflect.Modifier

/** Marker-аннотация для docs-классов, которые должны попасть в OpenAPI-документацию. */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@interface OpenApiRoute {
    /**
     * Имя реального REST-метода; если не указано, вычисляется из имени docs-класса.
     *
     * @return имя REST-метода или пустая строка для автоопределения
     */
    String value() default ""
}

/** Marker-аннотация для модуля, информация из которого должна попасть в OpenAPI-документацию. */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@interface OpenApiDocs {}

/**
 * Базовый DSL-конфиг для описания OpenAPI-документа пакетного модуля.
 *
 * Используется как декларативный мини-конфиг:
 *
 * <pre>
 * class Docs extends OpenApiModuleConfig {
 *     {
 *         name "zabbixController - Naumen Service Desk Package"
 *         description "Пакет для корректного общения с Zabbix"
 *         version "2.0.1"
 *         author "Erilov.NA"
 *
 *         changelog(
 *                 "2026-06-23: Initial release",
 *                 "2026-06-24: Migration to OpenApiCollector"
 *         )
 *     }
 * }
 * </pre>
 *
 * Данные этого класса попадают в блок OpenAPI:
 *
 * <pre>
 * info.title
 * info.version
 * info.description
 * info.contact.name
 * </pre>
 */
abstract class OpenApiModuleConfig {
    /** Название OpenAPI-документа. Попадает в info.title. */
    String name = ""

    /** HTML/текстовое описание модуля. Используется внутри info.description. */
    String description = ""

    /** Версия API-документа. Попадает в info.version. */
    String version = OpenApiConstants.DEFAULT_API_VERSION

    /** Автор или владелец модуля. Попадает в info.contact.name. */
    String author = ""

    /** История изменений модуля. Встраивается в info.description отдельным HTML-блоком. */
    List<String> changelog = []

    /**
     * Устанавливает название OpenAPI-документа.
     *
     * @param value название модуля/API
     */
    void name(String value) {
        this.name = value
    }

    /**
     * Устанавливает описание модуля.
     *
     * Может содержать обычный текст или HTML, который будет отображаться в Swagger UI.
     *
     * @param value описание модуля
     */
    void description(String value) {
        this.description = value
    }

    /**
     * Устанавливает версию OpenAPI-документа.
     *
     * @param value версия API/модуля
     */
    void version(String value) {
        this.version = value
    }

    /**
     * Устанавливает автора или владельца модуля.
     *
     * @param value имя автора
     */
    void author(String value) {
        this.author = value
    }

    /**
     * Устанавливает историю изменений через varargs.
     *
     * Пример:
     *
     * <pre>
     * changelog(
     *     "2026-06-23: Initial release",
     *     "2026-06-24: Migration"
     * )
     * </pre>
     *
     * @param values список записей changelog
     */
    void changelog(String... values) {
        this.changelog = values?.toList() ?: []
    }

    /**
     * Устанавливает историю изменений через List.
     *
     * @param values список записей changelog
     */
    void changelog(List<String> values) {
        this.changelog = values ?: []
    }
}

/**
 * Базовый DSL-конфиг для описания OpenAPI-документации REST-метода.
 *
 * Используется вместе с аннотацией {@link OpenApiRoute}.
 *
 * Пример:
 *
 * <pre>
 * //@OpenApiRoute
 * class GetDomainsDoc extends OpenApiRouteConfig {
 *     {
 *         summary "Метод для получения доменов"
 *         description "Метод получает список доменов с возможностью фильтрации"
 *         httpMethod "GET"
 *         tags "Find", "General"
 *
 *         queryModel QueryParamsDto.GetDomains
 *
 *         defaultResponse DefaultResponse.list(
 *                 ResponseModelDto.Domain,
 *                 "Список доменов успешно получен"
 *         )
 *
 *         responses(
 *                 Responses.validationError(ResponseModelDto.ErrorMessage),
 *                 Responses.badRequest(ResponseModelDto.ErrorMessage),
 *                 Responses.notFound(ResponseModelDto.ErrorMessage)
 *         )
 *     }
 * }
 * }
 * </pre>
 *
 * Если в {@link OpenApiRoute} не указано имя метода, оно вычисляется по имени класса:
 *
 * <pre>
 * GetDomainsDoc -> getDomains
 * DocsDoc       -> docs
 * </pre>
 */
abstract class OpenApiRouteConfig {
    /** Краткое описание операции. Попадает в operation.summary. */
    String summary = ""

    /** Подробное описание операции. Попадает в operation.description. */
    String description = ""

    /** HTTP-метод операции: GET, POST, PUT, DELETE и т.д. По умолчанию GET. */
    String httpMethod = "GET"

    /** Список тегов операции. Используется для группировки методов в Swagger UI. */
    List<String> tags = []

    /** DTO-класс query-параметров. Его поля будут раскрыты как OpenAPI query parameters. */
    Class queryModel = null

    /** DTO-класс тела запроса. Используется для генерации requestBody. */
    Class bodyModel = null

    /** Основной успешный ответ операции. Обычно HTTP 200. */
    DefaultResponse defaultResponse = null

    /** Дополнительные ответы операции: 400, 404, 422 и другие. */
    List<ResponseDoc> responses = []

    /**
     * Устанавливает краткое описание операции.
     *
     * @param value текст summary
     */
    void summary(String value) {
        this.summary = value
    }

    /**
     * Устанавливает подробное описание операции.
     *
     * Может содержать обычный текст или HTML/Markdown, если это поддерживается Swagger UI.
     *
     * @param value текст description
     */
    void description(String value) {
        this.description = value
    }

    /**
     * Устанавливает HTTP-метод операции.
     *
     * Примеры:
     *
     * <pre>
     * httpMethod "GET"
     * httpMethod "POST"
     * </pre>
     *
     * @param value HTTP-метод
     */
    void httpMethod(String value) {
        this.httpMethod = value
    }

    /**
     * Устанавливает список тегов через varargs.
     *
     * Пример:
     *
     * <pre>
     * tags "Find", "General"
     * </pre>
     *
     * @param values теги операции
     */
    void tags(String... values) {
        this.tags = values?.toList() ?: []
    }

    /**
     * Устанавливает список тегов через List.
     *
     * @param values теги операции
     */
    void tags(List<String> values) {
        this.tags = values ?: []
    }

    /**
     * Устанавливает DTO-класс query-параметров.
     *
     * Поля этого класса будут добавлены в operation.parameters.
     *
     * @param value DTO-класс query-параметров
     */
    void queryModel(Class value) {
        this.queryModel = value
    }

    /**
     * Устанавливает DTO-класс тела запроса.
     *
     * Класс будет добавлен в operation.requestBody как JSON-schema.
     *
     * @param value DTO-класс тела запроса
     */
    void bodyModel(Class value) {
        this.bodyModel = value
    }

    /**
     * Устанавливает основной успешный ответ операции.
     *
     * Примеры:
     *
     * <pre>
     * defaultResponse DefaultResponse.object(ResponseModelDto.Domain)
     *
     * defaultResponse DefaultResponse.list(
     *     ResponseModelDto.Domain,
     *     "Список доменов успешно получен"
     * )
     * </pre>
     *
     * @param value описание успешного ответа
     */
    void defaultResponse(DefaultResponse value) {
        this.defaultResponse = value
    }

    /**
     * Устанавливает дополнительные ответы операции через varargs.
     *
     * Пример:
     *
     * <pre>
     * responses(
     *     Responses.validationError(ResponseModelDto.ErrorMessage),
     *     Responses.notFound(ResponseModelDto.ErrorMessage)
     * )
     * </pre>
     *
     * @param values список дополнительных ответов
     */
    void responses(ResponseDoc... values) {
        this.responses = values?.toList() ?: []
    }

    /**
     * Устанавливает дополнительные ответы операции через List.
     *
     * @param values список дополнительных ответов
     */
    void responses(List<ResponseDoc> values) {
        this.responses = values ?: []
    }

    /**
     * Добавляет один дополнительный ответ к уже существующему списку responses.
     *
     * Удобно, если нужно накапливать ответы по одному.
     *
     * Пример:
     *
     * <pre>
     * response Responses.notFound(ResponseModelDto.ErrorMessage)
     * response Responses.validationError(ResponseModelDto.ErrorMessage)
     * </pre>
     *
     * @param value дополнительный ответ
     */
    void response(ResponseDoc value) {
        if (value) this.responses.add(value)
    }
}

/** Reader для безопасного чтения полей docs-объектов. */
class OpenApiDocReader {

    /**
     * Возвращает значение свойства или defaultValue, если источник пустой или свойство недоступно.
     *
     * @param source объект-источник, из которого читается свойство
     * @param name имя свойства
     * @param defaultValue значение по умолчанию
     * @return значение свойства или defaultValue
     */
    static Object prop(Object source, String name, Object defaultValue = null) {
        if (!source) return defaultValue

        try {
            def value = source."$name"
            return value != null ? value : defaultValue
        } catch (Exception ignored) {
            return defaultValue
        }
    }

    /**
     * Возвращает строковое значение свойства или defaultValue.
     *
     * @param source объект-источник, из которого читается свойство
     * @param name имя свойства
     * @param defaultValue строковое значение по умолчанию
     * @return строковое значение свойства или defaultValue
     */
    static String string(Object source, String name, String defaultValue = "") {
        prop(source, name, defaultValue)?.toString()
    }

    /**
     * Возвращает свойство как список, оборачивая одиночное значение в List.
     *
     * @param source объект-источник, из которого читается свойство
     * @param name имя свойства
     * @param defaultValue список по умолчанию
     * @return значение свойства в виде списка
     */
    static List list(Object source, String name, List defaultValue = []) {
        Object value = prop(source, name, defaultValue)

        if (value == null) return defaultValue
        if (value instanceof List) return value
        if (value instanceof Collection) return value as List

        [value]
    }

    /**
     * Возвращает свойство как Class или null.
     *
     * @param source объект-источник, из которого читается свойство
     * @param name имя свойства
     * @return значение свойства как Class или null
     */
    static Class clazz(Object source, String name) {
        Object value = prop(source, name, null)
        value instanceof Class ? value : null
    }

    /**
     * Возвращает defaultResponse docs-объекта.
     *
     * @param source docs-объект метода
     * @return defaultResponse или null
     */
    static DefaultResponse defaultResponse(Object source) {
        Object value = prop(source, "defaultResponse", null)
        value instanceof DefaultResponse ? value : null
    }

    /**
     * Возвращает список дополнительных ответов docs-объекта.
     *
     * @param source docs-объект метода
     * @return список дополнительных ResponseDoc
     */
    static List<ResponseDoc> responses(Object source) {
        list(source, "responses", [])
                .findAll { it instanceof ResponseDoc }
                .collect { it as ResponseDoc }
    }
}

/** Успешный ответ метода. */
class DefaultResponse {
    Integer code = 200
    String description = "OK"
    Class schema
    Boolean list = false
    Object example

    /**
     * Создает описание успешного ответа с объектной схемой.
     *
     * @param schema класс DTO-схемы ответа
     * @param description описание ответа
     * @param example пример тела ответа
     * @return описание успешного ответа
     */
    static DefaultResponse object(Class schema, String description = "OK", Object example = null) {
        new DefaultResponse(
                code: 200,
                description: description,
                schema: schema,
                list: false,
                example: example
        )
    }

    /**
     * Создает описание успешного ответа со списком объектов.
     *
     * @param schema класс DTO-схемы элемента списка
     * @param description описание ответа
     * @param example пример тела ответа
     * @return описание успешного ответа со списком объектов
     */
    static DefaultResponse list(Class schema, String description = "OK", Object example = null) {
        new DefaultResponse(
                code: 200,
                description: description,
                schema: schema,
                list: true,
                example: example
        )
    }
}

/** Дополнительный ответ метода: 400, 404, 422 и т.д. */
class ResponseDoc {
    Integer code
    String description
    Class schema
    String schemaName
    Boolean list = false
    Object example
    String contentType = OpenApiConstants.CONTENT_TYPE_JSON
}

/** Helper-фабрика типовых ответов. */
class Responses {
    /**
     * Создает описание ответа 400.
     *
     * @param schema класс DTO-схемы ответа
     * @param example пример тела ответа
     * @param contentType content-type тела ответа
     * @param schemaName имя схемы в components.schemas, если схема не представлена Class
     * @return описание ответа 400
     */
    @SuppressWarnings("unused")
    static ResponseDoc badRequest(
            Class schema = null,
            Object example = [code: 400, message: "Bad request"],
            String contentType = OpenApiConstants.CONTENT_TYPE_JSON,
            String schemaName = null
    ) {
        error(400, "Некорректный запрос", schema, example, contentType, schemaName)
    }

    /**
     * Создает описание ответа 401.
     *
     * @param schema класс DTO-схемы ответа
     * @param example пример тела ответа
     * @param contentType content-type тела ответа
     * @param schemaName имя схемы в components.schemas, если схема не представлена Class
     * @return описание ответа 401
     */
    @SuppressWarnings("unused")
    static ResponseDoc unauthorized(
            Class schema = null,
            Object example = [code: 401, message: "Unauthorized"],
            String contentType = OpenApiConstants.CONTENT_TYPE_JSON,
            String schemaName = null
    ) {
        error(401, "Ошибка авторизации", schema, example, contentType, schemaName)
    }

    /**
     * Создает описание ответа 404.
     *
     * @param schema класс DTO-схемы ответа
     * @param example пример тела ответа
     * @param contentType content-type тела ответа
     * @param schemaName имя схемы в components.schemas, если схема не представлена Class
     * @return описание ответа 404
     */
    @SuppressWarnings("unused")
    static ResponseDoc notFound(
            Class schema = null,
            Object example = [code: 404, message: "Objects not found"],
            String contentType = OpenApiConstants.CONTENT_TYPE_JSON,
            String schemaName = null
    ) {
        error(404, "Объекты не найдены", schema, example, contentType, schemaName)
    }

    /**
     * Создает описание ответа 422.
     *
     * @param schema класс DTO-схемы ответа
     * @param example пример тела ответа
     * @param contentType content-type тела ответа
     * @param schemaName имя схемы в components.schemas, если схема не представлена Class
     * @return описание ответа 422
     */
    @SuppressWarnings("unused")
    static ResponseDoc validationError(
            Class schema = null,
            Object example = [code: 422, message: "Validation error"],
            String contentType = OpenApiConstants.CONTENT_TYPE_JSON,
            String schemaName = null
    ) {
        error(422, "Некорректно заполнены параметры", schema, example, contentType, schemaName)
    }

    /**
     * Создает описание встроенной ошибки недействительного ключа доступа.
     *
     * @return описание встроенной ошибки 500
     */
    @SuppressWarnings("unused")
    static ResponseDoc noValidAccessKey() {
        error(
                500,
                "Ключ доступа не действительный **(Встроенная ошибка NSMP)**",
                null,
                null,
                OpenApiConstants.CONTENT_TYPE_TEXT,
                OpenApiConstants.SCHEMA_NO_VALID_ACCESS_KEY
        )
    }

    /**
     * Создает описание встроенной ошибки отсутствующего ключа доступа.
     *
     * @return описание встроенной ошибки 401
     */
    @SuppressWarnings("unused")
    static ResponseDoc emptyAccessKey() {
        ResponseDoc response = unauthorized(
                null,
                null,
                OpenApiConstants.CONTENT_TYPE_TEXT,
                OpenApiConstants.SCHEMA_EMPTY_ACCESS_KEY
        )
        response.description = "Ключ доступа не найден **(Встроенная ошибка NSMP)**"
        response
    }

    /**
     * Создает описание ответа с произвольным HTTP-кодом.
     *
     * @param code HTTP-код ответа
     * @param description описание ответа
     * @param schema класс DTO-схемы ответа
     * @param example пример тела ответа
     * @param contentType content-type тела ответа
     * @param schemaName имя схемы в components.schemas, если схема не представлена Class
     * @return описание ответа
     */
    @SuppressWarnings("unused")
    static ResponseDoc error(
            Integer code,
            String description,
            Class schema = null,
            Object example = null,
            String contentType = OpenApiConstants.CONTENT_TYPE_JSON,
            String schemaName = null
    ) {
        new ResponseDoc(
                code: code,
                description: description,
                schema: schema,
                schemaName: schemaName,
                example: example,
                contentType: contentType
        )
    }
}

/** Описание простой OpenAPI-схемы, которая не строится из DTO-класса. */
class SchemaDoc {
    String name
    String type
    String title
    String description
    Object example
}

/** Helper-фабрика встроенных OpenAPI-схем. */
class Schemas {
    /**
     * Создает описание схемы ошибки недействительного ключа доступа.
     *
     * @return описание схемы NoValidAccessKey
     */
    static SchemaDoc noValidAccessKey() {
        new SchemaDoc(
                name: OpenApiConstants.SCHEMA_NO_VALID_ACCESS_KEY,
                type: "string",
                title: OpenApiConstants.SCHEMA_NO_VALID_ACCESS_KEY,
                description: "Ключ доступа не действительный",
                example: "Переход не может быть выполнен: ключ авторизации *********************** не найден или не может быть использован повторно"
        )
    }

    /**
     * Создает описание схемы ошибки отсутствующего ключа доступа.
     *
     * @return описание схемы EmptyAccessKey
     */
    static SchemaDoc emptyAccessKey() {
        new SchemaDoc(
                name: OpenApiConstants.SCHEMA_EMPTY_ACCESS_KEY,
                type: "string",
                title: OpenApiConstants.SCHEMA_EMPTY_ACCESS_KEY,
                description: "Ключ доступа не найден",
                example: "Ошибка авторизации. Необходимо указать ключ авторизации в параметре accessKey"
        )
    }
}

/** Константы OpenAPI-коллектора. */
class OpenApiConstants {
    static final String OPENAPI_VERSION = "3.0.4"
    static final String DEFAULT_API_VERSION = "1.0.0"

    static final String CONTENT_TYPE_JSON = "application/json"
    static final String CONTENT_TYPE_TEXT = "text/plain"

    static final String QUERY_LOCATION = "query"

    static final String PARAM_PARAMS = "params"
    static final String PARAM_ACCESS_KEY = "accessKey"
    static final String PARAM_RAW = "raw"

    static final String DEFAULT_GLOBAL_PARAMS = ""

    static final String SCHEMA_NO_VALID_ACCESS_KEY = "NoValidAccessKey"
    static final String SCHEMA_EMPTY_ACCESS_KEY = "EmptyAccessKey"
}

/** Reflection-утилиты. */
class OpenApiReflection {

    /**
     * Возвращает все приватные нестатические поля класса и его родителей.
     *
     * @param clazz класс, поля которого нужно собрать
     * @return список полей от базового класса к наследнику
     */
    static List<Field> getAllFields(Class clazz) {
        List<Field> fields = []

        while (clazz != null && clazz != Object) {
            fields.addAll(clazz.declaredFields.findAll {!it.synthetic && Modifier.isPrivate(it.modifiers) && !Modifier.isStatic(it.modifiers)})
            clazz = clazz.superclass
        }

        fields.reverse()
    }

    /**
     * Проверяет, можно ли считать класс DTO-схемой.
     *
     * @param clazz проверяемый класс
     * @return true, если класс подходит для генерации DTO-схемы
     */
    static boolean isDtoClass(Class clazz) {
        if (!clazz) return false
        if (clazz.isAnnotation()) return false
        if (clazz.isEnum()) return false
        if (clazz.isInterface()) return false
        if (Modifier.isAbstract(clazz.modifiers)) return false

        String packageName = clazz.package?.name ?: ''
        if (packageName.startsWith('java.')) return false
        if (packageName.startsWith('groovy.')) return false

        getAllFields(clazz)?.size() > 0
    }

    /**
     * Проверяет, помечено ли поле аннотацией RequiredParam.
     *
     * @param field проверяемое поле
     * @return true, если поле является обязательным
     */
    static boolean isRequired(Field field) {
        field.annotations.any {it.annotationType().simpleName == 'RequiredParam'}
    }

    /**
     * Ищет вложенный enum для поля модели по соглашению fieldNameEnum.
     *
     * @param modelClass класс модели, в котором выполняется поиск
     * @param fieldName имя поля модели
     * @return найденный enum-класс или null
     */
    static Class<? extends Enum> findNestedEnumForField(Class<?> modelClass, String fieldName) {
        String pascalEnumName = fieldName.capitalize() + 'Enum'
        String camelEnumName = fieldName + 'Enum'

        modelClass.declaredClasses.find { Class nested -> nested.isEnum() && (nested.simpleName.equalsIgnoreCase(pascalEnumName) || nested.simpleName.equalsIgnoreCase(camelEnumName))} as Class<? extends Enum>
    }

    /**
     * Возвращает значения enum, используя code при его наличии.
     *
     * @param enumType enum-класс
     * @return список строковых значений enum
     */
    static List<String> getEnumValues(Class<? extends Enum> enumType) {
        enumType.enumConstants.collect { enumConstant ->
            try {
                enumConstant.metaClass.respondsTo(enumConstant, 'getCode')
                        ? enumConstant.code?.toString()
                        : enumConstant.name()
            } catch (Exception ignored) {
                enumConstant.name()
            }
        }
    }
}

/** Утилиты именования. */
class OpenApiNames {

    /**
     * Возвращает имя модуля по классу контроллера.
     *
     * @param controllerClass класс контроллера
     * @return имя модуля
     */
    static String moduleName(Class controllerClass) {
        String name = controllerClass.getName()
        name.contains('unknownScriptDate') ? 'ConsoleNSMP' : name.tokenize('.')?.last()
    }

    /**
     * Возвращает имя OpenAPI-схемы для класса.
     *
     * @param clazz класс схемы
     * @return имя схемы без внешних классов
     */
    static String schemaName(Class clazz) {
        clazz.name.tokenize('$').last()
    }

    /**
     * Нормализует текстовое имя схемы в имя components.schemas.
     *
     * @param rawName исходное имя схемы
     * @return нормализованное имя схемы
     */
    static String schemaNameFromText(String rawName) {
        if (!rawName) return rawName

        rawName
                .replace('.', '$')
                .tokenize('$')
                .last()
                .replace('$', '_')
                .replace('.', '_')
    }

    /**
     * Возвращает OpenAPI $ref для класса схемы.
     *
     * @param clazz класс схемы
     * @return ссылка на схему в components.schemas
     */
    static String ref(Class clazz) {
        "#/components/schemas/${schemaName(clazz)}"
    }

    /**
     * Возвращает OpenAPI $ref для текстового имени схемы.
     *
     * @param rawName исходное имя схемы
     * @return ссылка на схему в components.schemas
     */
    static String ref(String rawName) {
        "#/components/schemas/${schemaNameFromText(rawName)}"
    }

    /**
     * Возвращает ожидаемое имя docs-класса для REST-метода.
     *
     * @param method REST-метод контроллера
     * @return имя docs-класса
     */
    static String methodDocClassName(Method method) {
        method.name.capitalize() + "Doc"
    }
}

/** Маппер Java/Groovy типов в OpenAPI-типы. */
class OpenApiTypeMapper {

    /**
     * Нормализует имя Java/Groovy-типа в OpenAPI-тип.
     *
     * @param rawType исходное имя типа
     * @return OpenAPI-тип
     */
    static String normalizeType(String rawType) {
        Map typeMapping = [
            byte: "integer",
            short: "integer",
            int: "integer",
            integer: "integer",
            long: "integer",
            float: "number",
            double: "number",
            bigdecimal: "number",

            boolean: "boolean",

            string: "string",
            char: "string",
            character: "string",
                
            date: "string",
            localdate: "string",
            localdatetime: "string",

            list: "array",
            set: "array",
            collection: "array",
            array: "array",
        ]

        typeMapping.getOrDefault(rawType?.toLowerCase(), "string")
    }

    /**
     * Возвращает простое имя класса без пакета.
     *
     * @param type класс типа
     * @return простое имя класса
     */
    static String simpleTypeName(Class type) {
        type.name.tokenize('.').last()
    }
}

/** Коллектор описаний методов. */
class MethodDocCollector {
    Class controllerClass
    List<Class> explicitDocClasses = []

    /**
     * Создает коллектор docs-классов для контроллера.
     *
     * @param controllerClass класс контроллера, внутри которого ищутся docs-классы
     * @param explicitDocClasses явно переданный список docs-классов
     */
    MethodDocCollector(Class controllerClass, List<Class> explicitDocClasses = []) {
        this.controllerClass = controllerClass
        this.explicitDocClasses = explicitDocClasses ?: []
    }

    /**
     * Собирает docs-классы из явного списка или вложенных классов контроллера.
     *
     * @return список docs-классов с аннотацией OpenApiRoute
     */
    List<Class> collectDocClasses() {
        List<Class> result = []
        if (explicitDocClasses) return explicitDocClasses.findAll {it.isAnnotationPresent(OpenApiRoute)}
        controllerClass.declaredClasses.each {collectAnnotatedDocClasses(it, result)}

        result
    }

    /**
     * Рекурсивно добавляет аннотированные docs-классы в результат.
     *
     * @param clazz класс, с которого начинается обход
     * @param result список для накопления найденных docs-классов
     */
    void collectAnnotatedDocClasses(Class clazz, List<Class> result) {
        if (!clazz) return
        if (clazz.isAnnotationPresent(OpenApiRoute)) result.add(clazz)

        clazz.declaredClasses.each {collectAnnotatedDocClasses(it, result)}
    }

    /**
     * Создает экземпляр docs-класса.
     *
     * @param docClass docs-класс метода
     * @return экземпляр docs-класса
     */
    @SuppressWarnings("GrMethodMayBeStatic")
    Object collectDoc(Class docClass) {
        docClass.newInstance()
    }

    /**
     * Возвращает имя REST-метода из аннотации или имени docs-класса.
     *
     * @param docClass docs-класс метода
     * @return имя REST-метода
     */
    String collectMethodName(Class docClass) {
        OpenApiRoute annotation = docClass.getAnnotation(OpenApiRoute) as OpenApiRoute
        if (annotation?.value()) return annotation.value()
        inferMethodNameFromDocClass(docClass)
    }

    /**
     * Вычисляет имя REST-метода из имени docs-класса.
     *
     * @param docClass docs-класс метода
     * @return имя REST-метода
     */
    @SuppressWarnings("GrMethodMayBeStatic")
    String inferMethodNameFromDocClass(Class docClass) {
        String name = docClass.simpleName
        if (name.endsWith('Doc')) name = name.substring(0, name.length() - 3)
        name[0].toLowerCase() + name.substring(1)
    }
}

/** Коллектор DTO-схем. */
class SchemaCollector {
    Class controllerClass
    MethodDocCollector methodDocCollector
    List<Class> explicitSchemas

    /**
     * Создает коллектор DTO-схем для контроллера и его docs-классов.
     *
     * @param controllerClass класс контроллера
     * @param methodDocCollector коллектор docs-классов методов
     * @param explicitSchemas явно переданный список DTO-схем
     */
    SchemaCollector(Class controllerClass, MethodDocCollector methodDocCollector, List<Class> explicitSchemas = null) {
        this.controllerClass = controllerClass
        this.methodDocCollector = methodDocCollector
        this.explicitSchemas = explicitSchemas
    }

    /**
     * Собирает DTO-схемы из явного списка или из моделей запросов и ответов.
     *
     * @return список DTO-классов, которые нужно добавить в components.schemas
     */
    List<Class> collectSchemas() {
        if (explicitSchemas != null) return explicitSchemas

        Set<Class> result = [] as Set

        methodDocCollector.collectDocClasses().each { Class docClass ->
            Object doc = methodDocCollector.collectDoc(docClass)

            // Модели параметров запроса не нужно добавлять в общий список
            //collectSchemaTree(OpenApiDocReader.clazz(doc, "queryModel"), result)
            collectSchemaTree(OpenApiDocReader.clazz(doc, "bodyModel"), result)

            DefaultResponse defaultResponse = OpenApiDocReader.defaultResponse(doc)
            if (defaultResponse?.schema) collectSchemaTree(defaultResponse.schema, result)

            OpenApiDocReader.responses(doc).each { ResponseDoc response ->
                collectSchemaTree(response.schema, result)
            }
        }

        result as List<Class>
    }

    /**
     * Рекурсивно добавляет DTO-класс и вложенные DTO-типы его полей.
     *
     * @param clazz DTO-класс для проверки и добавления
     * @param result набор для накопления найденных DTO-классов
     */
    void collectSchemaTree(Class clazz, Set<Class> result) {
        if (!clazz) return
        if (!OpenApiReflection.isDtoClass(clazz)) return
        if (result.contains(clazz)) return

        result.add(clazz)

        OpenApiReflection.getAllFields(clazz).each { Field field ->
            Class fieldType = field.type
            if (OpenApiReflection.isDtoClass(fieldType)) collectSchemaTree(fieldType, result)

            if (Collection.isAssignableFrom(fieldType)) {
                def genericType = null

                try {
                    genericType = field.genericType.hasProperty('actualTypeArguments')
                            ? field.genericType.actualTypeArguments?.first()
                            : null
                } catch (Exception ignored) {}

                if (genericType instanceof Class && OpenApiReflection.isDtoClass(genericType)) {
                    collectSchemaTree(genericType, result)
                }
            }
        }
    }
}

/** Реестр DTO-схем. */
class SchemaRegistry {
    List<Class> schemas = []

    /**
     * Создает реестр доступных DTO-схем.
     *
     * @param schemas список DTO-классов
     */
    SchemaRegistry(List<Class> schemas = []) {
        this.schemas = schemas ?: []
    }

    /**
     * Проверяет, есть ли класс в реестре схем.
     *
     * @param clazz проверяемый класс
     * @return true, если класс зарегистрирован как DTO-схема
     */
    boolean contains(Class clazz) {
        schemas.contains(clazz)
    }

    /**
     * Ищет схему по полному, простому или нормализованному имени.
     *
     * @param name имя схемы
     * @return найденный класс схемы или null
     */
    Class findByName(String name) {
        if (!name) return null

        String normalizedExpected = name.replace('.', '$')

        schemas.find { Class schema ->
            schema.name == normalizedExpected ||
                    schema.name.endsWith('.' + normalizedExpected) ||
                    schema.simpleName == name ||
                    schema.name.tokenize('.').last() == normalizedExpected ||
                    OpenApiNames.schemaName(schema) == OpenApiNames.schemaNameFromText(name)
        }
    }
}

/** Коллектор OpenAPI components.schemas. */
class ComponentsCollector {
    SchemaRegistry registry

    /**
     * Создает коллектор components на основе реестра схем.
     *
     * @param registry реестр DTO-схем
     */
    ComponentsCollector(SchemaRegistry registry) {
        this.registry = registry
    }

    /**
     * Собирает корневой объект OpenAPI components.
     *
     * @return объект components с блоком schemas
     */
    Map collectComponents() {[
        schemas: collectSchemas()
    ]}

    /**
     * Собирает все схемы components.schemas, включая встроенные ошибки.
     *
     * @return карта OpenAPI-схем по их именам
     */
    Map collectSchemas() {[
        *: collectInternalErrorSchemas(),
        *: registry.schemas.collectEntries { Class schema ->
            collectSchema(schema)
        }
    ]}

    /**
     * Собирает OpenAPI-схему для одного DTO-класса.
     *
     * @param schemaClass DTO-класс, для которого строится схема
     * @return карта с именем схемы и ее OpenAPI-описанием
     */
    Map collectSchema(Class schemaClass) {
        Map properties = OpenApiReflection.getAllFields(schemaClass)
                .collectEntries { Field field ->
                    [(field.name): collectFieldSchema(schemaClass, field)]
                }

        List<String> requiredFields = OpenApiReflection.getAllFields(schemaClass)
                .findAll { OpenApiReflection.isRequired(it) }
                .collect { it.name }

        Map schema = [
                type: 'object',
                title: OpenApiNames.schemaName(schemaClass),
                description: '',
                properties: properties
        ]

        if (requiredFields) schema.required = requiredFields

        [ (OpenApiNames.schemaName(schemaClass)): schema ]
    }

    /**
     * Собирает OpenAPI-схему из явного описания схемы.
     *
     * @param schemaDoc описание схемы
     * @return карта с именем схемы и ее OpenAPI-описанием
     */
    Map collectSchema(SchemaDoc schemaDoc) {
        [
                (schemaDoc.name): [
                        type: schemaDoc.type,
                        title: schemaDoc.title,
                        description: schemaDoc.description,
                        example: schemaDoc.example
                ]
        ]
    }

    /**
     * Собирает OpenAPI-схему для поля DTO.
     *
     * @param ownerClass DTO-класс, которому принадлежит поле
     * @param field поле DTO
     * @return OpenAPI-схема поля
     */
    Map collectFieldSchema(Class ownerClass, Field field) {
        Class enumClass = OpenApiReflection.findNestedEnumForField(ownerClass, field.name)

        if (enumClass) return [
            type: 'string',
            enum: OpenApiReflection.getEnumValues(enumClass)
        ]

        Class fieldType = field.type

        if (registry.contains(fieldType)) return [
            '$ref': OpenApiNames.ref(fieldType)
        ]

        if (Collection.isAssignableFrom(fieldType)) return collectCollectionSchema(field)

        [ type: OpenApiTypeMapper.normalizeType(OpenApiTypeMapper.simpleTypeName(fieldType)) ]
    }

    /**
     * Собирает OpenAPI-схему для коллекционного поля.
     *
     * @param field поле-коллекция
     * @return OpenAPI-схема массива
     */
    Map collectCollectionSchema(Field field) {
        def genericType = null

        try {
            genericType = field.genericType.hasProperty('actualTypeArguments')
                    ? field.genericType.actualTypeArguments?.first()
                    : null
        } catch (Exception ignored) {}

        Map result = [type: 'array']

        if (Set.isAssignableFrom(field.type)) result.uniqueItems = true

        if (genericType instanceof Class && registry.contains(genericType)) {
            result.items = [
                    '$ref': OpenApiNames.ref(genericType)
            ]
            return result
        }

        String genericName = genericType instanceof Class
                ? OpenApiTypeMapper.simpleTypeName(genericType)
                : 'object'

        result.items = [
                type: OpenApiTypeMapper.normalizeType(genericName)
        ]

        result
    }

    /**
     * Возвращает схемы встроенных текстовых ошибок NSMP.
     *
     * @return карта встроенных схем ошибок
     */
    Map collectInternalErrorSchemas() {
        [
                Schemas.noValidAccessKey(),
                Schemas.emptyAccessKey()
        ].collectEntries { SchemaDoc schema ->
            collectSchema(schema)
        }
    }
}

/** Коллектор OpenAPI responses. */
class ResponseCollector {

    /**
     * Преобразует defaultResponse в OpenAPI responses.
     *
     * @param response описание успешного ответа метода
     * @return карта OpenAPI responses для успешного ответа
     */
    Map collectDefaultResponse(DefaultResponse response) {
        if (!response) return [:]

        collectResponse(new ResponseDoc(
                code: response.code,
                description: response.description,
                schema: response.schema,
                list: response.list,
                example: response.example
        ))
    }

    /**
     * Собирает OpenAPI-описание одного ответа.
     *
     * @param response описание ответа метода
     * @return карта OpenAPI responses с одним HTTP-кодом
     */
    Map collectResponse(ResponseDoc response) {
        if (!response) return [:]
        Map contentDataMap = [
                schema: collectResponseSchema(response.schema, response.list, response.schemaName)
        ]
        if (response.example != null) contentDataMap.example = response.example

        [
                (response.code.toString()): [
                        description: response.description ?: '',
                        content: [
                                (response.contentType ?: OpenApiConstants.CONTENT_TYPE_JSON): contentDataMap
                        ]
                ]
        ]
    }

    /**
     * Собирает схему тела ответа.
     *
     * @param schema класс DTO-схемы ответа
     * @param list true, если ответ является списком объектов
     * @param schemaName имя схемы в components.schemas, если схема не представлена Class
     * @return OpenAPI-схема тела ответа
     */
    Map collectResponseSchema(Class schema, Boolean list = false, String schemaName = null) {
        if (schemaName) return [
            '$ref': OpenApiNames.ref(schemaName)
        ]

        if (!schema) {
            return [
                    type: 'object'
            ]
        }

        if (list) {
            return [
                    type: 'array',
                    items: [
                            '$ref': OpenApiNames.ref(schema)
                    ]
            ]
        }

        [
                '$ref': OpenApiNames.ref(schema)
        ]
    }

    /**
     * Возвращает встроенные ответы ошибок авторизации NSMP.
     *
     * @return карта встроенных ответов ошибок
     */
    Map collectInternalErrorResponses() {
        [
                Responses.noValidAccessKey(),
                Responses.emptyAccessKey()
        ].collectEntries { ResponseDoc response ->
            collectResponse(response)
        }
    }
}

/** Коллектор OpenAPI parameters/requestBody. */
class ParameterCollector {
    ComponentsCollector componentsCollector

    /**
     * Создает коллектор параметров с доступом к схемам компонентов.
     *
     * @param componentsCollector коллектор components.schemas
     */
    ParameterCollector(ComponentsCollector componentsCollector) {
        this.componentsCollector = componentsCollector
    }

    /**
     * Собирает базовые query-параметры, обязательные для REST-вызова NSMP.
     *
     * @param httpMethod HTTP-метод операции
     * @return список базовых query-параметров
     */
    static List<Map> collectBaseParameters(String httpMethod) {
        Map params = [
            in: OpenApiConstants.QUERY_LOCATION,
            name: OpenApiConstants.PARAM_PARAMS,
            allowEmptyValue: true,
            schema: [
                    type: "string",
                    enum: [OpenApiConstants.DEFAULT_GLOBAL_PARAMS],
                    default: OpenApiConstants.DEFAULT_GLOBAL_PARAMS
            ],
            required: true,
            description: "Глобальные параметры (Не изменять, оставить пустым!)"
        ]

        Map accessKey = [
            in: OpenApiConstants.QUERY_LOCATION,
            name: OpenApiConstants.PARAM_ACCESS_KEY,
            schema: [
                    type: "string"
            ],
            required: true,
            description: "Ключ аутентификации"
        ]

        Map raw = [
            in: OpenApiConstants.QUERY_LOCATION,
            name: OpenApiConstants.PARAM_RAW,
            schema: [
                    type: "boolean",
                    enum: [true],
                    default: true
            ],
            required: true,
            description: "Глобальные параметры (Не изменять!)"
        ]

        httpMethod?.toLowerCase() == 'post' ? [params, accessKey, raw] : [params, accessKey]
    }

    /**
     * Собирает query-параметры из DTO-модели запроса.
     *
     * @param queryModel DTO-класс query-модели
     * @return список OpenAPI query-параметров
     */
    List<Map> collectQueryParameters(Class queryModel) {
        if (!queryModel) return []

        OpenApiReflection.getAllFields(queryModel).collect { Field field -> [
            in: OpenApiConstants.QUERY_LOCATION,
            name: field.name,
            schema: componentsCollector.collectFieldSchema(queryModel, field),
            required: OpenApiReflection.isRequired(field)
        ]}
    }

    /**
     * Собирает requestBody для DTO-модели тела запроса.
     *
     * @param bodyModel DTO-класс тела запроса
     * @return OpenAPI requestBody или null
     */
    static Map collectRequestBody(Class bodyModel) {
        if (!bodyModel) return null

        [
            required: true,
            content: [
                (OpenApiConstants.CONTENT_TYPE_JSON): [
                    schema: [
                        '$ref': OpenApiNames.ref(bodyModel)
                    ]
                ]
            ]
        ]
    }
}

/** Коллектор OpenAPI servers. */
class ServerCollector {

    /**
     * Собирает список OpenAPI servers из базового URL приложения.
     *
     * @param baseUrl базовый URL приложения
     * @return список OpenAPI servers
     */
    static List<Map> collectServers(String baseUrl) {
        if (!baseUrl) return []

        URI uri = new URI(baseUrl)
        String host = uri.host

        if (!host) return []

        [[
            url: "https://${host}/sd/services/rest",
            description: "${detectInstallation(host)} ${detectEnvironment(host)}"
        ]]
    }

    /**
     * Определяет инсталляцию по имени хоста.
     *
     * @param host имя хоста
     * @return имя инсталляции
     */
    private static String detectInstallation(String host) {
        String normalizedHost = host?.toLowerCase() ?: ''

        if (normalizedHost.contains('support')) return 'Internal'

        ''
    }

    /**
     * Определяет окружение по имени хоста.
     *
     * @param host имя хоста
     * @return имя окружения
     */
    private static String detectEnvironment(String host) {
        String normalizedHost = host?.toLowerCase() ?: ''

        if (normalizedHost.contains('.dev.') || normalizedHost.startsWith('dev.')) return 'Dev'
        if (normalizedHost.contains('.staging.') || normalizedHost.startsWith('staging.')) return 'Stage'
        if (normalizedHost.contains('.test.') || normalizedHost.startsWith('test.')) return 'Test'

        ''
    }
}

/** Коллектор OpenAPI paths. */
class PathCollector {
    String moduleName
    MethodDocCollector methodDocCollector
    ParameterCollector parameterCollector
    ResponseCollector responseCollector

    /**
     * Создает коллектор paths для модуля и зависимых коллекторов.
     *
     * @param moduleName имя модуля
     * @param methodDocCollector коллектор docs-классов методов
     * @param parameterCollector коллектор параметров и requestBody
     * @param responseCollector коллектор responses
     */
    PathCollector(
            String moduleName,
            MethodDocCollector methodDocCollector,
            ParameterCollector parameterCollector,
            ResponseCollector responseCollector
    ) {
        this.moduleName = moduleName
        this.methodDocCollector = methodDocCollector
        this.parameterCollector = parameterCollector
        this.responseCollector = responseCollector
    }

    /**
     * Собирает все OpenAPI paths по docs-классам методов.
     *
     * @return карта OpenAPI paths
     */
    Map collectPaths() {
        methodDocCollector.collectDocClasses().collectEntries { Class docClass -> [(collectPathUrl(docClass)): collectPathDoc(docClass)]}
    }

    /**
     * Строит URL path для REST-метода модуля.
     *
     * @param docClass docs-класс метода
     * @return URL path операции
     */
    String collectPathUrl(Class docClass) {
        Object doc = methodDocCollector.collectDoc(docClass)
        String httpMethod = OpenApiDocReader
                .string(doc, "httpMethod", "GET")
                .toLowerCase()
        String methodName = methodDocCollector.collectMethodName(docClass)
        String.format(
                "/exec%sfunc=modules.%s.%s",
                httpMethod == "post" ? "-post?" : "?",
                moduleName,
                methodName
        )
    }

    /**
     * Собирает OpenAPI-описание path item для docs-класса.
     *
     * @param docClass docs-класс метода
     * @return OpenAPI path item с операцией
     */
    Map collectPathDoc(Class docClass) {
        Object doc = methodDocCollector.collectDoc(docClass)
        String httpMethod = OpenApiDocReader.string(doc, "httpMethod", "GET").toLowerCase()
        String methodName = methodDocCollector.collectMethodName(docClass)

        Map operation = [
                tags: OpenApiDocReader.list(doc, "tags", []),
                summary: OpenApiDocReader.string(doc, "summary", ""),
                description: OpenApiDocReader.string(doc, "description", ""),
                operationId: methodName,
                parameters: parameterCollector.collectBaseParameters(httpMethod) + parameterCollector.collectQueryParameters(OpenApiDocReader.clazz(doc, "queryModel")),
                responses: collectResponses(doc)
        ]
        Map requestBody = parameterCollector.collectRequestBody(OpenApiDocReader.clazz(doc, "bodyModel"))
        if (requestBody) operation.requestBody = requestBody

        [ (httpMethod): operation ]
    }

    /**
     * Собирает все ответы операции, включая встроенные ошибки.
     *
     * @param doc docs-объект метода
     * @return карта OpenAPI responses
     */
    Map collectResponses(Object doc) {
        Map responses = [:]

        responses += responseCollector.collectDefaultResponse(OpenApiDocReader.defaultResponse(doc))
        OpenApiDocReader.responses(doc).each { ResponseDoc response -> responses += responseCollector.collectResponse(response)}
        responses += responseCollector.collectInternalErrorResponses()

        responses
    }
}
/** Коллектор корневого OpenAPI-документа. */
@InjectApi
class OpenApiCollector {
    Class controllerClass
    Object moduleDoc
    List<Class> explicitSchemas
    List<Class> routeDocs

    String moduleName

    MethodDocCollector methodDocCollector
    SchemaCollector schemaCollector
    SchemaRegistry schemaRegistry
    ComponentsCollector componentsCollector
    ParameterCollector parameterCollector
    ResponseCollector responseCollector
    PathCollector pathCollector

    /**
     * Создает корневой коллектор OpenAPI-документа для контроллера.
     *
     * @param controllerClass класс контроллера модуля
     * @param moduleDoc docs-объект модуля
     * @param explicitSchemas явно переданный список DTO-схем
     * @param routeDocs явно переданный список docs-классов маршрутов
     */
    OpenApiCollector(Class controllerClass, Object moduleDoc = null, List<Class> explicitSchemas = null, List<Class> routeDocs = []) {
        this.controllerClass = controllerClass
        this.moduleDoc = moduleDoc ?: findOpenApiDocClass(controllerClass)
        this.explicitSchemas = explicitSchemas
        this.routeDocs = routeDocs ?: findOpenApiRouteClasses(controllerClass)

        this.moduleName = OpenApiNames.moduleName(controllerClass)

        this.methodDocCollector = new MethodDocCollector(controllerClass, this.routeDocs)
        this.schemaCollector = new SchemaCollector(controllerClass, methodDocCollector, explicitSchemas)
        this.schemaRegistry = new SchemaRegistry(schemaCollector.collectSchemas())

        this.componentsCollector = new ComponentsCollector(schemaRegistry)
        this.parameterCollector = new ParameterCollector(componentsCollector)
        this.responseCollector = new ResponseCollector()
        this.pathCollector = new PathCollector(moduleName, methodDocCollector, parameterCollector, responseCollector)
    }

    private static List<Class> findOpenApiRouteClasses(Class controllerClass) {
        (controllerClass.classLoader.localClasses as List<Class>)
                .findAll { Class cls ->
                    cls?.name?.startsWith(controllerClass.package.name + '.')
                    && cls.getAnnotation(OpenApiRoute) != null
                    && OpenApiRouteConfig.isAssignableFrom(cls)
                }
    }

    private static Object findOpenApiDocClass(Class controllerClass) {
        (controllerClass.classLoader.localClasses as List<Class>)
                .find { Class cls ->
                    cls?.name?.startsWith(controllerClass.package.name + '.')
                    && cls.getAnnotation(OpenApiDocs) != null
                }?.newInstance()
    }

    /**
     * Собирает корневую структуру OpenAPI-документа.
     *
     * @return карта OpenAPI-документа
     */
    Map collect() {[
        openapi: OpenApiConstants.OPENAPI_VERSION,
        info: collectInfo(),
        servers: collectServers(),
        paths: pathCollector.collectPaths(),
        components: componentsCollector.collectComponents(),
        tags: collectTags()
    ]}

    /**
     * Собирает блок OpenAPI info из описания модуля.
     *
     * @return OpenAPI info
     */
    Map collectInfo() {
        Map info = [
            title: OpenApiDocReader.string(moduleDoc, "name", moduleName),
            version: OpenApiDocReader.string(moduleDoc, "version", OpenApiConstants.DEFAULT_API_VERSION),
            description: collectDescription()
        ]

        String author = OpenApiDocReader.string(moduleDoc, "author", "")
        if (author) info.contact = [name: author]

        info
    }

    /**
     * Собирает HTML-описание модуля с историей изменений.
     *
     * @return HTML-описание модуля
     */
    String collectDescription() {
        String description = OpenApiDocReader.string(moduleDoc, "description", "")
        List<String> changelog = OpenApiDocReader.list(moduleDoc, "changelog", [])
        List<String> parts = []

        if (description) parts += ["<h2>Описание:</h2>", "<p>${description}</p>"]
        if (description && changelog) parts += "<br>"

        if (changelog) {
            parts += ["<h2>История изменений:</h2>", "<ul>"]
            parts += changelog.collect {"<li>${it}</li>"}
            parts += "</ul>"
        }

        parts.join('\n')
    }

    /**
     * Собирает список OpenAPI servers из runtime baseUrl.
     *
     * @return список OpenAPI servers
     */
    List<Map> collectServers() {
        try {
            return ServerCollector.collectServers(api.web.baseUrl)
        } catch (Exception ignored) {
            return []
        }
    }

    /**
     * Собирает уникальные OpenAPI tags из docs-классов методов.
     *
     * @return список OpenAPI tags
     */
    List<Map> collectTags() {
        Set<String> tags = [] as LinkedHashSet

        methodDocCollector.collectDocClasses().each { Class docClass ->
            Object doc = methodDocCollector.collectDoc(docClass)

            OpenApiDocReader.list(doc, "tags", [])
                    .findAll { it }
                    .each { tags.add(it.toString()) }
        }

        tags.collect {[name: it]}
    }

    /**
     * Возвращает OpenAPI-документ в виде pretty JSON.
     *
     * @return JSON-строка OpenAPI-документа
     */
    String build() {
        StringEscapeUtils.unescapeJava(new JsonBuilder(collect()).toPrettyString())
    }

    /**
     * Возвращает OpenAPI-документ в интерактивной HTML страницы.
     *
     * @return HTML OpenAPI-документ
     */
    String preview() {
        String clearJson = build()
                .replace("\\", "")
                .replace("\r\n", "")
                .replace("\r", "")
                .replace("\n", "")
        """
<!doctype html>
    <html>
    <head>
        <title>Swagger UI</title>
        <link rel="stylesheet" href="https://unpkg.com/swagger-ui-dist/swagger-ui.css">
    </head>
    <body>
        <div id="swagger-ui"></div>
        <script src="https://unpkg.com/swagger-ui-dist/swagger-ui-bundle.js"></script>
        <script>
            window.onload = () => {
                SwaggerUIBundle({
                    "spec": $clearJson,
                    "dom_id": "#swagger-ui"
                });
            };
        </script>
    </body>
</html>
        """
    }
}
