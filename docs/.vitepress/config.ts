import { defineConfig } from 'vitepress'

export default defineConfig({
  title: 'OpenApiCollector',
  description: 'OpenAPI 3.0.4 documentation generator for NSMP Groovy modules',
  base: '/nsmp-openapi/',
  cleanUrls: true,
  lastUpdated: true,
  themeConfig: {
    logo: '/logo.png',
    nav: [
      { text: 'Документация', link: '/' },
      { text: 'GitHub', link: 'https://github.com/ErilovNikita/nsmp-openapi' }
    ],
    sidebar: [
      {
        text: 'Начало',
        collapsed: false,
        items: [
          { text: 'Обзор', link: '/' },
          { text: 'Быстрый старт', link: '/quick-start' },
          { text: 'Полный пример', link: '/full-example' }
        ]
      },
      {
        text: 'Описание API',
        collapsed: false,
        items: [
          { text: 'Основной модуль контроллер', link: '/module-doc' },
          { text: 'DTO-модели', link: '/dto-models' },
          { text: 'REST-ручки', link: '/routes' },
          { text: 'Ответы и requestBody', link: '/responses' }
        ]
      },
      {
        text: 'Генерация и публикация',
        collapsed: false,
        items: [
          { text: 'OpenAPI JSON', link: '/generation' },
          { text: 'Правила коллектора', link: '/collector-rules' }
        ]
      },
      {
        text: 'Справка',
        collapsed: false,
        items: [
          { text: 'Частые ошибки', link: '/troubleshooting' }
        ]
      }
    ],
    socialLinks: [
      { icon: 'github', link: 'https://github.com/ErilovNikita/nsmp-openapi' },
    ],
    search: {
      provider: 'local'
    },
    outline: {
      level: [2, 3]
    },
    docFooter: {
      prev: 'Предыдущая',
      next: 'Следующая',
    },
    lastUpdated: {
      text: 'Обновлено',
      formatOptions: {
        dateStyle: 'short',
        timeStyle: 'short',
      },
    },
  }
})
