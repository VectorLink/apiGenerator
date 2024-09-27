package com.vector.generator;

import java.io.IOException;

public interface ApiGenerator {
    void generateFile(String var1, String var2, String var3) throws Exception;

    void clean(String var1) throws IOException;
}
