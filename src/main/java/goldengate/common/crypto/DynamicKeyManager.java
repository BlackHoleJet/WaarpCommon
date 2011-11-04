/**
 * This file is part of GoldenGate Project (named also GoldenGate or GG).
 * 
 * Copyright 2009, Frederic Bregier, and individual contributors by the @author
 * tags. See the COPYRIGHT.txt in the distribution for a full listing of
 * individual contributors.
 * 
 * All GoldenGate Project is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 * 
 * GoldenGate is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * GoldenGate . If not, see <http://www.gnu.org/licenses/>.
 */
package goldengate.common.crypto;

import goldengate.common.exception.CryptoException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;

/**
 * 
 * Can implements AES, ARCFOUR, Blowfish, DES, DESede, RC2, RC4<br>
 * <br>
 * The time ratio are: RC4,ARCFOUR=1; AES,RC2=1,5; DES=2; Blowfish,DESede=4<br>
 * <b>AES is the best compromise in term of security and efficiency.</b>
 * 
 * @author frederic bregier
 * 
 */
public class DynamicKeyManager extends KeyManager {
    /**
     * Manager of Dynamic Key
     */
    public static final DynamicKeyManager dynamicKeyManager = new DynamicKeyManager();
    /**
     * Extra information file extension
     */
    public static final String INFEXTENSION = ".inf";

    /*
     * (non-Javadoc)
     * 
     * @see goldengate.common.crypto.KeyManager#createKeyObject()
     */
    @Override
    public KeyObject createKeyObject() {
        throw new InstantiationError(
                "DynamicKeyManager does not implement this function");
    }

    @Override
    public List<String> initFromList(List<String> keys, String extension) {
        LinkedList<String> wrong = new LinkedList<String>();
        for (String filename: keys) {
            File file = new File(filename);
            if (file.canRead()) {
                String basename = file.getName();
                int lastpos = basename.lastIndexOf(extension);
                if (lastpos <= 0) {
                    wrong.add(filename);
                    continue;
                }
                String firstname = basename.substring(0, lastpos - 1);
                int len = (int) file.length();
                byte[] key = new byte[len];
                FileInputStream inputStream = null;
                try {
                    inputStream = new FileInputStream(file);
                } catch (FileNotFoundException e) {
                    // should not be
                    wrong.add(filename);
                    continue;
                }
                int read = 0;
                int offset = 0;
                while (read > 0) {
                    try {
                        read = inputStream.read(key, offset, len);
                    } catch (IOException e) {
                        wrong.add(filename);
                        read = -2;
                        break;
                    }
                    offset += read;
                    if (offset < len) {
                        len -= read;
                    } else {
                        break;
                    }
                }
                try {
                    inputStream.close();
                } catch (IOException e) {
                }
                if (read < -1) {
                    // wrong
                    continue;
                }
                String infFilename = filename + INFEXTENSION;
                File infFile = new File(infFilename);
                inputStream = null;
                try {
                    inputStream = new FileInputStream(infFile);
                } catch (FileNotFoundException e) {
                    // should not be
                    wrong.add(filename);
                    continue;
                }
                KeyObject keyObject;
                try {
                    int keySize = inputStream.read();
                    String algo = readString(inputStream);
                    if (algo == null) {
                        wrong.add(filename);
                        continue;
                    }
                    String instance = readString(inputStream);
                    if (instance == null) {
                        wrong.add(filename);
                        continue;
                    }
                    keyObject = new DynamicKeyObject(keySize, algo, instance,
                            extension);
                } catch (IOException e1) {
                    wrong.add(filename);
                    continue;
                } finally {
                    try {
                        inputStream.close();
                    } catch (IOException e) {
                    }
                }
                keyObject.setSecretKey(key);
                this.setKey(firstname, keyObject);
            } else {
                wrong.add(filename);
            }
        }
        this.isInitialized.set(true);
        return wrong;
    }
    /**
     * Specific functions to ease the process of reading the "inf" file
     * @param inputStream
     * @return the String that should be read
     */
    private String readString(FileInputStream inputStream) {
        int len;
        try {
            len = inputStream.read();
        } catch (IOException e1) {
            return null;
        }
        byte[] readbyte = new byte[len];
        for (int i = 0; i < len; i ++) {
            try {
                readbyte[i] = (byte) inputStream.read();
            } catch (IOException e) {
                return null;
            }
        }
        return new String(readbyte);
    }

    @Override
    public void saveToFiles(String extension) throws CryptoException,
            IOException {
        Enumeration<String> names = keysConcurrentHashMap.keys();
        while (names.hasMoreElements()) {
            String name = names.nextElement();
            KeyObject key = keysConcurrentHashMap.get(name);
            key.saveSecretKey(new File(name + "." + extension));
            FileOutputStream outputStream = new FileOutputStream(new File(name +
                    "." + extension + INFEXTENSION));
            try {
                outputStream.write(key.getKeySize());
                String algo = key.getAlgorithm();
                String instance = key.getInstance();
                outputStream.write(algo.length());
                outputStream.write(algo.getBytes());
                outputStream.write(instance.length());
                outputStream.write(instance.getBytes());
                outputStream.close();
            } finally {
                outputStream.close();
            }
        }
    }

}
