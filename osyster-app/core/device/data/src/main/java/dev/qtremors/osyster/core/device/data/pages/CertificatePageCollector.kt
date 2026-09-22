package dev.qtremors.osyster.core.device.data.pages

import dev.qtremors.osyster.core.device.data.R
import dev.qtremors.osyster.core.device.data.format.DistinguishedName
import dev.qtremors.osyster.core.device.domain.Availability
import dev.qtremors.osyster.core.device.domain.InfoSection
import java.security.KeyStore
import java.security.cert.X509Certificate
import java.text.DateFormat
import java.util.Date
import javax.security.auth.x500.X500Principal

/**
 * The Certificates page: the public certificate authorities Android trusts, from the
 * AndroidCAStore key store. Only public certificates are read; there are no private keys here.
 */
internal class CertificatePageCollector(private val text: DeviceText) {

    fun collect(): List<InfoSection> {
        val store = runCatching { KeyStore.getInstance("AndroidCAStore").apply { load(null) } }.getOrNull()
            ?: return listOf(section(text, "certs", R.string.section_system_certs, "AndroidCAStore") {
                row("count", R.string.row_certificate_count, Availability.Failed("The certificate store did not open", retryable = true))
            })
        val entries = store.aliases().toList().mapNotNull { alias ->
            (runCatching { store.getCertificate(alias) }.getOrNull() as? X509Certificate)?.let { alias to it }
        }
        val system = entries.filter { it.first.startsWith("system:") }.map { it.second }
        val user = entries.filter { it.first.startsWith("user:") }.map { it.second }
        return listOf(
            certificates("system", R.string.section_system_certs, system),
            certificates("user", R.string.section_user_certs, user)
        )
    }

    private fun certificates(key: String, title: Int, certs: List<X509Certificate>): InfoSection {
        val builder = SectionBuilder(text, "AndroidCAStore")
        if (certs.isEmpty()) {
            builder.row("none", R.string.row_certificate_count, text[R.string.value_none_installed], copyable = false)
        }
        val now = Date()
        val dates = DateFormat.getDateInstance(DateFormat.MEDIUM)
        certs.sortedBy { commonName(it.subjectX500Principal).lowercase() }.forEachIndexed { index, cert ->
            val expired = cert.notAfter.before(now)
            val detail = buildString {
                append(organization(cert.issuerX500Principal) ?: commonName(cert.issuerX500Principal))
                append(" · ")
                append(text.format(if (expired) R.string.value_expired_on else R.string.value_valid_until, dates.format(cert.notAfter)))
            }
            builder.row("${key}_$index", commonName(cert.subjectX500Principal), builder.available(detail))
        }
        builder.foldAfter(FOLD_AFTER)
        return builder.build(key, text.format(R.string.section_codec_count, text[title], certs.size))
    }

    private fun rfc(principal: X500Principal): String = principal.getName(X500Principal.RFC2253)

    private fun commonName(principal: X500Principal): String = DistinguishedName.displayName(rfc(principal))

    private fun organization(principal: X500Principal): String? = DistinguishedName.field(rfc(principal), "O")

    private companion object {
        const val FOLD_AFTER = 8
    }
}
