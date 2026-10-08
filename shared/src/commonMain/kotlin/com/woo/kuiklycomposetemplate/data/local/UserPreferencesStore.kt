package com.woo.kuiklycomposetemplate.data.local

import com.tencent.kuikly.core.module.SharedPreferencesModule
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject
import com.woo.kuiklycomposetemplate.core.theme.ReshapeThemeId
import com.woo.kuiklycomposetemplate.core.theme.ThemeStateHolder
import com.woo.kuiklycomposetemplate.domain.model.Gender
import com.woo.kuiklycomposetemplate.domain.model.UserProfile

/**
 * 做组重量与完成次数历史推算记录 (三大项反推用)
 */
data class LiftEstimateHistory(
    val squatWeight: String = "80.0",
    val squatReps: String = "5",
    val benchWeight: String = "60.0",
    val benchReps: String = "5",
    val deadliftWeight: String = "105.0",
    val deadliftReps: String = "5"
)

/**
 * 用户配置与偏好设置存储
 */
object UserPreferencesStore {

    private const val KEY_PROFILE = "reshape_user_profile"
    private const val KEY_THEME = "reshape_theme_id"
    private const val KEY_ESTIMATE_HISTORY = "reshape_1rm_estimate_history"
    private const val KEY_ONBOARDING_COMPLETED = "reshape_onboarding_completed_v1"

    // 内存缓存层
    private var cachedProfile: UserProfile = UserProfile()
    private var cachedEstimateHistory: LiftEstimateHistory = LiftEstimateHistory()
    private var cachedOnboardingCompleted: Boolean? = null

    fun isOnboardingCompleted(spModule: SharedPreferencesModule? = null): Boolean {
        if (cachedOnboardingCompleted != null) return cachedOnboardingCompleted!!
        if (spModule != null) {
            try {
                val str = spModule.getString(KEY_ONBOARDING_COMPLETED)
                if (!str.isNullOrEmpty()) {
                    val completed = str == "true"
                    cachedOnboardingCompleted = completed
                    return completed
                }
            } catch (_: Exception) {}
        }
        return false
    }

    fun setOnboardingCompleted(completed: Boolean, spModule: SharedPreferencesModule? = null) {
        cachedOnboardingCompleted = completed
        if (spModule != null) {
            try {
                spModule.setString(KEY_ONBOARDING_COMPLETED, if (completed) "true" else "false")
            } catch (_: Exception) {}
        }
    }

    fun getProfile(spModule: SharedPreferencesModule? = null): UserProfile {
        if (spModule != null) {
            try {
                val jsonStr = spModule.getString(KEY_PROFILE)
                if (!jsonStr.isNullOrEmpty()) {
                    val obj = JSONObject(jsonStr)
                    cachedProfile = JsonUtils.profileFromJson(obj)
                }
            } catch (_: Exception) {
                // 忽略异常，降级使用内存缓存
            }
        }
        return cachedProfile
    }

    fun saveProfile(profile: UserProfile, spModule: SharedPreferencesModule? = null) {
        cachedProfile = profile
        if (spModule != null) {
            try {
                val json = JsonUtils.profileToJson(profile)
                spModule.setString(KEY_PROFILE, json.toString())
            } catch (_: Exception) {}
        }
    }

    fun getEstimateHistory(spModule: SharedPreferencesModule? = null): LiftEstimateHistory {
        if (spModule != null) {
            try {
                val jsonStr = spModule.getString(KEY_ESTIMATE_HISTORY)
                if (!jsonStr.isNullOrEmpty()) {
                    val obj = JSONObject(jsonStr)
                    cachedEstimateHistory = LiftEstimateHistory(
                        squatWeight = obj.optString("squatWeight", "80.0"),
                        squatReps = obj.optString("squatReps", "5"),
                        benchWeight = obj.optString("benchWeight", "60.0"),
                        benchReps = obj.optString("benchReps", "5"),
                        deadliftWeight = obj.optString("deadliftWeight", "105.0"),
                        deadliftReps = obj.optString("deadliftReps", "5")
                    )
                }
            } catch (_: Exception) {}
        }
        return cachedEstimateHistory
    }

    fun saveEstimateHistory(history: LiftEstimateHistory, spModule: SharedPreferencesModule? = null) {
        cachedEstimateHistory = history
        if (spModule != null) {
            try {
                val obj = JSONObject().apply {
                    put("squatWeight", history.squatWeight)
                    put("squatReps", history.squatReps)
                    put("benchWeight", history.benchWeight)
                    put("benchReps", history.benchReps)
                    put("deadliftWeight", history.deadliftWeight)
                    put("deadliftReps", history.deadliftReps)
                }
                spModule.setString(KEY_ESTIMATE_HISTORY, obj.toString())
            } catch (_: Exception) {}
        }
    }

    fun updateTheme(themeId: ReshapeThemeId, spModule: SharedPreferencesModule? = null) {
        ThemeStateHolder.switchTheme(themeId)
        cachedProfile = cachedProfile.copy(preferredThemeCode = themeId.name)
        if (spModule != null) {
            try {
                spModule.setString(KEY_THEME, themeId.name)
                saveProfile(cachedProfile, spModule)
            } catch (_: Exception) {}
        }
    }

    fun updateGender(gender: Gender, spModule: SharedPreferencesModule? = null) {
        cachedProfile = cachedProfile.copy(gender = gender)
        saveProfile(cachedProfile, spModule)
    }

    fun updateBigThree1Rm(
        bench: Float = cachedProfile.bench1RmKg,
        squat: Float = cachedProfile.squat1RmKg,
        deadlift: Float = cachedProfile.deadlift1RmKg,
        spModule: SharedPreferencesModule? = null
    ) {
        cachedProfile = cachedProfile.copy(
            bench1RmKg = bench,
            squat1RmKg = squat,
            deadlift1RmKg = deadlift
        )
        saveProfile(cachedProfile, spModule)
        try {
            PlanRepository.refreshSuggestedWeightsWithProfile(cachedProfile, spModule)
        } catch (_: Exception) {}
    }
}
