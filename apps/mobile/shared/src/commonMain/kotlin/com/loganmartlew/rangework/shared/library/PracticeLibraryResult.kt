package com.loganmartlew.rangework.shared.library

import com.loganmartlew.rangework.shared.model.ValidationIssue

sealed interface PracticeLibraryResult<out T> {
    data class Saved<T>(val value: T) : PracticeLibraryResult<T>
    data class Invalid(val issues: List<ValidationIssue>) : PracticeLibraryResult<Nothing>
}
