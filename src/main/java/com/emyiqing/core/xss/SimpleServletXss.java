package com.emyiqing.core.xss;

import com.emyiqing.dto.Result;
import com.emyiqing.parser.ParseUtil;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.expr.*;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@SuppressWarnings("all")
public class SimpleServletXss {
    private static Logger logger = Logger.getLogger(SimpleServletXss.class);

    private static final String SERVLET_REQUEST_IMPORT = "javax.servlet.http.HttpServletRequest";
    private static final String SERVLET_RESPONSE_IMPORT = "javax.servlet.http.HttpServletResponse";
    private static final String SERVLET_REQUEST_CLASS = "HttpServletRequest";
    private static final String SERVLET_RESPONSE_CLASS = "HttpServletResponse";

    public static List<Result> check(CompilationUnit compilationUnit) {
        List<Result> results = new ArrayList<>();
        // 是否导入servlet依赖
        boolean imported = ParseUtil.isImported(compilationUnit, SERVLET_REQUEST_IMPORT) &&
                ParseUtil.isImported(compilationUnit, SERVLET_RESPONSE_IMPORT);
        if (!imported) {
            logger.warn("no servlet xss");
            return results;
        }
        compilationUnit.findAll(ClassOrInterfaceDeclaration.class).stream()
                // 接口和抽象类不考虑
                .filter(c -> !c.isInterface() && !c.isAbstract()).forEach(c -> {
                    // 遍历方法，不考虑main方法
                    c.getMethods().stream().filter(m -> !m.getName().asString().equals("main")).forEach(m -> {
                        // 请求和相应的具体参数
                        Map<String, String> params = new HashMap();
                        m.getParameters().forEach(p -> {
                            if (p.getType().asString().equals(SERVLET_RESPONSE_CLASS)) {
                                params.put("response", p.getName().asString());
                            }
                            if (p.getType().asString().equals(SERVLET_REQUEST_CLASS)) {
                                params.put("request", p.getName().asString());
                            }
                        });
                        // 参数校验
                        if (params.get("request") != null && params.get("request").equals("") ||
                                params.get("response") != null && params.get("response").equals("")) {
                            return;
                        }
                        Map<String, String> data = new HashMap<>();
                        AtomicInteger flag = new AtomicInteger(0);
                        // 遍历方法中的赋值语句
                        m.findAll(VariableDeclarationExpr.class).forEach(a -> {
                            // 左值名
                            String left = a.getVariables().get(0).getNameAsString();
                            data.put("name", left);
                            // 遍历赋值语句中的方法调用
                            a.findAll(MethodCallExpr.class).forEach(am -> {
                                // 判断是否调用getParameter方法
                                if (am.getScope().get().toString().equals(params.get("request"))) {
                                    if (am.getName().asString().equals("getParameter")) {
                                        flag.getAndIncrement();
                                    }
                                }
                            });
                            if (flag.get() != 0) {
                                logger.info("find servlet parameter: " + data.get("name"));
                            }
                        });
                        // 遍历方法调用
                        m.findAll(MethodCallExpr.class).forEach(im -> {
                            if (im.getScope().get().toString().equals(params.get("response"))) {
                                // 如果调用了response.getWriter
                                if (im.getName().asString().equals("getWriter")) {
                                    MethodCallExpr method;
                                    if (im.getParentNode().get() instanceof MethodCallExpr) {
                                        method = (MethodCallExpr) im.getParentNode().get();
                                    } else {
                                        return;
                                    }
                                    // response.getWriter.print();
                                    // response.getWriter.write();
                                    if (method.getName().asString().equals("print") ||
                                            method.getName().asString().equals("write")) {
                                        method.findAll(NameExpr.class).forEach(n -> {
                                            // 验证是否是输入参数
                                            if (n.getName().asString().equals(data.get("name"))) {
                                                // 封装结果
                                                Result result = new Result();
                                                result.setSuccess(true);
                                                result.setClassName(c.getNameAsString());
                                                result.setMethodName(m.getNameAsString());
                                                String keyword = String.format("%s.%s",
                                                        im.getNameAsString(), method.getNameAsString());
                                                result.setKeyword(keyword);
                                                results.add(result);
                                            }
                                        });
                                    }
                                }
                                if (im.getName().asString().equals("getOutputStream")) {
                                    MethodCallExpr method = (MethodCallExpr) im.getParentNode().get();
                                    // response.getOutputStream.print();
                                    // response.getOutputStream.println();
                                    if (method.getName().asString().equals("print") ||
                                            method.getName().asString().equals("println")) {
                                        method.findAll(NameExpr.class).forEach(n -> {
                                            // 验证是否是输入参数
                                            if (n.getName().asString().equals(data.get("name"))) {
                                                // 封装结果
                                                Result result = new Result();
                                                result.setSuccess(true);
                                                result.setClassName(c.getNameAsString());
                                                result.setMethodName(m.getNameAsString());
                                                String keyword = String.format("%s.%s",
                                                        im.getNameAsString(), method.getNameAsString());
                                                result.setKeyword(keyword);
                                                results.add(result);
                                            }
                                        });
                                    }
                                }
                            }
                        });
                    });
                });
        return results;
    }
}
