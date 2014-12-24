package com.google.protobuf.jruby;

import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.Descriptors;
import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.RubyObject;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.*;
import org.jruby.runtime.builtin.IRubyObject;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by isaiah on 12/12/14.
 */
@JRubyClass(name = "DescriptorPool")
public class RubyDescriptorPool extends RubyObject {
    public static void createRubyDescriptorPool(Ruby runtime) {
        RubyModule protobuf = runtime.getClassFromPath("Google::Protobuf");
        RubyClass cDescriptorPool = protobuf.defineClassUnder("DescriptorPool", runtime.getObject(), new ObjectAllocator() {
            @Override
            public IRubyObject allocate(Ruby runtime, RubyClass klazz) {
                return new RubyDescriptorPool(runtime, klazz);
            }
        });

        cDescriptorPool.defineAnnotatedMethods(RubyDescriptorPool.class);
    }

    public RubyDescriptorPool(Ruby ruby, RubyClass klazz) {
        super(ruby, klazz);
    }

    @JRubyMethod
    public IRubyObject initialize(ThreadContext context) {
        this.symtab = new HashMap<IRubyObject, IRubyObject>();
        this.cBuilder = (RubyClass) context.runtime.getClassFromPath("Google::Protobuf::Builder");
        this.builder = DescriptorProtos.FileDescriptorProto.newBuilder();
        return this;
    }

    @JRubyMethod
    public IRubyObject build(ThreadContext context, Block block) {
        RubyBuilder ctx = (RubyBuilder) cBuilder.newInstance(context, Block.NULL_BLOCK);
        if (block.arity() == Arity.ONE_ARGUMENT) {
            block.yield(context, ctx);
        } else {
            Binding binding = block.getBinding();
            binding.setSelf(ctx);
            block.yieldSpecific(context);
        }
        ctx.finalizeToPool(context, this);
        buildFileDescriptor(context);
        return context.runtime.getNil();
    }

    @JRubyMethod
    public IRubyObject lookup(ThreadContext context, IRubyObject name) {
        IRubyObject descriptor = this.symtab.get(name);
        if (descriptor == null) {
            context.runtime.getOutputStream().println(name);
        }
        return descriptor;
    }

    protected void addToSymtab(ThreadContext context, RubyDescriptor def) {
        symtab.put(def.getName(context), def);
        this.builder.addMessageType(def.getBuilder());
    }

    protected void addToSymtab(ThreadContext context, RubyEnumDescriptor def) {
        symtab.put(def.getName(context), def);
        this.builder.addEnumType(def.getBuilder());
    }

    private void buildFileDescriptor(ThreadContext context) {
        Ruby runtime = context.runtime;
        try {
            final Descriptors.FileDescriptor fileDescriptor = Descriptors.FileDescriptor.buildFrom(
                    this.builder.build(), new Descriptors.FileDescriptor[]{});

            for (Descriptors.EnumDescriptor enumDescriptor : fileDescriptor.getEnumTypes()) {
                if (enumDescriptor.findValueByNumber(0) == null) {
                    throw runtime.newTypeError("Enum definition " + enumDescriptor.getName()
                            + " does not contain a value for '0'");
                }
                ((RubyEnumDescriptor) symtab.get(runtime.newString(enumDescriptor.getName()))).setDescriptor(enumDescriptor);
            }
            for (Descriptors.Descriptor descriptor : fileDescriptor.getMessageTypes()) {
                RubyDescriptor rubyDescriptor = ((RubyDescriptor) symtab.get(runtime.newString(descriptor.getName())));
                for (Descriptors.FieldDescriptor fieldDescriptor : descriptor.getFields()) {
                    if (fieldDescriptor.isRequired()) {
                        throw runtime.newTypeError("Required fields are unsupported in proto3");
                    }
                    RubyFieldDescriptor rubyFieldDescriptor = rubyDescriptor.lookup(fieldDescriptor.getName());
                    rubyFieldDescriptor.setFieldDef(fieldDescriptor);
                    if (fieldDescriptor.getJavaType() == Descriptors.FieldDescriptor.JavaType.MESSAGE) {
                        RubyDescriptor subType = (RubyDescriptor) lookup(context,
                                runtime.newString(fieldDescriptor.getMessageType().getName()));
                        rubyFieldDescriptor.setSubType(subType);
                    }
                    if (fieldDescriptor.getJavaType() == Descriptors.FieldDescriptor.JavaType.ENUM) {
                        RubyEnumDescriptor subType = (RubyEnumDescriptor) lookup(context,
                                runtime.newString(fieldDescriptor.getEnumType().getName()));
                        rubyFieldDescriptor.setSubType(subType);
                    }
                }
                rubyDescriptor.setDescriptor(descriptor);
            }
        } catch (Descriptors.DescriptorValidationException e) {
            context.runtime.getOutputStream().println("descriptor validation exception");
            e.printStackTrace(context.runtime.getOutputStream());
            throw runtime.newRuntimeError(e.getMessage());
        }
    }

    private RubyClass cBuilder;
    private Map<IRubyObject, IRubyObject> symtab;
    private DescriptorProtos.FileDescriptorProto.Builder builder;
}
