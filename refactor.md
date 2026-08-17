Проаналізуй поточний Java/Spring Boot проєкт і налаштуй єдиний code style.

Потрібно:

створити .editorconfig;
для Java використовувати 4 пробіли, для HTML/CSS/JS/JSON/YAML/XML — 2;
UTF-8, LF, видалення trailing whitespace, newline в кінці файлів;
підключити Spotless до Maven через pom.xml;
для Java використовувати googleJavaFormat;
увімкнути removeUnusedImports;
налаштувати mvn spotless:check для перевірки;
застосувати mvn spotless:apply до існуючого Java-коду;
після форматування виконати mvn clean test або mvn clean package;
створити CODE_STYLE.md з коротким описом стандартів проєкту.

Важливо:

не змінюй бізнес-логіку;
не роби архітектурний рефакторинг;
не перейменовуй класи, методи, API endpoints або DB-сутності;
не змінюй поведінку застосунку;
форматуй тільки те, що необхідно для приведення коду до стандарту.

Перед внесенням змін спочатку проаналізуй pom.xml і структуру проєкту, щоб не додавати те, що вже налаштовано.

Після завершення покажи:

які файли були створені;
які файли були змінені;
які правила форматування застосовані;
результат mvn spotless:check;
результат тестів/збірки.

Поки що не додавай Checkstyle. Спочатку приведемо форматування всього проєкту до єдиного стандарту.

---

# Статус виконання

Гілка: `refactor-code-style-and-tests`

> Проєкт використовує Gradle, а не Maven: у репозиторії немає `pom.xml`, натомість є
> `build.gradle` і Gradle Wrapper. Тому Maven-команди з початкового завдання адаптовано
> до еквівалентних Gradle-команд без додавання другого build system.

## Виконано

- [x] Проаналізовано build-конфігурацію і структуру проєкту.
- [x] Створено `.editorconfig`.
- [x] Налаштовано UTF-8, LF, newline в кінці файлів і видалення trailing whitespace.
- [x] Налаштовано 4 пробіли для Java та 2 для HTML/CSS/JS/JSON/YAML/XML.
- [x] Підключено Spotless `8.9.0` до Gradle.
- [x] Для Java підключено Google Java Format у режимі AOSP (4 пробіли).
- [x] Увімкнено `removeUnusedImports`.
- [x] Налаштовано `spotlessCheck` і застосовано `spotlessApply` до Java-коду.
- [x] Створено `CODE_STYLE.md` з командами та правилами.
- [x] Підключено JaCoCo і HTML/XML-звіти.
- [x] Додано мінімальний поріг загального line coverage 75%.
- [x] Розширено unit/integration-тести сервісів, security, REST-контролерів і DTO.
- [x] Бізнес-логіку, назви класів/методів, API endpoints і DB-сутності не змінено.

## Створені файли

- `.editorconfig`
- `CODE_STYLE.md`
- `src/test/java/example/timeflows/service/DepartmentServiceImplTests.java`
- `src/test/java/example/timeflows/service/DivisionServiceImplTests.java`
- `src/test/java/example/timeflows/service/UserServiceImplTests.java`
- `src/test/java/example/timeflows/security/JwtServiceTests.java`
- `src/test/java/example/timeflows/security/JwtAuthenticationFilterTests.java`
- `src/test/java/example/timeflows/controller/CrudControllerTests.java`
- `src/test/java/example/timeflows/controller/OvertimeControllerTests.java`

## Змінені файли

- `build.gradle` — Spotless, JaCoCo, coverage verification.
- `src/main/java/**/*.java` — лише автоматичне форматування та видалення невикористаних imports.
- `src/test/java/**/*.java` — форматування й розширення тестових сценаріїв.
- `src/main/resources/**/*.{html,css,js,yaml}` — нормалізація пробілів і newline.
- `refactor.md` — цей журнал виконання.

## Результати перевірки

- `spotlessApply`: **успішно**.
- Тести: **82/82 успішно**.
- JaCoCo line coverage: **77.1%** загалом.
- Покриття за ключовими пакетами: config 100%, model 100%, security 100%,
  service 93.2%, DTO 95.5%, controller 62.0%.
- Фінальні `spotlessCheck`, `clean test`, `jacocoTestReport` і
  `jacocoTestCoverageVerification`: **BUILD SUCCESSFUL**.

---

# Рефакторинг бізнес-логіки контролерів

## Завдання

- Перенести бізнес-логіку з контролерів на сервісний рівень.
- Додати окремий мапер для створення entity та response DTO.
- Зберегти наявні endpoints і поведінку застосунку.

## Виконано

- [x] Створено `TimeflowsMapper` як окремий Spring-компонент.
- [x] Прибрано ручне створення `Department`, `Division` і `User` з REST-контролерів.
- [x] Прибрано статичне мапування response DTO з REST-контролерів; перетворення списків
  і окремих об'єктів виконує мапер.
- [x] Мапування профілю користувача винесено з `EmployeePageController`.
- [x] Розрахунок календаря, списків місяців/років, заголовків днів, робочих годин,
  overtime-ставки та підсумкової виплати винесено в `OvertimeViewService`.
- [x] Авторизаційне правило погодження/відхилення overtime перенесено в
  `OvertimeServiceImpl` і виконується в транзакційному сервісному методі.
- [x] Перевірки доступу менеджера до користувачів і бонусів централізовано в
  `ManagementAccessService`.
- [x] Нормалізацію назв і описів під час створення департаментів та підвідділів
  перенесено з `OrganizationPageController` у відповідні сервіси.
- [x] Контролерні тести адаптовано: вони перевіряють HTTP-рівень, мапування та
  делегування сервісам, не дублюючи сервісні правила.

## Створені файли

- `src/main/java/example/timeflows/mapper/TimeflowsMapper.java`
- `src/main/java/example/timeflows/service/ManagementAccessService.java`
- `src/main/java/example/timeflows/service/OvertimeViewService.java`

## Основні змінені файли

- `DepartmentController`, `DivisionController`, `UserController`, `OvertimeController` —
  делегування маперу та сервісам.
- `EmployeePageController`, `OvertimeReviewController`, `BonusController`,
  `UsersPageController`, `OrganizationPageController` — вилучення розрахунків,
  перевірок доступу та ручного створення об'єктів.
- `OvertimeService` / `OvertimeServiceImpl` — сервісне правило доступу до review-рішень.
- `DepartmentService` / `DepartmentServiceImpl` і `DivisionService` /
  `DivisionServiceImpl` — сервісне створення та нормалізація організаційних сутностей.
- `CrudControllerTests`, `OvertimeControllerTests` — адаптація до нових залежностей і
  меж відповідальності.

## Результати перевірки

- `spotlessApply`: **успішно**.
- `compileJava`: **BUILD SUCCESSFUL**.
- `test`: **82/82 успішно**.
- JaCoCo line coverage після рефакторингу: **75.6%**.
- Публічні API endpoints і схема БД не змінювалися.

## Додаткове розділення Service / ServiceImpl

Після повторного архітектурного перегляду виконано такі зміни:

- [x] `OvertimeViewService` перетворено на інтерфейс.
- [x] Реалізацію винесено в `OvertimeViewServiceImpl` та позначено `@Service`.
- [x] `ManagementAccessService` перетворено на інтерфейс.
- [x] Реалізацію винесено в `ManagementAccessServiceImpl` та позначено `@Service`.
- [x] Створено `EmployeePageService` / `EmployeePageServiceImpl`.
- [x] З `EmployeePageController` винесено пошук користувача й overtime, групування за
  датою, визначення місяця, перевірку привілейованої ролі та підготовку атрибутів
  overtime/settings сторінок.
- [x] Створено `OvertimeReviewPageService` / `OvertimeReviewPageServiceImpl`.
- [x] З `OvertimeReviewController` винесено визначення effective department/division,
  фільтрацію користувачів, формування списків департаментів і підвідділів, групування
  overtime, bonus totals, category totals, pending counts і payment rows.
- [x] `EmployeePageController` та `OvertimeReviewController` залишені web-рівнем:
  приймання параметрів, `BindingResult`, наповнення `Model` готовою мапою атрибутів,
  redirects і виклик сервісних команд.

### Додатково створені файли

- `src/main/java/example/timeflows/service/OvertimeViewServiceImpl.java`
- `src/main/java/example/timeflows/service/ManagementAccessServiceImpl.java`
- `src/main/java/example/timeflows/service/EmployeePageService.java`
- `src/main/java/example/timeflows/service/EmployeePageServiceImpl.java`
- `src/main/java/example/timeflows/service/OvertimeReviewPageService.java`
- `src/main/java/example/timeflows/service/OvertimeReviewPageServiceImpl.java`

### Повторна перевірка

- `spotlessApply` і `spotlessCheck`: **успішно**.
- Тести: **82/82 успішно**.
- `jacocoTestCoverageVerification`: **успішно**.
- Фінальний результат: **BUILD SUCCESSFUL**.

## Уточнення формату Excel за UI-зразками

- [x] Для кожного місяця створюються дві вкладки: `{місяць} Календар` і
  `{місяць} Фінанси`.
- [x] Календар побудовано як матрицю: працівники в рядках, усі дні місяця в колонках.
- [x] Фінансовий підсумок містить окрему колонку для кожної бонусної категорії.
- [x] До календаря потрапляють виключно overtime зі статусом `APPROVED`.
- [x] До фінансового підсумку потрапляють виключно бонуси зі статусом `APPROVED`.
- [x] `PENDING` та `REJECTED` overtime і бонуси повністю виключено з Excel-файлу.
- [x] Окремий workbook-тест підтверджує наявність `APPROVED` записів і відсутність
  `PENDING`/`REJECTED` записів у календарних клітинках та фінансових сумах.

---

# Admin-only експорт Excel

Гілка: `admin-excel-export`

## Реалізовано

- [x] Додано окремий пункт меню `Експорт Excel`, видимий лише користувачам з роллю
  `ADMIN`.
- [x] Додано сторінку `/api/admin/export` з фільтрами департаменту, опціонального
  підвідділу та початкового/кінцевого місяця.
- [x] Доступ до сторінки й download endpoint захищено через SecurityFilterChain та
  `@PreAuthorize("hasRole('ADMIN')")`.
- [x] Додано формування `.xlsx` через Apache POI.
- [x] Для кожного місяця вибраного періоду формуються окремі вкладки календаря та
  фінансового підсумку.
- [x] Якщо підвідділ не вибрано, у файл входять усі підвідділи вибраного департаменту.
- [x] Перевіряється належність підвідділу до вибраного департаменту.
- [x] Період валідовується та обмежений 24 місяцями.
- [x] Назва файлу має формат `вивантаження {департамент} {підвідділ} {період}.xlsx`;
  якщо підвідділ не вибрано, його назва пропускається.

## Створені файли

- `src/main/java/example/timeflows/controller/ExcelExportController.java`
- `src/main/java/example/timeflows/service/ExcelExportService.java`
- `src/main/java/example/timeflows/service/ExcelExportServiceImpl.java`
- `src/main/resources/templates/admin/excel-export.html`
- `src/test/java/example/timeflows/service/ExcelExportServiceImplTests.java`

## Перевірка

- Apache POI dependency: `org.apache.poi:poi-ooxml:5.4.1`.
- Тести: **86/86 успішно**.
- Перевірено створення окремих вкладок для декількох місяців.
- Перевірено заборону сторінки для `EMPLOYEE` і `MANAGER` та доступ для `ADMIN`.
- `spotlessApply`: **успішно**.
- `jacocoTestCoverageVerification`: **успішно**.
- Фінальний результат: **BUILD SUCCESSFUL**.
