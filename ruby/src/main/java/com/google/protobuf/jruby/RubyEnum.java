package com.google.protobuf.jruby;

import com.google.protobuf.Descriptors;
import org.jruby.RubyModule;
import org.jruby.RubyNumeric;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * Created by isaiah on 12/16/14.
 */
public class RubyEnum {
    /*
     * call-seq:
     *     Enum.lookup(number) => name
     *
     * This module method, provided on each generated enum module, looks up an enum
     * value by number and returns its name as a Ruby symbol, or nil if not found.
     */
    @JRubyMethod(meta = true)
    public static IRubyObject lookup(ThreadContext context, IRubyObject recv, IRubyObject number) {
        RubyEnumDescriptor rubyEnumDescriptorescriptor = (RubyEnumDescriptor) getDescriptor(context, recv);
        Descriptors.EnumDescriptor descriptor = rubyEnumDescriptorescriptor.getDescriptor();
        Descriptors.EnumValueDescriptor value = descriptor.findValueByNumber(RubyNumeric.num2int(number));
        if (value == null) return context.runtime.getNil();
        return context.runtime.newSymbol(value.getName());
    }

    @JRubyMethod(meta = true)
    public static IRubyObject resolve(ThreadContext context, IRubyObject recv, IRubyObject name) {
        RubyEnumDescriptor rubyEnumDescriptorescriptor = (RubyEnumDescriptor) getDescriptor(context, recv);
        Descriptors.EnumDescriptor descriptor = rubyEnumDescriptorescriptor.getDescriptor();
        Descriptors.EnumValueDescriptor value = descriptor.findValueByName(name.asJavaString());
        if (value == null) return context.runtime.getNil();
        return context.runtime.newFixnum(value.getNumber());
    }

    @JRubyMethod(meta = true, name = "descriptor")
    public static IRubyObject getDescriptor(ThreadContext context, IRubyObject recv) {
        return ((RubyModule)recv).getInstanceVariable("@descriptor");
    }
}
