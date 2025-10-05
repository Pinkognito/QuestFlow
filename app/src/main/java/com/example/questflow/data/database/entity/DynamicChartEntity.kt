package com.example.questflow.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "dynamic_charts")
data class DynamicChartEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val chartType: String,           // ChartType enum as string
    val dataSource: String,          // DataSource enum as string
    val xAxisField: String,          // Field ID
    val yAxisField: String?,         // Field ID (nullable for pie charts)
    val yAxisAggregation: String,    // AggregationFunction enum as string
    val groupingType: String?,       // GroupingType enum as string
    val dateInterval: String?,       // DateInterval enum as string
    val filters: String?,            // JSON array of filters
    val timeRangeType: String? = null, // TimeRangeType enum as string
    val sortBy: String?,             // Field ID
    val sortDirection: String,       // SortDirection enum as string
    val showLegend: Boolean = true,
    val showValues: Boolean = true,
    val showAxisLabels: Boolean = true,
    val colorScheme: String? = null,
    val categoryId: Long? = null,    // Category-specific chart
    val isTemplate: Boolean = false, // Save as template
    val position: Int = 0,
    val height: Int = 300,           // Height in dp
    val isVisible: Boolean = true
)
