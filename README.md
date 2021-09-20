# XVulnFinder

# 介绍

静态Java代码安全审计工具（开发中）

语法分析基于：https://github.com/javaparser/javaparser

暂时只写了个开头，能对单个文件的基础Servlet XSS和JDBC SQL注入进行分析

# 案例

## XSS

对`XssServlet1.java`文件的分析：`java -jar xxx.jar -f XssServlet1.java`

![](https://github.com/EmYiQing/XVulnFinder/blob/master/img/001.png)

```java
package testcode.xss.servlets;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.lang.StringEscapeUtils;
import org.owasp.esapi.ESAPI;

public class XssServlet1 extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String input1 = req.getParameter("input1");

        resp.getWriter().write(input1);

        resp.getWriter().write(ESAPI.encoder().encodeForHTML(input1));
        resp.getWriter().write(StringEscapeUtils.escapeHtml(input1));

        resp.getOutputStream().print(input1);
        resp.getOutputStream().println(input1);
    }
}

```

## SQLi

对`Jdbc.java`文件的分析：`java -jar xxx.jar -f Jdbc.java`

![](https://github.com/EmYiQing/XVulnFinder/blob/master/img/002.png)

```java
package testcode.sqli;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class Jdbc {

    Connection con;

    public void query1(String input) throws SQLException {
        Statement stmt = con.createStatement();
        ResultSet rs = stmt.executeQuery("select * from Users where name = '" + input + "'");
    }
    
}
```
