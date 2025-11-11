package school.redrover.common;

import org.testng.*;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class FilterForTests implements IMethodInterceptor {

    @Override
    public List<IMethodInstance> intercept(List<IMethodInstance> methods, ITestContext context) {

        String files = System.getenv("LIST_OF_CHANGED_FILES");
        if (files != null) {
            List<String> fileList = Arrays.asList(files.split(";"));

            Set<String> deletedFiles = new HashSet<>();
            Set<String> otherFiles = new HashSet<>();

            for (String file : fileList) {
                char status = file.charAt(0);
                String path = file.substring(1).trim();

                if (status == 'D') {
                    deletedFiles.add(path);
                } else {
                    otherFiles.add(path);
                }
            }

            boolean hasDeletedNonTest = deletedFiles.stream()
                    .anyMatch(f -> !f.endsWith("Test.java"));

            if (hasDeletedNonTest) {
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
