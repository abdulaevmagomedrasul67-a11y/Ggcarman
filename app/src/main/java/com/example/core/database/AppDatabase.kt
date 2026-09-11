package com.example.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.core.model.BreakpointRule
import com.example.core.model.RewriteRule
import com.example.core.model.RuleActionType
import com.example.core.model.TrafficEntry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [TrafficEntry::class, RewriteRule::class, BreakpointRule::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun trafficDao(): TrafficDao
    abstract fun rulesDao(): RulesDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "api_flow_inspector.db"
                ).addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // Seed default preset rules for development testing
                        CoroutineScope(Dispatchers.IO).launch {
                            val rulesDao = getInstance(context).rulesDao()
                            seedDefaultRules(rulesDao)
                        }
                    }
                }).build()
                INSTANCE = instance
                instance
            }
        }

        private suspend fun seedDefaultRules(dao: RulesDao) {
            dao.insertRewriteRule(
                RewriteRule(
                    name = "Mock /api/v1/auth/status",
                    isEnabled = false,
                    targetHostPattern = "*",
                    targetPathPattern = "*/auth/status*",
                    targetMethod = "GET",
                    actionType = RuleActionType.MOCK_RESPONSE,
                    mockStatusCode = 200,
                    mockContentType = "application/json",
                    mockResponseBody = "{\n  \"authenticated\": true,\n  \"userId\": \"mock_admin_777\",\n  \"role\": \"SECURITY_AUDITOR\"\n}"
                )
            )
            dao.insertRewriteRule(
                RewriteRule(
                    name = "Inject Custom Auth Token Header",
                    isEnabled = false,
                    targetHostPattern = "*",
                    targetPathPattern = "*",
                    targetMethod = "ALL",
                    actionType = RuleActionType.REPLACE_HEADER,
                    targetHeaderKey = "X-Audit-Inspector-Token",
                    targetHeaderValue = "api_flow_debug_bearer_xyz999"
                )
            )
            dao.insertBreakpointRule(
                BreakpointRule(
                    name = "Pause on /v1/checkout",
                    isEnabled = false,
                    urlPattern = ".*/checkout.*",
                    method = "POST",
                    pauseOnRequest = true,
                    pauseOnResponse = false
                )
            )
        }
    }
}
