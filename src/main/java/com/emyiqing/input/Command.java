package com.emyiqing.input;

import com.beust.jcommander.Parameter;

public class Command {
    @Parameter(names = {"-h", "--help"}, description = "Help Info",help = true)
    public boolean help;

    @Parameter(names = {"-f", "--file"}, description = "Scan Java File",
            validateWith = FilePathExist.class,required = true)
    public String filename;
}
