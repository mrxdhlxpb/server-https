/*
 *  Copyright (C) 2024 mrxdhlxpb
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package personal.mrxdhlxpb.server.https.test.extension;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author mrxdhlxpb
 */
public class RandomIntParameterResolver implements ParameterResolver {
    private final Map<Parameter, Generator> parameterGeneratorMap = new HashMap<>();

    /**
     * @return a random int in the range [min, max] if min < max, or min if min = max
     * @throws IllegalArgumentException if min > max
     */
    static int randomInt(int min, int max) {
        if (min > max) throw new IllegalArgumentException();
        double f = Math.random() / Math.nextDown(1.0);
        double x = min * (1.0 - f) + max * f;
        return (int) x;
    }

    private boolean validate(RandomInt annotation) throws ParameterResolutionException {
        if (annotation.fixed() == null || annotation.max() < annotation.min())
            throw new ParameterResolutionException("invalid annotation");
        return true;
    }

    private boolean createGeneratorIfAbsent(ParameterContext parameterContext) {
        parameterGeneratorMap.putIfAbsent(parameterContext.getParameter(),
                new Generator(parameterContext.getParameter().getAnnotation(RandomInt.class)));
        return true;
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext,
                                     ExtensionContext extensionContext)
            throws ParameterResolutionException {
        return parameterContext.isAnnotated(RandomInt.class) &&
                Set.of(int.class, long.class, float.class, double.class, Integer.class)
                        .contains(parameterContext.getParameter().getType()) &&
                validate(parameterContext.getParameter().getAnnotation(RandomInt.class)) &&
                createGeneratorIfAbsent(parameterContext);
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext,
                                   ExtensionContext extensionContext)
            throws ParameterResolutionException {
        return parameterGeneratorMap.get(parameterContext.getParameter()).generate();
    }

    private static final class Generator {
        final RandomInt annotation;
        int index = 0;

        Generator(RandomInt annotation) {
            this.annotation = annotation;
        }

        int generate() {
            return index >= annotation.fixed().length ?
                    randomInt(annotation.min(), annotation.max()) :
                    annotation.fixed()[index++];
        }
    }

}
