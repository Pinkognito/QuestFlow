package com.example.questflow.domain.usecase

import com.example.questflow.data.database.entity.StatisticsConfigEntity
import com.example.questflow.data.repository.StatisticsRepository
import com.example.questflow.domain.model.*
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GetTaskStatisticsUseCase @Inject constructor(
    private val repository: StatisticsRepository
) {
    suspend operator fun invoke(filter: StatisticsFilter): TaskStatistics {
        return repository.getTaskStatistics(filter)
    }
}

@Singleton
class GetXpStatisticsUseCase @Inject constructor(
    private val repository: StatisticsRepository
) {
    suspend operator fun invoke(filter: StatisticsFilter): XpStatistics {
        return repository.getXpStatistics(filter)
    }
}

@Singleton
class GetProductivityStatisticsUseCase @Inject constructor(
    private val repository: StatisticsRepository
) {
    suspend operator fun invoke(filter: StatisticsFilter): ProductivityStats {
        return repository.getProductivityStatistics(filter)
    }
}

@Singleton
class GetCategoryStatisticsUseCase @Inject constructor(
    private val repository: StatisticsRepository
) {
    suspend operator fun invoke(filter: StatisticsFilter): CategoryStats {
        return repository.getCategoryStatistics(filter)
    }
}

@Singleton
class GetDifficultyDistributionUseCase @Inject constructor(
    private val repository: StatisticsRepository
) {
    suspend operator fun invoke(filter: StatisticsFilter): Map<Int, DifficultyDistribution> {
        return repository.getDifficultyDistribution(filter)
    }
}

@Singleton
class GetPriorityDistributionUseCase @Inject constructor(
    private val repository: StatisticsRepository
) {
    suspend operator fun invoke(filter: StatisticsFilter): Map<String, PriorityDistribution> {
        return repository.getPriorityDistribution(filter)
    }
}

@Singleton
class GetXpTrendDataUseCase @Inject constructor(
    private val repository: StatisticsRepository
) {
    suspend operator fun invoke(filter: StatisticsFilter): List<ChartDataPoint> {
        return repository.getXpTrendData(filter)
    }
}

@Singleton
class GetTaskCompletionTrendDataUseCase @Inject constructor(
    private val repository: StatisticsRepository
) {
    suspend operator fun invoke(filter: StatisticsFilter): List<ChartDataPoint> {
        return repository.getTaskCompletionTrendData(filter)
    }
}

@Singleton
class GetStatisticsConfigUseCase @Inject constructor(
    private val repository: StatisticsRepository
) {
    operator fun invoke(): Flow<StatisticsConfigEntity?> {
        return repository.getConfig()
    }

    suspend fun getOnce(): StatisticsConfigEntity? {
        return repository.getConfigOnce()
    }
}

@Singleton
class SaveStatisticsConfigUseCase @Inject constructor(
    private val repository: StatisticsRepository
) {
    suspend operator fun invoke(config: StatisticsConfigEntity) {
        repository.saveConfig(config)
    }
}
