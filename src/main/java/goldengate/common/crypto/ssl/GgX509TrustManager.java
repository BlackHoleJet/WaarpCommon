/**
   This file is part of GoldenGate Project (named also GoldenGate or GG).

   Copyright 2009, Frederic Bregier, and individual contributors by the @author
   tags. See the COPYRIGHT.txt in the distribution for a full listing of
   individual contributors.

   All GoldenGate Project is free software: you can redistribute it and/or 
   modify it under the terms of the GNU General Public License as published 
   by the Free Software Foundation, either version 3 of the License, or
   (at your option) any later version.

   GoldenGate is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU General Public License for more details.

   You should have received a copy of the GNU General Public License
   along with GoldenGate .  If not, see <http://www.gnu.org/licenses/>.
 */
package goldengate.common.crypto.ssl;

import goldengate.common.exception.CryptoException;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

/**
 * GoldenGate X509 Trust Manager implementation
 *
 * @author Frederic Bregier
 *
 */
public class GgX509TrustManager implements X509TrustManager {
    /**
     * First using default X509TrustManager returned by the global TrustManager.
     * Then delegate decisions to it, and fall back to the logic in this class
     * if the default doesn't trust it.
     */
    private X509TrustManager defaultX509TrustManager = null;
    /**
     * Create an "always-valid" X509TrustManager
     */
    public GgX509TrustManager() {
        defaultX509TrustManager = null;
    }
    /**
     * Create a "default" X509TrustManager
     * @param tmf
     * @throws CryptoException
     */
    public GgX509TrustManager(TrustManagerFactory tmf) throws CryptoException {
        TrustManager tms[] = tmf.getTrustManagers();
        /**
         * Iterate over the returned trustmanagers, look for an instance
         * of X509TrustManager and use it as the default
         */
        for (int i=0; i < tms.length; i++) {
            if (tms[i] instanceof X509TrustManager) {
                defaultX509TrustManager = (X509TrustManager) tms[i];
                return;
            }
        }
        /**
         * Could not initialize, maybe try to build it from scratch?
         */
        throw new CryptoException("Cannot initialize the GgX509TrustManager");
    }

    /* (non-Javadoc)
     * @see javax.net.ssl.X509TrustManager#checkClientTrusted(java.security.cert.X509Certificate[], java.lang.String)
     */
    @Override
    public void checkClientTrusted(X509Certificate[] arg0, String arg1)
            throws CertificateException {
        if (defaultX509TrustManager == null) {
            return; // valid
        }
        defaultX509TrustManager.checkClientTrusted(arg0, arg1);
    }

    /* (non-Javadoc)
     * @see javax.net.ssl.X509TrustManager#checkServerTrusted(java.security.cert.X509Certificate[], java.lang.String)
     */
    @Override
    public void checkServerTrusted(X509Certificate[] arg0, String arg1)
            throws CertificateException {
        if (defaultX509TrustManager == null) {
            return; // valid
        }
        defaultX509TrustManager.checkServerTrusted(arg0, arg1);
    }

    /* (non-Javadoc)
     * @see javax.net.ssl.X509TrustManager#getAcceptedIssuers()
     */
    @Override
    public X509Certificate[] getAcceptedIssuers() {
        if (defaultX509TrustManager == null) {
            return new X509Certificate[0]; // none valid
        }
        return defaultX509TrustManager.getAcceptedIssuers();
    }

}
