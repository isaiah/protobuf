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

@JRubyClass(name = "OneofDescriptor", include = "Enumerable")
public class RubyOneofDescriptor extends RubyObject {

    public static void createRubyOneofDescriptor(Ruby runtime) {
        RubyModule protobuf = runtime.getClassFromPath("Google::Protobuf");
        RubyClass cRubyOneofDescriptor = protobuf.defineClassUnder("OneofDescriptor", runtime.getObject(), new ObjectAllocator() {
            @Override
            public IRubyObject allocate(Ruby ruby, RubyClass rubyClass) {
                return new RubyOneofDescriptor(ruby, rubyClass);
            }
        });
        cRubyOneofDescriptor.defineAnnotatedMethods(RubyOneofDescriptor.class);
        cRubyOneofDescriptor.includeModule(runtime.getEnumerable());
    }

    public RubyOneofDescriptor(Ruby ruby, RubyClass rubyClass) {
        super(ruby, rubyClass);
    }

    @JRubyMethod
    public IRubyObject initialize(ThreadContext context) {
        return this;
    }

    @JRubyMethod
    public IRubyObject name(ThreadContext context) {
        return name;
    }

    @JRubyMethod(name = "name=")
    public IRubyObject setName(ThreadContext context, IRubyObject name) {
        this.name = name;
        return context.runtime.getNil();
    }

    private IRubyObject name;
}
