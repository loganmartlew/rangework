package com.loganmartlew.rangework.shared.model

data class ValidationIssue(
    val field: String,
    val message: String,
)

class SharedValidationException(
    val issues: List<ValidationIssue>,
) : IllegalArgumentException(
    issues.joinToString(separator = "; ") { issue -> "${issue.field}: ${issue.message}" },
)
