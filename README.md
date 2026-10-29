# WebView Preview — Android App

Приложение для просмотра локальных `index.html` на телефоне.

## Функции
- Открытие HTML-файла или папки с проектом
- Поддержка JS, CSS, DOM Storage
- Drag-and-Drop файлов в WebView
- История последних файлов (10 штук)
- Переключение Desktop / Mobile режима
- Тёмная / светлая тема
- Обновление страницы (Reload)

## Как собрать APK
1. Открой в Android Studio папку проекта
2. File → Sync Project with Gradle Files
3. Build → Build Bundle(s)/APK(s) → Build APK(s)
4. APK появится в `app/release/app-release.apk`

## Как использовать
- Нажми «Файл» — выбери `index.html`
- Нажми «Папку» — выбери папку проекта (css/js/img подгрузятся)
- Перетаскивай файлы прямо в экран
- «Последние» — быстро открыть предыдущий файл
- «Десктоп» — режим широкого экрана
- «Тема» — тёмная / светлая

## Релиз APK
После сборки загрузи `app/release/app-release.apk` в GitHub Releases.

## GitHub
Создай репозиторий `webview-preview`, затем:
```bash
git remote set-url origin https://github.com/ТВОЙ_НИК/webview-preview.git
git push -u origin master
```
Затем зали APK в Releases.
