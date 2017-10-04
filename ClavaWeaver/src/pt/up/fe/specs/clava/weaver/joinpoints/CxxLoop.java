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

package pt.up.fe.specs.clava.weaver.joinpoints;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.lara.interpreter.utils.DefMap;

import com.google.common.base.Preconditions;

import pt.up.fe.specs.clava.ClavaLog;
import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ClavaNodes;
import pt.up.fe.specs.clava.ast.ClavaNodeFactory;
import pt.up.fe.specs.clava.ast.expr.DeclRefExpr;
import pt.up.fe.specs.clava.ast.expr.data.ExprUse;
import pt.up.fe.specs.clava.ast.stmt.ForStmt;
import pt.up.fe.specs.clava.ast.stmt.LiteralStmt;
import pt.up.fe.specs.clava.ast.stmt.LoopStmt;
import pt.up.fe.specs.clava.ast.stmt.Stmt;
import pt.up.fe.specs.clava.ast.stmt.WhileStmt;
import pt.up.fe.specs.clava.weaver.CxxJoinpoints;
import pt.up.fe.specs.clava.weaver.abstracts.ACxxWeaverJoinPoint;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.ALoop;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AScope;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AStatement;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.enums.ALoopKindEnum;
import pt.up.fe.specs.clava.weaver.defs.CxxLoopDefs;
import pt.up.fe.specs.util.SpecsEnums;
import pt.up.fe.specs.util.lazy.Lazy;
import pt.up.fe.specs.util.lazy.ThreadSafeLazy;

public class CxxLoop extends ALoop {

    private static final Lazy<Map<Class<? extends LoopStmt>, ALoopKindEnum>> LOOP_TYPE = new ThreadSafeLazy<>(
            () -> buildLoopTypeMap());

    private static Map<Class<? extends LoopStmt>, ALoopKindEnum> buildLoopTypeMap() {
        HashMap<Class<? extends LoopStmt>, ALoopKindEnum> loopTypes = new HashMap<>();

        loopTypes.put(ForStmt.class, ALoopKindEnum.FOR);
        loopTypes.put(WhileStmt.class, ALoopKindEnum.WHILE);

        return loopTypes;
    }

    private final LoopStmt loop;
    private final ACxxWeaverJoinPoint parent;

    public CxxLoop(LoopStmt loop, ACxxWeaverJoinPoint parent) {
        super(new CxxStatement(loop, parent));

        this.loop = loop;
        this.parent = parent;
    }

    @Override
    public String getKindImpl() {
        ALoopKindEnum loopType = LOOP_TYPE.get().get(loop.getClass());

        Preconditions.checkNotNull(loopType,
                "Could not determine type of node '" + loop.getClass().getSimpleName() + "'");

        return loopType.name().toLowerCase();
    }

    @Override
    protected DefMap<?> getDefMap() {
        return CxxLoopDefs.getDefMap();
    }

    /*
    @Override
    public int getNumIterations() {
        // TODO Auto-generated method stub
        return 0;
    }
    */

    /*
    @Override
    public int getIncrementValue() {
        // Only supported for loops of type 'for'
        if (!(loop instanceof ForStmt)) {
            return 0;
        }

        ForStmt forLoop = (ForStmt) loop;

        Stmt inc = forLoop.getInc().orElse(null);
        if (inc == null) {
            return 0;
        }

        // TODO: Regular expression for <VAR_NAME>++; / <VAR_NAME>--;
        System.out.println("INC CODE:" + inc);

        return 0;
    }
    */

    @Override
    public Boolean getIsInnermostImpl() {
        // Loop is innermost if none of its descendants is a loop
        Optional<ClavaNode> anotherLoop = loop.getDescendantsStream()
                .filter(node -> node instanceof LoopStmt)
                .findFirst();

        return !anotherLoop.isPresent();
    }

    @Override
    public Boolean getIsOutermostImpl() {
        // Loop is outermost if none of its ancestors is a loop
        Optional<ClavaNode> anotherLoop = loop.getAscendantsStream()
                .filter(node -> node instanceof LoopStmt)
                .findFirst();

        return !anotherLoop.isPresent();
    }

    @Override
    public Integer getNestedLevelImpl() {
        // Go back and count how many Loops there are
        long parentLoops = loop.getAscendantsStream()
                .filter(node -> node instanceof LoopStmt)
                .count();

        return (int) parentLoops;
    }

    @Override
    public String getControlVarImpl() {

        // Only supported for loops of type 'for'
        if (!(loop instanceof ForStmt)) {
            return null;
        }

        ForStmt forStmt = (ForStmt) loop;

        Stmt inc = forStmt.getInc().orElse(null);
        if (inc == null) {
            return null;
        }

        List<String> controlVars = inc.getDescendants(DeclRefExpr.class).stream()
                .filter(d -> d.use() == ExprUse.READWRITE || d.use() == ExprUse.WRITE).map(DeclRefExpr::getRefName)
                .collect(Collectors.toList());

        if (controlVars.isEmpty()) {

            ClavaLog.info("Could not find control variable for loop in location: " + loop.getLocation());

            return null;
        }

        if (controlVars.size() > 1) {

            ClavaLog.info("Found more than one control variable (" + controlVars + ") for loop in location: "
                    + loop.getLocation());
        }

        return controlVars.get(0);

        // // 1. Find control var in the initialization
        // Stmt init = forStmt.getInit().orElse(null);
        // if (init != null) {
        //
        // // 1.1 When there is only initialization
        // DeclRefExpr expr = init.getFirstDescendantsAndSelf(DeclRefExpr.class).orElse(null);
        // if (expr != null) {
        // return expr.getRefName();
        // }
        //
        // // 1.2 When there is declaration and initialization
        // VarDecl decl = init.getFirstDescendantsAndSelf(VarDecl.class).orElse(null);
        // if (decl != null) {
        // return decl.getDeclName();
        // }
        //
        // }
        //
        // // 2. Find control var in the condition
        // Stmt cond = forStmt.getCond().orElse(null);
        // if (cond != null) {
        // DeclRefExpr expr = cond.getFirstDescendantsAndSelf(DeclRefExpr.class).orElse(null);
        // if (expr != null) {
        // return expr.getRefName();
        // }
        // }
        //
        // // 3. Find control var in the increment
        // Stmt inc = forStmt.getInc().orElse(null);
        // if (inc != null) {
        // DeclRefExpr expr = inc.getFirstDescendantsAndSelf(DeclRefExpr.class).orElse(null);
        // if (expr != null) {
        // return expr.getRefName();
        // }
        // }
        // return null;
    }

    @Override
    public List<? extends AStatement> selectInit() {
        if (!(loop instanceof ForStmt)) {
            return Collections.emptyList();
        }

        Stmt init = ((ForStmt) loop).getInit().orElse(null);
        if (init == null) {
            return Collections.emptyList();

        }

        return Arrays.asList(CxxJoinpoints.create(init, this, AStatement.class));
    }

    @Override
    public List<? extends AStatement> selectCond() {
        ClavaNode condition = loop.getStmtCondition().orElse(null);

        if (condition == null) {
            return Collections.emptyList();
        }

        return Arrays.asList(CxxJoinpoints.create(ClavaNodes.toStmt(condition), this, AStatement.class));
    }

    @Override
    public List<? extends AStatement> selectStep() {
        if (!(loop instanceof ForStmt)) {
            return Collections.emptyList();
        }

        Stmt inc = ((ForStmt) loop).getInc().orElse(null);
        if (inc == null) {
            return Collections.emptyList();

        }

        return Arrays.asList(CxxJoinpoints.create(inc, this, AStatement.class));
    }

    @Override
    public List<? extends AScope> selectBody() {
        return Arrays.asList(CxxJoinpoints.create(loop.getBody(), this, AScope.class));
    }

    @Override
    public ACxxWeaverJoinPoint getParentImpl() {
        return parent;
    }

    @Override
    public LoopStmt getNode() {
        return loop;
    }

    @Override
    public String getRankImpl() {
        throw new RuntimeException("Not implemented yet");
    }

    @Override
    public Boolean getIsParallelImpl() {
        return loop.isParallel();
        /*
        // Map<String, Consumer<? extends Object>> defMap = new HashMap<>();
        // defMap.put("qq", obj -> consumerString(obj));

        // Check if loop is annotated with pragma "parallel"
        List<Pragma> pragmas = ClavaNodes.getPragmas(getNode());

        boolean result = pragmas.stream()
                .filter(pragma -> pragma.getName().equals("clava"))
                .filter(clavaPragma -> clavaPragma.getContent().equals("parallel"))
                .findFirst()
                .isPresent();

        return result;
        */
    }

    // private void consumerString(String s) {
    //
    // }

    @Override
    public Integer getIterationsImpl() {
        return loop.getIterations();
    }

    @Override
    public void setKindImpl(String kind) {
        ALoopKindEnum loopKind = SpecsEnums.valueOf(ALoopKindEnum.class, kind.toUpperCase());

        if (loopKind == null) {
            ClavaLog.warning("Unsupported loop kind:" + kind);
            return;
        }

        switch (loopKind) {
        case WHILE:
            convertToWhile();
            break;
        default:
            throw new RuntimeException("Not implemented: " + loopKind);
        }

    }

    @Override
    public void changeKindImpl(String kind) {
        setKindImpl(kind);
    }

    private void convertToWhile() {
        if (loop instanceof WhileStmt) {
            return;
        }

        if (loop instanceof ForStmt) {

            WhileStmt whileStmt = ClavaNodeFactory.whileStmt(loop.getInfo(), ((ForStmt) loop).getCond().orElse(null),
                    loop.getBody());

            replaceWith(CxxJoinpoints.create(whileStmt, getParentImpl()));

            return;
        }

        throw new RuntimeException("Case not implemented:" + loop.getClass());

    }

    @Override
    public void setInitImpl(String initCode) {

        if (!(loop instanceof ForStmt)) {
            return; // TODO: warn user?
        }

        LiteralStmt literalStmt = ClavaNodeFactory.literalStmt(initCode + ";");

        ((ForStmt) loop).setInit(literalStmt);
    }

    @Override
    public void setCondImpl(String condCode) {

        if (!(loop instanceof ForStmt)) {
            return; // TODO: warn user?
        }

        LiteralStmt literalStmt = ClavaNodeFactory.literalStmt(condCode + ";");

        ((ForStmt) loop).setCond(literalStmt);
    }

    @Override
    public void setStepImpl(String stepCode) {

        if (!(loop instanceof ForStmt)) {
            return; // TODO: warn user?
        }

        LiteralStmt literalStmt = ClavaNodeFactory.literalStmt(stepCode);

        ((ForStmt) loop).setInc(literalStmt);
    }
}
