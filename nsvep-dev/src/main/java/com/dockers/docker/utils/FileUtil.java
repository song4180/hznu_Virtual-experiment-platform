package com.dockers.docker.utils;

import javax.servlet.http.HttpServletRequest;
import java.io.File;

/**
 * @author zxy
 */
public class FileUtil {
    private static File file;

    /**
     * 判断文件是否存在
     *
     * @param path 文件路径
     * @return boolean
     */
    public static boolean fileIsExists(String path) {
        if (path == null) {
            return false;
        }
        file = new File(path);
        return file.exists();
    }

    /**
     * 判断服务器文件是否存在
     *
     * @param path    文件路径
     * @param request HttpServletRequest
     * @return boolean
     */
    public static boolean fileIsExists(String path, HttpServletRequest request) {
        if (path == null) {
            return false;
        }
        file = new File(request.getServletContext().getRealPath(path));
        return file.exists();
    }

    /**
     * 获取服务器的文件
     *
     * @param path    文件路径
     * @param request HttpServletRequest
     * @return boolean
     */
    public static String getRealPath(String path, HttpServletRequest request) {
        if (path == null) {
            throw new RuntimeException("Null Path");
        }
        return request.getServletContext().getRealPath(path);
    }

    /**
     * 获取文件名不保留后缀
     *
     * @param pathandname 文件名
     * @return 文件名不保留后缀
     */
    public static String getFileName(String pathandname) {
        int start = pathandname.lastIndexOf("/");
        int end = pathandname.lastIndexOf(".");
        if (start != -1 && end != -1) {
            return pathandname.substring(start + 1, end);
        } else {
            return null;
        }
    }

    /**
     * 保留文件名及后缀
     *
     * @param pathandname 文件名
     * @return 文件名及后缀
     */
    public static String getFileNameWithSuffix(String pathandname) {
        int start = pathandname.lastIndexOf(File.separator);
        if (start != -1) {
            return pathandname.substring(start + 1);
        } else {
            return null;
        }
    }

    /**
     * 创建父级文件夹
     *
     * @param file 完整路径文件名(注:不是文件夹)
     */
    public static void createParentPath(File file) {
        File parentFile = file.getParentFile();
        if (null != parentFile && !parentFile.exists()) {
            // 创建文件夹
            parentFile.mkdirs();
            // 递归创建父级目录
            createParentPath(parentFile);
        }
    }

    /**
     * 删除文件，可以是文件或文件夹
     *
     * @param fileName 要删除的文件名
     * @return 删除成功返回true，否则返回false
     */
    public static boolean delete(String fileName) {
        File file = new File(fileName);
        if (!file.exists()) {
            return false;
        } else {
            if (file.isFile()) {
                return deleteFile(fileName);
            } else {
                return deleteDirectory(fileName);
            }
        }
    }

    /**
     * 删除单个文件
     *
     * @param fileName 要删除的文件的文件名
     * @return 单个文件删除成功返回true，否则返回false
     */
    public static boolean deleteFile(String fileName) {
        File file;
        file = new File(fileName);
        // 如果文件路径所对应的文件存在，并且是一个文件，则直接删除
        if (file.exists() && file.isFile()) {
            return file.delete();
        } else {
            return false;
        }
    }

    /**
     * 删除目录文件
     *
     * @param dir 要删除的目录的文件路径
     * @return 目录删除成功返回true，否则返回false
     */
    public static boolean deleteDirectory(String dir) {
        // 如果dir不以文件分隔符结尾，自动添加文件分隔符
        if (!dir.endsWith(File.separator)) {
            dir = dir + File.separator;
        }
        File dirFile = new File(dir);
        // 如果dir对应的文件不存在，或者不是一个目录，则退出
        if ((!dirFile.exists()) || (!dirFile.isDirectory())) {
            return false;
        }
        boolean flag = true;
        // 删除文件夹中的所有文件包括子目录
        File[] files = dirFile.listFiles();
        for (File value : files) {
            // 删除子文件
            if (value.isFile()) {
                flag = FileUtil.deleteFile(value.getAbsolutePath());
                if (!flag) {
                    break;
                }
            }
            // 删除子目录
            else if (value.isDirectory()) {
                flag = FileUtil.deleteDirectory(value.getAbsolutePath());
                if (!flag) {
                    break;
                }
            }
        }
        if (!flag) {
            return false;
        }
        // 删除当前目录
        return dirFile.delete();
    }

}
