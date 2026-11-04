package com.ghostteam.findetect.feature.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

private const val BASE_URL = "http://10.0.2.2:8080"

private val Ink = Color(0xFF1E2E47)
private val Paper = Color(0xFFF8F8F5)
private val Yellow = Color(0xFFFFDF45)
private val Muted = Color(0xFF758092)

@Composable
fun AuthGate(
    content: @Composable (onLogout: () -> Unit) -> Unit
) {
    var token by remember { mutableStateOf<String?>(null) }
    var register by rememberSaveable { mutableStateOf(true) }

    var name by rememberSaveable { mutableStateOf("") }
    var login by rememberSaveable { mutableStateOf("") }

    var password by remember { mutableStateOf("") }
    var repeated by remember { mutableStateOf("") }

    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()

    if (token != null) {
        content {
            if (!busy) {
                busy = true
                val currentToken = token

                scope.launch {
                    try {
                        withContext(Dispatchers.IO) {
                            request(
                                endpoint = "logout",
                                body = "{}",
                                token = currentToken
                            )
                        }

                        token = null
                        register = false
                        password = ""
                        repeated = ""
                        error = null
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        error = "Не удалось завершить сеанс. " +
                                "Проверьте соединение и повторите выход."
                    } finally {
                        busy = false
                    }
                }
            }
        }

        error?.let { message ->
            AlertDialog(
                onDismissRequest = { error = null },
                title = {
                    Text("Выход из аккаунта")
                },
                text = {
                    Text(message)
                },
                confirmButton = {
                    TextButton(onClick = { error = null }) {
                        Text("Понятно")
                    }
                }
            )
        }

        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Paper)
            .safeDrawingPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Text(
            text = "FinDetect",
            color = Ink,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(30.dp))

        Text(
            text = if (register) {
                "Создать аккаунт"
            } else {
                "С возвращением"
            },
            color = Ink,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )

        if (!register) {
            Spacer(Modifier.height(12.dp))

            Text(
                text = "Ваши финансы под контролем",
                color = Muted
            )
        }

        Spacer(Modifier.height(16.dp))

        if (register) {
            AuthField(
                label = "Имя",
                value = name,
                onChange = { name = it },
                busy = busy
            )
        }

        AuthField(
            label = "Электронная почта или телефон",
            value = login,
            onChange = { login = it },
            busy = busy
        )

        AuthField(
            label = "Пароль",
            value = password,
            onChange = { password = it },
            secret = true,
            busy = busy
        )

        if (register) {
            AuthField(
                label = "Повторите пароль",
                value = repeated,
                onChange = { repeated = it },
                secret = true,
                busy = busy
            )
        }

        error?.let { message ->
            Text(
                text = message,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }

        Button(
            onClick = {
                error = when {
                    register && name.isBlank() ->
                        "Введите имя"

                    register && name.trim().length > 100 ->
                        "Имя должно быть не длиннее 100 символов"

                    login.isBlank() ->
                        "Введите почту или телефон"

                    password.length !in 8..128 ->
                        "Пароль: от 8 до 128 символов"

                    register && password != repeated ->
                        "Пароли не совпадают"

                    else -> null
                }

                if (error == null) {
                    busy = true

                    val endpoint = if (register) {
                        "register"
                    } else {
                        "login"
                    }

                    val body = JSONObject()
                        .put("name", name)
                        .put("login", login)
                        .put("password", password)
                        .toString()

                    scope.launch {
                        try {
                            val result = withContext(Dispatchers.IO) {
                                request(
                                    endpoint = endpoint,
                                    body = body
                                )
                            }

                            token = result.getString("token")
                            password = ""
                            repeated = ""
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            error = e.message ?: "Не удалось подключиться"
                        } finally {
                            busy = false
                        }
                    }
                }
            },
            enabled = !busy,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Yellow,
                contentColor = Ink
            )
        ) {
            Text(
                text = when {
                    busy -> "Подождите…"
                    register -> "Создать аккаунт"
                    else -> "Войти"
                }
            )
        }

        Spacer(Modifier.height(12.dp))

        if (!register) {
            OutlinedButton(
                onClick = {
                    error = "Восстановление пароля пока не подключено"
                },
                enabled = !busy,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "Забыли пароль?",
                    color = Ink
                )
            }

            Spacer(Modifier.height(12.dp))

            Text(
                text = "Ещё нет аккаунта?",
                color = Muted,
                fontSize = 12.sp
            )

            Spacer(Modifier.height(8.dp))
        }

        OutlinedButton(
            onClick = {
                register = !register
                error = null
                password = ""
                repeated = ""
            },
            enabled = !busy,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = if (register) {
                    "Уже есть аккаунт"
                } else {
                    "Зарегистрироваться"
                },
                color = Ink
            )
        }
    }
}

@Composable
private fun AuthField(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    secret: Boolean = false,
    busy: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = {
            Text(
                text = label,
                fontSize = 12.sp
            )
        },
        singleLine = true,
        enabled = !busy,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        shape = RoundedCornerShape(12.dp),
        visualTransformation = if (secret) {
            PasswordVisualTransformation()
        } else {
            VisualTransformation.None
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = if (secret) {
                KeyboardType.Password
            } else {
                KeyboardType.Text
            }
        ),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Ink,
            unfocusedTextColor = Ink,
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White,
            unfocusedBorderColor = Color(0xFFDEDFE1),
            focusedBorderColor = Ink
        )
    )
}

private fun request(
    endpoint: String,
    body: String,
    token: String? = null
): JSONObject {
    val connection = URL("$BASE_URL/api/auth/$endpoint")
        .openConnection() as HttpURLConnection

    try {
        connection.requestMethod = "POST"
        connection.connectTimeout = 10000
        connection.readTimeout = 15000
        connection.doOutput = true

        connection.setRequestProperty(
            "Content-Type",
            "application/json; charset=utf-8"
        )

        if (token != null) {
            connection.setRequestProperty(
                "Authorization",
                "Bearer $token"
            )
        }

        connection.outputStream.use { stream ->
            stream.write(body.toByteArray(Charsets.UTF_8))
        }

        val code = connection.responseCode

        val stream = if (code in 200..299) {
            connection.inputStream
        } else {
            connection.errorStream
        }

        val responseText = stream
            ?.bufferedReader(Charsets.UTF_8)
            ?.use { it.readText() }
            ?: "{}"

        val json = runCatching {
            JSONObject(responseText)
        }.getOrDefault(JSONObject())

        if (code !in 200..299) {
            throw IllegalStateException(
                json.optString(
                    "message",
                    "Ошибка сервера ($code)"
                )
            )
        }

        return json
    } catch (e: IOException) {
        throw IllegalStateException(
            "Сервер недоступен. Проверьте запуск бэкенда " +
                    "и адрес подключения.",
            e
        )
    } finally {
        connection.disconnect()
    }
}