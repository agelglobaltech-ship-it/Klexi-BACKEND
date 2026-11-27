package com.example

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import java.sql.Connection
import java.sql.Statement

fun Application.configureRouting() {
    DatabaseFactory.init()

    routing {
        get("/") {
            call.respondText("Servidor Klexi funcionando correctamente")
        }

        route("/api/auth") {

            // Registro menor de 18 años + tutor
            post("/registro-menor") {
                val req = call.receive<RegistroMenorRequest>()
                try {
                    transaction {
                        val conn = TransactionManager.current().connection.connection as Connection
                        val stmt = conn.prepareCall("{CALL registrar_menor_con_tutor(?, ?, ?, ?, ?, ?, ?)}")
                        stmt.setString(1, req.nombreMenor)
                        stmt.setString(2, req.userMenor)
                        stmt.setInt(3, req.rangoId)
                        stmt.setString(4, req.nombreTutor)
                        stmt.setString(5, req.userTutor)
                        stmt.setString(6, req.correoTutor)
                        stmt.setString(7, req.contrasena)
                        stmt.execute()
                        stmt.close()
                    }
                    call.respond(HttpStatusCode.Created, AuthResponse(true, "Perfiles de menor y tutor creados"))
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, AuthResponse(false, "Error al registrar: ${e.message}"))
                }
            }

            // Registro mayor de 18 años (rangos 5 al 7)
            post("/registro-adulto") {
                val req = call.receive<RegistroAdultoRequest>()
                try {
                    var nuevoId = 0
                    transaction {
                        val conn = TransactionManager.current().connection.connection as Connection
                        val stmtUser = conn.prepareStatement(
                            "INSERT INTO usuarios (rol_id, nombre_completo, nombre_usuario, correo_electronico, contrasena) VALUES (1, ?, ?, ?, ?)",
                            Statement.RETURN_GENERATED_KEYS
                        )
                        stmtUser.setString(1, req.nombreCompleto)
                        stmtUser.setString(2, req.nombreUsuario)
                        stmtUser.setString(3, req.correoElectronico)
                        stmtUser.setString(4, req.contrasena)
                        stmtUser.executeUpdate()

                        val keys = stmtUser.generatedKeys
                        if (keys.next()) nuevoId = keys.getInt(1)
                        stmtUser.close()

                        val stmtPerfil = conn.prepareStatement(
                            "INSERT INTO perfiles_pacientes (paciente_id, rango_id) VALUES (?, ?)"
                        )
                        stmtPerfil.setInt(1, nuevoId)
                        stmtPerfil.setInt(2, req.rangoId)
                        stmtPerfil.executeUpdate()
                        stmtPerfil.close()
                    }
                    call.respond(HttpStatusCode.Created, AuthResponse(true, "Usuario registrado", nuevoId, 1, req.nombreCompleto))
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, AuthResponse(false, "Error al registrar: ${e.message}"))
                }
            }

            // Login para los 4 roles
            post("/login") {
                val req = call.receive<LoginRequest>()
                var respuesta = AuthResponse(false, "Usuario o contraseña incorrectos")

                transaction {
                    val conn = TransactionManager.current().connection.connection as Connection
                    val stmt = conn.prepareStatement(
                        "SELECT usuario_id, rol_id, nombre_completo, estado_cuenta FROM usuarios WHERE nombre_usuario = ? AND contrasena = ?"
                    )
                    stmt.setString(1, req.nombreUsuario)
                    stmt.setString(2, req.contrasena)
                    val rs = stmt.executeQuery()

                    if (rs.next()) {
                        when (rs.getString("estado_cuenta")) {
                            "bloqueado" -> respuesta = AuthResponse(false, "Cuenta bloqueada por el administrador")
                            "baja" -> respuesta = AuthResponse(false, "Esta cuenta fue dada de baja")
                            else -> respuesta = AuthResponse(
                                exito = true,
                                mensaje = "Bienvenido a Klexi",
                                usuarioId = rs.getInt("usuario_id"),
                                rolId = rs.getInt("rol_id"),
                                nombreCompleto = rs.getString("nombre_completo")
                            )
                        }
                    }
                    rs.close()
                    stmt.close()
                }

                val status = if (respuesta.exito) HttpStatusCode.OK else HttpStatusCode.Unauthorized
                call.respond(status, respuesta)
            }
        }
    }
}