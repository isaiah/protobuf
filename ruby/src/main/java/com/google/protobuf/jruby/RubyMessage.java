package com.google.protobuf.jruby;

import com.google.protobuf.ByteString;
import com.google.protobuf.Descriptors;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.InvalidProtocolBufferException;
import com.googlecode.protobuf.format.JsonFormat;
import org.jcodings.specific.ASCIIEncoding;
import org.jcodings.specific.USASCIIEncoding;
import org.jcodings.specific.UTF8Encoding;
import org.jruby.*;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by isaiah on 12/14/14.
 */
public class RubyMessage extends RubyObject {
    public RubyMessage(Ruby ruby, RubyClass klazz, Descriptors.Descriptor descriptor) {
        super(ruby, klazz);
        this.descriptor = descriptor;
    }

    @JRubyMethod(optional = 1)
    public IRubyObject initialize(final ThreadContext context, IRubyObject[] args) {
        final Ruby runtime = context.runtime;
        cRepeatedField = (RubyClass) runtime.getClassFromPath("Google::Protobuf::RepeatedField");
        this.builder = DynamicMessage.newBuilder(this.descriptor);
        this.repeatedFields = new HashMap<Descriptors.FieldDescriptor, RubyRepeatedField>();
        if (args.length == 1) {
            if (!(args[0] instanceof RubyHash)) {
                throw runtime.newArgumentError("expected Hash arguments.");
            }
            RubyHash hash = args[0].convertToHash();
            hash.visitAll(new RubyHash.Visitor() {
                @Override
                public void visit(IRubyObject key, IRubyObject value) {
                    if (!(key instanceof RubySymbol))
                        throw runtime.newTypeError("Expected symbols as hash keys in initialization map.");
                    Descriptors.FieldDescriptor fieldDescriptor = findField(context, key);
                    if (fieldDescriptor.isRepeated()) {
                        if (!(value instanceof RubyArray))
                            throw runtime.newTypeError("Expected array as initializer var for repeated field.");
                        RubyArray arr = value.convertToArray();
                        RubyRepeatedField repeatedField = repeatedFieldForFieldDescriptor(context, fieldDescriptor);
                        for (int i = 0; i < arr.size(); i++) {
                            repeatedField.push(context, arr.eltInternal(i));
                        }
                        addRepeatedField(fieldDescriptor, repeatedField);
                    } else {
                        builder.setField(fieldDescriptor, convert(context, fieldDescriptor, value));
                    }

                }
            });
        }
        return this;
    }

    /*
     * call-seq:
     *     Message.[]=(index, value)
     *
     * Sets a field's value by field name. The provided field name should be a
     * string.
     */
    @JRubyMethod(name = "[]=")
    public IRubyObject indexSet(ThreadContext context, IRubyObject fieldName, IRubyObject value) {
        Descriptors.FieldDescriptor fieldDescriptor = findField(context, fieldName);
        return setField(context, fieldDescriptor, value);
    }

    @JRubyMethod(name = "[]")
    public IRubyObject index(ThreadContext context, IRubyObject fieldName) {
        Descriptors.FieldDescriptor fieldDescriptor = findField(context, fieldName);
        return getField(context, fieldDescriptor);
    }

    /*
     * call-seq:
     *     Message.inspect => string
     *
     * Returns a human-readable string representing this message. It will be
     * formatted as "<MessageType: field1: value1, field2: value2, ...>". Each
     * field's value is represented according to its own #inspect method.
     */
    @JRubyMethod
    public IRubyObject inspect() {
        String cname = metaClass.getName();
        StringBuilder sb = new StringBuilder("<");
        sb.append(cname);
        sb.append(": ");
        sb.append(this.layoutInspect());
        sb.append(">");

        return getRuntime().newString(sb.toString());
    }

    @JRubyMethod
    public IRubyObject hash(ThreadContext context) {
        int hashCode = System.identityHashCode(this);
        return context.runtime.newString(Integer.toHexString(hashCode));
    }

    @JRubyMethod(name = "==")
    public IRubyObject eq(ThreadContext context, IRubyObject other) {
        Ruby runtime = context.runtime;
        if (!(other instanceof RubyMessage))
            return runtime.getFalse();
        RubyMessage message = (RubyMessage) other;
        if (descriptor != message.descriptor) {
            return runtime.getFalse();
        }

        for (Descriptors.FieldDescriptor fdef : descriptor.getFields()) {
            IRubyObject thisVal = getField(context, fdef);
            IRubyObject thatVal = message.getField(context, fdef);
            IRubyObject ret = thisVal.callMethod(context, "==", thatVal);
            if (!ret.isTrue()) {
                return runtime.getFalse();
            }
        }
        return runtime.getTrue();
    }

    @JRubyMethod(name = "method_missing", rest = true)
    public IRubyObject methodMissing(ThreadContext context, IRubyObject[] args) {
        if (args.length == 1) {
            return this.index(context, args[0]);
        } else {
            // fieldName is RubySymbol
            RubyString field = args[0].asString();
            if (field.end_with_p(context, context.runtime.newString("=")).isTrue()) {
                field.chomp_bang(context, context.runtime.newString("="));
            }
            return this.indexSet(context, field, args[1]);
        }
    }

    /**
     * call-seq:
     * Message.dup => new_message
     * Performs a shallow copy of this message and returns the new copy.
     */
    @JRubyMethod
    public IRubyObject dup(ThreadContext context) {
        RubyMessage dup = (RubyMessage) metaClass.newInstance(context, Block.NULL_BLOCK);
        for (Descriptors.FieldDescriptor fdef : this.descriptor.getFields()) {
            if (fdef.isRepeated()) {
                dup.addRepeatedField(fdef, this.getRepeatedField(context, fdef));
            } else {
                dup.builder.setField(fdef, this.builder.getField(fdef));

            }
        }
        return dup;
    }

    /*
     * call-seq:
     *     Message.descriptor => descriptor
     *
     * Class method that returns the Descriptor instance corresponding to this
     * message class's type.
     */
    @JRubyMethod(name = "descriptor", meta = true)
    public static IRubyObject getDescriptor(ThreadContext context, IRubyObject recv) {
        return ((RubyClass) recv).getInstanceVariable("@descriptor");
    }

    /*
     * call-seq:
     *     MessageClass.encode(msg) => bytes
     *
     * Encodes the given message object to its serialized form in protocol buffers
     * wire format.
     */
    @JRubyMethod(meta = true)
    public static IRubyObject encode(ThreadContext context, IRubyObject recv, IRubyObject message) {
        RubyMessage rbVal = (RubyMessage) message;
        return context.runtime.newString(new ByteList(rbVal.build(context).toByteArray()));
    }

    @JRubyMethod(meta = true)
    public static IRubyObject decode(ThreadContext context, IRubyObject recv, IRubyObject data) {
        byte[] bin = data.convertToString().getBytes();
        RubyMessage ret = (RubyMessage) ((RubyClass) recv).newInstance(context, Block.NULL_BLOCK);
        RubyDescriptor rubyDescriptor = (RubyDescriptor) ((RubyClass) recv).getInstanceVariable("@descriptor");
        try {
            DynamicMessage dynamicMessage = DynamicMessage.parseFrom(rubyDescriptor.getDescriptor(), bin);
            ret.buildFrom(dynamicMessage);
        } catch (InvalidProtocolBufferException e) {
            e.printStackTrace(context.runtime.getOutputStream());
            return context.runtime.getNil();
        }
        return ret;
    }

    /*
     * call-seq:
     *     MessageClass.encode_json(msg) => json_string
     *
     * Encodes the given message object into its serialized JSON representation.
     */
    @JRubyMethod(name = "encode_json", meta = true)
    public static IRubyObject encodeJson(ThreadContext context, IRubyObject recv, IRubyObject msgRb) {
        RubyMessage message = (RubyMessage) msgRb;
        String jsonStr = JsonFormat.printToString(message.build(context));
        return context.runtime.newString(jsonStr);
    }

    /*
     * call-seq:
     *     MessageClass.decode_json(data) => message
     *
     * Decodes the given data (as a string containing bytes in protocol buffers wire
     * format) under the interpretration given by this message class's definition
     * and returns a message object with the corresponding field values.
     */
    @JRubyMethod(name = "decode_json", meta = true)
    public static IRubyObject decodeJson(ThreadContext context, IRubyObject recv, IRubyObject json) {
        RubyMessage ret = (RubyMessage) ((RubyClass) recv).newInstance(context, Block.NULL_BLOCK);
        RubyDescriptor rubyDescriptor = (RubyDescriptor) ((RubyClass) recv).getInstanceVariable("@descriptor");
        try {
            DynamicMessage.Builder dynamicMessageBuilder = DynamicMessage.newBuilder(rubyDescriptor.getDescriptor());
            JsonFormat.merge(json.asJavaString(), dynamicMessageBuilder);
            ret.buildFrom(dynamicMessageBuilder.build());
        } catch (JsonFormat.ParseException e) {
            e.printStackTrace(context.runtime.getOutputStream());
            return context.runtime.getNil();
        }
        return ret;
    }

    protected DynamicMessage build(ThreadContext context) {
        for (Map.Entry<Descriptors.FieldDescriptor, RubyRepeatedField> entry : this.repeatedFields.entrySet()) {
            Descriptors.FieldDescriptor fieldDescriptor = entry.getKey();
            RubyRepeatedField repeatedField = entry.getValue();
            this.builder.clearField(fieldDescriptor);
            for (int i = 0; i < repeatedField.size(); i++) {
                IRubyObject item = repeatedField.get(i);
                if (item instanceof RubyString) {
                    this.builder.addRepeatedField(fieldDescriptor, item.asJavaString());
                } else {
                    this.builder.addRepeatedField(fieldDescriptor, convert(context, fieldDescriptor, item));
                }
            }
        }
        return this.builder.build();
    }

    protected Descriptors.Descriptor getDescriptor() {
        return this.descriptor;
    }

    // Internal use only, called by Google::Protobuf.deep_copy
    protected IRubyObject deepCopy(ThreadContext context) {
        RubyMessage copy = (RubyMessage) metaClass.newInstance(context, Block.NULL_BLOCK);
        for (Descriptors.FieldDescriptor fdef : this.descriptor.getFields()) {
            if (fdef.isRepeated()) {
                copy.addRepeatedField(fdef, this.getRepeatedField(context, fdef).deepCopy(context));
            } else {
                copy.builder.setField(fdef, this.builder.getField(fdef));

            }
        }
        return copy;
    }

    private RubyRepeatedField getRepeatedField(ThreadContext context, Descriptors.FieldDescriptor fieldDescriptor) {
        if (this.repeatedFields.containsKey(fieldDescriptor)) {
            return this.repeatedFields.get(fieldDescriptor);
        }

        int count = this.builder.getRepeatedFieldCount(fieldDescriptor);
        RubyRepeatedField ret = repeatedFieldForFieldDescriptor(context, fieldDescriptor);
        for (int i = 0; i < count; i++) {
            ret.push(context, wrapField(context, fieldDescriptor, this.builder.getRepeatedField(fieldDescriptor, i)));
        }
        return ret;
    }

    private void addRepeatedField(Descriptors.FieldDescriptor fieldDescriptor, RubyRepeatedField repeatedField) {
        this.repeatedFields.put(fieldDescriptor, repeatedField);
    }

    private IRubyObject buildFrom(DynamicMessage dynamicMessage) {
        this.builder.mergeFrom(dynamicMessage);
        return this;
    }

    private Descriptors.FieldDescriptor findField(ThreadContext context, IRubyObject fieldName) {
        String nameStr = fieldName.asJavaString();
        Descriptors.FieldDescriptor ret = this.descriptor.findFieldByName(nameStr.replace(".", Utils.BADNAME_REPLACEMENT));
        if (ret == null)
            throw context.runtime.newArgumentError("field " + fieldName.asJavaString() + " is not found");
        return ret;
    }

    private void checkRepeatedFieldType(ThreadContext context, IRubyObject value,
                                        Descriptors.FieldDescriptor fieldDescriptor) {
        Ruby runtime = context.runtime;
        if (!(value instanceof RubyRepeatedField)) {
            throw runtime.newTypeError("Expected repeated field array");
        }
    }


    // convert a ruby object to protobuf type, with type check
    private Object convert(ThreadContext context, Descriptors.FieldDescriptor fieldDescriptor, IRubyObject value) {
        Ruby runtime = context.runtime;
        Object val = null;
        switch (fieldDescriptor.getType()) {
            case INT32:
            case INT64:
            case UINT32:
            case UINT64:
                if (!Utils.isRubyNum(value)) {
                    throw runtime.newTypeError("Expected number type for integral field.");
                }
                Utils.checkIntTypePrecision(context, fieldDescriptor.getType(), value);
                switch (fieldDescriptor.getType()) {
                    case INT32:
                        val = RubyNumeric.num2int(value);
                        break;
                    case INT64:
                        val = RubyNumeric.num2long(value);
                        break;
                    case UINT32:
                        val = Utils.num2uint(value);
                        break;
                    case UINT64:
                        if (value instanceof RubyFloat) {
                            RubyBignum bignum = RubyBignum.newBignum(runtime, ((RubyFloat) value).getDoubleValue());
                            val = RubyBignum.big2ulong(bignum);
                        } else if (value instanceof RubyBignum) {
                            val = RubyBignum.big2ulong((RubyBignum) value);
                        } else {
                            val = RubyNumeric.num2long(value);
                        }
                        break;
                    default:
                        break;
                }
                break;
            case FLOAT:
                if (!Utils.isRubyNum(value))
                    throw runtime.newTypeError("Expected number type for float field.");
                val = (float) RubyNumeric.num2dbl(value);
                break;
            case DOUBLE:
                if (!Utils.isRubyNum(value))
                    throw runtime.newTypeError("Expected number type for double field.");
                val = RubyNumeric.num2dbl(value);
                break;
            case BOOL:
                if (!(value instanceof RubyBoolean))
                    throw runtime.newTypeError("Invalid argument for boolean field.");
                val = value.isTrue();
                break;
            case BYTES:
            case STRING:
                if (!(value instanceof RubyString))
                    throw runtime.newTypeError("Invalid argument for string field.");
                RubyString str = (RubyString) value;
                switch (fieldDescriptor.getType()) {
                    case BYTES:
                        if (str.getEncoding() == ASCIIEncoding.INSTANCE) {
                            val = ByteString.copyFrom(str.getBytes());
                            break;
                        }
                        throw runtime.newTypeError("Encoding for bytes fields" +
                                " must be \"ASCII-8BIT\", but was " + str.getEncoding());
                    case STRING:
                        if (str.getEncoding() == UTF8Encoding.INSTANCE
                                || str.getEncoding() == USASCIIEncoding.INSTANCE) {
                            val = str.asJavaString();
                            break;
                        }
                        throw runtime.newTypeError("Encoding for string fields" +
                                " must be \"UTF-8 or ASCII\", but was " + str.getEncoding());
                    default:
                        break;
                }
                break;
            case MESSAGE:
                RubyClass typeClass = (RubyClass) ((RubyDescriptor) getDescriptorForField(context, fieldDescriptor)).msgclass(context);
                if (!value.getMetaClass().equals(typeClass))
                    throw runtime.newTypeError(value, "Invalid type to assign to submessage field.");
                val = ((RubyMessage) value).build(context);
                break;
            case ENUM:
                Descriptors.EnumDescriptor enumDescriptor = fieldDescriptor.getEnumType();

                if (Utils.isRubyNum(value)) {
                    val = enumDescriptor.findValueByNumberCreatingIfUnknown(RubyNumeric.num2int(value));
                } else if (value instanceof RubySymbol) {
                    val = enumDescriptor.findValueByName(value.asJavaString());
                } else {
                    throw runtime.newTypeError("Expected number or symbol type for enum field.");
                }
                if (val == null) {
                    throw runtime.newNameError("Enum value " + value + " is not found.", enumDescriptor.getName());
                }
                break;
            default:
                break;
        }
        return val;
    }

    private IRubyObject wrapField(ThreadContext context, Descriptors.FieldDescriptor fieldDescriptor, Object value) {
        Ruby runtime = context.runtime;
        switch (fieldDescriptor.getType()) {
            case INT32:
            case INT64:
            case UINT32:
            case UINT64:
            case FLOAT:
            case DOUBLE:
            case BOOL:
            case BYTES:
            case STRING:
                return Utils.wrapPrimaryValue(context, fieldDescriptor.getType(), value);
            case MESSAGE:
                if (!((DynamicMessage) value).isInitialized()) {
                    return runtime.getNil();
                }
                RubyClass typeClass = (RubyClass) ((RubyDescriptor) getDescriptorForField(context, fieldDescriptor)).msgclass(context);
                RubyMessage msg = (RubyMessage) typeClass.newInstance(context, Block.NULL_BLOCK);
                return msg.buildFrom((DynamicMessage) value);
            case ENUM:
                Descriptors.EnumValueDescriptor enumValueDescriptor = (Descriptors.EnumValueDescriptor) value;
                if (enumValueDescriptor.getIndex() == -1) { // UNKNOWN ENUM VALUE
                  return runtime.newFixnum(enumValueDescriptor.getNumber());
                }
                return runtime.newSymbol(enumValueDescriptor.getName());
            default:
                return runtime.newString(value.toString());
        }
    }

    private RubyRepeatedField repeatedFieldForFieldDescriptor(ThreadContext context,
                                                              Descriptors.FieldDescriptor fieldDescriptor) {
        IRubyObject typeClass = context.runtime.getNilClass();

        IRubyObject descriptor = getDescriptorForField(context, fieldDescriptor);
        if (fieldDescriptor.getJavaType() == Descriptors.FieldDescriptor.JavaType.MESSAGE) {
            typeClass = ((RubyDescriptor) descriptor).msgclass(context);

        } else if (fieldDescriptor.getJavaType() == Descriptors.FieldDescriptor.JavaType.ENUM) {
            typeClass = ((RubyEnumDescriptor) descriptor).enummodule(context);
        }
        return new RubyRepeatedField(context.runtime, cRepeatedField, fieldDescriptor.getType(), typeClass);
    }

    protected IRubyObject getField(ThreadContext context, Descriptors.FieldDescriptor fieldDescriptor) {
        if (fieldDescriptor.isRepeated()) {
            return getRepeatedField(context, fieldDescriptor);
        }
        Object value = this.builder.getField(fieldDescriptor);
        return wrapField(context, fieldDescriptor, value);
    }

    protected IRubyObject setField(ThreadContext context, Descriptors.FieldDescriptor fieldDescriptor, IRubyObject value) {
        if (fieldDescriptor.isRepeated()) {
            checkRepeatedFieldType(context, value, fieldDescriptor);
            RubyRepeatedField repeatedField = (RubyRepeatedField) value;
            addRepeatedField(fieldDescriptor, repeatedField);
        } else {
            this.builder.setField(fieldDescriptor, convert(context, fieldDescriptor, value));
        }

        return context.runtime.getNil();
    }

    private String layoutInspect() {
        ThreadContext context = getRuntime().getCurrentContext();
        StringBuilder sb = new StringBuilder();
        for (Descriptors.FieldDescriptor fdef : this.descriptor.getFields()) {
            sb.append(fdef.getName());
            sb.append(": ");
            sb.append(getField(context, fdef).inspect());
            sb.append(", ");
        }
        return sb.substring(0, sb.length() - 2);
    }

    private IRubyObject getDescriptorForField(ThreadContext context, Descriptors.FieldDescriptor fieldDescriptor) {
        RubyDescriptor thisRbDescriptor = (RubyDescriptor) getDescriptor(context, metaClass);
        return thisRbDescriptor.lookup(fieldDescriptor.getName()).getSubType(context);
    }

    private Descriptors.Descriptor descriptor;
    private DynamicMessage.Builder builder;
    private RubyClass cRepeatedField;
    private Map<Descriptors.FieldDescriptor, RubyRepeatedField> repeatedFields;
}
