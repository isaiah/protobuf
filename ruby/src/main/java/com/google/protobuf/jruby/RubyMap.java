package com.google.protobuf.jruby;

import com.google.protobuf.Descriptors;
import com.google.protobuf.MapEntry;
import org.jruby.*;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.Block;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by isaiah on 08/02/15.
 */

@JRubyClass(name = "Map", include = "Enumerable")
public class RubyMap extends RubyObject {
    public static void createRubyMap(Ruby runtime) {
        RubyModule protobuf = runtime.getClassFromPath("Google::Protobuf");
        RubyClass cMap = protobuf.defineClassUnder("Map", runtime.getObject(), new ObjectAllocator() {
            @Override
            public IRubyObject allocate(Ruby ruby, RubyClass rubyClass) {
                return new RubyMap(ruby, rubyClass);
            }
        });
        cMap.includeModule(runtime.getEnumerable());
        cMap.defineAnnotatedMethods(RubyMap.class);
    }

    public RubyMap(Ruby ruby, RubyClass rubyClass) {
        super(ruby, rubyClass);
    }

    /*
     * call-seq:
     *     Map.new(key_type, value_type, value_typeclass = nil, init_hashmap = {})
     *     => new map
     *
     * Allocates a new Map container. This constructor may be called with 2, 3, or 4
     * arguments. The first two arguments are always present and are symbols (taking
     * on the same values as field-type symbols in message descriptors) that
     * indicate the type of the map key and value fields.
     *
     * The supported key types are: :int32, :int64, :uint32, :uint64, :bool,
     * :string, :bytes.
     *
     * The supported value types are: :int32, :int64, :uint32, :uint64, :bool,
     * :string, :bytes, :enum, :message.
     *
     * The third argument, value_typeclass, must be present if value_type is :enum
     * or :message. As in RepeatedField#new, this argument must be a message class
     * (for :message) or enum module (for :enum).
     *
     * The last argument, if present, provides initial content for map. Note that
     * this may be an ordinary Ruby hashmap or another Map instance with identical
     * key and value types. Also note that this argument may be present whether or
     * not value_typeclass is present (and it is unambiguously separate from
     * value_typeclass because value_typeclass's presence is strictly determined by
     * value_type). The contents of this initial hashmap or Map instance are
     * shallow-copied into the new Map: the original map is unmodified, but
     * references to underlying objects will be shared if the value type is a
     * message type.
     */

    @JRubyMethod(required = 2, optional = 2)
    public IRubyObject initialize(ThreadContext context, IRubyObject[] args) {
        this.table = new HashMap<IRubyObject, IRubyObject>();
        this.keyType = Utils.rubyToFieldType(args[0]);
        this.valueType = Utils.rubyToFieldType(args[1]);

        switch(keyType) {
            case INT32:
            case INT64:
            case UINT32:
            case UINT64:
            case BOOL:
            case STRING:
            case BYTES:
                // These are OK.
                break;
            default:
                throw context.runtime.newArgumentError("Invalid key type for map.");
        }

        int initValueArg = 2;
        if (needTypeclass(this.valueType) && args.length > 2) {
            this.valueTypeClass = args[2];
            validateTypeClass(context, this.valueType, this.valueTypeClass);
            initValueArg = 3;
        } else {
            this.valueTypeClass = context.runtime.getNilClass();
        }

        // Table value type is always UINT64: this ensures enough space to store the
        // native_slot value.
        if (args.length > initValueArg) {
            mergeIntoSelf(context, args[initValueArg]);
        }
        return this;
    }

    /*
     * call-seq:
     *     Map.[]=(key, value) => value
     *
     * Inserts or overwrites the value at the given key with the given new value.
     * Throws an exception if the key type is incorrect. Returns the new value that
     * was just inserted.
     */
    @JRubyMethod(name = "[]=")
    public IRubyObject indexSet(ThreadContext context, IRubyObject key, IRubyObject value) {
        checkKeyType(context, key);
        Utils.checkType(context, valueType, value, (RubyModule) valueTypeClass);
        this.table.put(key, value);
        return value;
    }

    /*
     * call-seq:
     *     Map.[](key) => value
     *
     * Accesses the element at the given key. Throws an exception if the key type is
     * incorrect. Returns nil when the key is not present in the map.
     */
    @JRubyMethod(name = "[]")
    public IRubyObject index(ThreadContext context, IRubyObject key) {
        if (table.containsKey(key))
            return this.table.get(key);
        return context.runtime.getNil();
    }

    /*
     * call-seq:
     *     Map.==(other) => boolean
     *
     * Compares this map to another. Maps are equal if they have identical key sets,
     * and for each key, the values in both maps compare equal. Elements are
     * compared as per normal Ruby semantics, by calling their :== methods (or
     * performing a more efficient comparison for primitive types).
     *
     * Maps with dissimilar key types or value types/typeclasses are never equal,
     * even if value comparison (for example, between integers and floats) would
     * have otherwise indicated that every element has equal value.
     */
    @JRubyMethod(name = "==")
    public IRubyObject eql(ThreadContext context, IRubyObject _other) {
        if (_other instanceof RubyHash)
            return toHash(context).op_equal(context, _other);
        RubyMap other = (RubyMap) _other;
        if (this == other) return context.runtime.getTrue();
        if (this.keyType != other.keyType ||
                this.valueType != other.valueType ||
                this.valueTypeClass != other.valueTypeClass ||
                this.table.size() != other.table.size())
            return context.runtime.getFalse();
        for (IRubyObject key : table.keySet()) {
            if (! other.table.containsKey(key))
                return context.runtime.getFalse();
            if (! other.table.get(key).eql(table.get(key)))
                return context.runtime.getFalse();
        }
        return context.runtime.getTrue();
    }

    /*
     * call-seq:
     *     Map.inspect => string
     *
     * Returns a string representing this map's elements. It will be formatted as
     * "{key => value, key => value, ...}", with each key and value string
     * representation computed by its own #inspect method.
     */
    @JRubyMethod
    public IRubyObject inspect(ThreadContext context) {
        return toHash(context).inspect();
    }

    /*
     * call-seq:
     *     Map.hash => hash_value
     *
     * Returns a hash value based on this map's contents.
     */
    @JRubyMethod
    public IRubyObject hash(ThreadContext context) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            for (IRubyObject key : table.keySet()) {
                digest.update((byte) key.hashCode());
                digest.update((byte) table.get(key).hashCode());
            }
            return context.runtime.newString(new ByteList(digest.digest()));
        } catch (NoSuchAlgorithmException ignore) {
            return context.runtime.newFixnum(System.identityHashCode(table));
        }
    }

    /*
     * call-seq:
     *     Map.keys => [list_of_keys]
     *
     * Returns the list of keys contained in the map, in unspecified order.
     */
    @JRubyMethod
    public IRubyObject keys(ThreadContext context) {
        return RubyArray.newArray(context.runtime, table.keySet());
    }

    /*
     * call-seq:
     *     Map.values => [list_of_values]
     *
     * Returns the list of values contained in the map, in unspecified order.
     */
    @JRubyMethod
    public IRubyObject values(ThreadContext context) {
        return RubyArray.newArray(context.runtime, table.values());
    }

    /*
     * call-seq:
     *     Map.clear
     *
     * Removes all entries from the map.
     */
    @JRubyMethod
    public IRubyObject clear(ThreadContext context) {
        table.clear();
        return context.runtime.getNil();
    }

    /*
     * call-seq:
     *     Map.each(&block)
     *
     * Invokes &block on each |key, value| pair in the map, in unspecified order.
     * Note that Map also includes Enumerable; map thus acts like a normal Ruby
     * sequence.
     */
    @JRubyMethod
    public IRubyObject each(ThreadContext context, Block block) {
        for (IRubyObject key : table.keySet()) {
            block.yieldSpecific(context, key, table.get(key));
        }
        return context.runtime.getNil();
    }

    /*
     * call-seq:
     *     Map.delete(key) => old_value
     *
     * Deletes the value at the given key, if any, returning either the old value or
     * nil if none was present. Throws an exception if the key is of the wrong type.
     */
    @JRubyMethod
    public IRubyObject delete(ThreadContext context, IRubyObject key) {
        return table.remove(key);
    }

    /*
     * call-seq:
     *     Map.has_key?(key) => bool
     *
     * Returns true if the given key is present in the map. Throws an exception if
     * the key has the wrong type.
     */
    @JRubyMethod(name = "has_key?")
    public IRubyObject hasKey(ThreadContext context, IRubyObject key) {
        return this.table.containsKey(key) ? context.runtime.getTrue() : context.runtime.getFalse();
    }

    /*
     * call-seq:
     *     Map.length
     *
     * Returns the number of entries (key-value pairs) in the map.
     */
    @JRubyMethod
    public IRubyObject length(ThreadContext context) {
        return context.runtime.newFixnum(this.table.size());
    }

    /*
     * call-seq:
     *     Map.dup => new_map
     *
     * Duplicates this map with a shallow copy. References to all non-primitive
     * element objects (e.g., submessages) are shared.
     */
    @JRubyMethod
    public IRubyObject dup(ThreadContext context) {
        RubyMap newMap = newThisType(context);
        newMap.table = new HashMap<IRubyObject, IRubyObject>();
        for (Map.Entry<IRubyObject, IRubyObject> entry : table.entrySet()) {
            newMap.table.put(entry.getKey(), entry.getValue());
        }
        return newMap;
    }

    private RubyMap newThisType(ThreadContext context) {
        RubyMap newMap;
        if (needTypeclass(valueType)) {
            newMap = (RubyMap) metaClass.newInstance(context,
                    Utils.fieldTypeToRuby(context, keyType),
                    Utils.fieldTypeToRuby(context, valueType),
                    valueTypeClass, Block.NULL_BLOCK);
        } else {
            newMap = (RubyMap) metaClass.newInstance(context,
                    Utils.fieldTypeToRuby(context, keyType),
                    Utils.fieldTypeToRuby(context, valueType),
                    Block.NULL_BLOCK);
        }
        return newMap;
    }

    private RubyHash toHash(ThreadContext context) {
        return RubyHash.newHash(context.runtime, table, context.runtime.getNil());
    }

    private boolean needTypeclass(Descriptors.FieldDescriptor.Type type) {
        switch(type) {
            case MESSAGE:
            case ENUM:
                return true;
            default:
                return false;
        }
    }

    private void validateTypeClass(ThreadContext context, Descriptors.FieldDescriptor.Type type, IRubyObject value) {
        RubyModule klass = (RubyModule) value;
        if (klass.getInstanceVariable(Utils.DESCRIPTOR_INSTANCE_VAR).isNil()) {
            throw context.runtime.newArgumentError("Type class has no descriptor. Please pass a " +
                            "class or enum as returned by the DescriptorPool.");
        }
        // TODO validate descriptor type
    }

    private RubyMap mergeIntoSelf(final ThreadContext context, IRubyObject hashmap) {
        if (hashmap instanceof RubyHash) {
            ((RubyHash) hashmap).visitAll(new RubyHash.Visitor() {
                @Override
                public void visit(IRubyObject key, IRubyObject val) {
                    indexSet(context, key, val);
                }
            });
        } else if (hashmap instanceof RubyMap) {
            RubyMap other = (RubyMap) hashmap;
            if (this.keyType != other.keyType || this.valueType != other.valueType ||
                    this.valueTypeClass != other.valueTypeClass) {
                throw context.runtime.newArgumentError("Attempt to merge Map with mismatching types");
            }
        } else {
            throw context.runtime.newArgumentError("Unknown type merging into Map");
        }
        return this;
    }

    private void checkKeyType(ThreadContext context, IRubyObject key) {
        switch(keyType) {
            case BYTES:
            case STRING:
                if (!(key instanceof RubyString))
                    throw context.runtime.newTypeError(key, "String");
                break;
            case BOOL:
            case INT32:
            case INT64:
            case UINT32:
            case UINT64:
                break;
            default:
                throw context.runtime.newTypeError(key, keyType.name());
        }
    }

    private Descriptors.FieldDescriptor.Type keyType;
    private Descriptors.FieldDescriptor.Type valueType;
    private IRubyObject valueTypeClass;
    private Map<IRubyObject, IRubyObject> table;
}
