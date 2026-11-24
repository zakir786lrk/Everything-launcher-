@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package rikazzz.everythinglauncher.ui.settings

import rikazzz.everythinglauncher.ui.theme.LocalFontShadowEnabled
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ListItemDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import rikazzz.everythinglauncher.R
import rikazzz.everythinglauncher.data.AgendaAppearance
import rikazzz.everythinglauncher.data.weather.WeatherCondition
import rikazzz.everythinglauncher.data.weather.WeatherHour
import rikazzz.everythinglauncher.data.weather.WeatherLocation
import rikazzz.everythinglauncher.data.weather.WeatherMeasure
import rikazzz.everythinglauncher.data.weather.WeatherPrecipitation
import rikazzz.everythinglauncher.data.weather.WeatherSnapshot
import rikazzz.everythinglauncher.data.weather.WeatherTemperature
import rikazzz.everythinglauncher.data.weather.WeatherWind
import rikazzz.everythinglauncher.ui.weather.HourlyWeather
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit

@Composable
internal fun AgendaHourlyWeatherPreview(snapshot: WeatherSnapshot?, appearance: AgendaAppearance, modifier: Modifier = Modifier) {
    // Keep preview data stable while sliders and ordering update; no provider reads or clock timer.
    val now = remember(snapshot) { Instant.now() }
    val zone = ZoneId.systemDefault()
    val location = stringResource(R.string.clock_weather_preview_location)
    val sample = remember(now, zone, location) { hourlyWeatherSample(now, zone, location) }
    // Never substitute another city's data when a real source is available.
    val weather = snapshot ?: sample
    Box(modifier.testTag("agenda_hourly_preview")
        .clip(ListItemDefaults.segmentedShapes(0, 1).shape).background(MaterialTheme.colorScheme.surfaceContainer)) {
        // Keep the source and expand/series controls reachable even with many text rows enabled.
        androidx.compose.runtime.CompositionLocalProvider(LocalFontShadowEnabled provides false) {
            HourlyWeather(weather, now, appearance = appearance, constrainContentHeight = true)
        }
    }
}

private fun hourlyWeatherSample(now: Instant, zone: ZoneId, location: String): WeatherSnapshot {
    val temperatures = listOf(15.0, 18.0, 22.0, 25.0, 27.0, 24.0, 21.0, 18.0)
    val humidity = listOf(76.0, 70.0, 62.0, 54.0, 50.0, 58.0, 64.0, 72.0)
    val airQuality = listOf(18.0, 30.0, 55.0, 80.0, 110.0, 65.0, 40.0, 25.0)
    val start = now.truncatedTo(ChronoUnit.HOURS).plusSeconds(3600)
    return WeatherSnapshot(
        location = WeatherLocation("hourly_preview", location, zone),
        current = null,
        hourly = temperatures.mapIndexed { index, temperature ->
            WeatherHour(
                at = start.plusSeconds(index * 3600L),
                temperature = WeatherTemperature(temperature, "c"),
                condition = if (index < 4) WeatherCondition.PartlyCloudy else WeatherCondition.Cloudy,
                isDaylight = start.plusSeconds(index * 3600L).atZone(zone).hour in 6..18,
                description = null,
                feelsLike = WeatherTemperature(temperature - 2, "c"),
                humidity = WeatherMeasure(humidity[index], "percent"),
                wind = WeatherWind(90.0 + index * 20, WeatherMeasure(2.0 + index * .4, "mps"),
                    WeatherMeasure(4.0 + index * .5, "mps")),
                airQuality = WeatherMeasure(airQuality[index], "aqi"),
                uv = WeatherMeasure((7 - index).toDouble(), "uvi"),
                precipitationProbability = WeatherPrecipitation(
                    total = WeatherMeasure(20.0 + index * 5, "percent"),
                    rain = WeatherMeasure(20.0 + index * 5, "percent"),
                    snow = WeatherMeasure(0.0, "percent"),
                    ice = WeatherMeasure(0.0, "percent"),
                    thunderstorm = WeatherMeasure(5.0 + index * 2, "percent"),
                ),
                precipitation = WeatherPrecipitation(
                    total = WeatherMeasure(index * .2, "mm"),
                    rain = WeatherMeasure(index * .2, "mm"),
                    snow = WeatherMeasure(0.0, "mm"),
                    ice = WeatherMeasure(0.0, "mm"),
                    thunderstorm = WeatherMeasure(0.0, "mm"),
                ),
                dewPoint = WeatherTemperature(temperature - 5, "c"),
                pressure = WeatherMeasure(1013.0 + index, "hpa"),
                cloudCover = WeatherMeasure(30.0 + index * 5, "percent"),
                visibility = WeatherMeasure(12.0 - index * .5, "km"),
            )
        },
        daily = emptyList(),
        updatedAt = now,
        attribution = "",
    )
}
