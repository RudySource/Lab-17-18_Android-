package com.rudy.weatherdashboard.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rudy.weatherdashboard.data.WeatherData
import com.rudy.weatherdashboard.data.WeatherRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class WeatherViewModel : ViewModel() {
    private val repository = WeatherRepository()
    private  val _weatherState = MutableStateFlow(WeatherData())

    val weatherState: StateFlow<WeatherData> = _weatherState.asStateFlow()

    init {
        loadWeatherData()
    }

    fun loadWeatherData() {
        viewModelScope.launch {
            _weatherState.value = _weatherState.value.copy(
                isLoading = true,
                error = null,
                loadingProgress = "Запуск программы..."
            )
            try {
                coroutineScope {
                    _weatherState.value = _weatherState.value.copy(
                        loadingProgress =  "Загружаем температуру, влажность, скорость ветра..."
                    )// Создаём scope, который НЕ отменяет родителя при ошибке ←
                    val tempDeferred = async { repository.fetchTemperature() }
                    val humDeferred = async { repository.fetchHumidity() }
                    val windDeferred = async { repository.fetchWindSpeed() }
                    val temperature = tempDeferred.await()
                    val humidity = humDeferred.await()
                    val windSpeed = windDeferred.await()

                    _weatherState.value = _weatherState.value.copy(
                        loadingProgress = "Вычисление индекса погоды..."
                    )

                    val weatherIndex = repository.calculateWeatherIndex(
                        temperature,
                        humidity,
                        windSpeed
                    )

                    _weatherState.value = WeatherData(
                        temperature = temperature,
                        humidity = humidity,
                        windSpeed = windSpeed,
                        weatherIndex = weatherIndex,
                        isLoading = false,
                        error = null,
                        loadingProgress = "Загрузка завершена!"
                    )
                }
            } catch (e: Exception) {
                _weatherState.value = _weatherState.value.copy(
                    isLoading = false,
                    error = "Ошибка загрузки: ${e.message}",
                    loadingProgress = ""
                )
            }
        }
    }

    fun toggleErrorSimulation(){
        repository.toggleErrorSimulation()
    }
}