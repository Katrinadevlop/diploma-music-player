package ru.musikkk.player.data.me

import javax.inject.Inject
import javax.inject.Singleton
import ru.musikkk.player.core.network.api.MeApi
import ru.musikkk.player.domain.me.Me

@Singleton
class MeRepositoryImpl @Inject constructor(
    private val meApi: MeApi,
) : MeRepository {

    override suspend fun fetchMe(): Me {
        val resp = meApi.me()
        val user = resp.user
            ?: error("server returned /api/me without a user payload")
        return Me(publicId = user.id, username = user.username)
    }
}
