package com.google.protobuf.jruby;

import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.Descriptors;
import org.jruby.*;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * Created by isaiah on 12/12/14.
 */
@JRubyClass(name = "FieldDescriptor")
public class RubyFieldDescriptor extends RubyObject {
    public static void createRubyFileDescriptor(Ruby runtime) {
        RubyModule mProtobuf = runtime.getClassFromPath("Google::Protobuf");
        RubyClass cFieldDescriptor = mProtobuf.defineClassUnder("FieldDescriptor", runtime.getObject(), new ObjectAllocator() {
            @Override
            public IRubyObject allocate(Ruby runtime, RubyClass klazz) {
                return new RubyFieldDescriptor(runtime, klazz);
            }
        });
        cFieldDescriptor.defineAnnotatedMethods(RubyFieldDescriptor.class);
    }

    public RubyFieldDescriptor(Ruby runtime, RubyClass klazz) {
        super(runtime, klazz);
    }

    @JRubyMethod
    public IRubyObject initialize(ThreadContext context) {
        builder = DescriptorProtos.FieldDescriptorProto.newBuilder();
        return this;
    }

    @JRubyMethod(name = "label=")
    public IRubyObject setLabel(ThreadContext context, IRubyObject value) {
        this.builder.setLabel(
                DescriptorProtos.FieldDescriptorProto.Label.valueOf("LABEL_" + value.asJavaString().toUpperCase()));
        return context.runtime.getNil();
    }

    @JRubyMethod(name = "name")
    public IRubyObject getName(ThreadContext context) {
        return this.name;
    }

    @JRubyMethod(name = "subtype")
    public IRubyObject getSubType(ThreadContext context) {
        return subType;
    }

    @JRubyMethod(name = "name=")
    public IRubyObject setName(ThreadContext context, IRubyObject value) {
        String nameStr = value.asJavaString();
        this.name = context.runtime.newString(nameStr);
        this.builder.setName(nameStr.replace(".", Utils.BADNAME_REPLACEMENT));
        return context.runtime.getNil();
    }

    @JRubyMethod(name = "type")
    public IRubyObject getType(ThreadContext context) {
        String typeName = this.builder.getType().name();
        return context.runtime.newSymbol(typeName.replace("TYPE_", "").toLowerCase());
    }

    @JRubyMethod(name = "type=")
    public IRubyObject setType(ThreadContext context, IRubyObject value) {
        this.builder.setType(DescriptorProtos.FieldDescriptorProto.Type.valueOf("TYPE_" + value.asJavaString().toUpperCase()));
        return context.runtime.getNil();
    }

    @JRubyMethod(name = "number=")
    public IRubyObject setNumber(ThreadContext context, IRubyObject value) {
        this.builder.setNumber(RubyNumeric.num2int(value));
        return context.runtime.getNil();
    }

    @JRubyMethod(name = "submsg_name=")
    public IRubyObject setSubmsgName(ThreadContext context, IRubyObject value) {
        this.builder.setTypeName(value.asJavaString());
        return context.runtime.getNil();
    }

    /*
     * call-seq:
     *     FieldDescriptor.get(message) => value
     *
     * Returns the value set for this field on the given message. Raises an
     * exception if message is of the wrong type.
     */
    @JRubyMethod(name = "get")
    public IRubyObject getValue(ThreadContext context, IRubyObject msgRb) {
        RubyMessage message = (RubyMessage) msgRb;
        if (message.getDescriptor() != fieldDef.getContainingType()) {
            throw context.runtime.newTypeError("set method called on wrong message type");
        }
        return message.getField(context, fieldDef);
    }
    /*
     * call-seq:
     *     FieldDescriptor.set(message, value)
     *
     * Sets the value corresponding to this field to the given value on the given
     * message. Raises an exception if message is of the wrong type. Performs the
     * ordinary type-checks for field setting.
     */
    @JRubyMethod(name = "set")
    public IRubyObject setValue(ThreadContext context, IRubyObject msgRb, IRubyObject value) {
        RubyMessage message = (RubyMessage) msgRb;
        if (message.getDescriptor() != fieldDef.getContainingType()) {
            throw context.runtime.newTypeError("set method called on wrong message type");
        }
        message.setField(context, fieldDef, value);
        return context.runtime.getNil();
    }

    protected void setSubType(IRubyObject rubyDescriptor) {
        this.subType = rubyDescriptor;
    }

    protected void setFieldDef(Descriptors.FieldDescriptor fieldDescriptor) {
        this.fieldDef = fieldDescriptor;
    }

    protected DescriptorProtos.FieldDescriptorProto.Builder getFieldDef() {
        return this.builder;
    }

    private DescriptorProtos.FieldDescriptorProto.Builder builder;
    private IRubyObject name;
    private IRubyObject subType;
    private Descriptors.FieldDescriptor fieldDef;
}
