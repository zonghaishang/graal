/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.oracle.truffle.nfi.test;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.InteropException;
import com.oracle.truffle.api.interop.KeyInfo;
import com.oracle.truffle.api.interop.Message;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.nfi.test.interop.BoxedPrimitive;
import com.oracle.truffle.tck.TruffleRunner;
import com.oracle.truffle.tck.TruffleRunner.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
@Parameterized.UseParametersRunnerFactory(TruffleRunner.ParametersFactory.class)
public class KeyInfoNFITest extends NFITest {

    private static void addTest(List<Object[]> ret, Object symbol, Supplier<TruffleObject> object, String description, boolean read, boolean invoke) {
        ret.add(new Object[]{symbol, object, description, read, invoke});
        ret.add(new Object[]{new BoxedPrimitive(symbol), object, description, read, invoke});
    }

    @Parameters(name = "{2}[{0}]")
    public static Collection<Object[]> data() {
        List<Object[]> ret = new ArrayList<>();
        addTest(ret, "increment_SINT32", () -> testLibrary, "testLibrary", true, false);
        addTest(ret, "__NOT_EXISTING__", () -> testLibrary, "testLibrary", false, false);
        addTest(ret, 42, () -> testLibrary, "testLibrary", false, false);

        Supplier<TruffleObject> symbol = () -> {
            try {
                return (TruffleObject) ForeignAccess.sendRead(Message.READ.createNode(), testLibrary, "increment_SINT32");
            } catch (InteropException e) {
                throw new AssertionError(e);
            }
        };
        addTest(ret, "bind", symbol, "symbol", false, true);
        addTest(ret, "__NOT_EXISTING__", symbol, "symbol", false, false);
        addTest(ret, 42, symbol, "symbol", false, false);

        Supplier<TruffleObject> boundSymbol = () -> lookupAndBind("increment_SINT32", "(sint32):sint32");
        addTest(ret, "bind", boundSymbol, "boundSymbol", false, true);
        addTest(ret, "__NOT_EXISTING__", boundSymbol, "boundSymbol", false, false);
        addTest(ret, 42, boundSymbol, "boundSymbol", false, false);

        return ret;
    }

    @Parameter(0) public Object symbol;
    @Parameter(1) public Supplier<TruffleObject> object;
    @Parameter(2) public String description;

    @Parameter(3) public boolean read;
    @Parameter(4) public boolean invoke;

    public static class KeyInfoNode extends NFITestRootNode {

        @Child Node keyInfo = Message.KEY_INFO.createNode();

        @Override
        public Object executeTest(VirtualFrame frame) throws InteropException {
            return ForeignAccess.sendKeyInfo(keyInfo, (TruffleObject) frame.getArguments()[0], frame.getArguments()[1]);
        }
    }

    private static void assertBoolean(String message, boolean expected, boolean actual) {
        if (expected) {
            Assert.assertTrue(message, actual);
        } else {
            Assert.assertFalse(message, actual);
        }
    }

    private void verifyKeyInfo(Object keyInfo) {
        Assert.assertThat(keyInfo, is(instanceOf(Integer.class)));
        int flags = (Integer) keyInfo;

        assertBoolean("isExisting", read || invoke, KeyInfo.isExisting(flags));

        assertBoolean("isReadable", read, KeyInfo.isReadable(flags));
        assertBoolean("isInvocable", invoke, KeyInfo.isInvocable(flags));

        Assert.assertFalse(KeyInfo.isWritable(flags));
        Assert.assertFalse(KeyInfo.isInternal(flags));
    }

    @Test
    public void testKeyInfo(@Inject(KeyInfoNode.class) CallTarget keyInfo) {
        Object result = keyInfo.call(object.get(), symbol);
        verifyKeyInfo(result);
    }
}
