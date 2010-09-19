/**
 * Copyright 2009, Frederic Bregier, and individual contributors
 * by the @author tags. See the COPYRIGHT.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 3.0 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package goldengate.common.crypto.ssl;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;

import goldengate.common.exception.CryptoException;
import goldengate.common.logging.GgInternalLogger;
import goldengate.common.logging.GgInternalLoggerFactory;

/**
 * SecureKeyStore for SLL
 *
 * @author Frederic Bregier
 *
 */
public class GgSecureKeyStore {
    /**
     * Internal Logger
     */
    private static final GgInternalLogger logger = GgInternalLoggerFactory
            .getLogger(GgSecureKeyStore.class);

    public KeyStore keyStore;
    public KeyManagerFactory keyManagerFactory;
    public String keyStorePasswd;
    public String keyPassword;
    public GgSecureTrustManagerFactory secureTrustManagerFactory;
    public KeyStore keyTrustStore;
    public TrustManagerFactory trustManagerFactory;
    public GgX509TrustManager ggX509TrustManager;
    public String trustStorePasswd;

    /**
     * Initialize empty KeyStore. No TrustStore is internally created.
     * @param _keyStorePasswd
     * @param _keyPassword
     * @throws CryptoException
     */
    public GgSecureKeyStore(String _keyStorePasswd, String _keyPassword) throws CryptoException {
        keyStorePasswd = _keyStorePasswd;
        keyPassword = _keyPassword;
        try {
            keyStore = KeyStore.getInstance("JKS");
        } catch (KeyStoreException e) {
            logger.error("Cannot create KeyStore Instance", e);
            throw new CryptoException("Cannot create KeyStore Instance", e);
        }
        try {
            // Empty keyStore created so null for the InputStream
            keyStore.load(null,
                    getKeyStorePassword());
        } catch (NoSuchAlgorithmException e) {
            logger.error("Cannot create KeyStore Instance", e);
            throw new CryptoException("Cannot create KeyStore Instance", e);
        } catch (CertificateException e) {
            logger.error("Cannot create KeyStore Instance", e);
            throw new CryptoException("Cannot create KeyStore Instance", e);
        } catch (FileNotFoundException e) {
            logger.error("Cannot create KeyStore Instance", e);
            throw new CryptoException("Cannot create KeyStore Instance", e);
        } catch (IOException e) {
            logger.error("Cannot create KeyStore Instance", e);
            throw new CryptoException("Cannot create KeyStore Instance", e);
        }
    }
    /**
     * Initialize the SecureKeyStore and TrustStore from files
     * @param keyStoreFilename
     * @param _keyStorePasswd
     * @param _keyPassword
     * @param trustStoreFilename if Null, no TrustKeyStore will be created
     * @param _trustStorePasswd
     * @throws CryptoException
     */
    public GgSecureKeyStore(
            String keyStoreFilename, String _keyStorePasswd, String _keyPassword,
            String trustStoreFilename, String _trustStorePasswd) throws CryptoException {
        keyStorePasswd = _keyStorePasswd;
        keyPassword = _keyPassword;
        // First keyStore itself
        try {
            keyStore = KeyStore.getInstance("JKS");
        } catch (KeyStoreException e) {
            logger.error("Cannot create KeyStore Instance", e);
            throw new CryptoException("Cannot create KeyStore Instance", e);
        }
        try {
            keyStore.load(new FileInputStream(keyStoreFilename),
                    getKeyStorePassword());
        } catch (NoSuchAlgorithmException e) {
            logger.error("Cannot create KeyStore Instance", e);
            throw new CryptoException("Cannot create KeyStore Instance", e);
        } catch (CertificateException e) {
            logger.error("Cannot create KeyStore Instance", e);
            throw new CryptoException("Cannot create KeyStore Instance", e);
        } catch (FileNotFoundException e) {
            logger.error("Cannot create KeyStore Instance", e);
            throw new CryptoException("Cannot create KeyStore Instance", e);
        } catch (IOException e) {
            logger.error("Cannot create KeyStore Instance", e);
            throw new CryptoException("Cannot create KeyStore Instance", e);
        }
        try {
            keyManagerFactory = KeyManagerFactory.getInstance(
                    KeyManagerFactory.getDefaultAlgorithm());
            //"SunX509");
        } catch (NoSuchAlgorithmException e) {
            logger.error("Cannot create KeyManagerFactory Instance", e);
            throw new CryptoException("Cannot create KeyManagerFactory Instance", e);
        }
        try {
            keyManagerFactory.init(keyStore, getCertificatePassword());
        } catch (UnrecoverableKeyException e) {
            logger.error("Cannot create KeyManagerFactory Instance", e);
            throw new CryptoException("Cannot create KeyManagerFactory Instance", e);
        } catch (KeyStoreException e) {
            logger.error("Cannot create KeyManagerFactory Instance", e);
            throw new CryptoException("Cannot create KeyManagerFactory Instance", e);
        } catch (NoSuchAlgorithmException e) {
            logger.error("Cannot create KeyManagerFactory Instance", e);
            throw new CryptoException("Cannot create KeyManagerFactory Instance", e);
        }

        // Now create the TrustKeyStore
        if (trustStoreFilename != null) {
            initTrustStore(trustStoreFilename, _trustStorePasswd);
        }
    }
    /**
     *
     * @param alias
     * @return True if entry is deleted
     */
    public boolean deleteKeyFromKeyStore(String alias) {
        try {
            keyStore.deleteEntry(alias);
        } catch (KeyStoreException e) {
            logger.error("Cannot delete Key from KeyStore Instance", e);
            return false;
        }
        return true;
    }
    /**
     *
     * @param alias
     * @param key
     * @param chain
     * @return True if entry is added
     */
    public boolean setKeytoKeyStore(String alias, Key key, Certificate[] chain) {
        try {
            keyStore.setKeyEntry(alias, key, getCertificatePassword(), chain);
        } catch (KeyStoreException e) {
            logger.error("Cannot add Key and Certificates to KeyStore Instance", e);
            return false;
        }
        return true;
    }
    /**
     *
     * @param filename
     * @return True if keyStore is saved to file
     */
    public boolean saveKeyStore(String filename) {
        FileOutputStream fos;
        try {
            fos = new FileOutputStream(filename);
        } catch (FileNotFoundException e) {
            logger.error("Cannot save to file KeyStore Instance", e);
            return false;
        }
        try {
            keyStore.store(fos, getKeyStorePassword());
        } catch (KeyStoreException e) {
            logger.error("Cannot save to file KeyStore Instance", e);
            return false;
        } catch (NoSuchAlgorithmException e) {
            logger.error("Cannot save to file KeyStore Instance", e);
            return false;
        } catch (CertificateException e) {
            logger.error("Cannot save to file KeyStore Instance", e);
            return false;
        } catch (IOException e) {
            logger.error("Cannot save to file KeyStore Instance", e);
            return false;
        }
        return true;
    }
    /**
     *
     * @param trustStoreFilename
     * @param _trustStorePasswd
     * @throws CryptoException
     */
    public void initTrustStore(String trustStoreFilename, String _trustStorePasswd) throws CryptoException {
        trustStorePasswd = _trustStorePasswd;
        try {
            keyTrustStore = KeyStore.getInstance("JKS");
        } catch (KeyStoreException e) {
            logger.error("Cannot create TrustManagerFactory Instance", e);
            throw new CryptoException("Cannot create TrustManagerFactory Instance", e);
        }
        try {
            keyTrustStore.load(new FileInputStream(trustStoreFilename),
                    getKeyTrustStorePassword());
        } catch (NoSuchAlgorithmException e) {
            logger.error("Cannot create TrustManagerFactory Instance", e);
            throw new CryptoException("Cannot create TrustManagerFactory Instance", e);
        } catch (CertificateException e) {
            logger.error("Cannot create TrustManagerFactory Instance", e);
            throw new CryptoException("Cannot create TrustManagerFactory Instance", e);
        } catch (FileNotFoundException e) {
            logger.error("Cannot create TrustManagerFactory Instance", e);
            throw new CryptoException("Cannot create TrustManagerFactory Instance", e);
        } catch (IOException e) {
            logger.error("Cannot create TrustManagerFactory Instance", e);
            throw new CryptoException("Cannot create TrustManagerFactory Instance", e);
        }
        try {
            trustManagerFactory = TrustManagerFactory.getInstance(
                    KeyManagerFactory.getDefaultAlgorithm());
        } catch (NoSuchAlgorithmException e1) {
            logger.error("Cannot create TrustManagerFactory Instance", e1);
            throw new CryptoException("Cannot create TrustManagerFactory Instance", e1);
        }
        try {
            trustManagerFactory.init(keyTrustStore);
        } catch (KeyStoreException e1) {
            logger.error("Cannot create TrustManagerFactory Instance", e1);
            throw new CryptoException("Cannot create TrustManagerFactory Instance", e1);
        }
        try {
            secureTrustManagerFactory = new GgSecureTrustManagerFactory(trustManagerFactory);
        } catch (CryptoException e) {
            logger.error("Cannot create TrustManagerFactory Instance", e);
            throw new CryptoException("Cannot create TrustManagerFactory Instance", e);
        }
    }
    /**
     *
     * @param _trustStorePasswd
     * @return True if correctly initialized empty
     */
    public boolean initEmptyTrustStore(String _trustStorePasswd) {
        trustStorePasswd = _trustStorePasswd;
        try {
            keyTrustStore = KeyStore.getInstance("JKS");
        } catch (KeyStoreException e) {
            logger.error("Cannot create keyTrustStore Instance", e);
            return false;
        }
        try {
            // Empty keyTrustStore created so null for the InputStream
            keyTrustStore.load(null,
                    getKeyTrustStorePassword());
        } catch (NoSuchAlgorithmException e) {
            logger.error("Cannot create keyTrustStore Instance", e);
            return false;
        } catch (CertificateException e) {
            logger.error("Cannot create keyTrustStore Instance", e);
            return false;
        } catch (FileNotFoundException e) {
            logger.error("Cannot create keyTrustStore Instance", e);
            return false;
        } catch (IOException e) {
            logger.error("Cannot create keyTrustStore Instance", e);
            return false;
        }
        return true;
    }
    /**
     *
     * @param alias
     * @return True if entry is deleted
     */
    public boolean deleteKeyFromTrustStore(String alias) {
        try {
            keyStore.deleteEntry(alias);
        } catch (KeyStoreException e) {
            logger.error("Cannot delete Key from keyTrustStore Instance", e);
            return false;
        }
        return true;
    }
    /**
     *
     * @param alias
     * @param cert
     * @return True if entry is added
     */
    public boolean setKeytoTrustStore(String alias, Certificate cert) {
        try {
            keyStore.setCertificateEntry(alias, cert);
        } catch (KeyStoreException e) {
            logger.error("Cannot add Certificate to keyTrustStore Instance", e);
            return false;
        }
        return true;
    }
    /**
     *
     * @param filename
     * @return the X509 Certificate from filename
     * @throws CertificateException
     * @throws FileNotFoundException
     */
    public static Certificate loadX509Certificate(String filename)
    throws CertificateException, FileNotFoundException {
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        FileInputStream in = new FileInputStream(filename);
        Certificate c = cf.generateCertificate(in);
        return c;
    }
    /**
     *
     * @param filename
     * @return True if keyTrustStore is saved to file
     */
    public boolean saveTrustStore(String filename) {
        FileOutputStream fos;
        try {
            fos = new FileOutputStream(filename);
        } catch (FileNotFoundException e) {
            logger.error("Cannot save to file keyTrustStore Instance", e);
            return false;
        }
        try {
            keyTrustStore.store(fos, getKeyTrustStorePassword());
        } catch (KeyStoreException e) {
            logger.error("Cannot save to file keyTrustStore Instance", e);
            return false;
        } catch (NoSuchAlgorithmException e) {
            logger.error("Cannot save to file keyTrustStore Instance", e);
            return false;
        } catch (CertificateException e) {
            logger.error("Cannot save to file keyTrustStore Instance", e);
            return false;
        } catch (IOException e) {
            logger.error("Cannot save to file keyTrustStore Instance", e);
            return false;
        }
        return true;
    }
    /**
     * @return the certificate Password
     */
    public char[] getCertificatePassword() {
        if (keyPassword != null) {
            return keyPassword.toCharArray();
        }
        return "secret".toCharArray();
    }

    /**
     * @return the KeyStore Password
     */
    public char[] getKeyStorePassword() {
        if (keyStorePasswd != null) {
            return keyStorePasswd.toCharArray();
        }
        return "secret".toCharArray();
    }
    /**
     * @return the KeyTrustStore Password
     */
    public char[] getKeyTrustStorePassword() {
        if (trustStorePasswd != null) {
            return trustStorePasswd.toCharArray();
        }
        return "secret".toCharArray();
    }

}
