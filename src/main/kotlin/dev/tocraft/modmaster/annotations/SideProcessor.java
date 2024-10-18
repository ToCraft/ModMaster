package dev.tocraft.modmaster.annotations;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.Writer;
import java.util.Set;

@SupportedAnnotationTypes("dev.tocraft.modmaster.annotations.Side")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SupportedOptions("envType")
public class SideProcessor extends AbstractProcessor {

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (Element element : roundEnv.getElementsAnnotatedWith(Side.class)) {
            Side sideAnnotation = element.getAnnotation(Side.class);
            Env env = sideAnnotation.value();

            TypeElement clazz = element.getKind().isClass()
        }
        return true;
    }

    private String replaceDistWithEnv(String sideValue) {
        // Logic to determine the environment based on the side value
        return "environment"; // You may want to change this to suit your requirements
    }

    private void generateEnvAnnotation(Element element, String envValue) throws IOException {
        // Get the qualified name of the original element
        String qualifiedName = element.getEnclosingElement().toString();
        String newAnnotation = String.format("@Env(\"%s\")", envValue);

        // Create a new Java file to output
        JavaFileObject file = processingEnv.getFiler().createSourceFile(qualifiedName);
        try (Writer writer = file.openWriter()) {
            // Write the original code with the new annotation added
            writer.write(newAnnotation + "\n");
            writer.write(element.toString() + "\n");
        }
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Set.of(Side.class.getCanonicalName());
    }

    @Override
    public Set<String> getSupportedOptions() {
        return Set.of("modLoader");
    }
}
