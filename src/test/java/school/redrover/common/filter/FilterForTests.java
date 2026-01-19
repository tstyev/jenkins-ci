package school.redrover.common.filter;

import org.testng.*;
import school.redrover.common.ProjectUtils;

import java.util.*;

import static school.redrover.common.ProjectUtils.*;

public class FilterForTests implements IMethodInterceptor {

    @Override
    public List<IMethodInstance> intercept(List<IMethodInstance> methods, ITestContext context) {
        String files = System.getenv("LIST_OF_CHANGED_FILES");
        log("Changed files: " + files);

        String dependenciesClasses = System.getenv("LIST_OF_DEPENDENCIES_CLASSES");

        if (files != null && dependenciesClasses != null) {
            return FilterForTestsUtils.filter(
                    Arrays.stream(files.split(";")).toList(),
                    dependenciesClasses,
                    methods);
        }

        return methods;
    }
}