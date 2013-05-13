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

import com.intellij.codeInsight.daemon.impl.HighlightInfo;
import com.intellij.codeInsight.daemon.impl.analysis.HighlightInfoHolder;
import com.intellij.codeInsight.daemon.impl.analysis.HighlightVisitorImpl;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@SuppressWarnings("WeakerAccess")
public class OOHighlightVisitorImpl extends HighlightVisitorImpl implements Cloneable
{
    @NotNull
    PsiResolveHelper myResolveHelper;

    public OOHighlightVisitorImpl(@NotNull PsiResolveHelper resolveHelper)
    {
        super(resolveHelper);
        myResolveHelper = resolveHelper;
    }

    @Override
    @NotNull
    public final OOHighlightVisitorImpl clone()
    {
        return new OOHighlightVisitorImpl(myResolveHelper);
    }

    @Override // Binary OO
    public void visitPolyadicExpression(PsiPolyadicExpression expression)
    {
        super.visitPolyadicExpression(expression);
        if (isHighlighted(expression)) {
            PsiExpression[] operands = expression.getOperands();
            PsiType lType = operands[0].getType();
            for (int i = 1; i < operands.length; i++) {
                PsiExpression operand = operands[i];
                PsiType rType = operand.getType();
                // TODO: case A + A + int ? A.add(A) : int
                lType = OOResolver.getOOType(lType, rType, expression.getTokenBeforeOperand(operand));
            }
            if (lType != OOResolver.NoType) {
                removeLastHighlight();
            }
        }
    }

    @Override // Unary OO
    public void visitPrefixExpression(PsiPrefixExpression expression)
    {
        super.visitPrefixExpression(expression);
        if (isHighlighted(expression)
            && OOResolver.getOOType(expression) != OOResolver.NoType) {
            removeLastHighlight();
        }
    }

    @Override // Index-Get OO
    public void visitExpression(PsiExpression expression)
    {
        super.visitExpression(expression);
        if (expression instanceof PsiArrayAccessExpression) {
            PsiArrayAccessExpression paa = (PsiArrayAccessExpression) expression;
            if (isHighlighted(paa.getArrayExpression()) && OOResolver.indexGet((PsiArrayAccessExpression) expression) != OOResolver.NoType) {
                removeLastHighlight();
            }
        }
    }

    @Override
    public void visitAssignmentExpression(PsiAssignmentExpression ass)
    {
        super.visitAssignmentExpression(ass);
        if ("=".equals(ass.getOperationSign().getText())) {
            // Index-Set OO
            if (ass.getLExpression() instanceof PsiArrayAccessExpression
                && isHighlighted(ass.getLExpression())) {
                PsiArrayAccessExpression paa = (PsiArrayAccessExpression) ass.getLExpression();
                if (OOResolver.indexSet(paa, ass.getRExpression()) != OOResolver.NoType) {
                    removeLastHighlight();
                }
            }
            // Implicit type conversion in assignment
            if (isHighlighted(ass) && OOResolver.isTypeConvertible(ass.getLExpression().getType(), ass.getRExpression())) {
                removeLastHighlight();
            }
        }
    }

    @Override // Implicit type conversion in variable declaration
    public void visitVariable(PsiVariable var)
    {
        super.visitVariable(var);
        if (var.hasInitializer() && isHighlighted(var) && OOResolver.isTypeConvertible(var.getType(), var.getInitializer())) {
            removeLastHighlight();
        }
    }

    private HighlightInfoHolder getMyHolder()
    {
        return (HighlightInfoHolder) Util.get(HighlightVisitorImpl.class, this, "myHolder", "b");
    }

    private boolean isHighlighted(PsiElement expression)
    {
        HighlightInfoHolder myHolder = getMyHolder();
        if (myHolder.hasErrorResults()) {
            HighlightInfo hi = myHolder.get(myHolder.size() - 1);
            if (hi.getSeverity() != HighlightSeverity.ERROR) {
                return false;
            }
            if (expression instanceof PsiVariable) { // workaround for variable declaration incompatible types highlight
                PsiVariable v = (PsiVariable) expression;
                return hi.startOffset == v.getTypeElement().getTextRange().getStartOffset() && hi.endOffset == v.getTextRange().getEndOffset();
            }
            TextRange tr = expression.getTextRange();
            return hi.startOffset == tr.getStartOffset() && hi.endOffset == tr.getEndOffset();
        }
        return false;
    }

    // TODO: what highlightInfo to delete?
    private void removeLastHighlight()
    {
        HighlightInfoHolder holder = getMyHolder();
        // remove highlight
        List<HighlightInfo> myInfos = (List<HighlightInfo>) Util.get(HighlightInfoHolder.class, holder, "myInfos", "e");
        myInfos.remove(getMyHolder().size() - 1);
        // update error count
        Util.set(HighlightInfoHolder.class, holder,
            (Integer) Util.get(HighlightInfoHolder.class, holder, "myErrorCount", "d") - 1, "myErrorCount", "d");
    }
}
