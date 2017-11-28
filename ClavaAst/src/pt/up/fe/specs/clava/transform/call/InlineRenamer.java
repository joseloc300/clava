/**
 * Copyright 2017 SPeCS.
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

package pt.up.fe.specs.clava.transform.call;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import pt.up.fe.specs.clava.ClavaNodeInfo;
import pt.up.fe.specs.clava.Types;
import pt.up.fe.specs.clava.ast.ClavaNodeFactory;
import pt.up.fe.specs.clava.ast.decl.FunctionDecl;
import pt.up.fe.specs.clava.ast.decl.ParmVarDecl;
import pt.up.fe.specs.clava.ast.decl.VarDecl;
import pt.up.fe.specs.clava.ast.decl.data.InitializationStyle;
import pt.up.fe.specs.clava.ast.decl.data.VarDeclData;
import pt.up.fe.specs.clava.ast.expr.ArraySubscriptExpr;
import pt.up.fe.specs.clava.ast.expr.CallExpr;
import pt.up.fe.specs.clava.ast.expr.DeclRefExpr;
import pt.up.fe.specs.clava.ast.expr.Expr;
import pt.up.fe.specs.clava.ast.expr.Literal;
import pt.up.fe.specs.clava.ast.expr.data.ExprData;
import pt.up.fe.specs.clava.ast.stmt.DeclStmt;
import pt.up.fe.specs.clava.ast.stmt.ReturnStmt;
import pt.up.fe.specs.clava.ast.stmt.Stmt;
import pt.up.fe.specs.clava.ast.type.Type;
import pt.up.fe.specs.clava.ast.type.data.TypeData;
import pt.up.fe.specs.clava.language.CastKind;
import pt.up.fe.specs.util.SpecsCollections;
import pt.up.fe.specs.util.classmap.BiConsumerClassMap;
import pt.up.fe.specs.util.collections.AccumulatorMap;

public class InlineRenamer {

    private final CallExpr call;
    private final FunctionDecl functionDecl;
    private final List<Stmt> stmts;
    private final Set<String> usedNames;

    private final BiConsumerClassMap<Expr, ParmVarDecl> argumentsRenamers;

    // Statements that need to be called before the inlined statements
    private final List<Stmt> prefixStmts;
    private final Map<String, String> renameMap;
    private final Set<String> newNames;
    private Expr callReplacement;

    private AccumulatorMap<String> newNamesCounter;

    public InlineRenamer(CallExpr call, FunctionDecl functionDecl, List<Stmt> stmts, Set<String> usedNames) {
        this.call = call;
        this.functionDecl = functionDecl;
        this.stmts = stmts;
        this.usedNames = usedNames;

        this.argumentsRenamers = buildArgumentsRenamers();

        this.prefixStmts = new ArrayList<>();
        this.renameMap = new HashMap<>();
        this.newNames = new HashSet<>();
        this.callReplacement = null;

        this.newNamesCounter = new AccumulatorMap<>();
    }

    private BiConsumerClassMap<Expr, ParmVarDecl> buildArgumentsRenamers() {
        BiConsumerClassMap<Expr, ParmVarDecl> argumentsRenamers = new BiConsumerClassMap<>();

        argumentsRenamers.put(DeclRefExpr.class, this::argumentRename);
        argumentsRenamers.put(ArraySubscriptExpr.class, this::argumentRename);
        argumentsRenamers.put(Literal.class, this::argumentRenameGeneric);

        return argumentsRenamers;
    }

    public List<Stmt> getPrefixStmts() {
        return prefixStmts;
    }

    public Map<String, String> getRenameMap() {
        return renameMap;
    }

    public List<Stmt> apply() {
        String calleeName = call.getCalleeName();

        List<ParmVarDecl> parameters = functionDecl.getParameters();
        List<Expr> arguments = call.getArgs();

        // Map declaration names
        for (int i = 0; i < parameters.size(); i++) {
            // If no more arguments, just prefix the name of the call
            if (arguments.size() < i) {
                renameParamWithoutArg(calleeName, parameters.get(i));
                continue;
            }

            argumentRename(arguments.get(i), parameters.get(i));
        }

        // Add VarDecls to rename map
        stmts.stream().flatMap(node -> node.getDescendantsAndSelfStream())
                .filter(VarDecl.class::isInstance)
                .map(VarDecl.class::cast)
                .forEach(varDecl -> renameVarDecl(calleeName, varDecl));

        // Apply renames
        return applyRenames();

    }

    private List<Stmt> applyRenames() {
        stmts.stream().flatMap(node -> node.getDescendantsAndSelfStream())
                .filter(node -> node instanceof VarDecl || node instanceof DeclRefExpr)
                .forEach(node -> {
                    if (node instanceof VarDecl) {
                        // Check if name is in rename map
                        String declName = ((VarDecl) node).getDeclName();
                        String newName = renameMap.get(declName);
                        if (newName == null) {
                            return;
                        }

                        ((VarDecl) node).setDeclName(newName);
                        return;
                    }

                    if (node instanceof DeclRefExpr) {
                        // Check if name is in rename map
                        String declName = ((DeclRefExpr) node).getRefName();
                        String newName = renameMap.get(declName);
                        if (newName == null) {
                            return;
                        }

                        ((DeclRefExpr) node).setRefName(newName);
                        return;
                    }

                    throw new RuntimeException("Not implemented: " + node.getNodeName());
                });

        Optional<Stmt> returnReplacement = getReturnReplacement();

        // Add return replacement
        returnReplacement.ifPresent(stmts::add);

        // Prefix new statements
        return SpecsCollections.concat(prefixStmts, stmts);
    }

    private Optional<Stmt> getReturnReplacement() {
        // If has a return statement, create temporary name a replace return with assigment to this variable
        Optional<ReturnStmt> returnStmt = SpecsCollections.reverseStream(stmts)
                .filter(ReturnStmt.class::isInstance)
                .map(ReturnStmt.class::cast)
                .findFirst();

        if (!returnStmt.isPresent()) {
            return Optional.empty();
        }

        // Create new name
        // Assign return expression to that name
        // Save new name expression in callReplacement

        throw new RuntimeException("Not supported yet when function to inline has a return statement");
    }

    public Optional<Expr> getCallReplacement() {
        return Optional.ofNullable(callReplacement);
    }

    private void argumentRename(Expr expr, ParmVarDecl parmVarDecl) {
        argumentsRenamers.accept(expr, parmVarDecl);
    }

    /**
     * Reference to a variable, rename to the name of the original argument, if different.
     * 
     * @param expr
     * @param parmVarDecl
     */
    private void argumentRename(DeclRefExpr expr, ParmVarDecl parmVarDecl) {
        String paramName = parmVarDecl.getDeclName();
        String argName = expr.getRefName();

        // If the name is the same, do nothing
        if (paramName.equals(argName)) {
            return;
        }

        // Rename parameter name to be the same as the argument name
        renameMap.put(paramName, argName);
    }

    private void argumentRename(ArraySubscriptExpr expr, ParmVarDecl parmVarDecl) {

        // Get new name
        String newName = getSimpleName(call.getCalleeName(), parmVarDecl.getDeclName());

        // Add renaming
        addRename(parmVarDecl.getDeclName(), newName);

        StringBuilder newStmt = new StringBuilder();

        // Get arity of parameter
        Type paramType = parmVarDecl.getType();
        System.out.println("PARAM TYPE:" + paramType);
        System.out.println("ARITY:" + Types.getPointerArity(paramType));
        System.out.println("ELEMENT TYPE:" + Types.getElement(paramType));

        int pointerArity = Types.getPointerArity(paramType);
        Type elementType = Types.getElement(paramType);

        Type newType = elementType;
        for (int i = 0; i < pointerArity; i++) {
            newType = ClavaNodeFactory.pointerType(new TypeData("dummy"), ClavaNodeInfo.undefinedInfo(),
                    newType);
        }

        Expr exprWithCast = ClavaNodeFactory.cStyleCastExpr(CastKind.NO_OP, new ExprData(newType),
                ClavaNodeInfo.undefinedInfo(), expr);

        System.out.println("NEW TYPE:" + newType.getCode());

        // Add statement with declaration of variable
        VarDeclData varDeclData = parmVarDecl.getVarDeclData().copy();
        varDeclData.setInitKind(InitializationStyle.CINIT);

        VarDecl varDecl = ClavaNodeFactory.varDecl(varDeclData, newName, newType,
                parmVarDecl.getDeclData(), ClavaNodeInfo.undefinedInfo(), exprWithCast);

        DeclStmt declStmt = ClavaNodeFactory.declStmt(ClavaNodeInfo.undefinedInfo(), Arrays.asList(varDecl));
        prefixStmts.add(declStmt);

    }

    private void argumentRenameGeneric(Expr expr, ParmVarDecl parmVarDecl) {

        // Get new name
        String newName = getSimpleName(call.getCalleeName(), parmVarDecl.getDeclName());

        // Add renaming
        addRename(parmVarDecl.getDeclName(), newName);

        // Add statement with declaration of variable
        VarDeclData varDeclData = parmVarDecl.getVarDeclData().copy();
        varDeclData.setInitKind(InitializationStyle.CINIT);

        VarDecl varDecl = ClavaNodeFactory.varDecl(varDeclData, newName, parmVarDecl.getType(),
                parmVarDecl.getDeclData(), ClavaNodeInfo.undefinedInfo(), expr);

        DeclStmt declStmt = ClavaNodeFactory.declStmt(ClavaNodeInfo.undefinedInfo(), Arrays.asList(varDecl));
        prefixStmts.add(declStmt);

        // System.out.println("ARRAY SUB");
        // System.out.println("ARG:" + expr.getCode());
        // System.out.println("PARAM:" + parmVarDecl.getCode());
        // System.out.println("ARG TYPE:" + expr.getType());
        // System.out.println("PARAM TYPE:" + parmVarDecl.getType());
    }

    /**
     * Renames a parameter that has no corresponding argument.
     * 
     * @param parmVarDecl
     * @param calleeName
     */
    private void renameParamWithoutArg(String calleeName, ParmVarDecl parmVarDecl) {
        // Get new name
        String newName = getSimpleName(calleeName, parmVarDecl.getDeclName());

        // Add renaming
        addRename(parmVarDecl.getDeclName(), newName);

        // Add statement with declaration of variable
        VarDecl varDecl = ClavaNodeFactory.varDecl(parmVarDecl.getVarDeclData(), newName, parmVarDecl.getType(),
                parmVarDecl.getDeclData(), ClavaNodeInfo.undefinedInfo(), parmVarDecl.getInit().orElse(null));

        DeclStmt declStmt = ClavaNodeFactory.declStmt(ClavaNodeInfo.undefinedInfo(), Arrays.asList(varDecl));
        prefixStmts.add(declStmt);
    }

    private void renameVarDecl(String calleeName, VarDecl varDecl) {
        // Get new name
        String newName = getSimpleName(calleeName, varDecl.getDeclName());

        // Add renaming
        addRename(varDecl.getDeclName(), newName);
    }

    private void addRename(String oldName, String newName) {
        String previousName = renameMap.put(oldName, newName);
        if (previousName != null) {
            throw new RuntimeException("Two variables with the same name inside the function (" + oldName
                    + "), are they in different scopes?");
        }

        usedNames.add(newName);
    }

    private String getSimpleName(String calleeName, String declName) {
        String newName = calleeName + "_" + declName;

        // If new name is valid, just return
        if (isNewNameValid(newName)) {
            return newName;
        }

        // Try creating a new name
        int id = newNamesCounter.getCount(newName);
        String newNameBuilder = newName + "_" + id;

        while (!isNewNameValid(newNameBuilder)) {
            id = newNamesCounter.add(newName);
            newNameBuilder = newName + "_" + id;
        }

        return newNameBuilder;
    }

    private boolean isNewNameValid(String newName) {
        if (newNames.contains(newName)) {
            return false;
        }

        if (usedNames.contains(newName)) {
            return false;
        }

        return true;
    }
}