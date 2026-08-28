# timeFlows 2.0

Внутрішній вебзастосунок для обліку перепрацювань, погодження бонусів і керування організаційною структурою компанії.

> Застосунок стартує без демонстраційних користувачів, перепрацювань і бонусів. Схема бази даних та початкові довідники створюються автоматично через Liquibase.

<a id="contents"></a>
## Зміст

1. [Summary](#summary)
2. [Функціонал](#features)
3. [Ролі та права](#roles)
4. [Логіка роботи](#logic)
5. [Безпека](#security)
6. [Технології та архітектура](#architecture)
7. [Конфігурація](#configuration)
8. [Варіанти запуску](#running)
9. [Перший запуск](#first-run)
10. [Тестування](#testing)
11. [Експлуатація на EC2](#operations)
12. [Корисні адреси](#urls)

<a id="summary"></a>
## 1. Summary

timeFlows допомагає працівникам фіксувати перепрацювання, а керівникам — переглядати, погоджувати або відхиляти їх. Адміністратор керує структурою компанії, користувачами, зарплатами, бонусами, категоріями бонусів та Excel-звітами.

Основні принципи:

- чистий старт без тестових облікових записів і фінансових даних;
- розмежування доступу між `EMPLOYEE`, `MANAGER` та `ADMIN`;
- окрема видимість даних для керівника його підрозділу;
- міграції бази даних при кожному запуску;
- готовий Docker Compose для застосунку та PostgreSQL;
- H2 in-memory база лише для автоматизованих тестів.

[Повернутися до змісту](#contents)

<a id="features"></a>
## 2. Функціонал

### Перепрацювання

- календар перепрацювань за місяць;
- створення одного запису на працівника на день;
- редагування та видалення доступних записів;
- статуси `PENDING`, `APPROVED`, `REJECTED`;
- погодження або відхилення з коментарем керівника;
- повторне подання відхиленого запису з обов’язковою причиною;
- перегляд за працівником, підрозділом або департаментом;
- фінансове зведення за місяць.

### Бонуси

- створення бонусів для працівників;
- категорії бонусів;
- погодження та відхилення;
- редагування суми, категорії й опису;
- фільтрація за організацією, місяцем і статусом;
- обмеження менеджера межами його підрозділу.

### Користувачі та організація

- реєстрація лише з корпоративною адресою `@vyriy.com`;
- департаменти й підрозділи;
- призначення керівника підрозділу;
- налаштування зарплати;
- деактивація користувача із зазначенням причини;
- перегляд деактивованих користувачів;
- редагування власного профілю та зміна пароля.

### Звітність

- місячні фінансові підсумки;
- динамічні колонки категорій бонусів;
- Excel-експорт для адміністратора.

[Повернутися до змісту](#contents)

<a id="roles"></a>
## 3. Ролі та права

| Можливість | EMPLOYEE | MANAGER | ADMIN |
|---|:---:|:---:|:---:|
| Власні перепрацювання | ✅ | ✅ | ✅ |
| Погодження перепрацювань | — | Свій підрозділ | Уся організація |
| Перегляд користувачів | — | Свій підрозділ | Уся організація |
| Керування бонусами | — | Свій підрозділ | Уся організація |
| Зарплати та деактивація | — | Свій підрозділ | Уся організація |
| Організаційна структура | — | Перегляд | Керування |
| Категорії бонусів | — | Перегляд | Керування |
| Excel-експорт | — | — | ✅ |
| Swagger / OpenAPI | — | — | ✅ |

[Повернутися до змісту](#contents)

<a id="logic"></a>
## 4. Логіка роботи

### Життєвий цикл перепрацювання

1. Працівник створює запис із датою, кількістю годин та описом.
2. Новий запис отримує статус `PENDING`.
3. Керівник відповідного підрозділу або адміністратор приймає рішення.
4. Після погодження статус змінюється на `APPROVED`.
5. При відхиленні потрібен коментар, а статус стає `REJECTED`.
6. Працівник може виправити відхилений запис і повторно подати його з поясненням.

Обмеження годин: до 6 годин у будній день і до 14 годин у суботу або неділю. Перепрацювання адміністратора погоджується автоматично.

### Організаційна модель

```text
Department
└── Division
    ├── Manager
    └── Users
        ├── Overtimes
        └── Bonuses
```

Менеджер може працювати лише з даними свого підрозділу. Адміністратор має організаційний доступ до всіх департаментів і підрозділів.

### Фінансові дані

Зарплата зберігається в профілі користувача. Бонус має отримувача, автора, категорію, суму, опис і статус. Місячне зведення поєднує зарплату, погоджені перепрацювання та бонуси.

[Повернутися до змісту](#contents)

<a id="security"></a>
## 5. Безпека

- Spring Security із role-based authorization;
- JWT зберігається в `HttpOnly` cookie;
- Google Authenticator TOTP є обов’язковим фінальним етапом реєстрації для всіх користувачів;
- enrollment вимагає два коректні коди з різних 30-секундних інтервалів;
- повний JWT видається лише після успішної MFA-перевірки та підтвердження збереження recovery-кодів;
- MFA secret шифрується AES-GCM, а recovery-коди зберігаються лише як BCrypt-хеші;
- recovery-коди можна скопіювати або завантажити як TXT; адміністратор може скинути MFA іншому користувачу, а операція записується в audit log;
- застосунок працює без серверної HTTP-сесії (`STATELESS`);
- паролі хешуються через `PasswordEncoder` і не зберігаються відкритим текстом;
- CSRF-захист для операцій, що змінюють дані;
- перевірка прав виконується як на URL-рівні, так і через `@PreAuthorize`;
- Swagger UI та `/v3/api-docs/**` доступні лише `ADMIN`;
- H2 не входить до production-збірки;
- PostgreSQL у Compose не публікує порт назовні;
- `.env` із секретами виключений із Git.

Для production обов’язково встановіть окремі випадкові `JWT_SECRET` і `MFA_ENCRYPTION_KEY` та унікальний пароль PostgreSQL. Не змінюйте або не втрачайте `MFA_ENCRYPTION_KEY`: без нього вже підключені Authenticator-акаунти потрібно буде налаштовувати повторно. Не використовуйте значення з `.env.example` без заміни.

[Повернутися до змісту](#contents)

<a id="architecture"></a>
## 6. Технології та архітектура

- Java 21;
- Spring Boot 4;
- Spring MVC, Thymeleaf та Spring Security;
- Spring Data JPA;
- PostgreSQL для запуску застосунку, H2 лише в тестах;
- Liquibase для версіонування схеми;
- Gradle Wrapper;
- Apache POI для Excel;
- Springdoc OpenAPI;
- Docker multi-stage build і Docker Compose.

Код розділений на шари `controller → service → repository → database`. DTO відокремлюють API-моделі від JPA-сутностей, а Liquibase є єдиним джерелом структури бази даних.

[Повернутися до змісту](#contents)

<a id="configuration"></a>
## 7. Конфігурація

Створіть локальний файл `.env` на основі шаблону:

```bash
cp .env.example .env
```

| Змінна | Призначення | Приклад |
|---|---|---|
| `POSTGRES_DB` | Назва бази у Compose | `timeflows` |
| `POSTGRES_USER` | Користувач PostgreSQL | `timeflows` |
| `POSTGRES_PASSWORD` | Пароль PostgreSQL | випадковий сильний пароль |
| `JWT_SECRET` | Ключ підпису JWT | випадковий секрет 32+ символи |
| `JWT_EXPIRATION` | Термін дії JWT | `PT8H` |
| `MFA_ENCRYPTION_KEY` | Окремий ключ шифрування TOTP-secret | довгий випадковий секрет |
| `MFA_ISSUER` | Назва у Google Authenticator | `timeFlows` |
| `MFA_ENABLED` | Увімкнення двофакторної автентифікації | `true` |
| `DEMO_DATA_ENABLED` | Створення мінімальних демонстраційних даних | `false` |
| `INITIAL_ADMIN_PASSWORD` | Початковий пароль адміністратора, потрібний лише при `DEMO_DATA_ENABLED=true` | випадковий сильний пароль |
| `APP_PORT` | Порт застосунку на хості | `8080` |

При запуску без Compose Spring використовує `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`, `JWT_EXPIRATION`, `MFA_ENABLED`, `MFA_ENCRYPTION_KEY`, `MFA_ISSUER`, `DEMO_DATA_ENABLED` та `INITIAL_ADMIN_PASSWORD`.

У `application.properties`, `.env.example` і Compose встановлено безпечний fallback `DEMO_DATA_ENABLED=false`. Якщо демонстраційні дані вмикаються свідомо, обов’язково задайте `INITIAL_ADMIN_PASSWORD`.

[Повернутися до змісту](#contents)

<a id="running"></a>
## 8. Варіанти запуску

### Варіант A — автоматизовані тести з H2

Тестове H2-оточення налаштоване окремо в `src/test/resources` і не потрапляє до production JAR.

```powershell
.\gradlew.bat test
```

Linux/macOS:

```bash
./gradlew bootRun
```

Застосунок буде доступний на `http://localhost:8080`.

### Варіант B — Docker Compose з PostgreSQL

Потрібні Docker Engine і Docker Compose plugin.

```bash
cp .env.example .env
# Відредагуйте всі секрети у .env
docker compose up -d --build
docker compose ps
docker compose logs -f app
```

Зупинка без видалення даних:

```bash
docker compose down
```

> Не додавайте `-v`, якщо хочете зберегти базу. Команда `docker compose down -v` видаляє volume PostgreSQL разом із даними.

### Варіант C — локальний Spring Boot із зовнішнім PostgreSQL

PowerShell:

```powershell
$env:DB_URL="jdbc:postgresql://localhost:5432/timeflows"
$env:DB_USERNAME="timeflows"
$env:DB_PASSWORD="your-password"
$env:JWT_SECRET="your-random-secret-with-at-least-32-characters"
$env:MFA_ENCRYPTION_KEY="your-independent-random-mfa-key"
$env:DEMO_DATA_ENABLED="false"
.\gradlew.bat bootRun
```

### Варіант D — EC2

```bash
git clone --branch main https://github.com/JohnGrey17/timeFlows-2.0.git
cd timeFlows-2.0
cp .env.example .env
nano .env
docker compose up -d --build
```

У Security Group відкрийте лише потрібний порт застосунку, наприклад `8080`, або `80/443` для reverse proxy. Порт PostgreSQL `5432` відкривати не потрібно.

[Повернутися до змісту](#contents)

<a id="first-run"></a>
## 9. Перший запуск

1. Відкрийте `/api/register` і зареєструйте першого користувача з адресою `@vyriy.com`.
2. Нова реєстрація отримує роль `EMPLOYEE`.
3. Одноразово надайте першому користувачу роль адміністратора через PostgreSQL:

```bash
docker compose exec postgres sh -c \
  'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -c "INSERT INTO user_roles (user_id, role) SELECT id, '\''ADMIN'\'' FROM users WHERE email = '\''serhii.hainovskyi@vyriy.com'\'' ON CONFLICT DO NOTHING;"'
```

Якщо shell не завантажив `.env`, підставте значення `POSTGRES_USER` і `POSTGRES_DB` безпосередньо в команду. Замініть `serhii.hainovskyi@vyriy.com` на email зареєстрованого адміністратора.

Після цього перезайдіть у застосунок. Адміністратор зможе керувати організацією, призначати менеджерів і працювати з усіма модулями.

Після заповнення реєстраційної форми кожен користувач одразу переходить до Google Authenticator. Потрібно відсканувати QR-код, ввести поточний шестизначний код, дочекатися нового коду через 30 секунд і підтвердити його повторно. Далі користувач копіює або завантажує одноразові recovery-коди та підтверджує їх збереження. Лише після цього видається JWT і відкривається dashboard.

[Повернутися до змісту](#contents)

<a id="testing"></a>
## 10. Тестування

Повний набір тестів:

```powershell
.\gradlew.bat test
```

Перевірка форматування та тестів:

```powershell
.\gradlew.bat spotlessCheck test --no-parallel
```

Звіт тестів: `build/reports/tests/test/index.html`. Звіт покриття JaCoCo: `build/reports/jacoco/test/html/index.html`.

[Повернутися до змісту](#contents)

<a id="operations"></a>
## 11. Експлуатація на EC2

### Оновлення

```bash
git pull origin main
docker compose up -d --build
docker image prune -f
```

### Логи та стан

```bash
docker compose ps
docker compose logs --tail=200 app
docker compose logs --tail=200 postgres
```

### Резервна копія PostgreSQL

```bash
docker compose exec -T postgres sh -c \
  'pg_dump -U "$POSTGRES_USER" "$POSTGRES_DB"' > timeflows-backup.sql
```

Файл `.env` і резервні копії не слід додавати до Git. Для публічного production-доступу рекомендовано поставити Nginx або інший reverse proxy перед застосунком і ввімкнути HTTPS.

[Повернутися до змісту](#contents)

<a id="urls"></a>
## 12. Корисні адреси

| Сторінка | URL | Доступ |
|---|---|---|
| Вхід | `/api/login` | Публічний |
| Реєстрація | `/api/register` | Публічний |
| Перепрацювання | `/api/overtime` | Авторизований користувач |
| Перевірка перепрацювань | `/api/overtime/review` | MANAGER / ADMIN |
| Бонуси | `/api/bonuses` | MANAGER / ADMIN |
| Користувачі | `/api/users` | MANAGER / ADMIN |
| Організація | `/api/organization` | ADMIN |
| Excel-експорт | `/api/admin/export` | ADMIN |
| Swagger UI | `/swagger-ui.html` | ADMIN |
| OpenAPI JSON | `/v3/api-docs` | ADMIN |

[Повернутися до змісту](#contents)

---

Перед production-запуском перевірте `.env`, резервне копіювання, HTTPS і правила AWS Security Group.
