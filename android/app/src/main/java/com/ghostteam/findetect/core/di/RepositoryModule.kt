package com.ghostteam.findetect.core.di

import com.ghostteam.findetect.data.session.UserDataScopes
import com.ghostteam.findetect.data.session.UserDataScope
import com.ghostteam.findetect.domain.model.SessionIdentity
import com.ghostteam.findetect.domain.repository.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped

/** Each ViewModel captures one immutable user scope; account changes destroy its navigation owner. */
@Module
@InstallIn(ViewModelComponent::class)
object RepositoryModule {
    @Provides @ViewModelScoped fun userScope(scopes: UserDataScopes): UserDataScope = scopes.requireCurrent()
    @Provides fun identity(user: UserDataScope): SessionIdentity = user.identity
    @Provides fun accounts(user: UserDataScope): AccountRepository = user.accounts
    @Provides fun categories(user: UserDataScope): CategoryRepository = user.categories
    @Provides fun transactions(user: UserDataScope): TransactionRepository = user.transactions
    @Provides fun adjustments(user: UserDataScope): BalanceAdjustmentRepository = user.adjustments
    @Provides fun balances(user: UserDataScope): BalanceRepository = user.balances
    @Provides fun activity(user: UserDataScope): ActivityRepository = user.activity
    @Provides fun xp(user: UserDataScope): XpRepository = user.xp
    @Provides fun settings(user: UserDataScope): SettingsRepository = user.settings
    @Provides fun pin(user: UserDataScope): PinRepository = user.pin
}
