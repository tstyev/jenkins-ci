package school.redrover.common;

import org.testng.*;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FilterForTests implements IMethodInterceptor {

    @Override
    public List<IMethodInstance> intercept(List<IMethodInstance> methods, ITestContext context) {

        String files = System.getenv("LIST_OF_CHANGED_FILES");
        if (files != null) {
            Set<String> deletedFiles = new HashSet<>();
            Set<String> otherFiles = new HashSet<>();

            for (String entry : files.split(";")) {
                char status = entry.charAt(0);
                String path = entry.substring(1).trim();

                if (status == 'D') {
                    deletedFiles.add(path);
                } else if (status == 'R') {
                    String[] parts = path.contains("\t")
                            ? path.substring(path.indexOf('\t') + 1).split("\\s+|\t")
                            : new String[]{path};
                    otherFiles.add(parts[parts.length - 1]);
                } else {
                    otherFiles.add(path);
                }
            }

            boolean hasNonTest = Stream.concat(deletedFiles.stream(), otherFiles.stream())
                    .anyMatch(f -> !f.endsWith("Test.java"));

            if (hasNonTest) {
                return methods;
            }

            Map<Class<?>, String> classMap = methods.stream()
                    .map(IMethodInstance::getMethod)
                    .map(ITestNGMethod::getTestClass)
                    .map(IClass::getRealClass)
                    .collect(Collectors.toMap(
                            Function.identity(),
                            clazz -> String.format("src/test/java/%s.java", clazz.getName().replace('.', '/')),
                            (pathA, pathB) -> pathA
                    ));

            return methods.stream()
                    .filter(method -> otherFiles.contains(classMap.get(method.getMethod().getTestClass().getRealClass())))
                    .collect(Collectors.toList());
        }

        return methods;
    }
}
