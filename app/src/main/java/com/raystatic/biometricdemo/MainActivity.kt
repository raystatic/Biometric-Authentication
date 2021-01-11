package com.raystatic.biometricdemo

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.Observer
import com.raystatic.biometricdemo.databinding.ActivityMainBinding

class MainActivity:AppCompatActivity() {

    private val TAG = "MainActivity"
    private lateinit var biometricPrompt: BiometricPrompt
    private val cryptographyManager = CryptographyManager()
    private val ciphertextWrapper
        get() = cryptographyManager.getCipherTextWrapperFromSharedPrefs(
            applicationContext,
            Constants.SHARED_PREFS_FILENAME,
            Context.MODE_PRIVATE,
            Constants.CIPHERTEXT_WRAPPER
        )
    private lateinit var binding: ActivityMainBinding
    private val loginWithPasswordViewModel by viewModels<LoginViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val canAuthenticate = BiometricManager.from(applicationContext).canAuthenticate()
        if (canAuthenticate == BiometricManager.BIOMETRIC_SUCCESS) {
            binding.useBiometrics.visibility = View.VISIBLE
            binding.useBiometrics.setOnClickListener {
                if (ciphertextWrapper != null) {
                    showBiometricPromptForDecryption()
                } else {
                    startActivity(Intent(this, EnableBiometricLoginActivity::class.java))
                }
            }
        } else {
            binding.useBiometrics.visibility = View.INVISIBLE
        }

        if (ciphertextWrapper == null) {
            setupForLoginWithPassword()
        }
    }

    override fun onResume() {
        super.onResume()

        if (ciphertextWrapper != null) {
            if (SampleUser.token == null) {
                showBiometricPromptForDecryption()
            } else {
                updateStatus(getString(R.string.already_signedin))
            }
        }
    }

    private fun showBiometricPromptForDecryption() {
        ciphertextWrapper?.let { textWrapper ->
            val secretKeyName = getString(R.string.secret_key_name)
            val cipher = cryptographyManager.getInitializedCipherForDecryption(
                secretKeyName, textWrapper.initalizationVector
            )
            biometricPrompt =
                BiometricPromptUtils.createBiometricPromt(
                    this,
                    ::decryptTokenFromStorage
                )
            val promptInfo = BiometricPromptUtils.createPromtInfo(this)
            biometricPrompt.authenticate(promptInfo, BiometricPrompt.CryptoObject(cipher))
        }
    }

    private fun decryptTokenFromStorage(authResult: BiometricPrompt.AuthenticationResult) {
        ciphertextWrapper?.let { textWrapper ->
            authResult.cryptoObject?.cipher?.let {
                val plaintext =
                    cryptographyManager.decryptData(textWrapper.cipherText, it)
                SampleUser.token = plaintext

                updateStatus(getString(R.string.already_signedin))
            }
        }
    }

    private fun setupForLoginWithPassword() {
        loginWithPasswordViewModel.loginWithPasswordFormState.observe(this, Observer { formState ->
            val loginState = formState ?: return@Observer
            when (loginState) {
                is SuccessfulLoginFormState -> binding.login.isEnabled = loginState.isDataValid
                is FailedLoginFormState -> {
                    loginState.usernameError?.let { binding.username.error = getString(it) }
                    loginState.passwordError?.let { binding.password.error = getString(it) }
                }
            }
        })
        loginWithPasswordViewModel.loginResult.observe(this, Observer {
            val loginResult = it ?: return@Observer
            if (loginResult.success) {
                updateStatus(
                    "You successfully signed up using password as: user " +
                            "${SampleUser.username} with fake token ${SampleUser.token}"
                )
            }
        })
        binding.username.doAfterTextChanged {
            loginWithPasswordViewModel.onLoginDataChanged(
                binding.username.text.toString(),
                binding.password.text.toString()
            )
        }
        binding.password.doAfterTextChanged {
            loginWithPasswordViewModel.onLoginDataChanged(
                binding.username.text.toString(),
                binding.password.text.toString()
            )
        }
        binding.password.setOnEditorActionListener { _, actionId, _ ->
            when (actionId) {
                EditorInfo.IME_ACTION_DONE ->
                    loginWithPasswordViewModel.login(
                        binding.username.text.toString(),
                        binding.password.text.toString()
                    )
            }
            false
        }
        binding.login.setOnClickListener {
            loginWithPasswordViewModel.login(
                binding.username.text.toString(),
                binding.password.text.toString()
            )
        }
        Log.d(TAG, "Username ${SampleUser.username}; fake token ${SampleUser.token}")
    }

    private fun updateStatus(successMsg: String) {
        binding.success.text = successMsg
    }

}