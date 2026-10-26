package dev.vml.es.acm.core.acl.utils;

import com.adobe.granite.crypto.CryptoException;
import com.adobe.granite.crypto.CryptoSupport;
import dev.vml.es.acm.core.acl.AclException;
import org.apache.commons.lang3.StringUtils;

public class CryptoDecryptor {

    private final CryptoSupport cryptoSupport;

    public CryptoDecryptor(CryptoSupport cryptoSupport) {
        this.cryptoSupport = cryptoSupport;
    }

    public String decrypt(String text) {
        if (cryptoSupport == null || StringUtils.isBlank(text) || !cryptoSupport.isProtected(text)) {
            return text;
        }
        try {
            return cryptoSupport.unprotect(text);
        } catch (CryptoException e) {
            throw new AclException("Failed to decrypt text", e);
        }
    }
}
