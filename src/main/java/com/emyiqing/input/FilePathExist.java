package com.emyiqing.input;

import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.ParameterException;
import org.apache.log4j.Logger;

import java.io.File;

public class FilePathExist implements IParameterValidator {
    Logger logger= Logger.getLogger(FilePathExist.class);

    @Override
    public void validate(String name, String value) throws ParameterException {
        File file = new File(value);
        if (!file.exists()) {
            logger.error("input file not exists");
            throw new ParameterException("input file not exists");
        }
    }
}
