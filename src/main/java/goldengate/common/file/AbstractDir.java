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
package goldengate.common.file;

import goldengate.common.command.exception.CommandAbstractException;
import goldengate.common.command.exception.Reply501Exception;
import goldengate.common.command.exception.Reply530Exception;
import goldengate.common.command.exception.Reply550Exception;
import goldengate.common.command.exception.Reply553Exception;
import goldengate.common.logging.GgInternalLogger;
import goldengate.common.logging.GgInternalLoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


/**
 * Abstract Main Implementation of Directory
 * 
 * @author Frederic Bregier
 *
 */
public abstract class AbstractDir implements DirInterface {
    /**
     * Internal Logger
     */
    private static final GgInternalLogger logger = GgInternalLoggerFactory
            .getLogger(AbstractDir.class);
    /**
     * Current Directory
     */
    protected String currentDir = null;
    /**
     * SessionInterface
     */
    protected SessionInterface session;

    /**
     * Opts command for MLSx. (-1) means not supported, 0 supported but not
     * active, 1 supported and active
     */
    protected OptsMLSxInterface optsMLSx;
    /**
     * Hack to say Windows or Unix (root like X:\ or /)
     */
    protected static Boolean ISUNIX = null;
    /**
     * Roots for Windows system
     */
    protected static File [] roots = null;
    /**
     * Init Windows Support
     */
    protected static void initWindowsSupport() {
        if (ISUNIX == null) {
            ISUNIX = (!System.getProperty("os.name")
                    .toLowerCase().startsWith("win"));
            if (! ISUNIX) {
                roots = File.listRoots();
            }
        }
    }
    /**
    *
    * @param file
    * @return The corresponding Root file
    */
    protected File getCorrespondingRoot(File file) {
       initWindowsSupport();
       if (ISUNIX) {
           return new File("/");
       }
       String path = file.getAbsolutePath();
       for (File root : roots) {
           if (path.startsWith(root.getAbsolutePath())) {
               return root;
           }
       }
       // hack !
       logger.warn("No root found for "+file.getAbsolutePath());
       return roots[0];
   }
    /**
     * Normalize Path to Internal unique representation
     *
     * @param path
     * @return the normalized path
     */
    public static String normalizePath(String path) {
        return path.replace('\\', SEPARATORCHAR).replace("//", "/");
    }
    /**
    *
    * @return the SessionInterface
    */
   public SessionInterface getSession() {
       return session;
   }
   public String validatePath(String path) throws CommandAbstractException {
       String extDir;
       if (isAbsoluteWindows(path)) {
           extDir = path;
           File newDir = new File(extDir);
           return validatePath(newDir);
       }
       extDir = consolidatePath(path);
       // Get the baseDir (mount point)
       String baseDir = getSession().getAuth().getBaseDirectory();
       // Get the translated real file path (removing '..')
       File newDir = new File(baseDir, extDir);
       return validatePath(newDir);
   }
   /**
    *
    * @param path
    * @return True if the given Path is an absolute one under Windows System
    */
   public boolean isAbsoluteWindows(String path) {
       initWindowsSupport();
       if (!ISUNIX) {
           File file = new File(path);
           return file.isAbsolute();
       }
       return false;
   }
   /**
    * Consolidate Path as relative or absolute path to an absolute path
    *
    * @param path
    * @return the consolidated path
    * @throws CommandAbstractException
    */
   protected String consolidatePath(String path)
           throws CommandAbstractException {
       if (path == null || path.length() == 0) {
           throw new Reply501Exception("Path must not be empty");
       }
       // First check if the path is relative or absolute
       if (isAbsoluteWindows(path)) {
           return path;
       }
       String extDir = null;
       if (path.charAt(0) == SEPARATORCHAR) {
           extDir = path;
       } else {
           extDir = currentDir + SEPARATOR + path;
       }
       return extDir;
   }
   /**
    * Construct the CanonicalPath without taking into account symbolic link
    * @param dir
    * @return the canonicalPath
    */
   protected String getCanonicalPath(File dir) {
       initWindowsSupport();
       if (ISUNIX) {
           // resolve it without getting symbolic links
           StringBuilder builder = new StringBuilder();
           // Get the path in reverse order from end to start
           List<String> list = new ArrayList<String>();
           File newdir = dir;
           String lastdir = newdir.getName();
           list.add(lastdir);
           File parent = newdir.getParentFile();
           while (parent != null) {
               newdir = parent;
               lastdir = newdir.getName();
               list.add(lastdir);
               parent = newdir.getParentFile();
           }
           // Now filter on '..' or '.'
           for (int i = list.size()-1; i >= 0; i--) {
               String curdir = list.get(i);
               if (curdir.equals(".")) {
                   list.remove(i);// removes '.'
               } else if (curdir.equals("..")) {
                       list.remove(i);// removes '..'
                       int len = list.size();
                       if (len > 0 && i < len) {
                           list.remove(i);// and removes parent dir
                       }
               }
           }
           if (list.isEmpty()) {
               return "/";
           }

           for (int i = list.size()-1; i >= 0; i--) {
               builder.append('/');
               builder.append(list.get(i));
           }
           return builder.toString();
       }
       //Windows version
       // no link so just use the default version of canonical Path
       try {
           return dir.getCanonicalPath();
       } catch (IOException e) {
           return dir.getAbsolutePath();
       }
   }
   /**
    * Same as validatePath but from a FileInterface
    *
    * @param dir
    * @return the construct and validated path (could be different than the one
    *         given as argument, example: '..' are removed)
    * @throws CommandAbstractException
    */
   protected String validatePath(File dir) throws CommandAbstractException {
       String extDir = null;
       extDir = normalizePath(getCanonicalPath(dir));
       // Get the relative business path
       extDir = getSession().getAuth().getRelativePath(extDir);
       // Check if this business path is valid
       if (getSession().getAuth().isBusinessPathValid(extDir)) {
           return extDir;
       }
       throw new Reply553Exception("Pathname not allowed");
   }
   /**
    * Finds all files matching a wildcard expression (based on '?', '~' or
    * '*').
    *
    * @param pathWithWildcard
    *            The wildcard expression with a business path.
    * @return List of String as relative paths matching the wildcard
    *         expression. Those files are tested as valid from business point
    *         of view. If Wildcard support is not active, if the path contains
    *         any wildcards, it will throw an error.
    * @throws CommandAbstractException
    */
   protected abstract List<String> wildcardFiles(String pathWithWildcard)
           throws CommandAbstractException;
   
   public String getPwd() throws CommandAbstractException {
       return currentDir;
   }
   public boolean changeParentDirectory() throws CommandAbstractException {
       return changeDirectory("..");
   }
   
   public FileInterface setFile(String path,
           boolean append) throws CommandAbstractException {
       checkIdentify();
       String newpath = consolidatePath(path);
       List<String> paths = wildcardFiles(newpath);
       if (paths.size() != 1) {
           throw new Reply550Exception("File not found: " +
                   paths.size() + " founds");
       }
       String extDir = paths.get(0);
       return newFile(extDir, append);
   }
   
   public void checkIdentify() throws Reply530Exception {
       if (!getSession().getAuth().isIdentified()) {
           throw new Reply530Exception("User not authentified");
       }
   }
   public void clear() {
       currentDir = null;
   }

   public void initAfterIdentification() {
       currentDir = getSession().getAuth().getBusinessPath();
   }

   public OptsMLSxInterface getOptsMLSx() {
       return optsMLSx;
   }
   
}
