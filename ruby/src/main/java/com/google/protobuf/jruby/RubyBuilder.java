package com.google.protobuf.jruby;

import org.jruby.*;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.*;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * Created by isaiah on 12/12/14.
 */
@JRubyClass(name = "Builder")
public class RubyBuilder  extends RubyObject {
    public static void createRubyBuilder(Ruby runtime) {
        RubyModule protobuf = runtime.getClassFromPath("Google::Protobuf");
        RubyClass cBuilder = protobuf.defineClassUnder("Builder", runtime.getObject(), new ObjectAllocator() {
            @Override
            public IRubyObject allocate(Ruby runtime, RubyClass klazz) {
                return new RubyBuilder(runtime, klazz);
            }
        });
        cBuilder.defineAnnotatedMethods(RubyBuilder.class);
    }

    public RubyBuilder(Ruby runtime, RubyClass metaClass) {
        super(runtime, metaClass);
        this.cDescriptor = (RubyClass) runtime.getClassFromPath("Google::Protobuf::Descriptor");
        this.cEnumDescriptor = (RubyClass) runtime.getClassFromPath("Google::Protobuf::EnumDescriptor");
        this.cMessageBuilderContext = (RubyClass) runtime.getClassFromPath("Google::Protobuf::MessageBuilderContext");
        this.cEnumBuilderContext = (RubyClass) runtime.getClassFromPath("Google::Protobuf::EnumBuilderContext");
    }

    @JRubyMethod
    public IRubyObject initialize(ThreadContext context) {
        Ruby runtime = context.runtime;
        this.pendingList = runtime.newArray();
        return this;
    }

    /*
     * call-seq:
     *     Builder.add_message(name, &block)
     *
     * Creates a new, empty descriptor with the given name, and invokes the block in
     * the context of a MessageBuilderContext on that descriptor. The block can then
     * call, e.g., MessageBuilderContext#optional and MessageBuilderContext#repeated
     * methods to define the message fields.
     *
     * This is the recommended, idiomatic way to build message definitions.
     */
    @JRubyMethod(name = "add_message")
    public IRubyObject addMessage(ThreadContext context, IRubyObject name, Block block) {
        RubyDescriptor msgdef = (RubyDescriptor) cDescriptor.newInstance(context, Block.NULL_BLOCK);
        IRubyObject ctx = cMessageBuilderContext.newInstance(context, msgdef, Block.NULL_BLOCK);
        msgdef.setName(context, name);
        if (block.isGiven()) {
            if (block.arity() == Arity.ONE_ARGUMENT) {
                block.yield(context, ctx);
            } else {
                Binding binding = block.getBinding();
                binding.setSelf(ctx);
                block.yieldSpecific(context);
            }
        }
        this.pendingList.add(msgdef);
        return context.runtime.getNil();
    }

    /*
     * call-seq:
     *     Builder.add_enum(name, &block)
     *
     * Creates a new, empty enum descriptor with the given name, and invokes the block in
     * the context of an EnumBuilderContext on that descriptor. The block can then
     * call EnumBuilderContext#add_value to define the enum values.
     *
     * This is the recommended, idiomatic way to build enum definitions.
     */
    @JRubyMethod(name = "add_enum")
    public IRubyObject addEnum(ThreadContext context, IRubyObject name, Block block) {
        RubyEnumDescriptor enumDef = (RubyEnumDescriptor) cEnumDescriptor.newInstance(context, Block.NULL_BLOCK);
        IRubyObject ctx = cEnumBuilderContext.newInstance(context, enumDef, Block.NULL_BLOCK);
        enumDef.setName(context, name);

        if (block.isGiven()) {
            if (block.arity() == Arity.ONE_ARGUMENT) {
                block.yield(context, ctx);
            } else {
                Binding binding = block.getBinding();
                binding.setSelf(ctx);
                block.yieldSpecific(context);
            }
        }

        this.pendingList.add(enumDef);
        return context.runtime.getNil();
    }

    @JRubyMethod(name = "finalize_to_pool")
    public IRubyObject finalizeToPool(ThreadContext context, IRubyObject rbPool) {
        RubyDescriptorPool pool = (RubyDescriptorPool) rbPool;
        for (int i = 0; i < this.pendingList.size(); i++) {
            IRubyObject defRb = this.pendingList.entry(i);
            if (defRb instanceof RubyDescriptor) {
                pool.addToSymtab(context, (RubyDescriptor) defRb);
            } else {
                pool.addToSymtab(context, (RubyEnumDescriptor) defRb);
            }
        }
        this.pendingList = context.runtime.newArray();
        return context.runtime.getNil();
    }

    private RubyArray pendingList;
    private RubyClass cDescriptor, cEnumDescriptor, cMessageBuilderContext, cEnumBuilderContext;
}
