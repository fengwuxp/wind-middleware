package com.wind.common.exception;


import com.wind.common.message.MessagePlaceholder;
import org.jspecify.annotations.Nullable;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * 断言工具
 * 断言优先于 if 判断
 * copy form {@link org.springframework.util.Assert}
 *
 * @author wuxp
 * @see BaseException
 */
public final class AssertUtils {

    private AssertUtils() {
        throw new AssertionError();
    }

    /**
     * Assert a boolean expression, throwing an {@code BaseException}
     * if the expression evaluates to {@code false}.
     * <p>Call {@link #isTrue} if you wish to throw an {@code BaseException}
     * on an assertion failure.
     * <pre class="code">Assert.state(id == null, "The id property must not already be initialized");</pre>
     *
     * @param expression        a boolean expression
     * @param exceptionSupplier the exception message to use if the assertion fails
     * @throws RuntimeException if {@code expression} is {@code false}
     */
    public static void state(boolean expression, Supplier<RuntimeException> exceptionSupplier) {
        if (!expression) {
            throw Objects.requireNonNull(nullSafeGet(exceptionSupplier), "exception must not null");
        }
    }

    public static void isTrue(boolean expression, MessagePlaceholder placeholder) {
        isTrue(expression, placeholder.pattern(), placeholder.args());
    }

    /**
     * Assert a boolean expression, throwing an {@code BaseException}
     * if the expression evaluates to {@code false}.
     * <pre class="code">Assert.isTrue(i &gt; 0, "The value must be greater than zero");</pre>
     *
     * @param expression a boolean expression
     * @param message    the exception message to use if the assertion fails
     * @param args       message placeholder arguments
     * @throws BaseException if {@code expression} is {@code false}
     */
    public static void isTrue(boolean expression, String message, Object... args) {
        state(expression, () -> BaseException.common(MessagePlaceholder.of(message, args)));
    }

    /**
     * Assert a boolean expression, throwing an {@code BaseException}
     * if the expression evaluates to {@code false}.
     * <pre class="code">
     * Assert.isTrue(i &gt; 0, () -&gt; "The value '" + i + "' must be greater than zero");
     * </pre>
     *
     * @param expression      a boolean expression
     * @param messageSupplier a supplier for the exception message to use if the
     *                        assertion fails
     * @throws BaseException if {@code expression} is {@code false}
     *
     */
    public static void isTrue(boolean expression, Supplier<String> messageSupplier) {
        state(expression, () -> BaseException.common(nullSafeGet(messageSupplier)));
    }

    public static void isFalse(boolean expression, MessagePlaceholder placeholder) {
        isFalse(expression, placeholder.pattern(), placeholder.args());
    }

    public static void isFalse(boolean expression, String message, Object... args) {
        isTrue(!expression, message, args);
    }

    public static void isFalse(boolean expression, Supplier<String> messageSupplier) {
        isTrue(!expression, messageSupplier);
    }

    /**
     * Assert that an object is {@code null}.
     * <pre class="code">Assert.isNull(value, "The value must be null");</pre>
     *
     * @param object  the object to check
     * @param message the exception message to use if the assertion fails
     * @param args    message placeholder arguments
     * @throws BaseException if the object is not {@code null}
     */
    public static void isNull(@Nullable Object object, String message, Object... args) {
        isTrue(object == null, message, args);
    }

    /**
     * Assert that an object is {@code null}.
     * <pre class="code">
     * Assert.isNull(value, () -&gt; "The value '" + value + "' must be null");
     * </pre>
     *
     * @param object          the object to check
     * @param messageSupplier a supplier for the exception message to use if the
     *                        assertion fails
     * @throws BaseException if the object is not {@code null}
     *
     */
    public static void isNull(@Nullable Object object, Supplier<String> messageSupplier) {
        isTrue(object == null, messageSupplier);
    }

    public static void notNull(Object object, MessagePlaceholder placeholder) {
        notNull(object, placeholder.pattern(), placeholder.args());
    }

    /**
     * Assert that an object is not {@code null}.
     * <pre class="code">Assert.notNull(clazz, "The class must not be null");</pre>
     *
     * @param object  the object to check
     * @param message the exception message to use if the assertion fails
     * @throws BaseException if the object is {@code null}
     */
    public static void notNull(@Nullable Object object, String message, Object... args) {
        isTrue(object != null, message, args);
    }

    /**
     * Assert that an object is not {@code null}.
     * <pre class="code">
     * Assert.notNull(entity.getId(),
     *     () -&gt; "ID for entity " + entity.getName() + " must not be null");
     * </pre>
     *
     * @param object          the object to check
     * @param messageSupplier a supplier for the exception message to use if the
     *                        assertion fails
     * @throws BaseException if the object is {@code null}
     *
     */
    public static void notNull(@Nullable Object object, Supplier<String> messageSupplier) {
        isTrue(object != null, messageSupplier);
    }

    public static void hasLength(String text, MessagePlaceholder placeholder) {
        hasLength(text, placeholder.pattern(), placeholder.args());
    }

    /**
     * Assert that the given String is not empty; that is,
     * it must not be {@code null} and not the empty String.
     * <pre class="code">Assert.hasLength(name, "Name must not be empty");</pre>
     *
     * @param text    the String to check
     * @param message the exception message to use if the assertion fails
     * @throws BaseException if the text is empty
     * @see StringUtils#hasLength
     */
    public static void hasLength(@Nullable String text, String message, Object... args) {
        isTrue(StringUtils.hasLength(text), message, args);
    }

    /**
     * Assert that the given String is not empty; that is,
     * it must not be {@code null} and not the empty String.
     * <pre class="code">
     * Assert.hasLength(account.getName(),
     *     () -&gt; "Name for account '" + account.getId() + "' must not be empty");
     * </pre>
     *
     * @param text            the String to check
     * @param messageSupplier a supplier for the exception message to use if the
     *                        assertion fails
     * @throws BaseException if the text is empty
     * @see StringUtils#hasLength
     *
     */
    public static void hasLength(@Nullable String text, Supplier<String> messageSupplier) {
        isTrue(StringUtils.hasLength(text), messageSupplier);
    }

    /**
     * Assert that the given String contains valid text content; that is, it must not
     * be {@code null} and must contain at least one non-whitespace character.
     * <pre class="code">Assert.hasText(name, "'name' must not be empty");</pre>
     *
     * @param text    the String to check
     * @param message the exception message to use if the assertion fails
     * @throws BaseException if the text does not contain valid text content
     * @see StringUtils#hasText
     */
    public static void hasText(@Nullable String text, String message, Object... args) {
        isTrue(StringUtils.hasText(text), message, args);
    }

    /**
     * Assert that the given String contains valid text content; that is, it must not
     * be {@code null} and must contain at least one non-whitespace character.
     * <pre class="code">
     * Assert.hasText(account.getName(),
     *     () -&gt; "Name for account '" + account.getId() + "' must not be empty");
     * </pre>
     *
     * @param text            the String to check
     * @param messageSupplier a supplier for the exception message to use if the
     *                        assertion fails
     * @throws BaseException if the text does not contain valid text content
     * @see StringUtils#hasText
     *
     */
    public static void hasText(@Nullable String text, Supplier<String> messageSupplier) {
        isTrue(StringUtils.hasText(text), messageSupplier);
    }

    /**
     * Assert that two objects are equal.
     *
     * @param actual   the actual value
     * @param expected the expected value
     * @param message  the exception message to use if the assertion fails
     */
    public static void equals(@Nullable Object actual, @Nullable Object expected, String message) {
        equals(actual, expected, message, MessagePlaceholder.of(message));
    }

    /**
     * Assert that two objects are equal.
     *
     * @param actual      the actual value
     * @param expected    the expected value
     * @param placeholder a supplier for the exception message to use if the assertion fails
     */
    public static void equals(@Nullable Object actual, @Nullable Object expected, MessagePlaceholder placeholder) {
        equals(actual, expected, placeholder.pattern(), placeholder.args());
    }

    /**
     * Assert that two objects are equal.
     *
     * @param actual   the actual value
     * @param expected the expected value
     * @param message  a supplier for the exception message to use if the assertion fails
     */
    public static void equals(@Nullable Object actual, @Nullable Object expected, String message, Object... args) {
        isTrue(Objects.equals(actual, expected), message, args);
    }

    /**
     * Assert that the given text does not contain the given substring.
     * <pre class="code">Assert.doesNotContain(name, "rod", "Name must not contain 'rod'");</pre>
     *
     * @param textToSearch the text to search
     * @param substring    the substring to find within the text
     * @param message      the exception message to use if the assertion fails
     * @throws BaseException if the text contains the substring
     */
    public static void doesNotContain(@Nullable String textToSearch, String substring, String message, Object... args) {
        isFalse(StringUtils.hasLength(textToSearch) && StringUtils.hasLength(substring) &&
                textToSearch.contains(substring), message, args);
    }

    /**
     * Assert that the given text does not contain the given substring.
     * <pre class="code">
     * Assert.doesNotContain(name, forbidden, () -&gt; "Name must not contain '" + forbidden + "'");
     * </pre>
     *
     * @param textToSearch    the text to search
     * @param substring       the substring to find within the text
     * @param messageSupplier a supplier for the exception message to use if the
     *                        assertion fails
     * @throws BaseException if the text contains the substring
     *
     */
    public static void doesNotContain(@Nullable String textToSearch, String substring, Supplier<String> messageSupplier) {
        isFalse(StringUtils.hasLength(textToSearch) && StringUtils.hasLength(substring) &&
                textToSearch.contains(substring), messageSupplier);
    }

    public static void notEmpty(@Nullable Object[] array, MessagePlaceholder placeholder) {
        notEmpty(array, placeholder.pattern(), placeholder.args());
    }

    /**
     * Assert that an array contains elements; that is, it must not be
     * {@code null} and must contain at least one element.
     * <pre class="code">Assert.notEmpty(array, "The array must contain elements");</pre>
     *
     * @param array   the array to check
     * @param message the exception message to use if the assertion fails
     * @throws BaseException if the object array is {@code null} or contains no elements
     */
    public static void notEmpty(@Nullable Object[] array, String message, Object... args) {
        isFalse(ObjectUtils.isEmpty(array), message, args);
    }

    /**
     * Assert that an array contains elements; that is, it must not be
     * {@code null} and must contain at least one element.
     * <pre class="code">
     * Assert.notEmpty(array, () -&gt; "The " + arrayType + " array must contain elements");
     * </pre>
     *
     * @param array           the array to check
     * @param messageSupplier a supplier for the exception message to use if the
     *                        assertion fails
     * @throws BaseException if the object array is {@code null} or contains no elements
     *
     */
    public static void notEmpty(@Nullable Object[] array, Supplier<String> messageSupplier) {
        isFalse(ObjectUtils.isEmpty(array), messageSupplier);
    }

    /**
     * Assert that an array contains no {@code null} elements.
     * <p>Note: Does not complain if the array is empty!
     * <pre class="code">Assert.noNullElements(array, "The array must contain non-null elements");</pre>
     *
     * @param array   the array to check
     * @param message the exception message to use if the assertion fails
     * @throws BaseException if the object array contains a {@code null} element
     */
    public static void noNullElements(@Nullable Object[] array, String message, Object... args) {
        if (array != null) {
            for (Object element : array) {
                if (element == null) {
                    throw BaseException.common(MessagePlaceholder.of(message, args));
                }
            }
        }
    }

    /**
     * Assert that an array contains no {@code null} elements.
     * <p>Note: Does not complain if the array is empty!
     * <pre class="code">
     * Assert.noNullElements(array, () -&gt; "The " + arrayType + " array must contain non-null elements");
     * </pre>
     *
     * @param array           the array to check
     * @param messageSupplier a supplier for the exception message to use if the
     *                        assertion fails
     * @throws BaseException if the object array contains a {@code null} element
     *
     */
    public static void noNullElements(@Nullable Object[] array, Supplier<String> messageSupplier) {
        if (array != null) {
            for (Object element : array) {
                if (element == null) {
                    throw BaseException.common(nullSafeGet(messageSupplier));
                }
            }
        }
    }

    public static void notEmpty(@Nullable Collection<?> collection, MessagePlaceholder placeholder) {
        notEmpty(collection, placeholder.pattern(), placeholder.args());
    }

    /**
     * Assert that a collection contains elements; that is, it must not be
     * {@code null} and must contain at least one element.
     * <pre class="code">Assert.notEmpty(collection, "Collection must contain elements");</pre>
     *
     * @param collection the collection to check
     * @param message    the exception message to use if the assertion fails
     * @throws BaseException if the collection is {@code null} or
     *                       contains no elements
     */
    public static void notEmpty(@Nullable Collection<?> collection, String message, Object... args) {
        isFalse(CollectionUtils.isEmpty(collection), message, args);
    }

    /**
     * Assert that a collection contains elements; that is, it must not be
     * {@code null} and must contain at least one element.
     * <pre class="code">
     * Assert.notEmpty(collection, () -&gt; "The " + collectionType + " collection must contain elements");
     * </pre>
     *
     * @param collection      the collection to check
     * @param messageSupplier a supplier for the exception message to use if the
     *                        assertion fails
     * @throws BaseException if the collection is {@code null} or
     *                       contains no elements
     *
     */
    public static void notEmpty(@Nullable Collection<?> collection, Supplier<String> messageSupplier) {
        isFalse(CollectionUtils.isEmpty(collection), messageSupplier);
    }

    /**
     * Assert that a collection contains no {@code null} elements.
     * <p>Note: Does not complain if the collection is empty!
     * <pre class="code">Assert.noNullElements(collection, "Collection must contain non-null elements");</pre>
     *
     * @param collection the collection to check
     * @param message    the exception message to use if the assertion fails
     * @throws BaseException if the collection contains a {@code null} element
     *
     */
    public static void noNullElements(@Nullable Collection<?> collection, String message, Object... args) {
        if (collection != null) {
            for (Object element : collection) {
                if (element == null) {
                    throw BaseException.common(MessagePlaceholder.of(message, args));
                }
            }
        }
    }

    /**
     * Assert that a collection contains no {@code null} elements.
     * <p>Note: Does not complain if the collection is empty!
     * <pre class="code">
     * Assert.noNullElements(collection, () -&gt; "Collection " + collectionName + " must contain non-null elements");
     * </pre>
     *
     * @param collection      the collection to check
     * @param messageSupplier a supplier for the exception message to use if the
     *                        assertion fails
     * @throws BaseException if the collection contains a {@code null} element
     */
    public static void noNullElements(@Nullable Collection<?> collection, Supplier<String> messageSupplier) {
        if (collection != null) {
            for (Object element : collection) {
                if (element == null) {
                    throw BaseException.common(nullSafeGet(messageSupplier));
                }
            }
        }
    }

    public static void notEmpty(@Nullable Map<?, ?> map, MessagePlaceholder placeholder) {
        notEmpty(map, placeholder.pattern(), placeholder.args());
    }

    /**
     * Assert that a Map contains entries; that is, it must not be {@code null}
     * and must contain at least one entry.
     * <pre class="code">Assert.notEmpty(map, "Map must contain entries");</pre>
     *
     * @param map     the map to check
     * @param message the exception message to use if the assertion fails
     * @throws BaseException if the map is {@code null} or contains no entries
     */
    public static void notEmpty(@Nullable Map<?, ?> map, String message, Object... args) {
        isFalse(CollectionUtils.isEmpty(map), message, args);
    }

    /**
     * Assert that a Map contains entries; that is, it must not be {@code null}
     * and must contain at least one entry.
     * <pre class="code">
     * Assert.notEmpty(map, () -&gt; "The " + mapType + " map must contain entries");
     * </pre>
     *
     * @param map             the map to check
     * @param messageSupplier a supplier for the exception message to use if the
     *                        assertion fails
     * @throws BaseException if the map is {@code null} or contains no entries
     *
     */
    public static void notEmpty(@Nullable Map<?, ?> map, Supplier<String> messageSupplier) {
        isFalse(CollectionUtils.isEmpty(map), messageSupplier);
    }

    /**
     * Assert that the provided object is an instance of the provided class.
     * <pre class="code">Assert.instanceOf(Foo.class, foo, "Foo expected");</pre>
     *
     * @param type    the type to check against
     * @param obj     the object to check
     * @param message a message which will be prepended to provide further context.
     *                If it is empty or ends in ":" or ";" or "," or ".", a full exception message
     *                will be appended. If it ends in a space, the name of the offending object's
     *                type will be appended. In any other case, a ":" with a space and the name
     *                of the offending object's type will be appended.
     * @throws BaseException if the object is not an instance of type
     */
    public static void isInstanceOf(Class<?> type, @Nullable Object obj, String message) {
        notNull(type, "Type to check against must not be null");
        if (!type.isInstance(obj)) {
            instanceCheckFailed(type, obj, message);
        }
    }

    /**
     * Assert that the provided object is an instance of the provided class.
     * <pre class="code">
     * Assert.instanceOf(Foo.class, foo, () -&gt; "Processing " + Foo.class.getSimpleName() + ":");
     * </pre>
     *
     * @param type            the type to check against
     * @param obj             the object to check
     * @param messageSupplier a supplier for the exception message to use if the
     *                        assertion fails. See {@link #isInstanceOf(Class, Object, String)} for details.
     * @throws BaseException if the object is not an instance of type
     *
     */
    public static void isInstanceOf(Class<?> type, @Nullable Object obj, Supplier<String> messageSupplier) {
        notNull(type, "Type to check against must not be null");
        if (!type.isInstance(obj)) {
            instanceCheckFailed(type, obj, nullSafeGet(messageSupplier));
        }
    }


    /**
     * Assert that {@code superType.isAssignableFrom(subType)} is {@code true}.
     * <pre class="code">Assert.isAssignable(Number.class, myClass, "Number expected");</pre>
     *
     * @param superType the super type to check against
     * @param subType   the sub type to check
     * @param message   a message which will be prepended to provide further context.
     *                  If it is empty or ends in ":" or ";" or "," or ".", a full exception message
     *                  will be appended. If it ends in a space, the name of the offending sub type
     *                  will be appended. In any other case, a ":" with a space and the name of the
     *                  offending sub type will be appended.
     * @throws BaseException if the classes are not assignable
     */
    public static void isAssignable(Class<?> superType, @Nullable Class<?> subType, String message) {
        notNull(superType, "Super type to check against must not be null");
        if (subType == null || !superType.isAssignableFrom(subType)) {
            assignableCheckFailed(superType, subType, message);
        }
    }

    /**
     * Assert that {@code superType.isAssignableFrom(subType)} is {@code true}.
     * <pre class="code">
     * Assert.isAssignable(Number.class, myClass, () -&gt; "Processing " + myAttributeName + ":");
     * </pre>
     *
     * @param superType       the super type to check against
     * @param subType         the sub type to check
     * @param messageSupplier a supplier for the exception message to use if the
     *                        assertion fails. See {@link #isAssignable(Class, Class, String)} for details.
     * @throws BaseException if the classes are not assignable
     *
     */
    public static void isAssignable(Class<?> superType, @Nullable Class<?> subType, Supplier<String> messageSupplier) {
        notNull(superType, "Super type to check against must not be null");
        if (subType == null || !superType.isAssignableFrom(subType)) {
            assignableCheckFailed(superType, subType, nullSafeGet(messageSupplier));
        }
    }

    /**
     * Assert that {@code superType.isAssignableFrom(subType)} is {@code true}.
     * <pre class="code">Assert.isAssignable(Number.class, myClass);</pre>
     *
     * @param superType the super type to check
     * @param subType   the sub type to check
     * @throws BaseException if the classes are not assignable
     */
    public static void isAssignable(Class<?> superType, Class<?> subType) {
        isAssignable(superType, subType, "");
    }


    private static void instanceCheckFailed(Class<?> type, @Nullable Object obj, @Nullable String msg) {
        String className = (obj != null ? obj.getClass().getName() : "null");
        String result = "";
        boolean defaultMessage = true;
        if (StringUtils.hasLength(msg)) {
            if (endsWithSeparator(msg)) {
                result = msg + " ";
            } else {
                result = messageWithTypeName(msg, className);
                defaultMessage = false;
            }
        }
        if (defaultMessage) {
            result = result + ("Object of class [" + className + "] must be an instance of " + type);
        }
        throw BaseException.common(result);
    }

    private static void assignableCheckFailed(Class<?> superType, @Nullable Class<?> subType, @Nullable String msg) {
        String result = "";
        boolean defaultMessage = true;
        if (StringUtils.hasLength(msg)) {
            if (endsWithSeparator(msg)) {
                result = msg + " ";
            } else {
                result = messageWithTypeName(msg, subType);
                defaultMessage = false;
            }
        }
        if (defaultMessage) {
            result = result + (subType + " is not assignable to " + superType);
        }
        throw BaseException.common(result);
    }

    private static boolean endsWithSeparator(String msg) {
        return (msg.endsWith(":") || msg.endsWith(";") || msg.endsWith(",") || msg.endsWith("."));
    }

    private static String messageWithTypeName(String msg, @Nullable Object typeName) {
        return msg + (msg.endsWith(" ") ? "" : ": ") + typeName;
    }

    @Nullable
    private static <T> T nullSafeGet(@Nullable Supplier<T> messageSupplier) {
        return (messageSupplier != null ? messageSupplier.get() : null);
    }
}
