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

import com.intellij.psi.*;
import com.intellij.psi.util.TypeConversionUtil;
import javaoo.OOMethods;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("WeakerAccess")
public class OOResolver
{
    public static final PsiType NoType = TypeConversionUtil.NULL_TYPE;

    private OOResolver() {}

    @NotNull
    public static PsiType getOOType(PsiBinaryExpression e)
    {
        if (e == null || e.getROperand() == null) {
            return NoType;
        }
        return getOOType(e.getLOperand().getType(), e.getROperand().getType(), e.getOperationSign());
    }

    @NotNull
    public static PsiType getOOType(@Nullable PsiType ltype, @Nullable PsiType rtype, @Nullable PsiJavaToken op)
    {
        if (op == null) {
            return NoType;
        }
        String methodname = OOMethods.binary.get(op.getText());
        if (methodname != null && rtype != null) {
            PsiType res = resolveMethod(ltype, methodname, rtype);
            if (res != null) {
                return res;
            }
        }
        return NoType;
    }

    @NotNull
    public static PsiType getOOType(PsiPrefixExpression e)
    {
        if (e == null || e.getOperand() == null) {
            return NoType;
        }
        PsiType optype = e.getOperand().getType();
        String methodname = OOMethods.unary.get(e.getOperationSign().getText());
        if (methodname != null) {
            PsiType res = resolveMethod(optype, methodname);
            if (res != null) {
                return res;
            }
        }
        return NoType;
    }

    @NotNull
    public static PsiType indexGet(PsiArrayAccessExpression e)
    {
        if (e == null || e.getIndexExpression() == null) {
            return NoType;
        }
        PsiType res = resolveMethod(e.getArrayExpression().getType(), OOMethods.indexGet, e.getIndexExpression().getType());
        return res != null ? res : NoType;
    }

    @NotNull
    public static PsiType indexSet(PsiArrayAccessExpression paa, @Nullable PsiExpression value)
    {
        if (paa == null) {
            return NoType;
        }
        for (String method : OOMethods.indexSet) {
            PsiType res = resolveMethod(paa.getArrayExpression(), method, paa.getIndexExpression(), value);
            if (res != null) {
                return res;
            }
        }
        return NoType;
    }

    public static boolean isTypeConvertible(@Nullable PsiType to, @Nullable PsiExpression from)
    {
        return from != null && resolveMethod(to, OOMethods.valueOf, from.getType()) != null;
    }

    @Nullable
    public static PsiType resolveMethod(PsiExpression clas, String methodName, @NotNull PsiExpression... args)
    {
        if (clas == null || methodName == null) {
            return null;
        }
        PsiType[] argTypes = new PsiType[args.length];
        for (int i = 0; i < args.length; i++) {
            if (args[i] == null) {
                return null;
            }
            argTypes[i] = args[i].getType();
        }
        return resolveMethod(clas.getType(), methodName, argTypes);
    }

    // TODO: find a better way to do it
    @Nullable
    public static PsiType resolveMethod(@Nullable PsiType type, String methodName, @NotNull PsiType... argTypes)
    {
        // fix for null error when class for method has not yet been imported in intellij
        if (type == null || !(type instanceof PsiClassType) || methodName == null) {
            return null;
        }
        for (PsiType a : argTypes) {
            if (a == null) {
                return null;
            }
        }
        PsiClassType clas = (PsiClassType) type;
        PsiSubstitutor subst = clas.resolveGenerics().getSubstitutor();

        PsiClass psiClass = clas.resolve();
        // fix for null error when class for method has not yet been imported in intellij
        if (psiClass != null) {
            PsiMethod[] methods = psiClass.findMethodsByName(methodName, true);
            for (PsiMethod method : methods) {
                PsiParameter[] pars = method.getParameterList().getParameters();
                if (pars.length == argTypes.length) {
                    boolean ok = true;
                    for (int i = 0; i < pars.length; i++) {
                        ok &= subst.substitute(pars[i].getType()).isAssignableFrom(argTypes[i]);
                    }
                    if (ok) {
                        return subst.substitute(method.getReturnType());
                    }
                }
            }
        }
        return null;
    }
}
