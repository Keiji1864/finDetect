package com.ghostteam.findetect.core.di

import com.ghostteam.findetect.data.repository.LocalAccountRepository
import com.ghostteam.findetect.data.repository.LocalBalanceAdjustmentRepository
import com.ghostteam.findetect.data.repository.LocalBalanceRepository
import com.ghostteam.findetect.data.repository.LocalCategoryRepository
import com.ghostteam.findetect.data.repository.LocalTransactionRepository
import com.ghostteam.findetect.domain.repository.AccountRepository
import com.ghostteam.findetect.domain.repository.BalanceAdjustmentRepository
import com.ghostteam.findetect.domain.repository.BalanceRepository
import com.ghostteam.findetect.domain.repository.CategoryRepository
import com.ghostteam.findetect.domain.repository.TransactionRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun accounts(implementation: LocalAccountRepository): AccountRepository

    @Binds
    @Singleton
    abstract fun categories(implementation: LocalCategoryRepository): CategoryRepository

    @Binds
    @Singleton
    abstract fun transactions(implementation: LocalTransactionRepository): TransactionRepository

    @Binds
    @Singleton
    abstract fun adjustments(implementation: LocalBalanceAdjustmentRepository): BalanceAdjustmentRepository

    @Binds
    @Singleton
    abstract fun balances(implementation: LocalBalanceRepository): BalanceRepository
}

