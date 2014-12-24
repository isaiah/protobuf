package com.google.protobuf.jruby;

import com.google.protobuf.ByteString;
import com.google.protobuf.Descriptors;
import org.jruby.*;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * Created by isaiah on 12/20/14.
 */
public class Utils {
    public static Descriptors.FieldDescriptor.Type rubyToFieldType(IRubyObject typeClass) {
        return Descriptors.FieldDescriptor.Type.valueOf(typeClass.asJavaString().toUpperCase());
    }

    public static void checkType(ThreadContext context, Descriptors.FieldDescriptor.Type fieldType,
                            IRubyObject value, RubyModule typeClass) {
        Ruby runtime = context.runtime;
        Object val;
        switch(fieldType) {
            case INT32:
            case INT64:
            case UINT32:
            case UINT64:
                if (!isRubyNum(value)) {
                    throw runtime.newTypeError("Expected number type for integral field.");
                }
                checkIntTypePrecision(context, fieldType, value);
                break;
            case FLOAT:
                if (!isRubyNum(value))
                    throw runtime.newTypeError("Expected number type for float field.");
                break;
            case DOUBLE:
                if (!isRubyNum(value))
                    throw runtime.newTypeError("Expected number type for double field.");
                break;
            case BOOL:
                if (!(value instanceof RubyBoolean))
                    throw runtime.newTypeError("Invalid argument for boolean field.");
                break;
            case BYTES:
                if (!(value instanceof RubyString))
                    throw runtime.newTypeError("Invalid argument for string field.");
                break;
            case STRING:
                if (!(value instanceof RubyString))
                    throw runtime.newTypeError("Invalid argument for string field.");
                break;
            case MESSAGE:
                if (value.getMetaClass() != typeClass) {
                    throw runtime.newTypeError(value, typeClass);
                }
                break;
            case ENUM:
                Descriptors.EnumDescriptor enumDescriptor = ((RubyEnumDescriptor) typeClass.getInstanceVariable("@descriptor")).getDescriptor();
                if (isRubyNum(value)) {
                    val = enumDescriptor.findValueByNumber((int) value.convertToInteger().getLongValue());
                } else if (value instanceof RubySymbol) {
                    val = enumDescriptor.findValueByName(value.asJavaString());
                } else {
                    throw runtime.newTypeError("Expected number or symbol type for enum field.");
                }
                if (val == null)
                    throw runtime.newNameError("Enum value " + value + " is not found.", enumDescriptor.getName());
                break;
            default:
                break;
        }
    }

    public static IRubyObject wrapPrimaryValue(ThreadContext context, Descriptors.FieldDescriptor.Type fieldType, Object value) {
        Ruby runtime = context.runtime;
        switch (fieldType) {
            case INT32:
                return runtime.newFixnum((Integer) value);
            case INT64:
                return runtime.newFixnum((Long) value);
            case UINT32:
                return runtime.newFixnum(((Integer) value) & (-1l >>> 32));
            case UINT64:
                long ret = (Long) value;
                return ret >= 0 ? runtime.newFixnum(ret) :
                        RubyBignum.newBignum(runtime, ((Long) value) + 1.8446744073709552E19); // + Math.pow(2, 64)
            case FLOAT:
                return runtime.newFloat((Float) value);
            case DOUBLE:
                return runtime.newFloat((Double) value);
            case BOOL:
                return runtime.newBoolean((Boolean) value);
            case BYTES:
                return runtime.newString(((ByteString) value).toStringUtf8());
            case STRING:
                return runtime.newString(value.toString());
            default:
                return runtime.getNil();
        }
    }

    public static int num2uint(IRubyObject value) {
        long longVal = RubyNumeric.num2long(value);
        if (longVal > UINT_MAX)
            throw value.getRuntime().newRangeError("Integer " + longVal + " too big to convert to 'unsigned int'");
        long num = longVal;
        if (num > Integer.MAX_VALUE || num < Integer.MIN_VALUE)
            num = (-longVal ^ (-1l >>> 32) ) + 1;
        RubyNumeric.checkInt(value, num);
        return (int) num;
    }

    public static void checkNameAvailability(ThreadContext context, String name) {
        if (context.runtime.getObject().getConstantAt(name) != null)
            throw context.runtime.newNameError(name + " is already defined", name);
    }

    protected static void checkIntTypePrecision(ThreadContext context, Descriptors.FieldDescriptor.Type type, IRubyObject value) {
        if (value instanceof RubyFloat) {
            double doubleVal = RubyNumeric.num2dbl(value);
            if (Math.floor(doubleVal) != doubleVal) {
                throw context.runtime.newRangeError("Non-integral floating point value assigned to integer field.");
            }
        }
        if (type == Descriptors.FieldDescriptor.Type.UINT32 || type == Descriptors.FieldDescriptor.Type.UINT64) {
            if (RubyNumeric.num2dbl(value) < 0) {
                throw context.runtime.newRangeError("Assigning negative value to unsigned integer field.");
            }
        }
    }

    protected static boolean isRubyNum(Object value) {
        return value instanceof RubyFixnum || value instanceof RubyFloat || value instanceof RubyBignum;
    }

    public static String BADNAME_REPLACEMENT = "YmFkbmFtZQ"; // badname disguised in base64

    private static long UINT_MAX = 0xffffffffl;
}
