package com.google.protobuf.jruby;

import org.jruby.*;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.Block;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * Created by isaiah on 12/12/14.
 */
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
