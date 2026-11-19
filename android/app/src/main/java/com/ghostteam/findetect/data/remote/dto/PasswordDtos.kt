package com.ghostteam.findetect.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class ChangePasswordRequestDto(val currentPassword: String, val newPassword: String)

@Serializable
data class PasswordResetRequestDto(val email: String)

@Serializable
data class PasswordResetChallengeDto(val requestId: String, val expiresInSeconds: Int, val resendAfterSeconds: Int)

@Serializable
data class VerifyPasswordResetRequestDto(val requestId: String, val code: String)

@Serializable
data class PasswordResetGrantDto(val resetToken: String, val expiresInSeconds: Int)

@Serializable
data class CompletePasswordResetRequestDto(val resetToken: String, val newPassword: String)
