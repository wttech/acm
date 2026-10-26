package dev.vml.es.acm.core.acl.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import com.adobe.granite.crypto.CryptoException;
import com.adobe.granite.crypto.CryptoSupport;
import dev.vml.es.acm.core.acl.AclException;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class CryptoDecryptorTest {

    @Test
    void returnsInputWhenCryptoSupportMissing() {
        CryptoDecryptor decryptor = new CryptoDecryptor(null);
        assertEquals("{abc}", decryptor.decrypt("{abc}"));
    }

    @Test
    void returnsInputWhenTextIsBlank() {
        CryptoDecryptor decryptor = new CryptoDecryptor(Mockito.mock(CryptoSupport.class));
        assertNull(decryptor.decrypt(null));
        assertEquals("", decryptor.decrypt(""));
        assertEquals("   ", decryptor.decrypt("   "));
    }

    @Test
    void returnsInputWhenNotProtected() throws CryptoException {
        CryptoSupport cryptoSupport = Mockito.mock(CryptoSupport.class);
        when(cryptoSupport.isProtected("plainText")).thenReturn(false);
        assertEquals("plainText", new CryptoDecryptor(cryptoSupport).decrypt("plainText"));
    }

    @Test
    void unprotectsEncryptedText() throws CryptoException {
        CryptoSupport cryptoSupport = Mockito.mock(CryptoSupport.class);
        when(cryptoSupport.isProtected("{encrypted}")).thenReturn(true);
        when(cryptoSupport.unprotect("{encrypted}")).thenReturn("decrypted");
        assertEquals("decrypted", new CryptoDecryptor(cryptoSupport).decrypt("{encrypted}"));
    }

    @Test
    void wrapsFailureInAclException() throws CryptoException {
        CryptoSupport cryptoSupport = Mockito.mock(CryptoSupport.class);
        when(cryptoSupport.isProtected("{broken}")).thenReturn(true);
        doThrow(new CryptoException("hmac mismatch")).when(cryptoSupport).unprotect("{broken}");
        AclException exception =
                assertThrows(AclException.class, () -> new CryptoDecryptor(cryptoSupport).decrypt("{broken}"));
        assertEquals("Failed to decrypt text", exception.getMessage());
    }
}
