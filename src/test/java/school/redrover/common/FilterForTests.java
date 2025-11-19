package school.redrover.common;

import org.testng.*;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class FilterForTests implements IMethodInterceptor {

    @Override
    public List<IMethodInstance> intercept(List<IMethodInstance> methods, ITestContext context) {
        String files = System.getenv("LIST_OF_CHANGED_FILES");
        String dependenciesFiles = System.getenv("LIST_OF_DEPENDENCIES_FILES");

        if (files != null && dependenciesFiles != null) {
            List<String> entryList = Arrays.stream(files.split(";")).toList();

            Set<String> changedFiles = entryList.stream()
                    .filter(e -> !e.startsWith("D="))
                    .map(e -> e.substring(e.lastIndexOf('=') + 1))
                    .collect(Collectors.toSet());
            System.out.println("Changed files" + changedFiles);
            Map<Class<?>, String> classMap = methods.stream()
                    .map(IMethodInstance::getMethod).map(ITestNGMethod::getTestClass).map(IClass::getRealClass)
                    .collect(Collectors.toMap(
                            Function.identity(),
                            clazz -> String.format("src/test/java/%s.java", clazz.getName().replace('.', '/')),
                            (pathA, pathB) -> pathA
                    ));

            Map<String, Set<String>> dependants = Arrays.stream(dependenciesFiles.split(";"))
                    .map(s -> s.split("<-"))
                    .collect(Collectors.groupingBy(
                            parts -> String.format("src/test/java/%s.java", parts[0].replace('.', '/')),
                            Collectors.mapping(parts -> String.format("src/test/java/%s.java", parts[1].replace('.', '/')), Collectors.toSet())
                    ));

            Queue<String> queue = new ArrayDeque<>(changedFiles);
            while (!queue.isEmpty()) {
                String file = queue.poll();
                dependants.getOrDefault(file, Set.of())
                        .forEach(dep -> {
                            if (changedFiles.add(dep)) {
                                queue.add(dep);
                            }
                        });
            }

            Set<String> affectedFiles = changedFiles.stream()
                    .filter(file ->
                            classMap.containsValue(file) ||                                   // это тест
                                    (changedFiles.contains(file) &&                                   // это изменённый файл
                                            dependants.getOrDefault(file, Set.of()).isEmpty())               // и от него никто не зависит
                    )
                    .collect(Collectors.toSet());

            System.out.println("Affected files" + affectedFiles);
            if (classMap.values().containsAll(affectedFiles)) {
                return methods.stream().filter(method -> affectedFiles.contains(classMap.get(method.getMethod().getTestClass().getRealClass()))).collect(Collectors.toList());
            }

        }

        return methods;
    }
}
