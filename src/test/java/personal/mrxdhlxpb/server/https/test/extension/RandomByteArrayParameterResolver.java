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

import java.util.Random;

/**
 * @author mrxdhlxpb
 */
public class RandomByteArrayParameterResolver implements ParameterResolver {
    private boolean validate(RandomByteArray annotation) throws ParameterResolutionException {
        if (annotation.minLen() >= 0 && annotation.maxLen() >= annotation.minLen()) return true;
        throw new ParameterResolutionException("invalid annotation");
    }

    private byte[] createRandomByteArray(int len) {
        byte[] result = new byte[len];
        new Random().nextBytes(result);
        return result;
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext,
                                     ExtensionContext extensionContext)
            throws ParameterResolutionException {
        return parameterContext.isAnnotated(RandomByteArray.class)
                && parameterContext.getParameter().getType() == byte[].class
                && validate(parameterContext.getParameter().getAnnotation(RandomByteArray.class));
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext,
                                   ExtensionContext extensionContext)
            throws ParameterResolutionException {
        RandomByteArray annotation =
                parameterContext.getParameter().getAnnotation(RandomByteArray.class);
        return createRandomByteArray(
                annotation.len() >= 0 ?
                        annotation.len() :
                        RandomIntParameterResolver.randomInt(annotation.minLen(),
                                annotation.maxLen()));
    }
}
