package ru.sla.clarify.auth.session.domain.entity

/**
 * A key which does not change for a single user session: from login until logout.
 * It doesn't change between token refreshes.
 *
 *
 * This key gets generated after login and lives as long as user stays
 * logged in, even between app kills, token refreshes. It gets
 * replaced/reset only after a) new user logs in b) current user logs out.
 *
 * It can be used as a sort-of replacement of the 'userId' in case it is
 * not yet available.
 *
 * For example this is useful to have when cache key is needed which will
 * uniquely identify the logged-in user, but no profile is available yet
 * (which contains user id)
 */
@JvmInline
value class SessionKey(val value: String) {

  override fun toString(): String {
    return value
  }
}
