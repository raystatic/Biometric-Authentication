package com.raystatic.biometricdemo

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.inputmethod.EditorInfo
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.Observer
import com.raystatic.biometricdemo.databinding.ActivityEnableBiometricLoginBinding

class EnableBiometricLoginActivity:AppCompatActivity() {
    private val TAG = "EnableBiometricLogin"
    private val loginViewModel by viewModels<LoginViewModel>()
    private lateinit var cryptographyManager: CryptographyManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityEnableBiometricLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.cancel.setOnClickListener {
            finish()
        }

        loginViewModel.loginWithPasswordFormState.observe(this, Observer {formState->
            val loginState = formState ?: return@Observer
            when(loginState){
                is SuccessfulLoginFormState -> binding.authorize.isEnabled = loginState.isDataValid
                is FailedLoginFormState -> {
                    loginState.usernameError?.let { binding.username.error = getString(it) }
                    loginState.passwordError?.let { binding.password.error = getString(it) }
                }
            }
        })

        loginViewModel.loginResult.observe(this, Observer {
            val loginResult = it ?: return@Observer
            if (loginResult.success){
                showBiometricPromptForEncryption()
            }
        })

        binding.username.doAfterTextChanged {
            loginViewModel.onLoginDataChanged(
                binding.username.text.toString(),
                binding.password.text.toString()
            )
        }

        binding.password.setOnEditorActionListener { v, actionId, event ->
            when(actionId){
                EditorInfo.IME_ACTION_DONE -> {
                    loginViewModel.login(
                        binding.username.text.toString(),
                        binding.password.text.toString()
                    )
                }
            }
            false
        }

        binding.authorize.setOnClickListener {
            loginViewModel.login(
                binding.username.text.toString(),
                binding.password.text.toString()
            )
        }

    }

    private fun showBiometricPromptForEncryption() {
        val canAuthenticate = BiometricManager.from(applicationContext).canAuthenticate()
        if (canAuthenticate == BiometricManager.BIOMETRIC_SUCCESS){
            val secretKeyName = "biometric_sample_encryption_key"
            cryptographyManager = CryptographyManager()
            val cipher = cryptographyManager.getInitializedCipherForEncryption(secretKeyName)
            val biometricPrompt = BiometricPromptUtils.createBiometricPromt(this, ::encryptAndStoreToken)
            val promtInfo = BiometricPromptUtils.createPromtInfo(this)
            biometricPrompt.authenticate(promtInfo,BiometricPrompt.CryptoObject(cipher))
        }
    }

    private fun encryptAndStoreToken(authenticationResult: BiometricPrompt.AuthenticationResult) {
        authenticationResult.cryptoObject?.cipher?.apply {
            SampleUser.token?.let { token->
                Log.d(TAG,"token: $token")
                val encryptServerTokenWrapper = cryptographyManager.encryptData(token, this)
                cryptographyManager.persistCipherTextWrapperToSharedPrefs(
                    encryptServerTokenWrapper,
                    applicationContext,
                    Constants.SHARED_PREFS_FILENAME,
                    Context.MODE_PRIVATE,
                    Constants.CIPHERTEXT_WRAPPER
                )
            }
        }
        finish()
    }

}