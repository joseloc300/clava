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

package pt.up.fe.specs.clava.ast.stmt;

import java.util.Collection;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ClavaNodeInfo;
import pt.up.fe.specs.clava.ClavaNodes;
import pt.up.fe.specs.clava.ast.DummyNode;
import pt.up.fe.specs.clava.ast.stmt.data.DummyStmtData;

/**
 * Dummy statement, for testing purposes.
 * 
 * @author JoaoBispo
 *
 */
public class DummyStmt extends Stmt implements DummyNode {

    public DummyStmt(DummyStmtData data, Collection<? extends ClavaNode> children) {
        super(data, children);
    }

    /**
     * Legacy support.
     * 
     * @param info
     * @param children
     */
    public DummyStmt(ClavaNodeInfo info, Collection<? extends ClavaNode> children) {
        super(info, children);
    }

    @Override
    public DummyStmtData getData() {
        return (DummyStmtData) super.getData();
    }

    public String getNodeCode() {
        return "/* Dummy statement '" + getData().getContent() + "'*/";
    }

    @Override
    public String getCode() {
        return ClavaNodes.toCode(getNodeCode(), this);
    }

    @Override
    public String getContent() {
        return getData().getContent();
    }

}
