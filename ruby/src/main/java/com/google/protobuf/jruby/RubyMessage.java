/*
 * Protocol Buffers - Google's data interchange format
 * Copyright 2014 Google Inc.  All rights reserved.
 * https://developers.google.com/protocol-buffers/
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 *     * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following disclaimer
 * in the documentation and/or other materials provided with the
 * distribution.
 *     * Neither the name of Google Inc. nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.google.protobuf.jruby;

import com.google.protobuf.*;
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

public class RubyMessage extends RubyObject {
    public RubyMessage(Ruby ruby, RubyClass klazz, Descriptors.Descriptor descriptor) {
        super(ruby, klazz);
        this.descriptor = descriptor;
    }

    /*
     * call-seq:
     *     Message.new(kwargs) => new_message
     *
     * Creates a new instance of the given message class. Keyword arguments may be
     * provided with keywords corresponding to field names.
     *
     * Note that no literal Message class exists. Only concrete classes per message
     * type exist, as provided by the #msgclass method on Descriptors after they
     * have been added to a pool. The method definitions described here on the
     * Message class are provided on each concrete message class.
     */
    @JRubyMethod(optional = 1)
    public IRubyObject initialize(final ThreadContext context, IRubyObject[] args) {
        final Ruby runtime = context.runtime;
        this.cRepeatedField = (RubyClass) runtime.getClassFromPath("Google::Protobuf::RepeatedField");
        this.cMap = (RubyClass) runtime.getClassFromPath("Google::Protobuf::Map");
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
                    final Descriptors.FieldDescriptor fieldDescriptor = findField(context, key);
                    if (fieldDescriptor.getType() == Descriptors.FieldDescriptor.Type.MESSAGE &&
                            fieldDescriptor.isRepeated() &&
                            fieldDescriptor.getMessageType().getOptions().getMapEntry()) {
                        if (!(value instanceof RubyHash))
                            throw runtime.newArgumentError("Expected Hash object as initializer value for map field.");
                        Descriptors.Descriptor mapDescriptor = fieldDescriptor.getContainingType();
                        final Descriptors.FieldDescriptor keyField = mapDescriptor.findFieldByName("key");
                        final Descriptors.FieldDescriptor valueField = mapDescriptor.findFieldByName("value");
                        final RubyClass mapClass = (RubyClass) ((RubyDescriptor) getDescriptorForField(context, fieldDescriptor)).msgclass(context);
                        ((RubyHash) value).visitAll(new RubyHash.Visitor() {
                            @Override
                            public void visit(IRubyObject k, IRubyObject v) {
                                RubyMessage map = (RubyMessage) mapClass.newInstance(context, Block.NULL_BLOCK);
                                map.setField(context, keyField, k);
                                map.setField(context, valueField, v);
                                builder.addRepeatedField(fieldDescriptor, map.build(context));
                            }
                        });
                    } else if (fieldDescriptor.isRepeated()) {
                        // XXX check is mapentry
                        if (!(value instanceof RubyArray))
                            throw runtime.newTypeError("Expected array as initializer var for repeated field.");
                        RubyRepeatedField repeatedField = rubyToRepeatedField(context, fieldDescriptor, value);
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

    /*
     * call-seq:
     *     Message.[](index) => value
     *
     * Accesses a field's value by field name. The provided field name should be a
     * string.
     */
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

    /*
     * call-seq:
     *     Message.hash => hash_value
     *
     * Returns a hash value that represents this message's field values.
     */
    @JRubyMethod
    public IRubyObject hash(ThreadContext context) {
        int hashCode = System.identityHashCode(this);
        return context.runtime.newFixnum(hashCode);
    }

    /*
     * call-seq:
     *     Message.==(other) => boolean
     *
     * Performs a deep comparison of this message with another. Messages are equal
     * if they have the same type and if each field is equal according to the :==
     * method's semantics (a more efficient comparison may actually be done if the
     * field is of a primitive type).
     */
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

    /*
     * call-seq:
     *     Message.method_missing(*args)
     *
     * Provides accessors and setters for message fields according to their field
     * names. For any field whose name does not conflict with a built-in method, an
     * accessor is provided with the same name as the field, and a setter is
     * provided with the name of the field plus the '=' suffix. Thus, given a
     * message instance 'msg' with field 'foo', the following code is valid:
     *
     *     msg.foo = 42
     *     puts msg.foo
     */
    @JRubyMethod(name = "method_missing", rest = true)
    public IRubyObject methodMissing(ThreadContext context, IRubyObject[] args) {
        if (args.length == 1) {
            return this.index(context, args[0]);
        } else {
            // fieldName is RubySymbol
            RubyString field = args[0].asString();
            RubyString equalSign = context.runtime.newString(Utils.EQUAL_SIGN);
            if (field.end_with_p(context, equalSign).isTrue()) {
                field.chomp_bang(context, equalSign);
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
            } else if (this.builder.hasField(fdef)) {
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
        return ((RubyClass) recv).getInstanceVariable(Utils.DESCRIPTOR_INSTANCE_VAR);
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

    /*
     * call-seq:
     *     MessageClass.decode(data) => message
     *
     * Decodes the given data (as a string containing bytes in protocol buffers wire
     * format) under the interpretration given by this message class's definition
     * and returns a message object with the corresponding field values.
     */
    @JRubyMethod(meta = true)
    public static IRubyObject decode(ThreadContext context, IRubyObject recv, IRubyObject data) {
        byte[] bin = data.convertToString().getBytes();
        RubyMessage ret = (RubyMessage) ((RubyClass) recv).newInstance(context, Block.NULL_BLOCK);
        try {
            ret.builder.mergeFrom(bin);
        } catch (InvalidProtocolBufferException e) {
            throw context.runtime.newRuntimeError(e.getMessage());
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
        RubyDescriptor rubyDescriptor = (RubyDescriptor) ((RubyClass) recv).getInstanceVariable(Utils.DESCRIPTOR_INSTANCE_VAR);
        try {
            DynamicMessage.Builder dynamicMessageBuilder = DynamicMessage.newBuilder(rubyDescriptor.getDescriptor());
            JsonFormat.merge(json.asJavaString(), dynamicMessageBuilder);
            ret.buildFrom(context, dynamicMessageBuilder.build());
        } catch (JsonFormat.ParseException e) {
            throw context.runtime.newRuntimeError(e.getMessage());
        }
        return ret;
    }

    protected DynamicMessage build(ThreadContext context) {
        for (Map.Entry<Descriptors.FieldDescriptor, RubyRepeatedField> entry : this.repeatedFields.entrySet()) {
            Descriptors.FieldDescriptor fieldDescriptor = entry.getKey();
            RubyRepeatedField repeatedField = entry.getValue();
            this.builder.clearField(fieldDescriptor);
            for (int i = 0; i < repeatedField.size(); i++) {
                Object item = convert(context, fieldDescriptor, repeatedField.get(i));
                this.builder.addRepeatedField(fieldDescriptor, item);
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
            } else if (this.builder.hasField(fdef)) {
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

    private IRubyObject buildFrom(ThreadContext context, DynamicMessage dynamicMessage) {
        this.builder.mergeFrom(dynamicMessage);
        return this;
    }

    private Descriptors.FieldDescriptor findField(ThreadContext context, IRubyObject fieldName) {
        String nameStr = fieldName.asJavaString();
        Descriptors.FieldDescriptor ret = this.descriptor.findFieldByName(Utils.escapeIdentifier(nameStr));
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
                        val = Utils.num2ulong(context.runtime, value);
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
                Utils.validateStringEncoding(context.runtime, fieldDescriptor.getType(), value);
                RubyString str = (RubyString) value;
                switch (fieldDescriptor.getType()) {
                    case BYTES:
                        val = ByteString.copyFrom(str.getBytes());
                        break;
                    case STRING:
                        val = str.asJavaString();
                        break;
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
                    throw runtime.newRangeError("Enum value " + value + " is not found.");
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
                return msg.buildFrom(context, (DynamicMessage) value);
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
        Descriptors.FieldDescriptor.Type type = fieldDescriptor.getType();
        if (type == Descriptors.FieldDescriptor.Type.MESSAGE) {
            typeClass = ((RubyDescriptor) descriptor).msgclass(context);

        } else if (type == Descriptors.FieldDescriptor.Type.ENUM) {
            typeClass = ((RubyEnumDescriptor) descriptor).enummodule(context);
        }
        return new RubyRepeatedField(context.runtime, cRepeatedField, type, typeClass);
    }

    protected IRubyObject getField(ThreadContext context, Descriptors.FieldDescriptor fieldDescriptor) {
        if (fieldDescriptor.isRepeated()) {
            return getRepeatedField(context, fieldDescriptor);
        }
        if (fieldDescriptor.getType() != Descriptors.FieldDescriptor.Type.MESSAGE || this.builder.hasField(fieldDescriptor)) {
            Object value = this.builder.getField(fieldDescriptor);
            return wrapField(context, fieldDescriptor, value);
        }
        return context.runtime.getNil();
    }

    protected IRubyObject setField(ThreadContext context, Descriptors.FieldDescriptor fieldDescriptor, IRubyObject value) {
        if (fieldDescriptor.isRepeated()) {
            checkRepeatedFieldType(context, value, fieldDescriptor);
            if (value instanceof RubyRepeatedField) {
                addRepeatedField(fieldDescriptor, (RubyRepeatedField) value);
            } else {
                RubyArray ary = value.convertToArray();
                RubyRepeatedField repeatedField = rubyToRepeatedField(context, fieldDescriptor, ary);
                addRepeatedField(fieldDescriptor, repeatedField);
            }
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

    private RubyRepeatedField rubyToRepeatedField(ThreadContext context,
                                                  Descriptors.FieldDescriptor fieldDescriptor, IRubyObject value) {
        RubyArray arr = value.convertToArray();
        RubyRepeatedField repeatedField = repeatedFieldForFieldDescriptor(context, fieldDescriptor);
        for (int i = 0; i < arr.size(); i++) {
            repeatedField.push(context, arr.eltInternal(i));
        }
        return repeatedField;
    }

    private Descriptors.Descriptor descriptor;
    private DynamicMessage.Builder builder;
    private RubyClass cRepeatedField;
    private RubyClass cMap;
    private Map<Descriptors.FieldDescriptor, RubyRepeatedField> repeatedFields;
}
