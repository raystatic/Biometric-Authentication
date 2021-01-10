package com.raystatic.biometricdemo

sealed class LoginFormState

data class FailedLoginFormState(
    var usernameError:Int?=null,
    val passwordError:Int?=null
):LoginFormState()

data class SuccessfulLoginFormState(
    val isDataValid:Boolean = false
):LoginFormState()