/**
 * Copyright 2018 SPeCS.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License. under the License.
 */

package pt.up.fe.specs.clava.ast.expr;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ClavaNodeInfo;
import pt.up.fe.specs.clava.ast.expr.data.ExprData;

/**
 * Represents a C++11 pack expansion that produces a sequence of expressions.
 *
 * @author JoaoBispo
 *
 */
public class PackExpansionExpr extends Expr {

    public PackExpansionExpr(ExprData exprData, ClavaNodeInfo info, Expr pattern) {
        this(exprData, info, Arrays.asList(pattern));
    }

    private PackExpansionExpr(ExprData exprData, ClavaNodeInfo info, Collection<? extends ClavaNode> children) {
        super(exprData, info, children);
    }

    @Override
    protected ClavaNode copyPrivate() {
        return new PackExpansionExpr(getExprData(), getInfo(), Collections.emptyList());
    }

    public Expr getPattern() {
        return getChild(Expr.class, 0);
    }

    @Override
    public String getCode() {
        return getPattern().getCode() + "...";
    }

}