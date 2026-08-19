package com.yaarapp.app.data

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromPlan(plan: Plan): String = plan.name

    @TypeConverter
    fun toPlan(value: String): Plan = Plan.valueOf(value)
}
