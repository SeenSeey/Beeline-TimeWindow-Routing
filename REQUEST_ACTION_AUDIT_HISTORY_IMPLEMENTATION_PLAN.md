# Plan

Реализовать неизменяемую историю бизнес-операций и объяснений для каждой заявки через `RequestActionAudit`. Diff вычисляется в момент операции, пока доступны состояния «до» и «после», а для каждой затронутой заявки сохраняется готовая самодостаточная строка объяснения. `Schedule` и `ScheduleSlot` остаются текущим изменяемым состоянием и не используются для восстановления прошлых планов.

## Scope

- In:
  - одна append-only audit-запись на одну заявку и одну бизнес-операцию;
  - готовое историческое объяснение в `RequestActionAudit.description`;
  - связь изменений одного перепланирования через `operationId`;
  - история назначений, переназначений, снятий, изменений порядка/времени, отмены и завершения заявки;
  - REST API для получения timeline конкретной заявки;
  - атомарное сохранение audit в той же транзакции, что и бизнес-изменение.
- Out:
  - версионирование `Schedule` и `ScheduleSlot`;
  - восстановление и отображение полного вида старого расписания;
  - повторное вычисление старого diff или перегенерация старого текста;
  - JSONB/event sourcing и глобальный журнал перепланирований;
  - JPA entity listeners, AOP-перехват изменений и отдельные `REQUIRES_NEW`-транзакции для audit.

## Decision context

- Объяснение строится из двух источников: бизнес-события, вызвавшего перепланирование, и наблюдаемой разницы между планом «до» и планом «после». Оно не должно упоминать score, штрафы, внутреннюю работу оптимизатора или утверждать, что найден глобально оптимальный план.
- Слой `PlanSnapshot → PlanDiff → ReplanningExplanation` остаётся независимым от источника плана: Timefold, другой VRP-оптимизатор, LLM/агент или ручное изменение. На вход он получает снимки, а не классы конкретного solver.
- Историчность здесь означает неизменяемый timeline конкретной заявки, а не версионирование всего расписания. Поэтому `Schedule` и `ScheduleSlot` остаются текущим изменяемым состоянием: старый маршрут восстановить нельзя и по требованиям не нужно.
- Текущих полей `RequestActionAudit` недостаточно для позднего восстановления diff. `timestamp` — время операции, а не плановое время; одна ссылка на бригаду не хранит значения «до»/«после»; изменение порядка или времени при той же бригаде не образует текущей audit-записи. Поэтому diff вычисляется сразу, пока оба состояния доступны, а audit впоследствии не replay-ится.
- Итоговая строка в `description` выбрана осознанно вместо JSONB. Frontend должен только показать историю; старый diff не требуется повторно анализировать. Строка фиксирует формулировку, имена бригад и времена на момент операции. Принятый trade-off: старые тексты не локализуются, не переформулируются и не исправляются задним числом.
- Одна audit-запись соответствует одной заявке в рамках одной бизнес-операции. Если заявка одновременно переназначена, перемещена в маршруте и сдвинута по времени, сохраняется одна строка с объединённым текстом. `Action` — primary category для иконки/фильтра, `description` — полное объяснение.
- `operationId` — обязательный внутренний UUID бизнес-операции. Он одинаков для всех изменённых этой операцией заявок, но не возвращается frontend в первой версии API.

## Current implementation context

- `server/src/main/java/stb/core/domain/dto/replanning/PlanSnapshot.java` уже описывает маршруты бригад, позиции, predecessor, плановые времена и неназначенные заявки. Для audit снимок существует только во время операции и не персистится.
- `server/src/main/java/stb/core/domain/service/replanning/PlanDiffDomainService.java` уже вычисляет `ADDED_TO_PLAN`, `ADDED_AS_UNASSIGNED`, `REMOVED_FROM_PLAN`, `ASSIGNED_FROM_UNASSIGNED`, `BECAME_UNASSIGNED`, `REASSIGNED`, `ORDER_CHANGED` и `PLANNED_TIME_CHANGED`. Эту логику не следует дублировать в audit-сервисе.
- `server/src/main/java/stb/core/domain/service/replanning/ReplanningExplanationDomainService.java` формирует объяснения для аварийной заявки, отмены заявки и недоступности бригады. `Change.message` вторично затронутой заявки не всегда содержит причину, поэтому audit-formatter должен добавить context prefix, а не просто скопировать сообщение.
- `server/src/main/java/stb/core/application/schedule/ScheduleEditUseCase.java` удаляет старые слоты и добавляет новые в тот же `Schedule`. Он уже пишет частичный audit назначений, но не фиксирует time/order-only изменения. Previous snapshot необходимо собрать до `removeAll/deleteAll` и до изменения `Request.assignedCrew`.
- `ScheduleEditUseCase` является ручным редактированием, но в `ReplanContext.TriggerType` нет manual trigger. Нельзя подставлять фиктивный `triggerSubjectId` или выдавать ручное изменение за аварию, отмену либо недоступность бригады. Для этого path formatter формирует причину «После ручного изменения расписания…» напрямую из `RequestChange`; `ReplanningExplanationDomainService` здесь не вызывается.
- `server/src/main/java/stb/core/application/schedule/ScheduleCalculateUseCase.java` выполняет первичные назначения без audit. Для него не нужен `PlanDiff`: на каждую фактически назначенную заявку сохраняется `INITIAL_ASSIGNMENT`.
- `server/src/main/java/stb/core/domain/model/RequestActionAudit.java` сейчас требует non-null `schedule` и `assignedCrew` и не хранит description/operation id. Таблица фактически не задействована, поэтому совместимость с накопленной audit-историей не требуется.

## Target flow

1. В начале write-операции создать один `operationId` и один `occurredAt`; все audit-строки операции получают эти значения.
2. Для изменения плана собрать previous snapshot до любой мутации JPA entities, а next snapshot — из уже провалидированного candidate/`EditPlan`. Next snapshot включает изменённые и неизменённые маршруты; множество неназначенных заявок также вычисляется для обоих временных снимков и после операции не хранится.
3. Вычислить `PlanDiff` до удаления слотов и изменения `Request.assignedCrew`. Поскольку persisted `Schedule` остаётся тем же, оба временных snapshot используют его id как логический plan id, а `newSnapshot.previousPlanId` также равен ему. Текущие validators не требуют разных previous/new ids; фиктивная версия плана не нужна.
4. Для событийного перепланирования сформировать реальный `ReplanContext`, вызвать `ReplanningExplanationDomainService` и дополнить каждый per-request message контекстом причины. Для ручного `ScheduleEditUseCase` сформировать description непосредственно из `RequestChange` с причиной «После ручного изменения расписания…».
5. Применить изменения к `Schedule`, `ScheduleSlot` и `Request`, затем сформировать одну audit-запись для каждого изменившегося request id. Неизменившиеся заявки записей не получают; комбинированные change types сворачиваются в одну строку с primary `Action`.
6. Сохранить batch через `saveAll` в той же Spring-транзакции. Любая ошибка до commit откатывает и бизнес-изменения, и audit.
7. При чтении истории возвращать сохранённый `description` без попытки заново загрузить старый план, имена бригад или шаблоны explanation.

## Description rules

- Текст пишется в прошедшем времени, самодостаточен и понятен без title/summary всего перепланирования.
- Event-driven replan включает каузальный prefix: «После добавления аварийной заявки…», «После отмены заявки…» или «Из-за недоступности бригады…». Для manual edit используется «После ручного изменения расписания…».
- В текст входят все изменения конкретной заявки. Стабильный порядок частей: назначение/снятие, порядок, плановое время.
- Текст не объясняет мотивы solver, не оценивает оптимальность и не выдумывает причину неназначения.
- Сохранённая строка больше не изменяется и не пересобирается из текущих JPA entities.

## Action items

- [ ] Зафиксировать инварианты audit-модели тестами и в коде: `request` всегда заполнен; `schedule` и `assignedCrew` могут быть `null`; `assignedCrew` означает основную/результирующую бригаду; `description` содержит причину и наблюдаемое изменение; `operationId` обязателен и одинаков для всех заявок одной бизнес-операции.
- [ ] Расширить `server/src/main/java/stb/core/domain/model/RequestActionAudit.java`: добавить `description TEXT NOT NULL` и `operationId UUID NOT NULL`, снять `nullable = false` с `schedule` и `assignedCrew`, переименовать `timestamp` в `occurredAt` и расширить `Action` значениями `REQUEST_CREATED`, `REQUEST_UPDATED`, `INITIAL_ASSIGNMENT`, `ASSIGNED_CREW_CHANGE`, `REMOVE_FROM_SCHEDULE`, `SCHEDULE_CHANGE`, `CANCEL_REQUEST`, `COMPLETE_REQUEST`; исправить `INITIAL_ASSIGNATION` до начала реального накопления данных.
- [ ] Обновить схему PostgreSQL для новых колонок и nullable-ограничений; так как audit ещё не используется, для hackathon-среды допустимо пересоздать пустую таблицу, но изменение должно быть повторяемым и проверено на чистой БД; добавить индекс `(request_id, occurred_at DESC, id DESC)` для timeline.
- [ ] Добавить единый application-сервис записи audit, который принимает заявку, action, готовое description, бригаду, schedule, `operationId` и время; для batch-изменений использовать `saveAll`, не открывать отдельную транзакцию и не размазывать `RequestActionAudit.builder()` по use case-ам.
- [ ] Выделить formatter исторических описаний с двумя явными path: event-driven replan превращает `ReplanContext` и `ReplanningExplanation.Change` в самодостаточную фразу, manual schedule edit превращает `RequestChange` в такую же фразу с причиной «ручное изменение расписания»; не расширять `ReplanContext` фиктивным manual subject только ради audit. Вторичные изменения должны сохранять причину; имена бригад и времена встраивать в строку при создании audit.
- [ ] Определить единую функцию выбора primary action для комбинированного `RequestChange`: `REASSIGNED` → `ASSIGNED_CREW_CHANGE`, `ADDED_TO_PLAN`/`ASSIGNED_FROM_UNASSIGNED` → `INITIAL_ASSIGNMENT`, `BECAME_UNASSIGNED`/`REMOVED_FROM_PLAN` → `REMOVE_FROM_SCHEDULE`, а изменения только порядка и/или времени → `SCHEDULE_CHANGE`; при нескольких change types сохранять одну объединённую audit-запись, а не дублировать строку для каждого типа.
- [ ] Интегрировать audit в `ScheduleEditUseCase.apply`: до удаления слотов построить previous snapshot, из `EditPlan` и неизменённых маршрутов — next snapshot, вычислить `PlanDiff`, затем применить план и сохранить по одной audit-записи на каждый `RequestChange`; для manual path не вызывать `ReplanningExplanationDomainService` без реального `ReplanContext`. Для in-place schedule использовать его id как логический plan id в обоих временных snapshots, а историю операций различать по `operationId`.
- [ ] Подключить централизованную запись audit ко всем текущим write-path: в `ScheduleCalculateUseCase` фиксировать первичные назначения; в `ScheduleEditUseCase` заменить локальный `audit(...)`; при добавлении use case-ов создания, изменения, отмены и завершения заявки записывать audit после доменной мутации, но до commit той же транзакции; preview/read-only операции audit не создают.
- [ ] Добавить чтение истории: repository-метод `findAllByRequestIdOrderByOccurredAtDescIdDesc`, application use case, `RequestHistoryItemDto` с `id`, `occurredAt`, `action`, `description`, nullable `crewId`/`scheduleId` и endpoint `GET /api/v1/requests/{requestId}/history`; не возвращать frontend внутренний `operationId`, не возвращать JPA entity напрямую и добавить детерминированную сортировку по `id` при равных timestamps. В первой версии endpoint возвращает весь timeline без пагинации.
- [ ] Добавить проверки: unit-тесты formatter для всех `RequestChange.Type` и комбинаций reassignment/time/order; тесты mapping primary action; application/integration-тесты на одну строку на заявку и операцию, общий `operationId`, отсутствие audit при preview/валидационной ошибке, rollback audit вместе с бизнес-изменением, initial assignment, time/order-only edit и порядок timeline; затем запустить целевые replanning-тесты и полный `./gradlew test`.

## Closed questions

- Для текущего сценария достаточно возвращать весь timeline заявки.
- Не нужно показывать на фронте `operationId`, оставить его только внутренней связью для диагностики и будущей группировки.
