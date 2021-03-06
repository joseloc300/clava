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

package pt.up.fe.specs.clava.ast.attr;

import java.util.Collection;
import java.util.Optional;

import org.suikasoft.jOptions.Datakey.DataKey;
import org.suikasoft.jOptions.Datakey.KeyFactory;
import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.attr.enums.AlignedAttrKind;
import pt.up.fe.specs.clava.ast.expr.Expr;
import pt.up.fe.specs.clava.ast.type.Type;

public abstract class AlignedAttr extends InheritableAttr {

    /// DATAKEYS BEGIN

    public final static DataKey<String> SPELLING = KeyFactory.string("spelling");
    public final static DataKey<AlignedAttrKind> ALIGNED_ATTR_KIND = KeyFactory
            .enumeration("alignedAttrKind", AlignedAttrKind.class);

    // These keys are for the derived classes
    public final static DataKey<Optional<Expr>> EXPR = KeyFactory.optional("expr");
    public final static DataKey<Type> TYPE = KeyFactory.object("type", Type.class);

    /// DATAKEYS END

    public AlignedAttr(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);
    }

    @Override
    public boolean isPostAttr() {
        return true;
    }

    /**
     * 
     * @return the value of the aligned value
     */
    protected abstract Optional<String> getValueCode();

    @Override
    public String getArgumentsCode() {
        return getValueCode().orElse(null);
    }
    /*
    @Override
    public String getCode() {
    
        String value = getValueCode()
                .map(code -> " (" + code + ")")
                .orElse("");
    
        String alignedCode = "aligned" + value;
    
        return getAttributeCode(alignedCode);
    }
    */
}
