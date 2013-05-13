/* Copyright 2013 Artem Melentyev <amelentev@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package javaoo.idea;

import com.intellij.lang.ASTNode;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.psi.impl.source.tree.JavaElementType;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

@SuppressWarnings({"UncheckedExceptionClass", "CheckedExceptionClass", "WeakerAccess"})
public class Util
{
    private Util() {}

    public static <T> Object get(Class<T> clas, T obj, String... fields)
    {
        try {
            for (Field classField : clas.getDeclaredFields()) {
                for (String field : fields) {
                    if (classField.getName().equals(field)) {
                        classField.setAccessible(true);
                        return classField.get(obj);
                    }
                }
            }
            // basically to avoid it returning null. Not the best solution really, but it should never
            // reach here either.
            Field f = clas.getDeclaredField(fields[0]);
            f.setAccessible(true);
            return f.get(obj);
        } catch (Exception e) {
            throw sneakyThrow(e);
        }
    }

    public static void set(Class<?> cl, Object obj, Object val, String... fields)
    {
        try {
            for (Field classField : cl.getDeclaredFields()) {
                for (String field : fields) {
                    if (classField.getName().equals(field)) {
                        classField.setAccessible(true);
                        classField.set(obj, val);
                    }
                }
            }
        } catch (Exception e) {
            throw sneakyThrow(e);
        }
    }

    public static Object invoke(Class<?> cl, String[] method, Class<?>[] targs, Object[] args)
    {
        try {
            for (Method possibleMethod : cl.getDeclaredMethods()) {
                for (String methodName : method) {
                    if (possibleMethod.getName().equals(methodName)) {
                        possibleMethod.setAccessible(true);
                        return possibleMethod.invoke(null, args);
                    }
                }
            }
            // basically to avoid it returning null. Not the best solution really, but it should never
            // reach here either.
            Method m = cl.getDeclaredMethod(method[0], targs);
            m.setAccessible(true);
            return m.invoke(null, args);
        } catch (InvocationTargetException e) {
            if (e.getTargetException() instanceof ProcessCanceledException) {
                return null; // cancelled?
            }
            throw sneakyThrow(e);
        } catch (Exception e) {
            throw sneakyThrow(e);
        }
    }

    public static void setJavaElementConstructor(IElementType et, Class<? extends ASTNode> clas)
    {
        try {
            set(JavaElementType.JavaCompositeElementType.class, et, clas.getConstructor(), "myConstructor", "a");
        } catch (Exception e) {
            throw sneakyThrow(e);
        }
    }

    public static RuntimeException sneakyThrow(Throwable ex)
    {
        return sneakyThrowInner(ex);
    }

    private static <T extends Throwable> T sneakyThrowInner(Throwable ex) throws T
    {
        throw (T) ex;
    }
}