package com.emyiqing.core.sqli;

import com.emyiqing.dto.Result;
import com.emyiqing.parser.ParseUtil;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import org.apache.log4j.Logger;

import java.util.*;

@SuppressWarnings("all")
public class JdbcSqlInject {
    // todo: 添加新规则
    private static Logger logger = Logger.getLogger(JdbcSqlInject.class);

    private static final String STATEMENT_IMPORT = "java.sql.Statement";

    public static List<Result> check(CompilationUnit compilationUnit) {
        List<Result> results = new ArrayList<>();
        // 是否导入servlet依赖
        boolean imported = ParseUtil.isImported(compilationUnit, STATEMENT_IMPORT);
        if (!imported) {
            logger.warn("no jdbc sql inject");
            return results;
        }

        compilationUnit.findAll(ClassOrInterfaceDeclaration.class).stream()
                // 接口和抽象类不考虑
                .filter(c -> !c.isInterface() && !c.isAbstract()).forEach(c -> {
                    Map<String, String> globalVar = new HashMap<>();
                    c.findAll(FieldDeclaration.class).forEach(f -> {
                        // 全局变量中是否包含Connection（这是常见操作）
                        if (f.getVariables().get(0).getType().asString().equals("Connection")) {
                            globalVar.put("Connection", f.getVariables().get(0).getNameAsString());
                        }
                    });
                    // 遍历方法，不考虑main方法
                    c.getMethods().stream().filter(m -> !m.getName().asString().equals("main")).forEach(m -> {
                        Map<String, String> methodVar = new HashMap<>();
                        Map<String, String> paramVar = new HashMap<>();
                        m.getParameters().forEach(p -> {
                            paramVar.put(p.getType().asString(), p.getName().asString());
                        });
                        // 遍历方法中的赋值语句
                        m.findAll(VariableDeclarationExpr.class).forEach(v -> {
                            String left = v.getVariables().get(0).getNameAsString();
                            MethodCallExpr next;
                            if (v.getVariables().get(0).getInitializer().get() instanceof MethodCallExpr) {
                                next = (MethodCallExpr) v.getVariables().get(0).getInitializer().get();
                            } else {
                                return;
                            }
                            String right = v.getVariables().get(0).getInitializer().get().toString();
                            if (v.getVariables().get(0).getType().asString().equals("Statement")) {
                                methodVar.put("Statement", v.getVariables().get(0).getNameAsString());
                            }
                            if (next.getScope().get().toString().equals(globalVar.get("Connection"))) {
                                if (next.getName().asString().equals("createStatement")) {
                                    logger.debug("call createStatement method");
                                }
                                if (next.getName().asString().equals("prepareStatement")) {
                                    logger.debug("call prepareStatement method");
                                    return;
                                }
                            }
                            // stat.executeQuery("SELECT ..." + param)
                            if (next.getScope().get().toString().equals(methodVar.get("Statement"))) {
                                if (next.getName().asString().equals("executeQuery")) {
                                    logger.debug("call executeQuery method");
                                    next.findAll(BinaryExpr.class).forEach(b -> {
                                        String sql = b.getLeft().toString();
                                        if (!sql.toUpperCase(Locale.ROOT).contains("SELECT")) {
                                            return;
                                        }
                                        if (!b.getOperator().asString().equals("+")) {
                                            return;
                                        }
                                        // 存在SQL语句拼接
                                        if (b.getLeft() instanceof BinaryExpr) {
                                            BinaryExpr sqlLeft = (BinaryExpr) b.getLeft();
                                            String sqlRight = sqlLeft.getRight().toString();
                                            // 输入是否可控
                                            if (!paramVar.containsValue(sqlRight)) {
                                                return;
                                            }
                                            logger.debug("find jdbc sql inject");
                                            Result result = new Result();
                                            result.setSuccess(true);
                                            result.setClassName(c.getNameAsString());
                                            result.setMethodName(m.getNameAsString());
                                            String keyword = String.format("%s.%s",
                                                    next.getScope().get().toString(),
                                                    next.getName().asString());
                                            result.setKeyword(keyword);
                                            results.add(result);
                                        }
                                    });
                                }
                            }
                        });
                    });
                });
        return results;
    }
}
