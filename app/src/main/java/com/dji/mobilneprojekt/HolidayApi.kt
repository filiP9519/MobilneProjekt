package com.dji.mobilneprojekt

import retrofit2.http.GET
import retrofit2.http.Path

interface HolidayApi {
    // URL: https://date.nager.at/api/v3/PublicHolidays/2025/SK
    @GET("api/v3/PublicHolidays/{year}/{countryCode}")
    suspend fun getHolidays(
        @Path("year") year: Int,
        @Path("countryCode") countryCode: String
    ): List<Holiday>

}