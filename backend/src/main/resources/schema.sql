CREATE TABLE IF NOT EXISTS users (
  id uuid PRIMARY KEY,
  email varchar(254) NOT NULL UNIQUE,
  name varchar(50) NOT NULL,
  password_hash text NOT NULL,
  revision bigint NOT NULL DEFAULT 0
);
CREATE TABLE IF NOT EXISTS sessions (
  access_hash char(64) PRIMARY KEY,
  refresh_hash char(64) NOT NULL UNIQUE,
  user_id uuid NOT NULL REFERENCES users ON DELETE CASCADE,
  access_until timestamptz NOT NULL,
  refresh_until timestamptz NOT NULL
);
CREATE INDEX IF NOT EXISTS sessions_user ON sessions(user_id);

CREATE TABLE IF NOT EXISTS categories (
  user_id uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  revision bigint NOT NULL,
  "id" varchar(256) NOT NULL,
  "key" varchar(256) NOT NULL,
  "nameRu" varchar(256) NOT NULL,
  "nameEn" varchar(256) NOT NULL,
  "iconId" varchar(256) NOT NULL,
  "isIncome" boolean NOT NULL,
  "isArchived" boolean NOT NULL,
  "legacyEmoji" varchar(256),
  PRIMARY KEY (user_id, "id")
);

CREATE TABLE IF NOT EXISTS accounts (
  user_id uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  revision bigint NOT NULL,
  "id" varchar(256) NOT NULL,
  "name" varchar(256) NOT NULL,
  "iconId" varchar(256) NOT NULL,
  "currency" varchar(256) NOT NULL CHECK (currency IN ('RUB','USD','EUR','CNY')),
  "initialBalanceMinor" bigint NOT NULL,
  "modifiedAt" timestamptz NOT NULL,
  "includeInForecast" boolean NOT NULL,
  "createdAt" timestamptz,
  "updatedAt" timestamptz,
  "deletedAt" timestamptz,
  "legacyEmoji" varchar(256),
  PRIMARY KEY (user_id, "id")
);

CREATE TABLE IF NOT EXISTS transactions (
  user_id uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  revision bigint NOT NULL,
  "id" varchar(256) NOT NULL,
  "accountId" varchar(256) NOT NULL,
  "categoryId" varchar(256) NOT NULL,
  "amountMinor" bigint NOT NULL CHECK ("amountMinor" > 0),
  "transactionDate" timestamptz NOT NULL,
  "enteredAt" timestamptz NOT NULL,
  "modifiedAt" timestamptz NOT NULL,
  "comment" varchar(2048),
  "createdAt" timestamptz,
  "updatedAt" timestamptz,
  "deletedAt" timestamptz,
  PRIMARY KEY (user_id, "id"),
  FOREIGN KEY (user_id,"accountId") REFERENCES accounts(user_id,id),
  FOREIGN KEY (user_id,"categoryId") REFERENCES categories(user_id,id)
);

CREATE TABLE IF NOT EXISTS balance_adjustments (
  user_id uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  revision bigint NOT NULL,
  "id" varchar(256) NOT NULL,
  "accountId" varchar(256) NOT NULL,
  "amountMinor" bigint NOT NULL,
  "occurredAt" timestamptz NOT NULL,
  "createdAt" timestamptz,
  PRIMARY KEY (user_id, "id"),
  FOREIGN KEY (user_id,"accountId") REFERENCES accounts(user_id,id)
);

CREATE TABLE IF NOT EXISTS balance_confirmations (
  user_id uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  revision bigint NOT NULL,
  "requestId" varchar(256) NOT NULL,
  "accountId" varchar(256) NOT NULL,
  "currency" varchar(256) NOT NULL CHECK (currency IN ('RUB','USD','EUR','CNY')),
  "actualMinor" bigint NOT NULL,
  "adjustmentId" varchar(256),
  PRIMARY KEY (user_id, "requestId"),
  FOREIGN KEY (user_id,"accountId") REFERENCES accounts(user_id,id)
);

CREATE TABLE IF NOT EXISTS activity_days (
  user_id uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  revision bigint NOT NULL,
  "date" date NOT NULL,
  "hadTransaction" boolean NOT NULL,
  "noSpendingConfirmed" boolean NOT NULL,
  "enteredAt" timestamptz NOT NULL,
  "modifiedAt" timestamptz NOT NULL,
  PRIMARY KEY (user_id, "date")
);

CREATE TABLE IF NOT EXISTS xp_state (
  user_id uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  revision bigint NOT NULL,
  "id" bigint NOT NULL CHECK (id=1),
  "startedAt" timestamptz NOT NULL,
  "legacyBestStreak" bigint NOT NULL CHECK ("legacyBestStreak" BETWEEN 0 AND 2147483647),
  PRIMARY KEY (user_id, "id")
);

CREATE TABLE IF NOT EXISTS achievement_state (
  user_id uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  revision bigint NOT NULL,
  "id" bigint NOT NULL CHECK (id=1),
  "startedAt" timestamptz NOT NULL,
  "startedOn" date NOT NULL,
  "zoneId" varchar(256) NOT NULL,
  "freshStartDate" date NOT NULL,
  PRIMARY KEY (user_id, "id")
);

CREATE TABLE IF NOT EXISTS achievement_transaction_facts (
  user_id uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  revision bigint NOT NULL,
  "transactionId" varchar(256) NOT NULL,
  "enteredOn" date NOT NULL,
  "transactionOn" date NOT NULL,
  "zoneId" varchar(256) NOT NULL,
  "enteredAt" timestamptz NOT NULL,
  PRIMARY KEY (user_id, "transactionId")
);

CREATE TABLE IF NOT EXISTS xp_events (
  user_id uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  revision bigint NOT NULL,
  "id" varchar(256) NOT NULL,
  "reason" varchar(256) NOT NULL CHECK (reason IN ('TRANSACTION','NO_SPENDING_MARK','STREAK_MILESTONE','ACHIEVEMENT')),
  "sourceId" varchar(256) NOT NULL,
  "reasonRef" varchar(256),
  "amount" bigint NOT NULL CHECK (amount BETWEEN 0 AND 500),
  "counted" boolean NOT NULL,
  "localDate" date NOT NULL,
  "occurredAt" timestamptz NOT NULL,
  "createdAt" timestamptz,
  PRIMARY KEY (user_id, "id"),
  UNIQUE (user_id, reason, "sourceId")
);

CREATE TABLE IF NOT EXISTS achievements (
  user_id uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  revision bigint NOT NULL,
  "code" varchar(256) NOT NULL CHECK (code IN ('FIRST_CLUE','STREAK_3','STREAK_7','STREAK_14','STREAK_30','STREAK_100','BACK_IN_ACTION','FRESH_CLUES','WITHIN_LIMIT','REACHED_INCOME','FORECASTER')),
  "earnedAt" timestamptz NOT NULL,
  "earnedOn" date NOT NULL,
  "isLegacy" boolean NOT NULL,
  "xpEventId" varchar(256),
  "createdAt" timestamptz,
  PRIMARY KEY (user_id, "code")
);

CREATE TABLE IF NOT EXISTS personal_settings (
  user_id uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  revision bigint NOT NULL,
  "id" bigint NOT NULL CHECK (id=1),
  "gameEnabled" boolean NOT NULL,
  "reminderEnabled" boolean NOT NULL,
  "nextIncomeDate" date,
  "incomeRepeat" varchar(12) CHECK ("incomeRepeat" IN ('MONTHLY','BIWEEKLY','WEEKLY','ONCE')),
  "reminderHour" bigint NOT NULL CHECK ("reminderHour" BETWEEN 18 AND 22),
  "modifiedAt" timestamptz NOT NULL,
  PRIMARY KEY (user_id, "id")
);
CREATE INDEX IF NOT EXISTS accounts_sync ON accounts(user_id,revision);
CREATE TABLE IF NOT EXISTS forecast_plans (
  user_id uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  revision bigint NOT NULL,
  currency text NOT NULL CHECK (currency IN ('RUB','USD','EUR','CNY')),
  "savingsMinor" bigint CHECK ("savingsMinor" BETWEEN 0 AND 99999999999),
  "paymentsMinor" bigint CHECK ("paymentsMinor" BETWEEN 0 AND 99999999999),
  "paymentDate" date,
  "manualDailyMinor" bigint CHECK ("manualDailyMinor" BETWEEN 0 AND 99999999999),
  "modifiedAt" timestamptz NOT NULL,
  CHECK (("paymentDate" IS NULL) = ("paymentsMinor" IS NULL)),
  PRIMARY KEY (user_id, currency)
);
CREATE TABLE IF NOT EXISTS forecast_days (
  user_id uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  revision bigint NOT NULL,
  id uuid NOT NULL,
  currency text NOT NULL CHECK (currency IN ('RUB','USD','EUR','CNY')),
  day date NOT NULL,
  stamp bigint NOT NULL,
  "accountIds" text NOT NULL,
  "fromUtc" timestamptz NOT NULL,
  "untilUtc" timestamptz NOT NULL CHECK ("untilUtc" > "fromUtc"),
  "limitMinor" bigint CHECK ("limitMinor" >= 0),
  PRIMARY KEY (user_id, id)
);
CREATE INDEX IF NOT EXISTS forecast_days_sync ON forecast_days(user_id,revision);

CREATE TABLE IF NOT EXISTS forecast_balances (
  user_id uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  revision bigint NOT NULL,
  id uuid NOT NULL,
  currency text NOT NULL CHECK (currency IN ('RUB','USD','EUR','CNY')),
  stamp bigint NOT NULL,
  "localDay" date NOT NULL,
  "targetDay" date CHECK ("targetDay" > "localDay"),
  rule text,
  "accountIds" text NOT NULL,
  "balanceMinor" bigint NOT NULL,
  PRIMARY KEY (user_id, id)
);
CREATE INDEX IF NOT EXISTS forecast_balances_sync ON forecast_balances(user_id,revision);
CREATE INDEX IF NOT EXISTS transactions_sync ON transactions(user_id,revision);
CREATE INDEX IF NOT EXISTS xp_events_sync ON xp_events(user_id,revision);
CREATE INDEX IF NOT EXISTS balance_adjustments_sync ON balance_adjustments(user_id,revision);
CREATE INDEX IF NOT EXISTS achievement_transaction_facts_sync ON achievement_transaction_facts(user_id,revision);
