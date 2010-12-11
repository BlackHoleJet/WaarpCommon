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
package goldengate.common.file.passthrough;

import java.nio.channels.FileChannel;
import java.util.List;

import org.jboss.netty.buffer.ChannelBuffer;

/**
 * This interface is for Object used in Passthrough mode.
 *
 * @author Frederic Bregier
 *
 */
public interface PassthroughFile {

    public boolean isDirectory();
    public boolean isFile();
    public ChannelBuffer read(int sizeblock) throws PassthroughException;
    public int write(ChannelBuffer buffer) throws PassthroughException;
    public long length();
    public boolean canRead();
    public boolean canWrite();
    public boolean isInReading();
    public boolean isInWriting();
    public boolean exists();
    public boolean delete() throws PassthroughException;
    public boolean renameTo(String path) throws PassthroughException;
    public void position(long position) throws PassthroughException;
    public void flush() throws PassthroughException;
    public long transferTo(FileChannel out) throws PassthroughException;
    public void close() throws PassthroughException;

    // Some extra functions that could be not implemented but just throwing the exception
    public List<String> wildcard(String subPath) throws PassthroughException;
    public boolean mkdir() throws PassthroughException;
    public boolean rmdir() throws PassthroughException;
    public boolean changeDirectory(String path) throws PassthroughException;
    /**
     * Return the Modification time
     *
     * @return the Modification time as a String YYYYMMDDHHMMSS.sss
     */
    public String getModificationTime() throws PassthroughException;
    public List<String> list() throws PassthroughException;
    public List<String> listFull(boolean lsFormat) throws PassthroughException;
    public String fileFull(boolean lsFormat) throws PassthroughException;
    public long getFreeSpace() throws PassthroughException;
    public long getCRC() throws PassthroughException;
    public byte[] getMD5() throws PassthroughException;
    public byte[] getSHA1() throws PassthroughException;

}
