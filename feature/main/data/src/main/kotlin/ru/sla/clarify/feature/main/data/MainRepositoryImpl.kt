package ru.sla.clarify.feature.main.data

import com.squareup.anvil.annotations.ContributesBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.sla.clarify.core.domain.di.scope.SingleIn
import ru.sla.clarify.database.InMemoryDB
import ru.sla.clarify.database.extension.executeAsStringOrZero
import ru.sla.clarify.database.firestore.Firestore
import ru.sla.clarify.feature.main.data.mapper.Mappers
import ru.sla.clarify.feature.main.domain.MainRepository
import ru.sla.clarify.feature.main.domain.di.MainScope
import ru.sla.clarify.feature.main.domain.entity.UserDetails
import javax.inject.Inject

@SingleIn(MainScope::class)
@ContributesBinding(MainScope::class)
class MainRepositoryImpl @Inject constructor(
  private val firestore: Firestore,
  private val inMemoryDB: InMemoryDB
) : MainRepository {

  override suspend fun fetchUserDetails(skipCache: Boolean) = withContext(Dispatchers.IO) {
    val cacheKey = getFetchCacheKey()
    val storedCacheKey = getStoredCacheKey()

    if (!skipCache && cacheKey == storedCacheKey) {
      return@withContext
    }

    val userDetails = requireNotNull(firestore.getUserDetails()) {
      "userDetails not found in Firestore"
    }

    inMemoryDB.transaction {
      inMemoryDB.userDetailsQueries.insert(
        chatSignature = userDetails.chatSignature
      )
      inMemoryDB.cacheKeyQueries.save(
        id = Firestore.GET_USER_DETAILS_CACHE_KEY,
        cacheKey = cacheKey
      )
    }
  }

  override suspend fun getUserDetails(): UserDetails {
    return inMemoryDB.userDetailsQueries
      .select(Mappers::mapToUserDetails)
      .executeAsOne()
  }

  private fun getFetchCacheKey(): String {
    return inMemoryDB.firestoreRequestLogQueries
      .selectLastSentAt(Firestore.GET_USER_DETAILS_CACHE_KEY)
      .executeAsStringOrZero()
  }

  private fun getStoredCacheKey(): String? {
    return inMemoryDB.cacheKeyQueries
      .get(Firestore.GET_USER_DETAILS_CACHE_KEY)
      .executeAsOneOrNull()
  }
}
