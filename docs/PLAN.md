# План лабораторной работы №1

Объём: модули `identity`, `catalog`, `profile`, `matching`. Модули `events` и `training`
из README в лабу 1 не входят.

Порядок работы: один коммит за раз, после каждого — проверка. Ветки создаёт, коммитит
и пушит Ksenia; правки в файлах делает Claude и отмечает статус в этой таблице.

Пакеты: `ru.itmo.gymbro.<модуль>.<слой>`, где модуль — `identity`, `catalog`, `profile`,
`matching`, `shared`, а слой — `model`, `repository`, `service`, `web`, `dto`, `api`.

Статусы: `не сделано` / `сделано`.

---

## 0. feature/project-setup

| Коммит | Что меняется | Статус |
|---|---|---|
| `chore: bootstrap Spring Boot project with Postgres and Liquibase` | Gradle на Spring Boot 4.0.8 и Java 21, Dockerfile, docker-compose с PostgreSQL 17, `.env.example`, `application.yml`, точка входа `GymBroApplication`, миграции Liquibase на девять таблиц | сделано |

---

## 1. feature/domain-model

Сущности и их маппинг в БД. К концу ветки объекты сохраняются в базу и читаются обратно.

| Коммит | Что меняется | Статус |
|---|---|---|
| `refactor(db): move profile fields into user_profiles table` | `users` сводится к учётным данным (email, пароль, роль, статус). Анкетные поля переезжают в новую таблицу `user_profiles`; `user_photos`, `user_sports`, `user_gyms` начинают ссылаться на `profile_id` вместо `user_id` | сделано |
| `feat: add domain entities for users, catalog, profile and matching` | Java-сущности под все таблицы: `User`, `Sport`, `Gym`, `GymRequest`, `UserProfile` с `UserPhoto`/`UserSport`/`UserGym`, `Like`, `Match`. Enum'ы `Role`, `UserStatus`, `RequestStatus`, `SportLevel`. Инварианты в конструкторах, без Spring | сделано |
| `feat: map entities to database with Spring Data JDBC` | `@Table`, `@Id`, `@MappedCollection` на сущностях. В каждом модуле пакет `repository`: интерфейс-порт, адаптер и Spring Data DAO. Ссылки между модулями — обычные `long`, без `AggregateReference` | сделано |
| `test: add repository integration tests on Testcontainers` | `AbstractIntegrationTest` с PostgreSQL в докере (один контейнер на весь прогон, откат транзакции после каждого теста), тесты «сохранили — прочитали — сошлось» по каждому агрегату, `ArchitectureTest` с правилами ArchUnit | сделано |
| `feat: validate entities before save with Bean Validation` | Аннотации Jakarta Validation на сущностях и `BeforeSaveCallback`, прогоняющий `Validator` перед записью — валидация на уровне Entity | сделано |

---

## 2. feature/shared

Общий фундамент для всех контроллеров.

| Коммит | Что меняется | Статус |
|---|---|---|
| `feat(shared): add error handling with problem details` | `@RestControllerAdvice`, ответы в формате `ProblemDetail` (RFC 7807), маппинг доменных исключений на 400/401/403/404/409 | сделано |
| `feat(shared): add pagination helpers for slices and total count` | `SliceResponse` для бесконечной прокрутки и `PageResponses` с заголовком `X-Total-Count` | сделано |
| `feat(shared): add current user abstraction` | Интерфейс `CurrentUserProvider` и временная реализация на заголовке `X-User-Id` — шов под spring-security в лабе 3 | сделано |
| `feat(shared): add openapi configuration` | Заголовок, версия и описание общего Swagger | сделано |

---

## 3. feature/identity

| Коммит | Что меняется | Статус |
|---|---|---|
| `feat(identity): add user registration and retrieval use cases` | Сценарии регистрации, чтения и удаления пользователя; хэширование пароля BCrypt; интерфейсы сценариев отдельно от реализаций | сделано |
| `feat(identity): add user rest controller with validation` | `POST/GET/PATCH/DELETE /api/v1/users`, пагинация на списке, `@Valid` на request-DTO, 201 с заголовком `Location`, 409 на занятый email, 204 на удаление. В DTO ответа пароля нет | сделано |
| `test(identity): cover user api with integration tests` | Интеграционные тесты на регистрацию, дубликат email, невалидное тело, пагинацию | сделано |

---

## 4. feature/catalog

| Коммит | Что меняется | Статус |
|---|---|---|
| `feat(catalog): add sports and gyms crud api` | CRUD по `/api/v1/sports` и `/api/v1/gyms` с пагинацией | сделано |
| `feat(catalog): add gym request submission` | `POST /api/v1/gym-requests` от пользователя, `GET /api/v1/gym-requests` для админа со страницами и общим количеством в `X-Total-Count` | сделано |
| `feat(catalog): add transactional gym request approval` | **Транзакция №1.** `POST /api/v1/gym-requests/{id}/approve` и `/reject` для админа. Одобрение блокирует заявку (`SELECT ... FOR UPDATE`), проверяет, что зала по этому адресу ещё нет, создаёт зал и переводит заявку в `APPROVED` со ссылкой на него. Анкету автора не трогает: `catalog` не зависит от `profile`. Без транзакции сбой между шагами оставил бы зал без закрытой заявки, а одновременные одобрения создали бы два зала или одобрили и отклонили одну заявку | сделано |
| `test(catalog): cover gym request approval transaction` | Тесты на одобрение, отклонение и откат транзакции при сбое | сделано |

---

## 5. feature/profile

| Коммит | Что меняется | Статус |
|---|---|---|
| `feat(profile): add profile retrieval and editing` | `GET /api/v1/profiles/{userId}`, `PUT /api/v1/profiles/me`. Текущий пользователь берётся из `CurrentUserProvider`, а не из пути | сделано |
| `feat(profile): add sports and gyms management in profile` | `PUT /api/v1/profiles/me/sports` и `/gyms`. Many-to-Many с дополнительным полем `level` | сделано |
| `feat(profile): add profile photos management` | `POST` и `DELETE /api/v1/profiles/me/photos`, порядок через `position` | сделано |
| `fix(profile): lock profile row while changing it` | Изменения анкеты читают её с `SELECT ... FOR UPDATE` (`@Lock(PESSIMISTIC_WRITE)`). Без блокировки два одновременных запроса читали одну версию агрегата, и второй при сохранении стирал изменения первого: из двух одновременно добавленных фото оставалось одно | сделано |
| `test(profile): cover profile aggregate updates` | Тесты агрегата на реальных коммитах: замена дочерних коллекций без «осиротевших» строк, позиции фото после удаления, откат всего агрегата при сбое сохранения, одновременные создание анкеты, замена видов спорта и добавление фото | сделано |

---

## 6. feature/matching

| Коммит | Что меняется | Статус |
|---|---|---|
| `feat(matching): add profile feed with infinite scroll pagination` | `GET /api/v1/feed` возвращает `Slice` без общего количества. Лента не показывает себя, забаненных и уже лайкнутых; сортировка по пересечению видов спорта и залов | сделано |
| `feat(matching): add transactional like with match creation` | **Транзакция №2.** `POST /api/v1/likes`: вставить лайк, проверить встречный, создать мэтч. Внутри транзакции берётся `pg_advisory_xact_lock` по упорядоченной паре пользователей. Без неё в READ COMMITTED при одновременных встречных лайках каждая транзакция не видит незакоммиченный лайк другой, и мэтч не создаётся вовсе (проверено тестом без блокировки). Двойной мэтч дополнительно запрещает уникальный индекс `matches_unique` | сделано |
| `feat(matching): add matches listing` | `GET /api/v1/matches`: мэтчи текущего пользователя с партнёром с любой стороны пары, сначала новые. Страницы с общим количеством в `X-Total-Count` | сделано |
| `test(matching): cover mutual like and match creation` | Тесты на односторонний лайк, взаимный лайк, повторный лайк, лайк самому себе | сделано |

---

## 7. feature/api-docs

| Коммит | Что меняется | Статус |
|---|---|---|
| `docs(api): annotate controllers with openapi metadata` | `@Tag` по модулям, `@Operation` и `@ApiResponse` на методах, примеры тел запросов | не сделано |
| `docs: describe architecture, transactions and pagination in readme` | Обновление `docs/README.md`: карта модулей, актуальная ER-диаграмма с `user_profiles`, обоснование обеих транзакций, таблица эндпоинтов с видом пагинации | не сделано |

---

## 8. feature/test-coverage

| Коммит | Что меняется | Статус |
|---|---|---|
| `test: add missing unit tests for use cases` | Юнит-тесты сценариев без поднятия Spring, добор покрытия до порога | не сделано |
| `build: enforce jacoco coverage threshold` | Подключение `check.dependsOn jacocoTestCoverageVerification` с порогом 70% | не сделано |

---

## 9. feature/release-check

| Коммит | Что меняется | Статус |
|---|---|---|
| `chore: verify full stack startup in docker compose` | Прогон `docker compose down -v && docker compose up --build` с нуля, проверка эндпоинтов через Swagger, сверка с чеклистом требований лабы | не сделано |

---

## Вне кода

- Согласовать схему БД с преподавателем до начала ветки `feature/domain-model`.
- Уточнить, достаточно ли четырёх модулей, или ожидается весь домен из README.

---

## 10. bug/fixing-bugs

| Коммит | Что меняется | Статус |
|---|---|---|
| `refactor(profile): remove profile photos` | Фото анкеты убраны целиком: таблица `user_photos` вырезана из исходной миграции (базу нужно пересоздать), `UserPhoto`, эндпоинты `/profiles/me/photos`, фото в ответах анкеты и карточке ленты. Тест блокировки анкеты переписан на одновременные изменения видов спорта и залов | сделано |
