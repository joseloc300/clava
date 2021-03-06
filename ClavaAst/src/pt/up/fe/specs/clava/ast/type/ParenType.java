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

package pt.up.fe.specs.clava.ast.type;

import java.util.Collection;

import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clava.ClavaNode;

public class ParenType extends Type {

    public ParenType(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);
    }

    // public ParenType(TypeData typeData, ClavaNodeInfo info, Type innerType) {
    // this(typeData, info, Arrays.asList(innerType));
    // }
    //
    // private ParenType(TypeData typeData, ClavaNodeInfo info, Collection<? extends ClavaNode> children) {
    // super(typeData, info, children);
    // }
    //
    // @Override
    // protected ClavaNode copyPrivate() {
    // return new ParenType(getTypeData(), getInfo(), Collections.emptyList());
    // }

    public Type getInnerType() {
        return get(UNQUALIFIED_DESUGARED_TYPE).get();
        // return getChild(Type.class, 0);
    }
    //
    // public boolean isSugared() {
    // return true;
    // }

    @Override
    public String getCode(ClavaNode sourceNode, String name) {
        // System.out.println("PAREN TYPE INNER:" + getInnerType());

        // return "(" + getInnerType().getCode(sourceNode, name) + ")";
        if (name == null) {
            return "(" + getInnerType().getCode(sourceNode) + ")";
        }

        return getInnerType().getCode(sourceNode, "(" + name + ")");
    }

}
