package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ApiFlowApp
import com.example.core.breakpoint.ActiveBreakpoint
import com.example.core.breakpoint.BreakpointDecision
import com.example.core.breakpoint.BreakpointManager
import com.example.core.model.BreakpointRule
import com.example.core.model.RewriteRule
import com.example.core.repository.RulesRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RulesViewModel(
    private val repository: RulesRepository = ApiFlowApp.instance.rulesRepository
) : ViewModel() {

    val rewriteRules: StateFlow<List<RewriteRule>> = repository.allRewriteRules
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val breakpointRules: StateFlow<List<BreakpointRule>> = repository.allBreakpointRules
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeBreakpoints: StateFlow<List<ActiveBreakpoint>> = BreakpointManager.activeBreakpoints

    fun toggleRewriteRule(rule: RewriteRule) {
        viewModelScope.launch {
            repository.saveRewriteRule(rule.copy(isEnabled = !rule.isEnabled))
        }
    }

    fun saveRewriteRule(rule: RewriteRule) {
        viewModelScope.launch {
            repository.saveRewriteRule(rule)
        }
    }

    fun deleteRewriteRule(rule: RewriteRule) {
        viewModelScope.launch {
            repository.deleteRewriteRule(rule)
        }
    }

    fun toggleBreakpointRule(rule: BreakpointRule) {
        viewModelScope.launch {
            repository.saveBreakpointRule(rule.copy(isEnabled = !rule.isEnabled))
        }
    }

    fun saveBreakpointRule(rule: BreakpointRule) {
        viewModelScope.launch {
            repository.saveBreakpointRule(rule)
        }
    }

    fun deleteBreakpointRule(rule: BreakpointRule) {
        viewModelScope.launch {
            repository.deleteBreakpointRule(rule)
        }
    }

    fun resumeBreakpoint(id: String, decision: BreakpointDecision) {
        BreakpointManager.resume(id, decision)
    }

    fun dropAllBreakpoints() {
        BreakpointManager.dropAll()
    }
}
