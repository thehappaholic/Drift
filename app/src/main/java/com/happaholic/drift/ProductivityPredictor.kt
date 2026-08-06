package com.happaholic.drift

import android.content.Context
import org.json.JSONObject
import kotlin.math.roundToInt

/** Inputs expected by the Kaggle productivity prototype. Values use daily units. */
data class ProductivityInputs(
    val gender: String,
    val age: Double,
    val studyHoursPerDay: Double,
    val sleepHours: Double,
    val phoneUsageHours: Double,
    val socialMediaHours: Double,
    val youtubeHours: Double,
    val gamingHours: Double,
    val breaksPerDay: Double,
    val coffeeIntakeMg: Double,
    val exerciseMinutes: Double,
    val assignmentsCompleted: Double,
    val attendancePercentage: Double,
    val stressLevel: Double,
    val focusScore: Double
) {
    fun numericValues(): Map<String, Double> = mapOf(
        "age" to age,
        "study_hours_per_day" to studyHoursPerDay,
        "sleep_hours" to sleepHours,
        "phone_usage_hours" to phoneUsageHours,
        "social_media_hours" to socialMediaHours,
        "youtube_hours" to youtubeHours,
        "gaming_hours" to gamingHours,
        "breaks_per_day" to breaksPerDay,
        "coffee_intake_mg" to coffeeIntakeMg,
        "exercise_minutes" to exerciseMinutes,
        "assignments_completed" to assignmentsCompleted,
        "attendance_percentage" to attendancePercentage,
        "stress_level" to stressLevel,
        "focus_score" to focusScore
    )
}

data class ProductivityPrediction(
    val score: Double,
    val modelName: String,
    val notice: String
) {
    val roundedScore: Int get() = score.roundToInt().coerceIn(0, 100)
}

/** Executes the exact fitted Ridge preprocessing and coefficients exported by Python. */
class ProductivityPredictor private constructor(private val spec: ModelSpec) {
    fun predict(inputs: ProductivityInputs): ProductivityPrediction {
        val values = inputs.numericValues()
        var result = spec.intercept
        spec.numericFeatures.forEachIndexed { index, feature ->
            val raw = values[feature] ?: spec.numericMedians[index]
            val safeValue = if (raw.isFinite()) raw else spec.numericMedians[index]
            val standardized = (safeValue - spec.numericMeans[index]) / spec.numericScales[index]
            result += standardized * spec.numericCoefficients[index]
        }
        val categoryIndex = spec.categoricalValues.indexOfFirst {
            it.equals(inputs.gender, ignoreCase = true)
        }
        if (categoryIndex >= 0) result += spec.categoricalCoefficients[categoryIndex]

        return ProductivityPrediction(
            score = result.coerceIn(spec.scoreMin, spec.scoreMax),
            modelName = spec.modelName,
            notice = spec.notice
        )
    }

    companion object {
        fun fromAssets(context: Context): ProductivityPredictor {
            val json = context.assets.open("productivity_model.json")
                .bufferedReader()
                .use { it.readText() }
            return ProductivityPredictor(ModelSpec.fromJson(JSONObject(json)))
        }
    }
}

private data class ModelSpec(
    val modelName: String,
    val scoreMin: Double,
    val scoreMax: Double,
    val intercept: Double,
    val numericFeatures: List<String>,
    val numericMedians: List<Double>,
    val numericMeans: List<Double>,
    val numericScales: List<Double>,
    val numericCoefficients: List<Double>,
    val categoricalValues: List<String>,
    val categoricalCoefficients: List<Double>,
    val notice: String
) {
    companion object {
        fun fromJson(json: JSONObject): ModelSpec = ModelSpec(
            modelName = json.getString("model_name"),
            scoreMin = json.getDouble("score_min"),
            scoreMax = json.getDouble("score_max"),
            intercept = json.getDouble("intercept"),
            numericFeatures = json.getJSONArray("numeric_features").strings(),
            numericMedians = json.getJSONArray("numeric_medians").doubles(),
            numericMeans = json.getJSONArray("numeric_means").doubles(),
            numericScales = json.getJSONArray("numeric_scales").doubles(),
            numericCoefficients = json.getJSONArray("numeric_coefficients").doubles(),
            categoricalValues = json.getJSONArray("categorical_values").strings(),
            categoricalCoefficients = json.getJSONArray("categorical_coefficients").doubles(),
            notice = json.getString("prototype_notice")
        )
    }
}

private fun org.json.JSONArray.strings(): List<String> =
    (0 until length()).map(::getString)

private fun org.json.JSONArray.doubles(): List<Double> =
    (0 until length()).map(::getDouble)

fun ageRangeMidpoint(ageRange: String): Double {
    val numbers = Regex("\\d+").findAll(ageRange).map { it.value.toDouble() }.toList()
    return when {
        numbers.size >= 2 -> (numbers[0] + numbers[1]) / 2.0
        numbers.size == 1 -> numbers[0]
        else -> 23.0
    }
}

