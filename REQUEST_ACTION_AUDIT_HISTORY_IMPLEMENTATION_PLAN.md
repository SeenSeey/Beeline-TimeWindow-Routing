# План разделения аудита заявок и объяснений изменений расписания

## Статус документа

Этот файл заменяет старый план, в котором объяснения перепланирования сохранялись в `RequestActionAudit.description`.
Старое решение признано неверным после ревью: аудит заявки и объяснение изменения слота являются разными бизнес-понятиями и не должны использовать одну таблицу, один DTO или один formatter.

Документ является источником принятых архитектурных решений для следующих агентов. Не следует заново объединять причины перепланирования с историей заявки или возвращать причины через endpoint истории заявки.

## Подтверждённые бизнес-решения

1. `RequestActionAudit` — неизменяемая история значимых действий над заявкой.
2. `SlotAlterationReason` — объяснение изменения расписания, относящееся к текущей версии `Schedule` и конкретному затронутому `ScheduleSlot`.
3. Причины создаются только при автоматическом перепланировании. Ручное изменение диспетчером создаёт audit-записи, но не создаёт `SlotAlterationReason`.
4. Объясняются только переходы, в которых заявка хотя бы в одной из двух сравниваемых версий имеет слот:
   - была в предыдущем расписании и исчезла из нового;
   - отсутствовала в предыдущем расписании и появилась в новом;
   - присутствует в обеих версиях, но изменились бригада или плановое время.
5. Заявка, которая отсутствовала и в предыдущем, и в новом расписании, причины не получает.
6. Причины старых версий не являются пользовательской историей. При чтении возвращаются только причины текущей версии расписания. Историчность жизненного цикла заявки обеспечивает только `RequestActionAudit`.
7. `operationId` не нужен и должен быть удалён.
8. В `RequestActionAudit` не должно быть ссылки на `Schedule` и свободно сформированного `description`.
9. Описание audit-действия — фиксированная строка у `RequestActionAudit.Action`. Подробности назначения/переноса передаются структурированными полями.
10. Текущая ветка уже перебазирована на `main`; реализация должна учитывать существующее версионирование `Schedule` через `previousSchedule`.
11. `SlotAlterationReason` ссылается только на один `ScheduleSlot`. Поля `resultSchedule` и `previousSlot` не нужны.
12. Для `ADDED_*`/`MOVED_*` reason ссылается на слот текущей версии; для `REMOVED_*` — на удалённый слот предыдущей версии.
13. Цепочка версий `Schedule` должна быть линейной: у одной версии не может быть нескольких непосредственных потомков.

## Почему бригада должна храниться в `ScheduleSlot`

Ссылка `ScheduleSlot -> EngineerCrew` нужна и не является обычным дублированием `Request.assignedCrew`.

`Request.assignedCrew` — текущее изменяемое состояние заявки. `ScheduleSlot.engineerCrew` — снимок назначения в конкретной версии расписания. При перепланировании один и тот же `Request` переназначается новой бригаде. Если предыдущий слот определяет свою бригаду через `slot.request.assignedCrew`, после мутации заявки старая версия начинает выглядеть так, будто заявка всегда принадлежала новой бригаде.

Без snapshot бригады в слоте невозможно надёжно:

- сравнить предыдущую и новую версии расписания;
- сформировать объяснение «была у бригады B, стала у бригады A»;
- отобразить старый слот корректно после переназначения заявки;
- скопировать неизменившийся слот в следующую версию расписания.

Правила использования:

- для `SlotType.REQUEST` бригада слота обязательна;
- при создании нового слота бригада передаётся явно, а не читается неявно из изменяемого `Request.assignedCrew`;
- `ScheduleSlot.createFrom(...)` копирует бригаду исходного слота;
- группировка timeline, repository queries и diff используют `ScheduleSlot.engineerCrew`, а не `Request.assignedCrew`;
- `Request.assignedCrew` остаётся текущим состоянием заявки и обновляется после подготовки diff/новой версии.

Альтернатива — сохранять previous/new crew непосредственно в каждой причине или во временном snapshot. Она создаёт несколько источников одних данных и не исправляет некорректность самих версий расписания, поэтому не выбрана.

## Границы моделей

### RequestActionAudit

Отвечает только на вопрос: «какое значимое действие произошло с заявкой?»

Целевая структура:

| Поле | Nullability | Семантика |
|---|---:|---|
| `request` | NOT NULL | Заявка, с которой произошло действие |
| `action` | NOT NULL | Категория действия |
| `assignedCrew` | NULL | Новая бригада только для назначения или фактической смены бригады |
| `plannedStart` | NULL | Новое плановое начало для назначения/переноса |
| `plannedEnd` | NULL | Новое плановое завершение для назначения/переноса |
| `timestamp` | NOT NULL | Время бизнес-действия |

Удаляемые поля:

- `schedule`;
- `description`;
- `operationId`.

Целевой `Action`:

| Action | Фиксированное отображаемое описание | Дополнительные поля |
|---|---|---|
| `REQUEST_CREATED` | «Заявка создана» | отсутствуют |
| `ASSIGNED` | «Назначена бригаде» | crew, plannedStart, plannedEnd |
| `RESCHEDULED` | «Перенесена» | новое время; crew только при смене бригады |
| `REMOVED_FROM_SCHEDULE` | «Снята с расписания» | отсутствуют |
| `STARTED` | «Взята в работу» | отсутствуют |
| `COMPLETED` | «Выполнена» | отсутствуют |
| `CANCELED` | «Отменена» | отсутствуют |

`REQUEST_UPDATED` не добавлять без реального use case и согласованной семантики «изменения заявки».

Инварианты создания audit:

- использовать именованные фабрики/методы для каждого действия, а не универсальный builder из use case;
- `ASSIGNED` требует бригаду и новое плановое время;
- `RESCHEDULED` всегда содержит новое плановое время;
- если при `RESCHEDULED` бригада не изменилась, `assignedCrew = null`;
- если бригада изменилась, сохраняется только новая бригада;
- timestamp передаётся из application use case, чтобы batch одной операции использовал согласованное время и тесты были детерминированы;
- audit сохраняется в той же транзакции, что и бизнес-изменение;
- preview и неуспешная валидация audit не создают.

### SlotAlterationReason

Отвечает только на вопрос: «почему слот изменился при автоматическом переходе к текущей версии расписания?»

Целевая структура:

| Поле | Nullability | Семантика |
|---|---:|---|
| `slot` | NOT NULL | Объясняемый слот: текущий для added/moved, удалённый слот предыдущей версии для removed |
| `action` | NOT NULL | Причина вместе с видом изменения |
| `causeRequest` | NULL | Заявка, вызвавшая перепланирование |
| `causeCrew` | NULL | Бригада, недоступность которой вызвала перепланирование |
| `createdAt` | NOT NULL | Время фиксации причины |

Отдельное поле `affectedRequest` не нужно: затронутая заявка определяется через `slot.request`. Поля причины должны называться `causeRequest` и `causeCrew`, чтобы не смешивать причинный и затронутый объекты.

Для первой версии используем один enum `Action`, как предложено на ревью. Он кодирует одновременно результат изменения и бизнес-причину. Минимально необходимые семейства значений:

- `ADDED_FOR_HIGHER_PRIORITY_REQUEST`;
- `MOVED_FOR_HIGHER_PRIORITY_REQUEST`;
- `REMOVED_FOR_HIGHER_PRIORITY_REQUEST`;
- `ADDED_AFTER_REQUEST_CANCELLATION`;
- `MOVED_AFTER_REQUEST_CANCELLATION`;
- `REMOVED_AFTER_REQUEST_CANCELLATION`;
- `ADDED_FOR_CREW_OFF_SHIFT`;
- `MOVED_FOR_CREW_OFF_SHIFT`;
- `REMOVED_FOR_CREW_OFF_SHIFT`.

Если число причин значительно вырастет, enum можно разделить на `AlterationType` и `CauseType`, но сейчас это не требуется.

Инварианты `SlotAlterationReason`:

- added: `slot` принадлежит новой текущей версии расписания;
- moved/reassigned: `slot` принадлежит новой текущей версии расписания; старый слот в reason не сохраняется;
- removed: `slot` — удалённый слот предыдущей версии расписания;
- действия `*_REQUEST_*` требуют `causeRequest` и запрещают `causeCrew`;
- действия `*_CREW_OFF_SHIFT` требуют `causeCrew` и запрещают `causeRequest`;
- одна затронутая заявка получает не более одной причины в рамках одного перехода версий;
- свободный `description` в таблице не сохраняется;
- пользовательский текст формируется при чтении из `action`, `slot` и причинных ссылок отдельным небольшим formatter/mapping-компонентом;
- reason хранит факт и причину изменения, но не before/after-сравнение; сравнение версий выполняется отдельным механизмом;
- старые причины не добавляются в request history и не возвращаются вместе с текущим расписанием.

Отдельная сущность `ReplanningEvent` сейчас не нужна: одна новая версия `Schedule` уже является границей одного события перепланирования.

## Версионирование Schedule и время жизни причины

Все применённые изменения расписания должны создавать новую версию `Schedule` через `previousSchedule`, включая ручное редактирование.

Это решение необходимо, даже несмотря на отсутствие причин у ручного редактирования:

- причины всегда относятся к неизменяемой результирующей версии;
- ручное редактирование не удаляет слоты, на которые ссылаются причины предыдущей версии;
- после ручного изменения текущей становится новая версия без причин, а изменения видны в audit;
- следующий автоматический replan сравнивает две стабильные версии;
- запрос текущего расписания продолжает выбирать leaf-версию через `NOT EXISTS` по `previousSchedule`.

Старые `Schedule` и `SlotAlterationReason` могут физически оставаться в БД для ссылочной целостности, но API возвращает только причины последнего перехода к текущему `Schedule`. Их нельзя использовать как историю заявки.

Цепочка версий должна быть линейной. `previous_schedule_id` должен быть уникальным среди ненулевых значений: одна версия может иметь не более одного непосредственного потомка. Это позволяет однозначно трактовать `REMOVED_*`, который ссылается только на слот исходной версии.

Начальное построение расписания создаёт первую версию без `previousSchedule` и не создаёт `SlotAlterationReason`, потому что это не перепланирование.

## Структурный diff расписаний

Старый `PlanSnapshot -> PlanDiff -> ReplanningExplanation` был создан для генерации текстов в audit и должен быть заменён минимальным независимым diff двух версий расписания.

Целевая модель diff:

- `added`: в предыдущей версии слота заявки не было, в новой есть;
- `removed`: в предыдущей версии слот был, в новой отсутствует;
- `moved`: слот есть в обеих версиях, но изменилась бригада, `startTimeWindow` или `endTimeWindow`;
- unchanged: слот есть в обеих версиях и перечисленные значения совпадают.

Правила:

- сравниваются только `SlotType.REQUEST`;
- ключ сравнения — `request.id`;
- в одной версии одна заявка не может встречаться более одного раза;
- crew берётся из `ScheduleSlot.engineerCrew`;
- заявки, отсутствующие в обеих версиях, вообще не входят во вход diff и не получают объяснений;
- изменение только порядка без изменения бригады и планового интервала отдельно не аудируется и не объясняется;
- diff является чистой структурой данных и не создаёт JPA entities самостоятельно;
- преобразование diff в audit и reasons выполняют отдельные фабрики/application orchestration;
- для version-to-version diff проверяется `newSchedule.previousSchedule == previousSchedule`.

Один и тот же diff используется двумя независимыми потребителями:

1. Audit projection создаёт `ASSIGNED`, `RESCHEDULED`, `REMOVED_FROM_SCHEDULE`.
2. Reason factory создаёт `SlotAlterationReason`, но только если replan автоматический и передан реальный причинный контекст.

## Контекст автоматического перепланирования

Application use case автоматического replan должен передать структурированный контекст причины, а не готовый текст:

- отмена заявки: canceled request;
- появление срочной заявки: higher-priority request;
- недоступность бригады: unavailable crew.

Контекст существует во время операции и не требует отдельной таблицы. `SlotAlterationReasonFactory` сопоставляет context + added/moved/removed с конкретным `Action` и заполняет `causeRequest` или `causeCrew`.

Ручной edit не передаёт причинный контекст и reason factory не вызывает.

## Целевые application flows

### Первичное построение расписания

1. Рассчитать первый `Schedule`.
2. Создать слоты с явным `engineerCrew`.
3. Обновить текущее назначение заявок.
4. Для каждого фактически созданного request-slot сохранить `ASSIGNED` audit с новой бригадой и временем.
5. Не создавать `SlotAlterationReason`.
6. Неназначенные заявки не получают ни assignment audit, ни reason.

### Ручное изменение диспетчером

1. Построить и провалидировать candidate без записи в БД.
2. После успешной валидации создать новую версию `Schedule`, не изменять старую in-place.
3. Скопировать неизменившиеся слоты вместе с snapshot бригады.
4. Создать изменённые слоты с явно указанной бригадой.
5. Вычислить diff old -> new.
6. Обновить `Request.assignedCrew`/статус.
7. По diff создать только audit:
   - added -> `ASSIGNED`;
   - moved -> `RESCHEDULED`;
   - removed -> `REMOVED_FROM_SCHEDULE`.
8. Не создавать `SlotAlterationReason`.
9. Preview и validation failure не создают версию, audit или reasons.

### Автоматическое перепланирование после отмены заявки

1. `RequestCancelUseCase` меняет статус заявки и в той же транзакции пишет `CANCELED` audit.
2. После commit существующий listener запускает отдельный replan.
3. Replan загружает текущий `Schedule` и реальный canceled request как cause.
4. Строит новую версию расписания и слоты с snapshot бригад.
5. Вычисляет diff old -> new до потери previous assignments.
6. Обновляет текущие назначения заявок, включая `unassign` для заявок, выпавших из расписания.
7. Создаёт audit по diff.
8. Создаёт reasons по diff с действиями `*_AFTER_REQUEST_CANCELLATION`.
9. Сохраняет новую версию, назначения, audit и reasons атомарно в транзакции replan.
10. Если async replan завершился ошибкой, `CANCELED` audit остаётся, но audit изменений расписания и reasons не появляются.

### Будущие автоматические replan-сценарии

Срочная заявка и недоступность бригады используют тот же flow и тот же diff. Отличается только причинный context и семейство `SlotAlterationReason.Action`. Нельзя копировать вычисление diff в каждый use case.

### Начало и завершение заявки

- `RequestMarkInProgressUseCase` после успешной доменной мутации пишет `STARTED` в той же транзакции.
- `RequestCompleteUseCase` должен быть транзакционным и писать `COMPLETED` в той же транзакции.
- Ошибка доменной проверки не должна оставлять audit.

### Создание заявки

- Реальный `RequestCreateUseCase` пишет `REQUEST_CREATED` после создания заявки в той же транзакции.
- `DemoDatasetLoader` обходит use case, поэтому после сохранения demo requests также должен создать `REQUEST_CREATED`, иначе история demo-заявок начинается не с создания.

## Read API

### История заявки

`GET /api/v1/requests/{id}/history` возвращает только `RequestActionAudit` в порядке `timestamp DESC, id DESC`.

Целевой `RequestHistoryItemDto`:

- `id`;
- `timestamp`;
- `action`;
- фиксированный `description = action.displayName`;
- nullable `crewId`/`crewName`;
- nullable `plannedStart`/`plannedEnd`.

Из DTO удалить `scheduleId`. Причины перепланирования в этот endpoint не добавлять.

### Текущее расписание и причины

`ScheduleTimelineFindCurrentByRegionUseCase`:

1. Находит текущую leaf-версию `Schedule`.
2. Загружает причины текущего перехода по двум правилам:
   - `ADDED_*` и `MOVED_*`: `reason.slot.schedule.id == currentSchedule.id`;
   - `REMOVED_*`: `reason.slot.schedule.id == currentSchedule.previousSchedule.id`.
3. Передаёт их в `ScheduleTimelineDto` отдельным top-level списком `alterationReasons`.

Из предыдущей версии нельзя загружать все reasons: разрешены только `REMOVED_*`. Иначе в ответ попадут `ADDED_*`/`MOVED_*`, описывавшие создание предыдущей версии.

Если `currentSchedule.previousSchedule == null`, у первой версии нет причин перепланирования.

Причины должны быть top-level, а не только вложенными в текущие timeline slots, потому что removed request не имеет слота в текущей версии.

Минимальный `SlotAlterationReasonDto`:

- `id`;
- `action`;
- сформированный `description`;
- `affectedRequestId`;
- `slotId`;
- `slotScheduleId`;
- nullable `causeRequestId`;
- nullable `causeCrewId`;
- данные бригады и времени из объясняемого `slot`, если это требуется UI.

Formatter причины использует структурированные ссылки. Он не должен зависеть от `RequestActionAudit` или старых `ReplanningExplanation` DTO.
Before/after-значения formatter причины не вычисляет: они относятся к отдельному механизму сравнения расписаний.

## Изменения текущего кода

### Удалить

- `server/src/main/java/stb/core/domain/service/explanation/RequestActionAuditFormatter.java`;
- `server/src/main/java/stb/core/domain/service/explanation/ReplanningExplanationDomainService.java`;
- `server/src/main/java/stb/core/domain/dto/explanation/ReplanningExplanation.java`;
- `server/src/main/java/stb/core/domain/dto/explanation/ReplanContext.java`;
- связанные unit tests;
- старую generic-модель `RequestActionAuditWriter.Entry`, содержащую description, schedule и operationId.

### Заменить минимальным schedule diff

Следующие старые классы можно удалить после переноса действительно нужной логики сравнения:

- `PlanSnapshot`;
- `PlanDiff`;
- `PlanDiffSummary`;
- `RequestChange`;
- `CrewRouteChange`;
- `PlanDiffDomainService`.

Новый diff не должен хранить summary, title, counters, сообщения, unassigned ids или идентификаторы «логических планов». Для текущей задачи нужны только added/moved/removed пары реальных versioned slots.

### Изменить

- `RequestActionAudit`: новая схема, enum display names и именованные фабрики.
- `ScheduleSlot`: добавить `engineerCrew`, копировать его между версиями.
- `Schedule`: методы добавления слотов должны принимать/сохранять crew snapshot.
- `ScheduleEditUseCase`: создавать новую версию, писать только audit.
- `ScheduleCalculateUseCase`: писать initial assignment audit, reasons не создавать.
- `ScheduleRecalculateOnRequestCancelUseCase`: diff, audit и cancellation reasons.
- `RequestCancelUseCase`, `RequestMarkInProgressUseCase`, `RequestCompleteUseCase`, будущий `RequestCreateUseCase`: lifecycle audit.
- `ScheduleTimelineDto`: группировать слоты по `slot.engineerCrew`, добавить top-level reasons.
- `ScheduleTimelineFindCurrentByRegionUseCase`: загружать reasons текущей версии.
- `ScheduleSlotRepository`: запросы crew должны использовать `schedule_slots.engineer_crew_id`, а не mutable request assignment.
- `RequestHistoryItemDto`: fixed action description, crew и planned interval, без schedule id.
- `DevStateResetRepository`: удалить reasons до slots/schedules и добавить таблицу в lock.
- entity graphs: при необходимости загружать `slots.engineerCrew` и ссылки причин.

### Добавить

- `SlotAlterationReason`;
- `SlotAlterationReasonRepository` с запросом причин текущего перехода по current/previous schedule и группам `Action`;
- `SlotAlterationReasonDto`;
- небольшой formatter DTO-текста причины;
- фабрику reasons из automatic replan context + schedule diff;
- чистую модель и сервис diff версий расписания.

## Схема БД

`request_actions_audit`:

- удалить `schedule_id`;
- удалить `description`;
- удалить `operation_id`;
- добавить nullable `planned_start`;
- добавить nullable `planned_end`;
- оставить nullable `assigned_crew_id`;
- сохранить индекс `(request_id, timestamp DESC, id DESC)`.

`schedule_slots`:

- добавить `engineer_crew_id`;
- для `REQUEST`-слотов обеспечивать non-null доменным инвариантом;
- добавить подходящий индекс для выборок по schedule/crew при необходимости.

`slot_alteration_reasons`:

- `id`;
- `slot_id NOT NULL`;
- `action NOT NULL`;
- `cause_request_id NULL`;
- `cause_crew_id NULL`;
- `created_at NOT NULL`;
- индекс `(slot_id, action)`.

`schedules`:

- добавить unique constraint/index на ненулевой `previous_schedule_id`, чтобы цепочка версий не ветвилась;
- конкурентное создание двух новых версий от одного current schedule должно завершаться конфликтом, а не образовывать две leaf-ветки.

Один слот может иметь больше одной записи reason на протяжении жизни цепочки: например, `MOVED_*` при создании его версии и позднее `REMOVED_*` при переходе к следующей. Поэтому уникальность только по `slot_id` вводить нельзя. Инвариант «одна причина на заявку в одном переходе» контролируется фабрикой/application service и тестами.

В проекте используется `spring.jpa.hibernate.ddl-auto=update`. Hibernate update не гарантирует удаление старых колонок. Для чистой hackathon-БД допустим контролируемый reset/recreate; если данные требуется сохранить, нужна явная SQL/Flyway-миграция с backfill `schedule_slots.engineer_crew_id` до включения ограничений.

## Последовательность реализации

### Этап 1. Зафиксировать новые доменные инварианты

1. Написать unit tests целевой модели `RequestActionAudit`.
2. Написать unit tests `ScheduleSlot` crew snapshot/copy.
3. Написать unit tests `SlotAlterationReason` для added/moved/removed, единственной ссылки `slot` и допустимых cause-ссылок.

Проверка: невозможны audit/reason с противоречивыми комбинациями полей.

### Этап 2. Ввести snapshot бригады в слот

1. Добавить связь с crew.
2. Обновить конструкторы/factories `Schedule` и `ScheduleSlot`.
3. Перевести timeline и repository queries на crew слота.
4. Добавить тест: после `Request.assignCrew(newCrew)` предыдущий слот продолжает ссылаться на oldCrew.

Проверка: существующие schedule/timeline tests проходят, старая версия не меняет отображаемую бригаду.

### Этап 3. Заменить старый explanation diff

1. Ввести минимальный version-to-version diff.
2. Покрыть added, removed, time-only moved, crew-only moved, combined moved и unchanged.
3. Проверить duplicate request и связь previous/current schedules.
4. После перевода потребителей удалить старые explanation DTO/services/tests.

Проверка: в production-коде больше нет зависимости audit от пакета `domain.dto.explanation`.

### Этап 4. Очистить audit-модель

1. Удалить schedule, description и operationId.
2. Добавить plannedStart/plannedEnd и display name у Action.
3. Заменить generic writer на именованные factory methods.
4. Обновить repository/DTO/history tests.

Проверка: history строится только из audit, текст не зависит от schedule/reason formatter.

### Этап 5. Подключить audit ко всем write-path

1. Demo/request creation.
2. Initial assignment.
3. Manual schedule version diff.
4. Automatic replan diff.
5. Start, complete, cancel.

Проверка: ровно одна audit-запись на одно значимое изменение заявки; crew у `RESCHEDULED` null при той же бригаде и равна новой бригаде при переназначении.

### Этап 6. Реализовать SlotAlterationReason

1. Добавить entity/repository/factory/formatter/DTO.
2. Подключить к cancellation replan.
3. Не подключать к initial scheduling или manual edit.
4. Подготовить расширяемый причинный context для urgent request и crew off-shift.

Проверка: added/moved ссылаются на слот новой версии, removed — на слот предыдущей версии; absent -> absent не получает.

### Этап 7. Разделить read API

1. Оставить request history только с actions.
2. Добавить reasons текущей версии в schedule timeline.
3. Проверить removed reason через top-level список.
4. Проверить, что из предыдущей версии возвращаются только `REMOVED_*` текущего перехода, а старые `ADDED_*`/`MOVED_*` не возвращаются.

### Этап 8. Обновить БД reset/schema и выполнить regression

1. Обновить reset deletion order и table locks.
2. Проверить схему на чистой PostgreSQL БД.
3. Запустить целевые unit/application tests.
4. Запустить полный `./gradlew test`.

## Обязательные тестовые сценарии

### Audit

- request creation;
- initial assignment;
- assignment previously unassigned request during replan/manual edit;
- time change with same crew: `RESCHEDULED`, crew null;
- reassignment: `RESCHEDULED`, crew points to new crew;
- removal from schedule;
- start, complete, cancel;
- no audit for unchanged copied slot;
- no audit for preview/validation failure;
- audit rolls back with failed business transaction;
- deterministic history ordering by timestamp and id.

### Reasons

- previous absent -> current present: ADDED reason;
- previous present -> current changed: MOVED reason referring only to current slot;
- previous present -> current absent: REMOVED reason referring to previous slot;
- absent -> absent: no reason;
- unchanged slot: no reason;
- initial schedule: no reasons;
- manual edit: no reasons;
- cancellation replan: causeRequest filled;
- crew unavailable: causeCrew filled;
- current schedule endpoint returns only current version reasons;
- current schedule endpoint получает `REMOVED_*` через `currentSchedule.previousSchedule`;
- старые `ADDED_*`/`MOVED_*` предыдущей версии не попадают в ответ;
- request history endpoint never returns reasons.

### Schedule versioning and slot snapshots

- automatic replan creates `new.previousSchedule = old`;
- manual apply also creates a new version;
- old slots are not deleted or mutated;
- copied slot keeps old crew snapshot;
- reassigned request has old crew in old slot and new crew in new slot;
- current schedule query returns only leaf version;
- timeline groups slots by `slot.engineerCrew`.

## Не делать

- не хранить explanation в `RequestActionAudit`;
- не добавлять schedule обратно в audit;
- не добавлять reasons в request history;
- не создавать reason для manual edit или initial scheduling;
- не создавать reason для заявки, отсутствующей в обеих версиях;
- не вычислять previous crew через mutable `Request.assignedCrew`;
- не сохранять готовый динамический текст причины без отдельного требования исторической фиксации текста;
- не создавать отдельный event sourcing/JSONB журнал;
- не дублировать diff в каждом replan use case;
- не использовать JPA entity listeners или AOP для бизнес-аудита.

## Критерий готовности

Результат считается готовым, когда:

- история заявки содержит только фиксированные бизнес-действия;
- тексты audit берутся из enum Action;
- audit не содержит schedule, description и operationId;
- каждая версия слота сохраняет свою бригаду;
- автоматический replan создаёт структурированные причины только для реально добавленных, перемещённых и удалённых слотов;
- ручное изменение создаёт новую версию и audit, но не reasons;
- schedule API показывает причины только текущей версии;
- старый explanation/audit formatter stack удалён;
- целевые и полные тесты проходят.
