package com.example

import kotlinx.serialization.Serializable

@Serializable
data class RegistroMenorRequest(
    val nombreMenor: String,
    val userMenor: String,
    val rangoId: Int,
    val nombreTutor: String,
    val userTutor: String,
    val correoTutor: String,
    val contrasena: String
)

@Serializable
data class RegistroAdultoRequest(
    val nombreCompleto: String,
    val nombreUsuario: String,
    val correoElectronico: String,
    val rangoId: Int,
    val contrasena: String
)

@Serializable
data class LoginRequest(
    val nombreUsuario: String,
    val contrasena: String
)

@Serializable
data class AuthResponse(
    val exito: Boolean,
    val mensaje: String,
    val usuarioId: Int? = null,
    val rolId: Int? = null,
    val nombreCompleto: String? = null
)