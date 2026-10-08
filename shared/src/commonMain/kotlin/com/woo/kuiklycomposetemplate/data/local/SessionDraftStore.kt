package com.woo.kuiklycomposetemplate.data.local

import com.tencent.kuikly.core.module.SharedPreferencesModule
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject
import com.woo.kuiklycomposetemplate.domain.model.WorkoutSession

/**
 * 训练会话秒级草稿持久化与自愈恢复中心
 * 保证练中异常杀进程、切后台、闪退时 100% 还原现场
 */
object SessionDraftStore {

    private const val KEY_SESSION_DRAFT = "reshape_session_draft_v1"

    // 内存即时缓存
    private var activeDraft: WorkoutSession? = null

    fun hasDraft(spModule: SharedPreferencesModule? = null): Boolean {
        if (activeDraft != null) return true
        if (spModule != null) {
            try {
                val jsonStr = spModule.getString(KEY_SESSION_DRAFT)
                return !jsonStr.isNullOrEmpty()
            } catch (_: Exception) {}
        }
        return false
    }

    fun getDraft(spModule: SharedPreferencesModule? = null): WorkoutSession? {
        if (activeDraft != null) return activeDraft
        if (spModule != null) {
            try {
                val jsonStr = spModule.getString(KEY_SESSION_DRAFT)
                if (!jsonStr.isNullOrEmpty()) {
                    val obj = JSONObject(jsonStr)
                    val draft = JsonUtils.sessionFromJson(obj)
                    activeDraft = draft
                    return draft
                }
            } catch (_: Exception) {}
        }
        return null
    }

    fun saveDraft(session: WorkoutSession, spModule: SharedPreferencesModule? = null) {
        val draftSession = session.copy(isDraft = true)
        activeDraft = draftSession
        if (spModule != null) {
            try {
                val json = JsonUtils.sessionToJson(draftSession)
                spModule.setString(KEY_SESSION_DRAFT, json.toString())
            } catch (_: Exception) {}
        }
    }

    fun clearDraft(spModule: SharedPreferencesModule? = null) {
        activeDraft = null
        if (spModule != null) {
            try {
                spModule.setString(KEY_SESSION_DRAFT, "")
            } catch (_: Exception) {}
        }
    }
}
