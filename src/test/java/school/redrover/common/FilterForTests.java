package school.redrover.common;

import org.testng.*;
import org.testng.annotations.Test;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class FilterForTests implements IMethodInterceptor {

    @Override
    public List<IMethodInstance> intercept(List<IMethodInstance> methods, ITestContext context) {

        String files = System.getenv("LIST_OF_CHANGED_FILES");
        if (files != null) {
            Set<String> fileSet = new HashSet<>(Arrays.asList(files.split(";")));

            boolean hasNonTest = fileSet.stream().anyMatch(className -> {
                try {
                    Class<?> clazz = Class.forName(className);
                    return Arrays.stream(clazz.getDeclaredMethods())
                            .noneMatch(m -> m.isAnnotationPresent(Test.class));
                } catch (ClassNotFoundException e) {
                    return true;
                }
            });

            if (hasNonTest) {
                return methods;
            }

            Map<Class<?>, String> classMap = methods.stream()
                    .map(IMethodInstance::getMethod)
                    .map(ITestNGMethod::getTestClass)
                    .map(IClass::getRealClass)
                    .filter(clazz -> Arrays.stream(clazz.getDeclaredMethods())
                            .anyMatch(m -> m.isAnnotationPresent(Test.class)))
                    .collect(Collectors.toMap(
                            Function.identity(),
                            clazz -> String.format("src/test/java/%s.java", clazz.getName().replace('.', '/')),
                            (pathA, pathB) -> pathA
                    ));

            return methods.stream()
                    .filter(method -> fileSet.contains(classMap.get(method.getMethod().getTestClass().getRealClass())))
                    .collect(Collectors.toList());
        }

        return methods;
    }
}
