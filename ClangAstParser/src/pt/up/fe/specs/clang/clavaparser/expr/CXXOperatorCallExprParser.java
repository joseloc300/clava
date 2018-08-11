/**
 * Copyright 2016 SPeCS.
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

package pt.up.fe.specs.clang.clavaparser.expr;

import java.util.List;

import pt.up.fe.specs.clang.ast.ClangNode;
import pt.up.fe.specs.clang.clavaparser.AClangNodeParser;
import pt.up.fe.specs.clang.clavaparser.ClangConverterTable;
import pt.up.fe.specs.clang.clavaparser.utils.ClangDataParsers;
import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.expr.CXXOperatorCallExpr;
import pt.up.fe.specs.clava.ast.expr.Expr;
import pt.up.fe.specs.clava.ast.expr.data.ExprData;
import pt.up.fe.specs.clava.ast.extra.NullNode;
import pt.up.fe.specs.util.SpecsCollections;
import pt.up.fe.specs.util.stringparser.StringParser;

public class CXXOperatorCallExprParser extends AClangNodeParser<CXXOperatorCallExpr> {

    public CXXOperatorCallExprParser(ClangConverterTable converter) {
        super(converter);
    }

    @Override
    protected CXXOperatorCallExpr parse(ClangNode node, StringParser parser) {

        // Examples:
        //
        // 'const value_type':'const class std::__cxx11::basic_string<char>' lvalue

        ExprData exprData = parser.apply(ClangDataParsers::parseExpr, node, getTypesMap());

        List<ClavaNode> children = parseChildren(node);

        // Check if last child is a null node
        if (SpecsCollections.last(children) instanceof NullNode) {
            children.remove(children.size() - 1);
        }

        // A CXXOperatorCall can have a variable number of operands (e.g., () operator )

        Expr operator = toExpr(children.get(0));
        List<Expr> operands = toExpr(SpecsCollections.subList(children, 1));

        throw new RuntimeException("deprecated");

        // return ClavaNodeFactory.cxxOperatorCallExpr(exprData, node.getInfo(), operator, operands);
    }

}
