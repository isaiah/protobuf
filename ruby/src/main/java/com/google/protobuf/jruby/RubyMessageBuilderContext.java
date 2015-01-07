/*
 * Protocol Buffers - Google's data interchange format
 * Copyright 2014 Google Inc.  All rights reserved.
 * https://developers.google.com/protocol-buffers/
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 *     * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following disclaimer
 * in the documentation and/or other materials provided with the
 * distribution.
 *     * Neither the name of Google Inc. nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

/*
 * Protocol Buffers - Google's data interchange format
 * Copyright 2014 Google Inc.  All rights reserved.
 * https://developers.google.com/protocol-buffers/
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 *     * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following disclaimer
 * in the documentation and/or other materials provided with the
 * distribution.
 *     * Neither the name of Google Inc. nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.google.protobuf.jruby;

import org.jruby.*;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.Block;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

@JRubyClass(name = "MessageBuilderContext")
public class RubyMessageBuilderContext extends RubyObject {
    public static void createRubyMessageBuilderContext(Ruby runtime) {
        RubyModule protobuf = runtime.getClassFromPath("Google::Protobuf");
        RubyClass cMessageBuilderContext = protobuf.defineClassUnder("MessageBuilderContext", runtime.getObject(), new ObjectAllocator() {
            @Override
            public IRubyObject allocate(Ruby runtime, RubyClass klazz) {
                return new RubyMessageBuilderContext(runtime, klazz);
            }
        });
        cMessageBuilderContext.defineAnnotatedMethods(RubyMessageBuilderContext.class);
    }

    public RubyMessageBuilderContext(Ruby ruby, RubyClass klazz) {
        super(ruby, klazz);
    }

    @JRubyMethod
    public IRubyObject initialize(ThreadContext context, IRubyObject descriptor) {
        this.cFieldDescriptor = (RubyClass) context.runtime.getClassFromPath("Google::Protobuf::FieldDescriptor");
        this.descriptor = (RubyDescriptor) descriptor;
        return this;
    }

    @JRubyMethod(required = 3, optional = 1)
    public IRubyObject optional(ThreadContext context, IRubyObject[] args) {
        Ruby runtime = context.runtime;
        IRubyObject typeClass = runtime.getNil();
        if (args.length > 3) typeClass = args[3];
        msgdefAddField(context, "optional", args[0], args[1], args[2], typeClass);
        return this;
    }

    @JRubyMethod(required = 3, optional = 1)
    public IRubyObject required(ThreadContext context, IRubyObject[] args) {
        IRubyObject typeClass = context.runtime.getNil();
        if (args.length > 3) typeClass = args[3];
        msgdefAddField(context, "required", args[0], args[1], args[2], typeClass);
        return this;
    }

    @JRubyMethod(required = 3, optional = 1)
    public IRubyObject repeated(ThreadContext context, IRubyObject[] args) {
        IRubyObject typeClass = context.runtime.getNil();
        if (args.length > 3) typeClass = args[3];
        msgdefAddField(context, "repeated", args[0], args[1], args[2], typeClass);
        return this;
    }

    private void msgdefAddField(ThreadContext context, String label, IRubyObject name, IRubyObject type, IRubyObject number, IRubyObject typeClass) {
        Ruby runtime = context.runtime;
        RubyFieldDescriptor fieldDef = (RubyFieldDescriptor) cFieldDescriptor.newInstance(context, Block.NULL_BLOCK);
        fieldDef.setLabel(context, runtime.newString(label));
        fieldDef.setName(context, name);
        fieldDef.setType(context, type);
        fieldDef.setNumber(context, number);

        if (! typeClass.isNil()) {
            if (! (typeClass instanceof RubyString)) {
                runtime.newArgumentError("expected string for type class");
            }
            ((RubyString) typeClass).prepend(context, runtime.newString("."));
            fieldDef.setSubmsgName(context, typeClass);
        }
        descriptor.addField(context, fieldDef);
    }

    private RubyDescriptor descriptor;
    private RubyClass cFieldDescriptor;
}
