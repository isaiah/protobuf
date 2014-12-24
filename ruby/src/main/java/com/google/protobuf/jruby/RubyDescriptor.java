package com.google.protobuf.jruby;

import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.Descriptors;
import org.jruby.*;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.Block;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import java.util.HashMap;
import java.util.Map;


/**
 * Created by isaiah on 12/12/14.
 */
@JRubyClass(name = "Descriptor", include = "Enumerable")
public class RubyDescriptor extends RubyObject {
    public static void createRubyDescriptor(Ruby runtime) {
        RubyModule protobuf = runtime.getClassFromPath("Google::Protobuf");
        RubyClass cDescriptor = protobuf.defineClassUnder("Descriptor", runtime.getObject(), new ObjectAllocator() {
            @Override
            public IRubyObject allocate(Ruby runtime, RubyClass klazz) {
                return new RubyDescriptor(runtime, klazz);
            }
        });
        cDescriptor.includeModule(runtime.getEnumerable());
        cDescriptor.defineAnnotatedMethods(RubyDescriptor.class);
    }

    public RubyDescriptor(Ruby runtime, RubyClass klazz) {
        super(runtime, klazz);
    }

    @JRubyMethod
    public IRubyObject initialize(ThreadContext context) {
        this.builder = DescriptorProtos.DescriptorProto.newBuilder();
        this.fieldDefMap = new HashMap<String, RubyFieldDescriptor>();
        return this;
    }

    @JRubyMethod(name = "name")
    public IRubyObject getName(ThreadContext context) {
        return this.name;
    }

    @JRubyMethod(name = "name=")
    public IRubyObject setName(ThreadContext context, IRubyObject name) {
        this.name = name;
        this.builder.setName(this.name.asJavaString());
        return context.runtime.getNil();
    }

    @JRubyMethod(name = "add_field")
    public IRubyObject addField(ThreadContext context, IRubyObject obj) {
        RubyFieldDescriptor fieldDef = (RubyFieldDescriptor) obj;
        this.fieldDefMap.put(fieldDef.getName(context).asJavaString(), fieldDef);
        this.builder.addField(fieldDef.getFieldDef());
        return context.runtime.getNil();
    }

    @JRubyMethod
    public IRubyObject lookup(ThreadContext context, IRubyObject fieldName) {
        return this.fieldDefMap.get(fieldName.asJavaString());
    }

    @JRubyMethod
    public IRubyObject msgclass(ThreadContext context) {
        if (this.klazz == null) {
            this.klazz = buildClassFromDescriptor(context);
        }
        return this.klazz;
    }

    @JRubyMethod
    public IRubyObject each(ThreadContext context, Block block) {
        for (Map.Entry<String, RubyFieldDescriptor> entry : fieldDefMap.entrySet()) {
            block.yield(context, entry.getValue());
        }
        return context.runtime.getNil();
    }

    public void setDescriptor(Descriptors.Descriptor descriptor) {
        this.descriptor = descriptor;
    }

    public Descriptors.Descriptor getDescriptor() {
        return this.descriptor;
    }

    private RubyModule buildClassFromDescriptor(ThreadContext context) {
        Ruby runtime = context.runtime;

        ObjectAllocator allocator = new ObjectAllocator() {
            @Override
            public IRubyObject allocate(Ruby runtime, RubyClass klazz) {
                return new RubyMessage(runtime, klazz, descriptor);
            }
        };

        // rb_define_class_id
        RubyClass klass = RubyClass.newClass(runtime, runtime.getObject());
        klass.setAllocator(allocator);
        klass.makeMetaClass(runtime.getObject().getMetaClass());
        klass.inherit(runtime.getObject());
        klass.instance_variable_set(runtime.newString("@descriptor"), this);
        klass.defineAnnotatedMethods(RubyMessage.class);
        return klass;
    }

    public DescriptorProtos.DescriptorProto.Builder getBuilder() {
        return builder;
    }

    protected RubyFieldDescriptor lookup(String fieldName) {
        return fieldDefMap.get(fieldName.replace(Utils.BADNAME_REPLACEMENT, "."));
    }

    private IRubyObject name;
    private RubyModule klazz;

    private DescriptorProtos.DescriptorProto.Builder builder;
    private Descriptors.Descriptor descriptor;
    private Map<String, RubyFieldDescriptor> fieldDefMap;
}
