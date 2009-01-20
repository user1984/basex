package org.basex.fuse;

/**
 * Interface to filesystem in userspace framework.
 * 
 * @author Workgroup DBIS, University of Konstanz 2008, ISC License
 * @author Alexander Holupirek
 */
public abstract class DeepFuse {

  /** File element. */
  public static final byte[] FILE = "file".getBytes();

  /** Directory element. */
  public static final byte[] DIR = "dir".getBytes();

  /** Name attribute. */
  public static final byte[] NAME = "name".getBytes();

  /** Type of file mask. (sys/stat.h) */
  public static final int S_IFMT = 0170000;

  /** Type of directory. (sys/stat.h) */
  public static final int S_IFDIR = 0040000;

  /** Regular file type. */
  public static final int S_IFREG = 0100000;

  /**
   * Check file mode for type directory.
   * @param mode of file
   * @return true if directory type, false otherwise
   */
  protected boolean isDir(final int mode) {
    return (mode & S_IFMT) == S_IFDIR;
  }

  /**
   * Check file mode for regular file type.
   * @param mode of file
   * @return true for regular file, false otherwise
   */
  protected boolean isFile(final int mode) {
    return (mode & S_IFMT) == S_IFREG;
  }
  
  /**
   * Get path to parent directory of a file, i.e., chop the file name (if any)
   * and return the path prefix. A directory path is returned as is.
   * @param path to file
   * @param mode of file
   * @return parent directory of file
   */
  protected String chopFilename(final String path, final int mode) {
    if(isDir(mode)) return path;

    int lastSlash = path.lastIndexOf('/');
    return lastSlash == 0 ? "/" : path.substring(0, lastSlash - 1);
  }

  /**
   * Get file name of a regular file.
   * @param path to file
   * @param mode of file
   * @return true for regular file, false otherwise
   */
  protected String getName(final String path, final int mode) {
    if(!isFile(mode)) return "";
    return path.substring(path.lastIndexOf('/') + 1, path.length());
  }
  
  /**
   * Get file attributes.
   * 
   * @param path to the file the stat information is requested for
   * @return int id of node/inode of the requested file
   */
  public abstract int getattr(final String path);

  /**
   * Read the target of a symbolic link.
   * @param path to the link
   * @return String link target
   */
  public abstract String readlink(final String path);

  /**
   * Create a file node.
   * 
   * This is called for creation of all non-directory, non-symlink nodes. If the
   * filesystem defines a create() method, then for regular files that will be
   * called instead.
   * @param path name of the file to be created.
   * @param mode specifies both the permissions to use and the type of node to
   *          be created.
   * @param dev If the file type is S_IFCHR or S_IFBLK then dev specifies the
   *          major and minor numbers of the newly created device special file;
   *          otherwise it is ignored.
   * @return zero on success, or -1 if an error occurred (in which case, errno
   *         is set appropriately).
   */
  public abstract int mknod(final String path, final int mode, final int dev);

  /**
   * Create a directory.
   * 
   * @param path to directory to be created
   * @param mode permissions for directory
   * @return zero on success, or -1 if an error occurred (in which case, errno
   *         is set appropriately).
   */
  public abstract int mkdir(final String path, final int mode);

  /**
   * Remove a file.
   * 
   * @param path to file to be removed
   * @return zero on success, or -1 if an error occurred (in which case, errno
   *         is set appropriately).
   */
  public abstract int unlink(final String path);

  /**
   * Remove a directory file.
   * 
   * @param path to file to be removed
   * @return zero on success, or -1 if an error occurred (in which case, errno
   *         is set appropriately).
   */
  public abstract int rmdir(final String path);

  /**
   * Make symbolic link to a file.
   * 
   * @param from link source
   * @param to link target
   * @return zero on success, or -1 if an error occurred (in which case, errno
   *         is set appropriately).
   */
  public abstract int symlink(final String from, final String to);

  /**
   * Rename a file.
   * 
   * @param from path to file to be renamed
   * @param to new name
   * @return zero on success, or -1 if an error occurred (in which case, errno
   *         is set appropriately).
   */
  public abstract int rename(final String from, final String to);

  /**
   * Create a hard link to a file.
   * 
   * @param name1 link source
   * @param name2 link target
   * @return zero on success, or -1 if an error occurred (in which case, errno
   *         is set appropriately).
   */
  public abstract int link(final String name1, final String name2);

  /**
   * Change the permission bits of a file.
   * 
   * @param path name of the file
   * @param mode permissions to be set
   * @return zero on success, or -1 if an error occurred
   */
  public abstract int chmod(final String path, final int mode);

  /**
   * Change the owner and group of a file.
   * 
   * @param path name of the file
   * @param owner uid
   * @param group gid
   * @return zero on success, or -1 if an error occurred
   */
  public abstract int chown(final String path, final int owner, final int group);

  /**
   * Change the size of a file.
   * 
   * @param path name of the file
   * @param off size to be set
   * @return zero on success, or -1 if an error occurred
   */
  public abstract int truncate(final String path, final long off);

  /**
   * File open operation.
   * 
   * @param path name of the file
   * @return zero on success, or -1 if an error occurred
   */
  public abstract int open(final String path);

  /**
   * Read data from an open file.
   * 
   * @param path name of the file
   * @param length number of bytes to read
   * @param offset from which to read
   * @return zero on success, or -1 if an error occurred
   */
  public abstract byte[] read(final String path, int length, int offset);

  /**
   * Write data to an open file.
   * 
   * @param path name of the file
   * @param length number of bytes to write
   * @param offset from which to write
   * @param data buffer from which to write
   * @return zero on success, or -1 if an error occurred
   */
  public abstract int write(final String path, int length, int offset, byte[] data);

  /**
   * Get file system statistics.
   * 
   * @param path name of the file
   * @return zero on success, or -1 if an error occurred
   */
  public abstract int statfs(final String path);

  /**
   * Possibly flush cached data.
   * 
   * BIG NOTE: This is not equivalent to fsync(). It's not a request to sync
   * dirty data.
   * @param path name of the file
   * @return zero on success, or -1 if an error occurred
   */
  public abstract int flush(final String path);

  /**
   * Release an open file.
   * 
   * @param path name of the file
   * @return zero on success, or -1 if an error occurred
   */
  public abstract int release(final String path);

  /**
   * Synchronize file contents.
   * @param path name of the file
   * @return zero on success, or -1 if an error occurred
   */
  public abstract int fsync(final String path);

  /**
   * Set extended attributes.
   * @param path name of the file
   * @return zero on success, or -1 if an error occurred
   */
  public abstract int setxattr(final String path);

  /**
   * Get extended attributes.
   * 
   * @param path name of the file
   * @return zero on success, or -1 if an error occurred
   */
  public abstract int getxattr(final String path);

  /**
   * List extended attributes.
   * 
   * @param path name of the file
   * @return zero on success, or -1 if an error occurred
   */
  public abstract int listxattr(final String path);

  /**
   * Remove extended attributes.
   * 
   * @param path name of the file
   * @return zero on success, or -1 if an error occurred
   */
  public abstract int removexattr(final String path);

  /**
   * Open directory.
   * 
   * @param path name of the file
   * @return zero on success, or -1 if an error occurred
   */
  public abstract int opendir(final String path);

  /**
   * Read directory.
   * @param path name of the file
   * @return zero on success, or -1 if an error occurred
   */
  public abstract int readdir(final String path);

  /**
   * Release directory.
   * 
   * @param path name of the file
   * @return zero on success, or -1 if an error occurred
   */
  public abstract int releasedir(final String path);

  /**
   * Synchronize directory contents.
   * 
   * @param path name of the file
   * @return zero on success, or -1 if an error occurred
   */
  public abstract int fsyncdir(final String path);

  /**
   * Initialize filesystem.
   * 
   * @return zero on success, or -1 if an error occurred
   */
  public abstract int init();

  /**
   * Clean up filesystem.
   * 
   * Called on filesystem exit.
   * @return zero on success, or -1 if an error occurred
   */
  public abstract int destroy();

  /**
   * Check file access permissions.
   * 
   * @param path name of the file
   * @param mode permission to check
   * @return zero on success, or -1 if an error occurred
   */
  public abstract int access(final String path, final int mode);

  /**
   * Create and open a file.
   * 
   * @param path for the file to be created
   * @param mode of file (directory, regular file ..., permission bits)
   * @return int id of newly created file or -1 on failure
   */
  public abstract int create(final String path, final int mode);

  /**
   * Change the size of an open file.
   * 
   * @param path name of the file
   * @param off new file size
   * @return zero on success, or -1 if an error occurred
   */
  public abstract int ftruncate(final String path, long off);

  /**
   * Get attributes from an open file.
   * 
   * This method is called instead of the getattr() method if the file
   * information is available.
   * @param path name of the file
   * @return zero on success, or -1 if an error occurred
   */
  public abstract int fgetattr(final String path);

  /**
   * Perform POSIX file locking operation.
   * 
   * @param path name of the file
   * @param cmd locking command id
   * @return zero on success, or -1 if an error occurred
   */
  public abstract int lock(final String path, final int cmd);

  /**
   * Change the access and modification times of a file with nanosecond
   * resolution.
   * 
   * @param path name of the file
   * @return zero on success, or -1 if an error occurred
   */
  public abstract int utimens(final String path);

  /**
   * Map block index within file to block index within device.
   * 
   * @param path name of the file
   * @param blocksize block size
   * @param idx block index
   * @return zero on success, or -1 if an error occurred
   */
  public abstract int bmap(final String path, final long blocksize, final long idx);
}