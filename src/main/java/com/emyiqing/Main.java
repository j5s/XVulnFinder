package com.emyiqing;

import com.beust.jcommander.JCommander;
import com.emyiqing.core.xss.ServletXSS;
import com.emyiqing.input.Command;
import com.emyiqing.input.Logo;
import com.emyiqing.util.FileUtil;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import org.apache.log4j.Logger;

import java.io.IOException;

public class Main {
    static Logger logger = Logger.getLogger(Main.class);

    public static void main(String[] args) {
        Logo.PrintLogo();
        Command command = new Command();
        JCommander jc = JCommander.newBuilder().addObject(command).build();
        jc.parse(args);
        if (command.help) {
            jc.usage();
        }
        String code;
        try {
            code = FileUtil.readFile(command.filename);
        } catch (IOException e) {
            logger.error("read code error");
            return;
        }

        CompilationUnit compilationUnit = StaticJavaParser.parse(code);
        ServletXSS.existRisk(compilationUnit);
    }
}
