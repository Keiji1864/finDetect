package com.ghostteam.findetect.data.local

import com.ghostteam.findetect.data.local.entity.CategoryEntity

internal val defaultCategories = listOf(
    CategoryEntity("10000000-0000-4000-8000-000000000001","salary","Зарплата","Salary","cash",true),
    CategoryEntity("10000000-0000-4000-8000-000000000002","groceries","Продукты","Groceries","basket",false),
    CategoryEntity("10000000-0000-4000-8000-000000000003","coffee","Кофе","Coffee","coffee",false),
    CategoryEntity("10000000-0000-4000-8000-000000000004","other_income","Другие доходы","Other income","cash",true),
    CategoryEntity("10000000-0000-4000-8000-000000000005","other_expense","Другие расходы","Other expenses","wallet",false),
)
