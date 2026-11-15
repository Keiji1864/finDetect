package com.ghostteam.findetect.data.remote.dto

import com.ghostteam.findetect.domain.model.SessionUser
import kotlinx.serialization.Serializable

@Serializable
data class LoginRequestDto(val email: String, val password: String)

@Serializable
data class RegisterRequestDto(val name: String, val email: String, val password: String)

@Serializable
data class RefreshRequestDto(val refreshToken: String)

@Serializable
data class TokenPairDto(val accessToken: String, val refreshToken: String, val expiresIn: Long)

@Serializable
data class AuthResponseDto(val user: SessionUser, val tokens: TokenPairDto)
