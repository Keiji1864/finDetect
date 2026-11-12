package com.ghostteam.findetect.data.repository

import com.ghostteam.findetect.data.local.dao.BalanceDao
import com.ghostteam.findetect.data.mapper.toDomain
import com.ghostteam.findetect.domain.model.AccountBalance
import com.ghostteam.findetect.domain.repository.BalanceRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class LocalBalanceRepository @Inject constructor(
    private val balanceDao: BalanceDao,
) : BalanceRepository {
    override fun observeAccount(accountId: String): Flow<AccountBalance?> =
        balanceDao.observeAccount(accountId).map { it?.toDomain() }

    override fun observeAccounts(currency: String): Flow<List<AccountBalance>> =
        balanceDao.observeAccounts(currency).map { rows -> rows.map { it.toDomain() } }
}
