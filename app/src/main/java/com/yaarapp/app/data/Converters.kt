package com.yaarapp.app.data

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromPlan(plan: Plan): String = plan.name

    @TypeConverter
    fun toPlan(value: String): Plan = Plan.valueOf(value)

    @TypeConverter
    fun fromCountry(country: Country): String = country.name

    @TypeConverter
    fun toCountry(value: String): Country = Country.valueOf(value)

    @TypeConverter
    fun fromSex(sex: Sex): String = sex.name

    @TypeConverter
    fun toSex(value: String): Sex = Sex.valueOf(value)
}
