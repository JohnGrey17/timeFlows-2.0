# TimeFlows workflow

## 2026-08-03

### Initial scope
- Create a Spring Boot application named `timeFlows`.
- Add registration and authorization through Spring Security with JWT.
- Use H2 as a test database.
- Use Liquibase for schema creation and seed data.
- Use Thymeleaf for the first UI screens:
  - login page;
  - registration page;
  - authenticated dashboard page.
- Model the organization structure:
  - `Department` has many `Division` records;
  - `Division` belongs to one `Department`;
  - `User` belongs to one `Division`;
  - `User` has many roles through `user_roles`.
- Dashboard layout after login:
  - left-side navigation;
  - navigation items: Settings, Add overtime, Overtime history.

### Implementation plan
- Update Gradle dependencies for JPA, Liquibase, H2, validation, and JWT.
- Configure H2, Liquibase, JPA, and JWT properties.
- Add database changelog with user, role, department, and division tables.
- Add models, repositories, services, Spring Security configuration, JWT service, and JWT authentication filter.
- Add MVC controllers, REST CRUD controllers, Swagger annotations, exception handler, and Thymeleaf templates.
- Verify with Gradle tests.

### Implemented
- Updated `build.gradle` with Spring Data JPA, Liquibase, H2, validation, and JJWT dependencies.
- Configured H2 in-memory database, H2 console, Liquibase changelog, JPA validation mode, and JWT properties in `application.properties`.
- Added Liquibase master changelog in `src/main/resources/db/changelog/db.changelog-master.yaml`.
- Added separate Liquibase files under `src/main/resources/db/changelog/changes/`.
- Seeded only department/division data:
  - department: `Масштабування`;
  - divisions: `IT`, `Архітектори`, `Аналітики`.
- Added domain entities:
  - `User`;
  - `Department`;
  - `Division`.
- Added repositories for users, departments, and divisions.
- Added registration and user details service logic in `UserService` / `UserServiceImpl`.
- Added role support with multiple roles per user.
- Added JWT support:
  - token generation and validation in `JwtService`;
  - request authentication in `JwtAuthenticationFilter`;
  - Spring Security rules in `SecurityConfig`;
  - password encoder in `PasswordConfig`.
- Added MVC controllers:
  - `AuthController` for login, registration, and logout;
  - `DashboardController` for the authenticated main page.
- Added REST CRUD controllers under `/api`:
  - `/api/users`;
  - `/api/departments`;
  - `/api/divisions`.
- Added Swagger/OpenAPI dependency and annotations.
- Added `OpenApiConfig` with Bearer JWT security scheme.
- Added model-specific exceptions and `GlobalExceptionHandler`.
- Added Thymeleaf pages:
  - `templates/auth/login.html`;
  - `templates/auth/register.html`;
  - `templates/dashboard.html`.
- Added base styling in `static/css/app.css`.
- Verified with `./gradlew.bat test`.
- Started the application with `./gradlew.bat bootRun`.
- Verified `/api/login` returns HTTP 200.
- Verified `/v3/api-docs` returns HTTP 200.
- Verified registration, login with department selection, JWT cookie authentication, `/api/dashboard` rendering, and `/api/departments` CRUD read endpoint.
- Stopped the temporary `bootRun` process so port `8080` remains available for local IDE runs.

### Runtime notes
- Local application URL: `http://localhost:8080`
- Login URL: `http://localhost:8080/api/login`
- Registration URL: `http://localhost:8080/api/register`
- Dashboard URL: `http://localhost:8080/api/dashboard`
- Swagger UI URL: `http://localhost:8080/swagger-ui.html`
- H2 console URL: `http://localhost:8080/h2-console`
- H2 JDBC URL: `jdbc:h2:mem:timeflows`
- H2 username: `sa`
- H2 password: empty

## 2026-08-03 Employee overtime iteration

### Requested changes
- Remove department selection from login.
- Registration must use email only, without separate username input.
- Allow only emails from the `@vyriy.com` domain.
- Add Employee role interface:
  - left navigation with `Over time` and `Налаштування`;
  - settings page for first name, last name, and password change;
  - overtime calendar page that loads the current month by default;
  - month/year dropdown navigation;
  - calendar weeks start on Monday;
  - each calendar day can create/view/edit/delete one overtime entry through a popup.
- Add overtime model, repository, service, and controller.
- Overtime fields:
  - `hours` as required `Double`;
  - required `description`;
  - `workDate` from the selected calendar cell;
  - approval status for manager decision;
  - optional manager rejection/decision comment.
- Business rules:
  - one overtime entry per user per day;
  - weekdays allow up to 6 hours;
  - weekends allow up to 14 hours;
  - approved/rejected overtime cannot be edited or deleted by employee.

### Implemented
- Updated login DTO and form to use only `email` and `password`.
- Updated registration DTO and form to use only `email`, `password`, and `divisionId`.
- Added `@vyriy.com` email validation in DTOs and service layer.
- Added `firstName` and `lastName` fields to `User`.
- Added Liquibase changes:
  - `006-add-user-profile-fields.yaml`;
  - `007-create-overtimes-table.yaml`.
- Added overtime domain:
  - `Overtime`;
  - `OvertimeStatus`.
- Added overtime layers:
  - `OvertimeRepository`;
  - `OvertimeService`;
  - `OvertimeServiceImpl`;
  - `OvertimeController`.
- Added `OvertimeException` and handler support in `GlobalExceptionHandler`.
- Added Employee pages:
  - `templates/employee/overtime.html`;
  - `templates/employee/settings.html`.
- Added popup/calendar JavaScript in `static/js/overtime.js`.
- Updated dashboard route to redirect to `/api/overtime`.
- Updated CSS for calendar, modal, and settings panels.

### Verified
- Ran `./gradlew.bat test`: build successful.

## 2026-08-03 Settings and logout fixes

### Requested changes
- Fix profile update when changing first name and last name.
- Fix password change returning HTTP 403.
- Remove JWT token from browser on logout.

### Implemented
- Made profile update persist inside a write transaction in `UserServiceImpl.updateProfile`.
- Added `currentUser` to the settings model when the settings page is rendered after validation or service errors.
- Cleared Spring Security context on logout before expiring the `TIMEFLOWS_JWT` cookie.

### Verified
- Ran `./gradlew.bat test`: build successful.
- Runtime checked registration, login, profile update, settings reload, password change, and login with the new password.
- Runtime checked logout response sets `TIMEFLOWS_JWT=; Max-Age=0; Expires=Thu, 01 Jan 1970 00:00:00 GMT`.

## 2026-08-03 Registration and overtime modal cleanup

### Requested changes
- During registration, first name and last name must be required.
- First name and last name should be shown under email for users.
- Remove the role title from the employee screen.
- Show `Чому потрібно погодити повторно` only when overtime was rejected by the division manager.
- Do not change anything else in this iteration.

### Implemented
- Added required `firstName` and `lastName` fields to `RegisterRequest`.
- Updated registration form with first name and last name inputs.
- Saved first name and last name during registration.
- Updated user listings to show email first and full name below it.
- Removed the `Employee` eyebrow from employee overtime/settings screens.
- Added global `[hidden] { display: none !important; }` CSS rule so JS-hidden modal fields are actually hidden.

### Verified
- Ran `./gradlew.bat test`: build successful.
- Runtime checked:
  - `/api/login` returns HTTP 200 and has no department select;
  - `/api/register` returns HTTP 200 and has no username field;
  - registration works with `@vyriy.com`;
  - login works with email/password;
  - `/api/overtime` returns HTTP 200;
  - `/api/settings` returns HTTP 200;
  - `POST /api/overtimes` creates overtime with HTTP 201;
  - `GET /api/overtimes?year=2026&month=8` returns created overtime.

## 2026-08-03 Admin and manager iteration

### Requested changes
- Show first name and last name in the top-right user chip instead of email when profile fields exist.
- Add overtime status legend:
  - green: approved by manager;
  - yellow: pending manager review;
  - red: rejected by manager.
- Let employee resend rejected overtime for approval with an extra reason.
- Add system admin with the same screens as employee plus `Користувачі`.
- Admin user management:
  - view all active users;
  - filter users by division;
  - assign one division manager;
  - one manager can manage only one division;
  - soft-delete users with deactivation reason;
  - view deactivated users separately.
- Add salary field for all users.
- Admin can view and update salary for all users.
- Manager can view and update salary for users in own division only.
- Admin can create overtime without manager approval; admin overtime is automatically approved.
- Add overtime review screen for admin/manager:
  - filter by month/year;
  - switch between `Співробітник` and `Відділ`;
  - manager sees only own division;
  - admin can review department/division data.

### Implemented
- Added user fields:
  - `salary`;
  - `active`;
  - `deactivationReason`.
- Added `Division.manager`.
- Added `Overtime.resubmissionReason`.
- Added Liquibase changes:
  - `008-add-user-admin-fields.yaml`;
  - `009-add-division-manager.yaml`;
  - `010-add-overtime-resubmission-reason.yaml`.
- Added startup admin initializer:
  - email: `admin@vyriy.com`;
  - password: `admin123`;
  - roles: `ADMIN`, `EMPLOYEE`.
- Disabled inactive users at Spring Security login.
- Added service methods for:
  - active/deactivated users;
  - users by division;
  - soft deactivation;
  - salary update;
  - division manager assignment.
- Added overtime resubmission endpoint:
  - `POST /api/overtimes/{id}/resubmit`.
- Added admin/manager pages:
  - `/api/users`;
  - `/api/users/deactivated`;
  - `/api/overtime/review`.
- Moved user REST API to `/api/users/rest` so `/api/users` can be the UI page.
- Added manager/admin checks on overtime review and REST approve/reject.
- Updated Employee sidebar for admin/manager with:
  - `Користувачі`;
  - `Перевірка overtime`.
- Added CSS for status legend, user tables, filters, and overtime division matrix.
- Added clickable overtime cells in manager/admin review for quick detail popup.

### Verified
- Ran `./gradlew.bat test`: build successful.
- Runtime checked admin login with `admin@vyriy.com` / `admin123`.
- Runtime checked:
  - `/api/overtime` returns HTTP 200 for admin;
  - `/api/users` returns HTTP 200 for admin;
  - `/api/overtime/review` returns HTTP 200 for admin;
  - top-right chip shows `TimeFlows Admin`;
  - admin overtime is created with status `APPROVED`.
- Re-ran `./gradlew.bat test` after clickable review-cell update: build successful.

## 2026-08-03 UI and validation cleanup

### Requested changes
- All validation and exception messages should be in Ukrainian.
- Replace `Email must use @vyriy.com domain` with `Будь ласка вкажіть корпоративний email`.
- Month dropdowns should show Ukrainian month names.
- Pending employee overtime should be highlighted yellow.
- The `Чому потрібно погодити повторно` field should be visible only after division manager rejection.

### Implemented
- Translated DTO validation messages to Ukrainian.
- Translated service and global exception handler messages to Ukrainian.
- Set the corporate email validation message to `Будь ласка вкажіть корпоративний email`.
- Added `MonthOption` DTO and Ukrainian month labels.
- Updated employee overtime and manager review month dropdowns to display Ukrainian names.
- Replaced the employee overtime template and JS with clean Ukrainian text.
- Strengthened pending overtime yellow highlight.
- Confirmed resubmission reason field is shown only for `REJECTED` overtime in `overtime.js`.

### Verified
- Ran `./gradlew.bat test`: build successful.

## 2026-08-13 Security, authorization and tests

### Requested changes
- Add tests for security and overtime business rules.
- Verify and enable CSRF protection for JWT cookie authentication.
- Add role-based `@PreAuthorize` restrictions to REST endpoints.
- Require a non-blank resubmission reason when rejected overtime is sent again.
- Prevent approving or rejecting overtime that already has a final decision.

### Implemented
- Enabled Spring Security SPA CSRF support with a cookie-based token repository.
- Added CSRF meta tags to the employee overtime page.
- Updated overtime `fetch` requests to send the CSRF token in the request header.
- Added role restrictions:
  - user REST CRUD is available only to `ADMIN`;
  - department/division reads are available to `ADMIN` and `MANAGER`;
  - department/division mutations are available only to `ADMIN`.
- Added grouped `@NotBlank` validation for `resubmissionReason`, so the field is required only by the resubmission endpoint.
- Added a defensive service-layer resubmission reason check.
- Added a pending-status guard to `approve()` and `reject()`.
- Added unit tests for overtime status transitions and the resubmission reason rule.
- Added DTO validation tests for regular creation and resubmission validation groups.
- Added MockMvc integration tests for CSRF enforcement and role-based endpoint access.

### Verified
- Ran `./gradlew.bat clean test`: build successful.
- All 14 tests passed.

## 2026-08-13 Runtime forms, navigation and demo users

### Requested changes
- Fix HTTP 403 when updating profile, salary, or assigning a division manager.
- Keep the overtime review navigation link visible from the users pages.
- Show the logout action on every application page.
- Improve overtime mutation error messages.
- Create three employees and two division managers and document all credentials.
- Make executed HTTP actions visible in the IDE Run console.

### Implemented
- Configured cookie CSRF with a plain request attribute handler suitable for both Thymeleaf forms and JavaScript headers.
- Prevented CSRF token rotation on every stateless JWT authentication request.
- Updated overtime JavaScript to send the raw `XSRF-TOKEN` cookie as `X-XSRF-TOKEN`.
- Improved overtime errors to show validation details, permission/session guidance for HTTP 403, and the HTTP status fallback.
- Added `Перевірка overtime` to active and deactivated users navigation.
- Made the desktop sidebar viewport-sticky so the logout button remains visible.
- Added INFO-level HTTP request completion logging with method, URI, status, and authenticated user.
- Extended the startup initializer with idempotent local demo data:
  - three employees across IT and Архітектори;
  - one manager for IT;
  - one manager for Архітектори;
  - the existing super admin.
- Added all local credentials to `user.md`.
- Added an integration test that verifies all demo accounts and both manager assignments.

### Runtime notes
- The configured database is the in-memory H2 database `jdbc:h2:mem:timeflows`.
- Data is visible only while the application process is running and is recreated on restart.
- After changing application source code, the IntelliJ run process must be stopped and started again; an already running JVM does not automatically use newly compiled classes.

### Verified
- Profile update returned HTTP 302.
- Salary update returned HTTP 302.
- Division manager assignment returned HTTP 302.
- JavaScript-style CSRF header validation reached the overtime controller.
- Users page contains the overtime review link and all six seeded accounts.

## 2026-08-13 Organization management and overtime review UI

### Requested changes
- Allow viewing all users of a department when no division is selected.
- Add an admin-only `Керування відділами` menu for creating departments and divisions.
- Let registration users select a department first and then one of its divisions.
- Add dependent department, division, and user filters to user and overtime review pages.
- Replace browser alerts with an informative overtime details popup.
- Draw a thin black grid in the all-users overtime matrix.
- Make the left navigation collapsible and resize the main content automatically.

### Implemented
- Added department-level user and overtime repository/service queries.
- Updated the users page filter hierarchy:
  - all users when no department is selected;
  - all department users when no division is selected;
  - division users when a division is selected.
- Added `/api/organization`, restricted to `ADMIN`.
- Added forms for creating departments and divisions under a selected department.
- Added the organization menu link to all admin navigation variants.
- Updated registration with dependent department and division selectors.
- Updated overtime review with department, division, and context-dependent user selection.
- Added an overtime details modal with user, date, hours, description, status, and manager comment.
- Added thin dark borders to every overtime matrix cell.
- Added a persistent sidebar toggle; collapsed state is stored in browser local storage.
- Added responsive organization management styling.

### Verified
- Added authorization and rendering tests for organization management.
- Added tests for registration organization selectors.
- Added tests for department-level users and overtime review rendering.
- Ran `./gradlew.bat clean test`: build successful.

## 2026-08-13 Unified warm UI style

### Branch
- Created `unified-user-ui-style` from `overtime-checker-page-solve-a-problem`.

### Implemented
- Applied the warm white/orange TimeFlows visual style from the provided reference to all authenticated pages.
- Unified sidebar spacing, rounded navigation items, cards, inputs, buttons, shadows, calendar, tables, modals, and responsive behavior.
- Added compact navigation icons.
- Standardized role-aware navigation order.
- Admin navigation order is:
  - `Мої перепрацювання`;
  - `Перевірка перепрацювань`;
  - `Користувачі`;
  - `Керування Відділами`;
  - `Налаштування`.
- Kept exactly the existing three overtime approval indicators:
  - approved — green;
  - pending — yellow;
  - rejected — red.

## 2026-08-13 Bonuses and organization fixes

### Branch
- Created `bonuses-and-organization-fixes` from `unified-user-ui-style`.

### Implemented
- Added Ukrainian weekday abbreviations (`пн`–`нд`) below every date in the all-users overtime matrix.
- Fixed department/division creation feedback and added case-insensitive duplicate-name validation.
- Preserved department and division filters after salary, manager assignment, and deactivation actions.
- Renamed the manager action to `Зробити керівником`.
- Highlighted the current division manager; non-current candidates use a gray button.
- Added a `?` tooltip explaining manager permissions and explicitly marking vacations as future functionality.
- Allowed managers to deactivate users only inside their own division.
- Added the `Bonus` entity with amount, description, creation date, creator, employee, status, and admin comment.
- Added bonus statuses `PENDING`, `APPROVED`, and `REJECTED`.
- Added month-filtered bonus storage with multiple bonuses per employee per month.
- Added a role-protected `Бонуси` menu for admins and managers.
- Managers can create and modify/delete only pending bonuses for their own division.
- Admins can create, edit, approve, reject, and delete bonuses.
- Added a bonus total column after the last day in the all-users overtime matrix.
- Added a separate monthly bonus list to the individual employee overtime view.

## 2026-08-13 Bonus filters, demo data, JWT boundary and assets

### Implemented
- Added admin bonus filters for department, division, status, month, and year.
- Kept manager bonus data and employee choices strictly scoped to the manager's own division.
- Added idempotent current-month demo records:
  - three pending overtime requests across IT and Архітектори;
  - three pending bonus requests across IT and Архітектори.
- Restricted anonymous access to login, registration, and required static assets.
- H2 Console, Swagger/OpenAPI, and every business page now require JWT authentication.
- Read and followed `TimeFlows_assets_README.md`.
- Copied the supplied brand assets into static resources.
- Replaced the text sidebar logo with `logo_timeFlows.png`.
- Replaced placeholder sidebar glyphs with the supplied settings, users, overtime-check, departments, and brand icons.
- Added the gradient favicon to the bonus page.
# Останні UI-виправлення бонусів

- У фільтрі бонусів числові значення місяців замінено українськими назвами.
- Для таблиці бонусів додано чітку тонку сітку між усіма комірками.
- Іконки бокового меню збільшено з 20 до 24 px.
- Для пункту «Бонуси» бренд-іконку замінено на помаранчеву зірку.
- У перегляді overtime керівник і співробітники показуються з ПІБ та email.
- У модальне вікно overtime додано погодження з коментарем і відхилення з обов'язковою причиною; серверна перевірка залишає адміну доступ до всіх відділів, а керівнику — лише до свого.
- У загальну таблицю додано базову ставку, погоджені години overtime, суму overtime та загальну місячну виплату. Overtime розраховується як подвійна погодинна ставка, де робочий день дорівнює 8 годинам, а робочими вважаються понеділок–п'ятниця.
- Сума бонусів стала інтерактивною: відкриває місячний список бонусів із створенням, редагуванням, видаленням та рішеннями відповідно до ролі.
- Керівникам дозволено погоджувати й відхиляти бонуси співробітників свого підвідділу; остаточні бонуси редагує або видаляє лише адміністратор.
- Звичайний користувач редагує та видаляє власний PENDING overtime; адміністратор і керівник додатково можуть редагувати та видаляти власні завершені записи у поточному місяці.
- Бонуси переведено на керовані категорії з опціональним додатковим описом; додано стартовий набір категорій і Liquibase-міграцію.
- Розділ «Бонуси» тепер показує історію, а адміністратору додатково дозволяє створювати, перейменовувати та видаляти невикористані категорії.
- BonusService розділено на інтерфейс BonusService та реалізацію BonusServiceImpl.
- У модальному вікні бонусів додано кольори статусів, візуальну перегородку, вибір категорії та редагування базової ставки з рольовою перевіркою.
- У деталях повторно поданого overtime показується причина повторного погодження.
- Розділ категорій візуально відокремлено від операцій з бонусами та додано контекстні підказки.
- Адміністратор працює з бонусами всіх відділів, менеджер — лише свого підвідділу; обидві ролі можуть редагувати, видаляти й приймати рішення в межах доступу.
- Редагування базової ставки перенесено з бонусного вікна безпосередньо в клітинку ставки та переведено на звичайну CSRF-захищену форму.
- Форму створення бонусу зроблено читабельнішою: категорія, сума й необов'язковий додатковий опис мають окремі зрозумілі поля.
- Для створення департаментів і підвідділів додано явний CSRF-токен та повернення зрозумілої помилки на сторінку.
- Іконку «Мої перепрацювання» замінено на секундомір, щоб вона не повторювала бренд-іконку; логотип sidebar використовує logo_timeFlows.png у збільшеному розмірі.
- Підказки у заголовках категорій і роботи з бонусами вирівняно в одному стилі; ролі ADMIN, MANAGER та EMPLOYEE отримали окремі кольорові бейджі.
- Редагування категорії, суми й додаткового опису бонусу перенесено у відповідні колонки; у «Діях» залишено лише погодження та відхилення.
- Із «Перевірки перепрацювань» прибрано зайвий перемикач конкретного співробітника — сторінка працює в єдиному табличному режимі.
- Роль ADMIN підсвічується жовтим; кнопки категорій вирівняно за розміром, а дії погодження бонусу розташовано вертикально.
- До сторінок користувачів, створення департаменту, створення підвідділу та перевірки перепрацювань додано зрозумілі контекстні підказки.
- Для кожного працівника показується пульсуючий лічильник бонусів PENDING; після створення бонусу повторно відкривається те саме модальне вікно.
- Фінансові колонки перейменовано на «Години перепрацювань», «Сума перепрацювань» і «Сума до сплати»; на сторінці власних записів слово overtime замінено на «перепрацювання».
- Кнопки в таблиці бонусів зменшено приблизно вдвічі; всі знаки питання мають круглу рамку й м'яку пульсацію.
- Місячну матрицю ущільнено до 25–28 px на день, а для ноутбуків, портретних і повернутих екранів додано окремі адаптивні правила.
- Фінальні адаптивні правила перенесено в кінець стилів: на вузьких desktop-екранах меню автоматично стискається, але логотип і кнопка виходу залишаються доступними; для портретних екранів таблиця отримує безпечне горизонтальне прокручування.
- У бонусах підказки перенесено безпосередньо до відповідних назв, а погодження й відхилення замінено двома компактними кнопками, які відкривають модальне вікно для коментаря або обов'язкової причини.
- Кнопки керування категоріями вирівняно за розміром, довгі повідомлення про помилки зроблено переносними, а в матриці перепрацювань email прибрано й ПІБ показується у два рядки.
- До «Моїх перепрацювань» додано контекстну підказку; підказки керування департаментом і підвідділом розташовуються поруч із назвами.
- Виправлено генерацію ID після початкового наповнення бази: лічильники departments і divisions синхронізуються, тому створення нових департаментів і підвідділів більше не конфліктує з тестовими записами.
- У «Перевірці перепрацювань» додано перемикач між календарною матрицею та фінансовим підсумком зі спільними фільтрами й чинними рольовими обмеженнями.
- Фінансовий підсумок показує ПІБ у два рядки, динамічні колонки всіх категорій бонусів, базову ставку, суму погоджених перепрацювань і суму до сплати; натискання на бонусну або overtime-суму відкриває деталізацію складових.
- Календарну матрицю перевірки перепрацювань розширено до читабельних розмірів: денні, бонусні та фінансові колонки більше не накладаються, а на вужчих екранах використовується горизонтальне прокручування.
- На сторінці деактивованих користувачів додано доступну підказку про призначення сторінки й обмеження деактивованих облікових записів.
- У фінансовому підсумку всі динамічні бонусні категорії об'єднано під спільним виразним заголовком «Категорії бонусів»; статусну легенду в цьому режимі приховано.
- У користувачах додано сортування-групування за відділом в алфавітному порядку або за роллю у порядку ADMIN, MANAGER, EMPLOYEE.
- Легенду календаря уточнено до «Погоджено керівником», «На розгляді у керівника» та «Не погоджено керівником»; усі таблиці отримали округлені кути.
- Пульсація знаків питання призупиняється при наведенні або фокусі, а ширину підказок обмежено межами екрана.
- Фінансовий підсумок для менеджера залишено лише для читання в межах його підвідділу; адміністратор отримав у ньому редагування базової ставки та створення, редагування і видалення бонусів.
- Прибрано постійне підсвічування чисел після відкриття деталізації; повідомлення всіх контекстних підказок відкриваються праворуч від знака питання.
- Перелік департаментів і підвідділів отримав заголовок «Схема Компанії» та окрему зрозумілу підказку.
