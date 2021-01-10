package com.raystatic.biometricdemo

import android.util.Patterns
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import java.util.*

class LoginViewModel:ViewModel() {

    private val _loginForm = MutableLiveData<LoginFormState>()
    val loginWithPasswordFormState:LiveData<LoginFormState> get() = _loginForm

    private val _loginResult = MutableLiveData<LoginResult>()
    val loginResult:LiveData<LoginResult> get() = _loginResult

    fun onLoginDataChanged(username:String, password:String){
        if (!isUsernameValid(username)){
            _loginForm.value = FailedLoginFormState(usernameError = R.string.invalid_username)
        }else if (!isPasswordValid(password)){
            _loginForm.value = FailedLoginFormState(passwordError = R.string.invalid_password)
        }else{
            _loginForm.value = SuccessfulLoginFormState(isDataValid = true)
        }
    }

    private fun isUsernameValid(username: String):Boolean{
        return if (username.contains('@')){
            Patterns.EMAIL_ADDRESS.matcher(username).matches()
        }else{
            username.isNotBlank()
        }
    }

    private fun isPasswordValid(password: String):Boolean{
        return password.length > 6
    }

    fun login(username:String, password: String){
        if (isUsernameValid(username) && isPasswordValid(password)){
            SampleUser.username = username
            SampleUser.token = UUID.randomUUID().toString()
            _loginResult.value = LoginResult(true)
        }else{
            _loginResult.value = LoginResult(false)
        }
    }

}