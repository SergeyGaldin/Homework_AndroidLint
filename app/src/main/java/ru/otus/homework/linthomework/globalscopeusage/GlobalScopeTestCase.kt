@file:OptIn(DelicateCoroutinesApi::class, ObsoleteCoroutinesApi::class)

package ru.otus.homework.linthomework.globalscopeusage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class GlobalScopeTestCaseViewModel(private val scope: CoroutineScope) : ViewModel() {

    fun case1() {
        GlobalScope.launch {
            delay(1000)
            println("Hello World")
        }
        GlobalScope.actor<String> {
            delay(1000)
            println("Hello World")
        }
    }

    fun case2() {
        viewModelScope.launch {
            val deferred = GlobalScope.async {
                delay(1000)
                "Hello World"
            }
            println(deferred.await())
        }
    }

    fun case3() {
        scope.launch {
            delay(1000)
            println("Hello World")
        }
    }
}