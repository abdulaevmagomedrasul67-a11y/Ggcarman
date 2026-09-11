package com.example.core.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.core.model.BreakpointRule
import com.example.core.model.RewriteRule
import kotlinx.coroutines.flow.Flow

@Dao
interface RulesDao {
    // Rewrite Rules
    @Query("SELECT * FROM rewrite_rules ORDER BY id DESC")
    fun getAllRewriteRulesFlow(): Flow<List<RewriteRule>>

    @Query("SELECT * FROM rewrite_rules WHERE isEnabled = 1")
    suspend fun getActiveRewriteRules(): List<RewriteRule>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRewriteRule(rule: RewriteRule): Long

    @Update
    suspend fun updateRewriteRule(rule: RewriteRule)

    @Delete
    suspend fun deleteRewriteRule(rule: RewriteRule)

    @Query("DELETE FROM rewrite_rules WHERE id = :id")
    suspend fun deleteRewriteRuleById(id: Long)

    // Breakpoint Rules
    @Query("SELECT * FROM breakpoint_rules ORDER BY id DESC")
    fun getAllBreakpointRulesFlow(): Flow<List<BreakpointRule>>

    @Query("SELECT * FROM breakpoint_rules WHERE isEnabled = 1")
    suspend fun getActiveBreakpointRules(): List<BreakpointRule>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBreakpointRule(rule: BreakpointRule): Long

    @Update
    suspend fun updateBreakpointRule(rule: BreakpointRule)

    @Delete
    suspend fun deleteBreakpointRule(rule: BreakpointRule)

    @Query("DELETE FROM breakpoint_rules WHERE id = :id")
    suspend fun deleteBreakpointRuleById(id: Long)
}
