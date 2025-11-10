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
            if (files.equals("FULL_RUN")) {
                return methods;
            }

            Set<String> fileSet = new HashSet<>(Arrays.asList(files.split(";")));

            Map<Class<?>, String> classMap = methods.stream()
                    .map(IMethodInstance::getMethod).map(ITestNGMethod::getTestClass).map(IClass::getRealClass)
                    .collect(Collectors.toMap(
                            Function.identity(),
                            clazz -> String.format("src/test/java/%s.java", clazz.getName().replace('.', '/')),
                            (pathA, pathB) -> pathA
                    ));

            // 1️⃣ фильтруем существующие методы по fileSet
            List<IMethodInstance> filtered = methods.stream()
                    .filter(method -> fileSet.contains(
                            classMap.get(method.getMethod().getTestClass().getRealClass())))
                    .collect(Collectors.toList());

            // 2️⃣ если в fileSet есть тестовые файлы, которых нет в classMap → добавляем их
            Set<String> existingClassPaths = new HashSet<>(classMap.values());
            boolean hasNewTest = fileSet.stream()
                    .anyMatch(f -> !existingClassPaths.contains(f) && f.endsWith("Test.java"));

            if (hasNewTest) {
                // просто возвращаем все методы, чтобы новые тесты точно запускались
                return methods;
            }

            return filtered;
        }

        return methods;
    }
}
