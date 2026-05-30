package com.geovision.mobile.core

import org.junit.Assert.*
import org.junit.Test

class UiStateTest {

    @Test
    fun loading_equality() {
        assertEquals(UiState.Loading, UiState.Loading)
    }

    @Test
    fun success_dataAccess() {
        val success = UiState.Success("data")
        assertEquals("data", success.data)
    }

    @Test
    fun error_messageAccess() {
        val error = UiState.Error("error message")
        assertEquals("error message", error.message)
        assertNull(error.throwable)
    }

    @Test
    fun error_withThrowable() {
        val cause = RuntimeException("root cause")
        val error = UiState.Error("error", cause)
        assertEquals("error", error.message)
        assertEquals(cause, error.throwable)
    }

    @Test
    fun map_loadingToLoading() {
        val result: UiState<Int> = UiState.Loading
        val mapped = result.map { it.toString() }
        assertTrue(mapped is UiState.Loading)
    }

    @Test
    fun map_successToSuccess() {
        val result: UiState<Int> = UiState.Success(42)
        val mapped = result.map { it * 2 }
        assertTrue(mapped is UiState.Success)
        assertEquals(84, (mapped as UiState.Success).data)
    }

    @Test
    fun map_success_transform() {
        val result = UiState.Success(listOf(1, 2, 3))
        val mapped = result.map { it.size }
        assertEquals(3, (mapped as UiState.Success).data)
    }

    @Test
    fun map_errorPreserved() {
        val error: UiState<Int> = UiState.Error("err")
        val mapped = error.map { it * 2 }
        assertTrue(mapped is UiState.Error)
        assertEquals("err", (mapped as UiState.Error).message)
    }

    @Test
    fun onSuccess_calledOnSuccess() {
        var called = false
        val success = UiState.Success("hello")
        success.onSuccess { called = true }
        assertTrue(called)
    }

    @Test
    fun onSuccess_notCalledOnLoading() {
        var called = false
        val loading: UiState<String> = UiState.Loading
        loading.onSuccess { called = true }
        assertFalse(called)
    }

    @Test
    fun onSuccess_notCalledOnError() {
        var called = false
        val error: UiState<String> = UiState.Error("err")
        error.onSuccess { called = true }
        assertFalse(called)
    }

    @Test
    fun onError_calledOnError() {
        var called = false
        val error = UiState.Error("oops")
        error.onError { _, _ -> called = true }
        assertTrue(called)
    }

    @Test
    fun onError_notCalledOnSuccess() {
        var called = false
        UiState.Success("ok").onError { _, _ -> called = true }
        assertFalse(called)
    }

    @Test
    fun onError_notCalledOnLoading() {
        var called = false
        val loading: UiState<String> = UiState.Loading
        loading.onError { _, _ -> called = true }
        assertFalse(called)
    }

    @Test
    fun onError_passesMessage() {
        var message = ""
        UiState.Error("test msg").onError { msg, _ -> message = msg }
        assertEquals("test msg", message)
    }
}
