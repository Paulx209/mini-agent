package com.getian.tool;

import java.io.File;
import java.io.IOException;

/**
 * 用来约束工具的权限边界 —— 因为涉及到写操作 s02暂时还没有涉及到权限系统
 * 主要用来解决路径逃逸问题，比如 ..\xx\xx， 执行路径不要逃出工作目录
 */
public class PathGuard {
    private final File workDir; //工作目录

    public PathGuard(File workDir) {
        this.workDir = workDir;
    }

    //用来解决路径逃逸问题 '..\' 直接升到上一级别
    public File resolve(String path) throws IOException {
        File target = new File(workDir, path).getCanonicalFile();
        File root = workDir.getCanonicalFile();
        if (!target.toPath().startsWith(root.toPath())) {
            throw new IOException("Path escapes workspace: " + path);
        }
        return target;
    }

}
