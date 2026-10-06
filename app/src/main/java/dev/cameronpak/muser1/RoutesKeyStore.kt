package dev.cameronpak.muser1

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.AtomicFile
import java.io.File
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Separate from Muse pairing credentials. This key never enters a manifest or APK. */
internal class RoutesKeyStore(private val context:Context) {
    private val state=AtomicFile(File(context.noBackupFilesDir,"google-routes.enc"))
    private fun key():SecretKey {
        val store=KeyStore.getInstance("AndroidKeyStore").apply{load(null)}
        (store.getKey("muse-r1-google-routes",null) as? SecretKey)?.let{return it}
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore").run{
            init(KeyGenParameterSpec.Builder("muse-r1-google-routes",KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build());generateKey()
        }
    }
    @Synchronized fun importPending(){
        val file=File(context.filesDir,"pending-google-routes-key")
        if(!file.exists())return
        try{
            require(file.length() in 1..256)
            val value=file.readText().trim();require(Regex("AIza[A-Za-z0-9_-]{35}").matches(value))
            val cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.ENCRYPT_MODE,key())
            val out=state.startWrite()
            try{out.write(cipher.iv+cipher.doFinal(value.toByteArray()));state.finishWrite(out)}catch(e:Exception){state.failWrite(out);throw e}
        }finally{file.delete()}
    }
    @Synchronized fun read():String? {
        if(!state.baseFile.exists())return null
        val bytes=state.readFully();require(bytes.size>=28)
        val cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.DECRYPT_MODE,key(),GCMParameterSpec(128,bytes.copyOfRange(0,12)))
        return String(cipher.doFinal(bytes.copyOfRange(12,bytes.size)),Charsets.UTF_8)
    }
}
