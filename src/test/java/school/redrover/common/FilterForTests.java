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

            // Фильтруем существующие методы по fileSet
            List<IMethodInstance> filtered = methods.stream()
                    .filter(method -> fileSet.contains(classMap.get(method.getMethod().getTestClass().getRealClass())))
                    .collect(Collectors.toList());

            // Проверяем, есть ли новые или переименованные тестовые классы
            Set<String> existingClassPaths = new HashSet<>(classMap.values());
            boolean hasNewOrRenamed = fileSet.stream().anyMatch(f -> !existingClassPaths.contains(f));

            if (hasNewOrRenamed) {
                return methods; // запускаем весь фреймворк, чтобы новые тесты точно выполнились
            }

            return filtered;
        }

        return methods;
    }
}
