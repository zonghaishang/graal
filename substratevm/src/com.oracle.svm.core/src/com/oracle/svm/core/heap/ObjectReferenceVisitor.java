/*
 * Copyright (c) 2013, 2017, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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
package com.oracle.svm.core.heap;

import org.graalvm.word.Pointer;

import com.oracle.svm.core.annotate.RestrictHeapAccess;

/**
 * Visit an object reference. The visitObjectReference method takes a Pointer as a parameter, but
 * that Pointer is *not* a pointer to an Object, but a Pointer to an object reference.
 */
public interface ObjectReferenceVisitor {

    /**
     * Called before any Object references are visited.
     *
     * @return true if visiting should continue, false if visiting should stop.
     */
    default boolean prologue() {
        return true;
    }

    /**
     * Visit an Object reference.
     *
     * To get the corresponding Object reference.readObject can be used.
     *
     * @param objRef The Object reference to be visited.
     * @param compressed True if the reference is in compressed form, false otherwise.
     * @return True if visiting should continue, false if visiting should stop.
     */
    @RestrictHeapAccess(access = RestrictHeapAccess.Access.UNRESTRICTED, overridesCallers = true, reason = "Some implementations allocate.")
    boolean visitObjectReference(Pointer objRef, boolean compressed);

    /** Like visitObjectReference(Pointer), but always inlined for performance. */
    @RestrictHeapAccess(access = RestrictHeapAccess.Access.UNRESTRICTED, overridesCallers = true, reason = "Some implementations allocate.")
    default boolean visitObjectReferenceInline(Pointer objRef, boolean compressed) {
        return visitObjectReference(objRef, compressed);
    }

    /**
     * Called after all Object references have been visited. If visiting terminates because a
     * visitor returned false, this method might not be called.
     */
    default boolean epilogue() {
        return true;
    }
}
