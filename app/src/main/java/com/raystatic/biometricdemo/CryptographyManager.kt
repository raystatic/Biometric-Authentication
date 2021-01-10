package com.raystatic.biometricdemo

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import com.google.gson.Gson
import java.nio.charset.Charset
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

interface CryptographyManager {

    fun getInitializedCipherForEncryption(keyName:String):Cipher

    fun getInitializedCipherForDecryption(keyName: String, initializationVector:ByteArray) : Cipher

    fun encryptData(text:String, cipher: Cipher):CipherTextWrapper

    fun decryptData(cipherText:ByteArray, cipher:Cipher):String

    fun persistCipherTextWrapperToSharedPrefs(
        cipherTextWrapper: CipherTextWrapper,
        context: Context,
        filename:String,
        mode:Int,
        prefKey:String
    )

    fun getCipherTextWrapperFromSharedPrefs(
        context: Context,
        filename:String,
        mode: Int,
        prefKey: String
    ):CipherTextWrapper?

}

fun CryptographyManager(): CryptographyManager = CryptographyManagerImpl()

private class CryptographyManagerImpl:CryptographyManager{

    private val KEY_SIZE = 256
    private val AndroidKeyStore = "AndroidKeyStore"
    private val ENCYPTED_BLOCK_MODE = KeyProperties.BLOCK_MODE_GCM
    private val ENCRYPTION_PADDING = KeyProperties.ENCRYPTION_PADDING_NONE
    private val ENCRYPTION_ALGORITHM =KeyProperties.KEY_ALGORITHM_AES

    override fun getInitializedCipherForEncryption(keyName: String): Cipher {
        val cipher = getCipher()
        val secretKey = getOrCreateSecretKey(keyName)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        return cipher
    }

    private fun getOrCreateSecretKey(keyName: String): SecretKey {
        val keyStore = KeyStore.getInstance(AndroidKeyStore)
        keyStore.load(null)
        keyStore.getKey(keyName,null)?.let { return it as SecretKey }

        val paramsBuilder = KeyGenParameterSpec.Builder(
            keyName,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
        paramsBuilder.apply {
            setBlockModes(ENCYPTED_BLOCK_MODE)
            setEncryptionPaddings(ENCRYPTION_PADDING)
            setKeySize(KEY_SIZE)
            setUserAuthenticationRequired(true)
        }

        val keyGenParams = paramsBuilder.build()
        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            AndroidKeyStore
        )
        keyGenerator.init(keyGenParams)
        return keyGenerator.generateKey()

    }

    private fun getCipher(): Cipher {
        val transformation = "$ENCRYPTION_ALGORITHM/$ENCYPTED_BLOCK_MODE/$ENCRYPTION_PADDING"
        return Cipher.getInstance(transformation)
    }

    override fun getInitializedCipherForDecryption(
        keyName: String,
        initializationVector: ByteArray
    ): Cipher {
        val cipher = getCipher()
        val secretKey = getOrCreateSecretKey(keyName)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(128, initializationVector))
        return cipher
    }

    override fun encryptData(text: String, cipher: Cipher): CipherTextWrapper {
        val cipherText = cipher.doFinal(text.toByteArray(Charset.forName("UTF-8")))
        return CipherTextWrapper(cipherText,cipher.iv)
    }

    override fun decryptData(cipherText: ByteArray, cipher: Cipher): String {
        val text = cipher.doFinal(cipherText)
        return String(text, Charset.forName("UTF-8"))
    }

    override fun persistCipherTextWrapperToSharedPrefs(
        cipherTextWrapper: CipherTextWrapper,
        context: Context,
        filename: String,
        mode: Int,
        prefKey: String
    ) {
        val json = Gson().toJson(cipherTextWrapper)
        context.getSharedPreferences(filename,mode).edit().putString(prefKey,json).apply()
    }

    override fun getCipherTextWrapperFromSharedPrefs(
        context: Context,
        filename: String,
        mode: Int,
        prefKey: String
    ):CipherTextWrapper? {
        val json = context.getSharedPreferences(filename,mode).getString(prefKey, null)
        return Gson().fromJson(json,CipherTextWrapper::class.java)
    }
}

