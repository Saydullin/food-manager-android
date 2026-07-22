package com.devsgroup.foodmanager.core.data.crypto

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import com.devsgroup.foodmanager.core.common.di.Dispatcher
import com.devsgroup.foodmanager.core.common.di.FoodDispatcher
import com.devsgroup.foodmanager.core.domain.repository.DeviceKeyRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.Signature
import java.util.Base64
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Generates and holds the device's login-signing key inside the Android Keystore
 * (`AndroidKeyStore` provider) — the private key material never leaves it and
 * every [sign] call is delegated to the keystore itself.
 *
 * Algorithm: RSA-2048 with `SHA256withRSA`, not Ed25519. `AndroidKeyStore`'s
 * Ed25519 support only landed on very recent Android versions, while RSA has been
 * supported since API 18 — well below this app's minSdk. The auth server
 * (`food-manager-server`) was built anticipating exactly this: it parses the SPKI
 * DER public key and dispatches verification to either Ed25519 or
 * RSASSA-PKCS1-v1.5/SHA-256 based on the key's own type, "the two key types
 * Android Keystore can produce for this use case" (see its `crypto.ts`).
 */
@Singleton
class AndroidKeystoreDeviceKeyRepository @Inject constructor(
    @Dispatcher(FoodDispatcher.IO) private val ioDispatcher: CoroutineDispatcher,
) : DeviceKeyRepository {

    private val keyStore: KeyStore by lazy {
        KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
    }

    override suspend fun hasDeviceKey(): Boolean = withContext(ioDispatcher) {
        keyStore.containsAlias(KEY_ALIAS)
    }

    override suspend fun getOrCreatePublicKey(): String = withContext(ioDispatcher) {
        val entry = privateKeyEntry() ?: run {
            generateKeyPair()
            requireNotNull(privateKeyEntry()) { "Key generation silently failed for $KEY_ALIAS" }
        }
        Base64.getEncoder().encodeToString(entry.certificate.publicKey.encoded)
    }

    override suspend fun sign(nonce: String): String = withContext(ioDispatcher) {
        val entry = requireNotNull(privateKeyEntry()) {
            "No device key for $KEY_ALIAS — call getOrCreatePublicKey() first"
        }
        val signatureBytes = Signature.getInstance(SIGNATURE_ALGORITHM).apply {
            initSign(entry.privateKey)
            update(nonce.toByteArray(Charsets.UTF_8))
        }.sign()
        Base64.getEncoder().encodeToString(signatureBytes)
    }

    private fun privateKeyEntry(): KeyStore.PrivateKeyEntry? =
        keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.PrivateKeyEntry

    private fun generateKeyPair() {
        val generator = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_RSA, ANDROID_KEYSTORE)
        generator.initialize(
            KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_SIGN)
                .setDigests(KeyProperties.DIGEST_SHA256)
                .setSignaturePaddings(KeyProperties.SIGNATURE_PADDING_RSA_PKCS1)
                .setKeySize(KEY_SIZE_BITS)
                .build(),
        )
        generator.generateKeyPair()
    }

    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS = "food_manager_device_key"
        const val SIGNATURE_ALGORITHM = "SHA256withRSA"
        const val KEY_SIZE_BITS = 2048
    }
}
