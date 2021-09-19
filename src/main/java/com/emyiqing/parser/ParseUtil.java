package com.emyiqing.parser;

import com.github.javaparser.ast.CompilationUnit;

public class ParseUtil {
    /**
     * 是否导入了某类
     * @param compilationUnit cu
     * @param fullName 类全名
     * @return bool
     */
    public static boolean isImported(CompilationUnit compilationUnit, String fullName) {
        final boolean[] flag = new boolean[1];
        compilationUnit.getImports().forEach(i -> {
            if (i.getName().asString().equals(fullName)) {
                flag[0] = true;
            }
        });
        return flag[0];
    }
}
