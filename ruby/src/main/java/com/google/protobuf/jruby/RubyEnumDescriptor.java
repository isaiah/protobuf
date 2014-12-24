package com.google.protobuf.jruby;

import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.Descriptors;
import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.RubyObject;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.Block;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * Created by isaiah on 15/12/14.
 */
@JRubyClass(name = "EnumDescriptor", include = "Enumerable")
public class RubyEnumDescriptor extends RubyObject {
    public static void createRubyEnumDescriptor(Ruby runtime) {
        RubyModule mProtobuf = runtime.getClassFromPath("Google::Protobuf");
        RubyClass cEnumDescriptor = mProtobuf.defineClassUnder("EnumDescriptor", runtime.getObject(), new ObjectAllocator() {
            @Override
            public IRubyObject allocate(Ruby runtime, RubyClass klazz) {
                return new RubyEnumDescriptor(runtime, klazz);
            }
        });
        cEnumDescriptor.includeModule(runtime.getEnumerable());
        cEnumDescriptor.defineAnnotatedMethods(RubyEnumDescriptor.class);
    }

    public RubyEnumDescriptor(Ruby runtime, RubyClass klazz) {
        super(runtime, klazz);
    }

    @JRubyMethod
    public IRubyObject initialize(ThreadContext context) {
        this.builder = DescriptorProtos.EnumDescriptorProto.newBuilder();
        return this;
    }

    @JRubyMethod(name = "name")
    public IRubyObject getName(ThreadContext context) {
        return this.name;
    }

    @JRubyMethod(name = "name=")
    public IRubyObject setName(ThreadContext context, IRubyObject name) {
        this.name = name;
        this.builder.setName(name.asJavaString());
        return context.runtime.getNil();
    }

    @JRubyMethod(name = "add_value")
    public IRubyObject addValue(ThreadContext context, IRubyObject name, IRubyObject number) {
        DescriptorProtos.EnumValueDescriptorProto.Builder valueBuilder = DescriptorProtos.EnumValueDescriptorProto.newBuilder();
        valueBuilder.setName(name.asJavaString());
        valueBuilder.setNumber((int) number.convertToInteger().getLongValue());
        this.builder.addValue(valueBuilder);
        return context.runtime.getNil();
    }

    @JRubyMethod
    public IRubyObject each(ThreadContext context, Block block) {
        Ruby runtime = context.runtime;
        for (Descriptors.EnumValueDescriptor enumValueDescriptor : descriptor.getValues()) {
            block.yield(context, runtime.newArray(runtime.newSymbol(enumValueDescriptor.getName()),
                    runtime.newFixnum(enumValueDescriptor.getNumber())));
        }
        return runtime.getNil();
    }

    @JRubyMethod
    public IRubyObject enummodule(ThreadContext context) {
        if (this.klazz == null) {
            this.klazz = buildModuleFromDescriptor(context);
        }
        return this.klazz;
    }

    public void setDescriptor(Descriptors.EnumDescriptor descriptor) {
        this.descriptor = descriptor;
    }

    public Descriptors.EnumDescriptor getDescriptor() {
        return this.descriptor;
    }

    public DescriptorProtos.EnumDescriptorProto.Builder getBuilder() {
        return this.builder;
    }

    private RubyModule buildModuleFromDescriptor(ThreadContext context) {
        Ruby runtime = context.runtime;
        Utils.checkNameAvailability(context, name.asJavaString());

        RubyModule enumModule = RubyModule.newModule(runtime);
        for (Descriptors.EnumValueDescriptor value: descriptor.getValues()) {
            enumModule.defineConstant(value.getName(), runtime.newFixnum(value.getNumber()));
        }

        enumModule.instance_variable_set(runtime.newString("@descriptor"), this);
        enumModule.defineAnnotatedMethods(RubyEnum.class);
        return enumModule;
    }

    private IRubyObject name;
    private RubyModule klazz;
    private Descriptors.EnumDescriptor descriptor;
    private DescriptorProtos.EnumDescriptorProto.Builder builder;
}
