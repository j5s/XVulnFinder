package com.emyiqing.core.xss;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.expr.*;
import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@SuppressWarnings("all")
public class ServletXSS {
    private static Logger logger = Logger.getLogger(ServletXSS.class);

    private static final String SERVLET_REQUEST_IMPORT = "javax.servlet.http.HttpServletRequest";
    private static final String SERVLET_RESPONSE_IMPORT = "javax.servlet.http.HttpServletResponse";
    private static final String SERVLET_REQUEST_CLASS = "HttpServletRequest";
    private static final String SERVLET_RESPONSE_CLASS = "HttpServletResponse";

    public static boolean existRisk(CompilationUnit compilationUnit) {
        boolean imported = isImported(compilationUnit);
        if (!imported) {
            return false;
        }
        compilationUnit.findAll(ClassOrInterfaceDeclaration.class).stream()
                .filter(c -> !c.isInterface() && !c.isAbstract()).forEach(c -> {
                    c.getMethods().stream().filter(m -> !m.getName().asString().equals("main")).forEach(m -> {
                        Map<String, String> params = new HashMap();
                        m.getParameters().forEach(p -> {
                            if (p.getType().asString().equals(SERVLET_RESPONSE_CLASS)) {
                                params.put("response", p.getName().asString());
                            }
                            if (p.getType().asString().equals(SERVLET_REQUEST_CLASS)) {
                                params.put("request", p.getName().asString());
                            }
                        });
                        if (!params.get("request").equals("") &&
                                !params.get("response").equals("")) {
                            logger.info("find servlet method");
                        } else {
                            return;
                        }
                        Map<String, String> data = new HashMap<>();
                        AtomicInteger flag = new AtomicInteger(0);
                        m.findAll(VariableDeclarationExpr.class).forEach(a -> {
                            data.put("name", a.getVariables().get(0).getNameAsString());
                            a.findAll(MethodCallExpr.class).forEach(am -> {
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
                        m.findAll(MethodCallExpr.class).forEach(im -> {
                            if (im.getScope().get().toString().equals(params.get("response"))) {
                                Node call = im.getParentNode().get();
                                MethodCallExpr expr = (MethodCallExpr) call;
                                if (expr.getScope().get().toString().equals(params.get("response") + ".getWriter()")) {
                                    if (expr.getChildNodes().get(2).toString().equals(data.get("name"))) {
                                        logger.info("find servlet xss");
                                    }
                                }
                            }
                        });
                    });
                });
        return false;
    }

    private static boolean isImported(CompilationUnit compilationUnit) {
        List<ImportDeclaration> importDeclarations = compilationUnit.getImports();
        for (ImportDeclaration importDeclaration : importDeclarations) {
            if (importDeclaration.getName().asString().equals(SERVLET_REQUEST_IMPORT) ||
                    importDeclaration.getName().asString().equals(SERVLET_RESPONSE_IMPORT)) {
                logger.info("find servlet import");
                return true;
            }
        }
        return false;
    }

}
