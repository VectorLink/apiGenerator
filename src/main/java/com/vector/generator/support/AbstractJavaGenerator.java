package com.vector.generator.support;

import java.io.BufferedWriter;
import java.io.IOException;
import com.vector.enums.Language;

/**
 * @author vector
 */
public abstract class AbstractJavaGenerator implements JavaGenerator {
    protected Language language;

    public AbstractJavaGenerator(Language language) {
        this.language = language;
    }

    @Override
    public void write(BufferedWriter writer) throws IOException {
        switch(this.language) {
            case SPRING:
                this.writeSpring(writer);
                break;
            case GATEWAY:
                this.writeGateway(writer);
                break;
            default:
                throw new RuntimeException("Invalid language");
        }

    }

    public void writeSpring(BufferedWriter writer) throws IOException {
    }

    public void writeGateway(BufferedWriter writer) throws IOException {
    }
}
