package com.vector.generator;

import java.io.File;
import java.io.IOException;
import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.logging.SystemStreamLog;

public abstract class AbstractGenerator implements ApiGenerator  {
    private Log log = new SystemStreamLog();

    public AbstractGenerator() {
    }

    protected Log getLog() {
        return this.log;
    }
@Override
    public void clean(String output) throws IOException {
        FileUtils.deleteDirectory(new File(output));
    }
}
