package com.leapfinance.infopulse.collector.exception

import com.leapfinance.infopulse.collector.dto.ApiResponse
import mu.KotlinLogging
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.AccessDeniedException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

private val logger = KotlinLogging.logger {}

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgument(e: IllegalArgumentException): ResponseEntity<ApiResponse<Nothing>> {
        logger.warn { "Bad request: ${e.message}" }
        return ResponseEntity.badRequest().body(
            ApiResponse.error(e.message ?: "Invalid request")
        )
    }

    @ExceptionHandler(OptimisticLockingFailureException::class)
    fun handleOptimisticLocking(e: OptimisticLockingFailureException): ResponseEntity<ApiResponse<Nothing>> {
        logger.warn { "Optimistic locking failure: ${e.message}" }
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
            ApiResponse.error("Resource was modified by another user. Please refresh and try again.")
        )
    }

    @ExceptionHandler(AccessDeniedException::class)
    fun handleAccessDenied(e: AccessDeniedException): ResponseEntity<ApiResponse<Nothing>> {
        logger.warn { "Access denied: ${e.message}" }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
            ApiResponse.error("Access denied")
        )
    }

    @ExceptionHandler(NoSuchElementException::class)
    fun handleNotFound(e: NoSuchElementException): ResponseEntity<ApiResponse<Nothing>> {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
            ApiResponse.error(e.message ?: "Resource not found")
        )
    }

    @ExceptionHandler(Exception::class)
    fun handleGenericException(e: Exception): ResponseEntity<ApiResponse<Nothing>> {
        logger.error("Unexpected error", e)
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
            ApiResponse.error("An unexpected error occurred")
        )
    }
}
