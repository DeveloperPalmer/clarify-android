package ru.sla.clarify.database

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Наблюдаемый результат запроса, в котором два одинаковых значения подряд не приходят никогда.
 *
 * Подписка на запрос перевыпускает результат на любую запись в его таблицы — в том числе на ту,
 * что выборку не изменила. Повтор, попавший в `flatMapLatest { remoteListener(...) }`, пересоздаёт
 * удалённый слушатель, а для Firestore это новая начальная выборка и оплачиваемые чтения: такой
 * поток уже выжигал дневную квоту за минуты. Попавший в состояние Compose, он вызывает
 * рекомпозицию на каждую постороннюю запись и под нагрузкой раздувает `SnapshotIdSet` до OOM.
 *
 * Поэтому дедупликация зашита в тип: конструктор закрыт и всегда добавляет
 * `distinctUntilChanged()`, а DAO отдают наблюдаемые запросы только этим типом.
 */
class DistinctFlow<T> internal constructor(
  upstream: Flow<T>
) : Flow<T> by upstream.distinctUntilChanged()
