package com.example.core.repository

import com.example.core.database.RulesDao
import com.example.core.model.BreakpointRule
import com.example.core.model.RewriteRule
import kotlinx.coroutines.flow.Flow

class RulesRepository(private val rulesDao: RulesDao) {
    val allRewriteRules: Flow<List<RewriteRule>> = rulesDao.getAllRewriteRulesFlow()
    val allBreakpointRules: Flow<List<BreakpointRule>> = rulesDao.getAllBreakpointRulesFlow()

    suspend fun getActiveRewriteRules(): List<RewriteRule> = rulesDao.getActiveRewriteRules()

    suspend fun getActiveBreakpointRules(): List<BreakpointRule> = rulesDao.getActiveBreakpointRules()

    suspend fun saveRewriteRule(rule: RewriteRule) {
        if (rule.id == 0L) {
            rulesDao.insertRewriteRule(rule)
        } else {
            rulesDao.updateRewriteRule(rule)
        }
    }

    suspend fun deleteRewriteRule(rule: RewriteRule) = rulesDao.deleteRewriteRule(rule)

    suspend fun saveBreakpointRule(rule: BreakpointRule) {
        if (rule.id == 0L) {
            rulesDao.insertBreakpointRule(rule)
        } else {
            rulesDao.updateBreakpointRule(rule)
        }
    }

    suspend fun deleteBreakpointRule(rule: BreakpointRule) = rulesDao.deleteBreakpointRule(rule)
}
