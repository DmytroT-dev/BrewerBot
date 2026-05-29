# BrewerBot

Telegram-бот, который автоматически постит в канал образовательные материалы по Java/Spring и AI — на основе твоих GitHub-коммитов и свежих новостей из HackerNews и Reddit. Все посты генерирует Claude (Anthropic).

## Что делает

**GitHub-мониторинг** — каждые 30 минут проверяет твои репозитории. Когда находит новый коммит с `.java` файлами — Claude анализирует diff и публикует образовательный пост: объясняет паттерн или best practice, показывает чистый пример кода.

**Java/Spring новости** — каждые 3 часа собирает топовые материалы из HackerNews и суbredditов (`java`, `SpringBoot`, `Kotlin` и др.), выбирает самые интересные с помощью Claude и публикует краткий пост со ссылкой.

**AI/ML новости** — каждые 4 часа то же самое, но по теме AI: HackerNews + `MachineLearning`, `LocalLLaMA`, `ClaudeAI` и др.

Все посты — на русском, в HTML-формате Telegram.

## Стек

- Java 21, Spring Boot 3.3
- Spring WebFlux (WebClient)
- Anthropic Claude API (`claude-sonnet-4-6`)
- GitHub API (`github-api`)
- HackerNews API, Reddit JSON API
- Lombok
- Состояние хранится в `bot-state.json`

## Быстрый старт

**1. Клонировать репозиторий**
```bash
git clone https://github.com/DmytroT-dev/BrewerBot.git
cd BrewerBot
```

**2. Создать `.env` из шаблона**
```bash
cp .env.example .env
```

**3. Заполнить `.env`**
```env
GITHUB_TOKEN=ghp_...         # github.com/settings/tokens
GITHUB_USERNAME=your-login
GITHUB_REPOS=                # пусто = все репо, или: repo1,repo2

TELEGRAM_BOT_TOKEN=123...    # от @BotFather
TELEGRAM_CHANNEL_ID=@channel # или -1001234567890

ANTHROPIC_API_KEY=sk-ant-... # console.anthropic.com
```

> Бот должен быть добавлен в канал как **администратор** с правом публикации.

**4. Запустить**
```bash
mvn spring-boot:run
```

При первом запуске бот запомнит последние SHA всех репозиториев (без публикации) — посты пойдут только с новыми коммитами.

## Переменные окружения

| Переменная | Обязательная | Описание |
|---|---|---|
| `GITHUB_TOKEN` | да | Personal Access Token (scope: `repo` или `public_repo`) |
| `GITHUB_USERNAME` | да | Логин GitHub-аккаунта |
| `GITHUB_REPOS` | нет | Список репо через запятую. Пусто = все |
| `TELEGRAM_BOT_TOKEN` | да | Токен от @BotFather |
| `TELEGRAM_CHANNEL_ID` | да | `@username` или числовой ID канала |
| `ANTHROPIC_API_KEY` | да | API-ключ Anthropic |
| `ANTHROPIC_MODEL` | нет | Модель (по умолчанию: `claude-sonnet-4-6`) |
| `GITHUB_CHECK_CRON` | нет | Cron расписание (по умолчанию: каждые 30 мин) |
| `JAVA_NEWS_CRON` | нет | Cron расписание (по умолчанию: каждые 3 часа) |
| `AI_NEWS_CRON` | нет | Cron расписание (по умолчанию: каждые 4 часа) |
| `STATE_FILE_PATH` | нет | Путь к файлу состояния (по умолчанию: `./bot-state.json`) |
