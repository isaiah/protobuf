package com.google.protobuf.jruby;

import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.RubyObject;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * Created by isaiah on 15/12/14.
 */
@JRubyClass(name = "EnumBuilderContext")
public class RubyEnumBuilderContext extends RubyObject {
    public static void createRubyEnumBuilderContext(Ruby runtime) {
        RubyModule protobuf = runtime.getClassFromPath("Google::Protobuf");
        RubyClass cMessageBuilderContext = protobuf.defineClassUnder("EnumBuilderContext", runtime.getObject(), new ObjectAllocator() {
            @Override
            public IRubyObject allocate(Ruby runtime, RubyClass klazz) {
                return new RubyEnumBuilderContext(runtime, klazz);
            }
        });
        cMessageBuilderContext.defineAnnotatedMethods(RubyEnumBuilderContext.class);
    }

    public RubyEnumBuilderContext(Ruby ruby, RubyClass klazz) {
        super(ruby, klazz);
    }

    @JRubyMethod
    public IRubyObject initialize(ThreadContext context, IRubyObject enumDescriptor) {
        this.enumDescriptor = (RubyEnumDescriptor) enumDescriptor;
        return this;
    }

    /*
     * call-seq:
     *     EnumBuilder.add_value(name, number)
     *
     * Adds the given name => number mapping to the enum type. Name must be a Ruby
     * symbol.
     */
    @JRubyMethod
    public IRubyObject value(ThreadContext context, IRubyObject name, IRubyObject number) {
        this.enumDescriptor.addValue(context, name, number);
        return context.runtime.getNil();
    }

    private RubyEnumDescriptor enumDescriptor;
}
