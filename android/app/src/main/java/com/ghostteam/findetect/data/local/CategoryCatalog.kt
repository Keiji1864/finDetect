package com.ghostteam.findetect.data.local

import com.ghostteam.findetect.data.local.entity.CategoryEntity
import com.ghostteam.findetect.domain.model.Category
import com.ghostteam.findetect.domain.model.CategoryNames
import com.ghostteam.findetect.domain.model.FinanceIcon

internal val defaultCategories = listOf(
    // Expenses.
    category(2, "groceries", "Продукты", "Groceries", FinanceIcon.BASKET),
    category(3, "coffee", "Кафе и рестораны", "Cafes and restaurants", FinanceIcon.COFFEE),
    category(6, "marketplaces", "Маркетплейсы", "Marketplaces", FinanceIcon.BASKET),
    category(7, "clothing_and_shoes", "Одежда и обувь", "Clothing and shoes", FinanceIcon.BASKET),
    category(8, "electronics", "Техника и электроника", "Electronics", FinanceIcon.GEAR),
    category(9, "housing_and_utilities", "Жильё и ЖКХ", "Housing and utilities", FinanceIcon.RECEIPT),
    category(10, "household_goods", "Товары для дома", "Household goods", FinanceIcon.BASKET),
    category(11, "repairs", "Ремонт", "Repairs", FinanceIcon.GEAR),
    category(12, "transport", "Транспорт и такси", "Transport and taxis", FinanceIcon.BUS),
    category(13, "car", "Автомобиль", "Car", FinanceIcon.BUS),
    category(14, "health", "Здоровье и аптеки", "Health and pharmacy", FinanceIcon.PLUS),
    category(15, "beauty_and_care", "Красота и уход", "Beauty and personal care", FinanceIcon.SPARK),
    category(16, "sports", "Спорт", "Sports", FinanceIcon.TROPHY),
    category(17, "phone_and_internet", "Связь и интернет", "Phone and internet", FinanceIcon.GEAR),
    category(18, "subscriptions", "Подписки", "Subscriptions", FinanceIcon.CALENDAR),
    category(19, "entertainment", "Развлечения", "Entertainment", FinanceIcon.SPARK),
    category(20, "education", "Образование", "Education", FinanceIcon.CHECK),
    category(21, "children", "Дети", "Children", FinanceIcon.USER),
    category(22, "pets", "Питомцы", "Pets", FinanceIcon.BASKET),
    category(23, "travel", "Путешествия", "Travel", FinanceIcon.BUS),
    category(24, "gifts", "Подарки", "Gifts", FinanceIcon.SPARK),
    category(25, "outgoing_transfers", "Переводы людям", "Transfers to others", FinanceIcon.CASH),
    category(26, "government_payments_and_taxes", "Госплатежи и налоги", "Government payments and taxes", FinanceIcon.RECEIPT),
    category(27, "bank_fees", "Банковские комиссии", "Bank fees", FinanceIcon.RECEIPT),
    category(28, "charity", "Благотворительность", "Charitable donations", FinanceIcon.CASH),
    category(5, "other_expense", "Другое", "Other", FinanceIcon.WALLET),
    // Income.
    category(1, "salary", "Зарплата", "Salary", FinanceIcon.CASH, isIncome = true),
    category(29, "bonuses", "Премии и бонусы", "Bonuses", FinanceIcon.TROPHY, isIncome = true),
    category(30, "freelance_and_side_jobs", "Подработка и фриланс", "Side jobs and freelance work", FinanceIcon.CASH, isIncome = true),
    category(31, "incoming_transfers", "Переводы от людей", "Transfers from others", FinanceIcon.CASH, isIncome = true),
    category(32, "cash_deposits", "Внесение наличных", "Cash deposits", FinanceIcon.CASH, isIncome = true),
    category(33, "purchase_refunds", "Возвраты за покупки", "Purchase refunds", FinanceIcon.RECEIPT, isIncome = true),
    category(34, "cashback", "Кешбэк", "Cashback", FinanceIcon.WALLET, isIncome = true),
    category(35, "deposit_interest", "Проценты по вкладам", "Interest on deposits", FinanceIcon.CHART, isIncome = true),
    category(36, "dividends", "Дивиденды", "Dividends", FinanceIcon.CHART, isIncome = true),
    category(37, "sale_of_items", "Продажа вещей", "Sales of personal items", FinanceIcon.BASKET, isIncome = true),
    category(38, "rental_income", "Доход от аренды", "Rental income", FinanceIcon.CASH, isIncome = true),
    category(39, "scholarships", "Стипендии", "Scholarships", FinanceIcon.CASH, isIncome = true),
    category(40, "pensions_and_benefits", "Пенсии и пособия", "Pensions and benefits", FinanceIcon.CASH, isIncome = true),
    category(4, "other_income", "Другое", "Other", FinanceIcon.CASH, isIncome = true),
)

private fun category(
    number: Int,
    key: String,
    nameRu: String,
    nameEn: String,
    icon: FinanceIcon,
    isIncome: Boolean = false,
): CategoryEntity = CategoryEntity(categoryId(number), key, nameRu, nameEn, icon.key, isIncome)

private fun categoryId(number: Int): String =
    "10000000-0000-4000-8000-" + number.toString().padStart(12, '0')

private val defaultCategoriesById = defaultCategories.associateBy { it.id }
private val previousCategoryNames = mapOf(
    categoryId(3) to CategoryNames("Кофе", "Coffee"),
    categoryId(4) to CategoryNames("Другие доходы", "Other income"),
    categoryId(5) to CategoryNames("Другие расходы", "Other expenses"),
)

/** Older local databases and sync snapshots can still carry the original system labels. */
internal fun Category.withUpdatedSystemNames(): Category {
    val previous = previousCategoryNames[id] ?: return this
    val current = defaultCategoriesById.getValue(id)
    val legacyKey = key == "legacy:$id"
    if (isIncome != current.isIncome || (key != current.key && !legacyKey)) return this
    val legacyName = legacyKey && names.ru == names.en && (names.ru == previous.ru || names.ru == previous.en)
    return if (names == previous || legacyName) copy(names = CategoryNames(current.nameRu, current.nameEn)) else this
}
