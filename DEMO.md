# timeFlows MFA presentation demo

Ця гілка призначена для показової презентації. Вона автоматично створює організацію,
користувачів, керівників, overtime та bonuses. Google Authenticator залишається
справжнім і обов'язковим: при першому вході кожен demo-користувач підключає власний
TOTP через QR-код.

## Запуск

```powershell
git switch presentation-demo-mfa
.\gradlew.bat bootRun
```

Відкрийте `http://localhost:8080/api/login`.

## Demo credentials

| Роль | Email | Пароль |
|---|---|---|
| ADMIN | `admin@vyriy.com` | `TimeFlows-Demo-Admin-2026!` |
| MANAGER (IT) | `it.manager@vyriy.com` | `TimeFlows-Demo-Manager-2026!` |
| MANAGER (Архітектори) | `architect.manager@vyriy.com` | `TimeFlows-Demo-Manager-2026!` |
| EMPLOYEE (IT) | `andrii.employee@vyriy.com` | `TimeFlows-Demo-Employee-2026!` |
| EMPLOYEE (IT) | `maria.employee@vyriy.com` | `TimeFlows-Demo-Employee-2026!` |
| EMPLOYEE (Архітектори) | `petro.employee@vyriy.com` | `TimeFlows-Demo-Employee-2026!` |

## Рекомендований сценарій показу

1. Увійдіть як `admin@vyriy.com`.
2. Відскануйте QR-код у Google Authenticator.
3. Введіть перший TOTP, дочекайтеся нового коду й введіть другий.
4. Покажіть copy/TXT recovery-кодів і підтвердіть checkbox.
5. Відкрийте користувачів: там уже є працівники, ролі, зарплати та керівники.
6. Відкрийте перевірку перепрацювань: seeded записи мають `APPROVED`, `PENDING` і
   `REJECTED` статуси.
7. Відкрийте бонуси: seeded записи також демонструють усі три статуси.
8. Покажіть фінансове зведення, організаційну структуру та Excel export.
9. За потреби підключіть MFA менеджера або працівника й покажіть role-based UI.

H2 працює in-memory, тому після повної зупинки та нового запуску дані створюються
заново. У PostgreSQL initializer є ідемпотентним і не дублює записи. Demo seed можна
вимкнути змінною `DEMO_DATA_ENABLED=false`.
