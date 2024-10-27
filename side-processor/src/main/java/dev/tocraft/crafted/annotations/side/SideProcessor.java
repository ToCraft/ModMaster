package dev.tocraft.crafted.annotations.side;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Set;
import java.util.stream.Collectors;

@SupportedAnnotationTypes("dev.tocraft.modmaster.annotations.Side")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SupportedOptions("envType")
public class SideProcessor extends AbstractProcessor {
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        System.out.println("RUNNING SIDE PROCESSOR");

        for (Element element : roundEnv.getElementsAnnotatedWith(Side.class)) {
            Side sideAnnotation = element.getAnnotation(Side.class);

            Env env = sideAnnotation.value();
            String modLoader = processingEnv.getOptions().get("modLoader");
            String target = getTarget(env);
            String replacement = getReplacement(env, modLoader);

            try {
                // Retrieve the fully qualified name and path to the source file
                String qualifiedName = ((TypeElement) element).getQualifiedName().toString();
                String relativePath = qualifiedName.replace('.', '/') + ".java";

                // Attempt to retrieve the source file via the Filer API (read-only)
                JavaFileObject sourceFile = (JavaFileObject) processingEnv.getFiler()
                        .getResource(javax.tools.StandardLocation.SOURCE_PATH, "", relativePath);

                // Read the original source file content
                String originalSource;
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(sourceFile.openInputStream()))) {
                    originalSource = reader.lines().collect(Collectors.joining("\n"));
                }

                // Perform the string replacement
                String modifiedSource = originalSource.replace(target, replacement);

                // Determine the file path on disk (e.g., from URI) and overwrite it directly
                Path sourceFilePath = Paths.get(sourceFile.toUri());

                // Write the modified content back to the original file
                try (BufferedWriter writer = Files.newBufferedWriter(sourceFilePath, StandardOpenOption.TRUNCATE_EXISTING)) {
                    writer.write(modifiedSource);
                }

                processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE,
                        "Replaced '" + target + "' with '" + replacement + "' in " + sourceFilePath);

            } catch (IOException e) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                        "Failed to process file: " + e.getMessage());
            }
        }
        return true;
    }

    private String getTarget(Env env) {
        return Env.class.getName() + "(" + Env.class.getName() + "." + env.name() + ");";
    }

    private String getReplacement(Env env, String modLoader) {
        return switch (modLoader.toLowerCase()) {
            case "fabric" -> switch (env) {
                case CLIENT -> "net.fabricmc.api.Environment(net.fabricmc.api.EnvType.CLIENT);";

                case DEDICATED_SERVER -> "net.fabricmc.api.Environment(net.fabricmc.api.EnvType.SERVER);";

            };
            case "forge" -> switch (env) {
                case CLIENT -> "net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.CLIENT);";
                case DEDICATED_SERVER ->
                        "net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.DEDICATED_SERVER);";
            };

            case "neoforge" -> switch (env) {
                case CLIENT -> "net.neoforged.api.istmarker.OnlyIn(net.minecraftforge.api.distmarker.CLIENT);";

                case DEDICATED_SERVER ->
                        "net.neoforged.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.DEDICATED_SERVER);";

            };

            default -> getTarget(env);
        };
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
