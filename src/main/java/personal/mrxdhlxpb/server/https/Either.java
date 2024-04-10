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
package personal.mrxdhlxpb.server.https;

import java.util.Objects;
import java.util.function.Function;

/**
 * @author mrxdhlxpb
 */
public final class Either<L, R> {

    private final L leftValue;

    private final R rightValue;

    private Either(L leftValue, R rightValue) {
        this.leftValue = leftValue;
        this.rightValue = rightValue;
    }

    public static <L, R> Either<L, R> of(L l, Void placeholder) {
        return new Either<>(Objects.requireNonNull(l), null);
    }

    public static <L, R> Either<L, R> of(Void placeholder, R r) {
        return new Either<>(null, Objects.requireNonNull(r));
    }

    public L getLeftValue() {
        return Objects.requireNonNull(leftValue, "Left value is uninitialized. Try right value.");
    }

    public R getRightValue() {
        return Objects.requireNonNull(rightValue, "Right value is uninitialized. Try left value.");
    }

    /**
     * @throws NullPointerException if {@code mapper} is {@code null}
     */
    public <T> Either<T, R> map(Function<L, T> mapper, Void placeholder) {
        Objects.requireNonNull(mapper);
        return hasRightValue() ?
                Either.of(null, getRightValue()) :
                Either.of(mapper.apply(getLeftValue()), null);
    }

    /**
     * @throws NullPointerException if {@code mapper} is {@code null}
     */
    public <T> Either<L, T> map(Void placeholder, Function<R, T> mapper) {
        Objects.requireNonNull(mapper);
        return hasLeftValue() ?
                Either.of(getLeftValue(), null) :
                Either.of(null, mapper.apply(getRightValue()));
    }

    public Object get() {
        return hasLeftValue() ? getLeftValue() : getRightValue();
    }

    public boolean hasLeftValue() {
        return rightValue == null;
    }

    public boolean hasRightValue() {
        return leftValue == null;
    }

    public boolean doesNotHaveLeftValue() {
        return leftValue == null;
    }

    public boolean doesNotHaveRightValue() {
        return rightValue == null;
    }

    @Override
    public String toString() {
        return getClass().getName() +
                (hasLeftValue() ? "(L=" + getLeftValue() + ")" : "(R=" + getRightValue() + ")");
    }
}
