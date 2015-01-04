$:.unshift(File.expand_path('../../lib', __FILE__))

require 'minitest/autorun'
require 'google/protobuf'

class TestProtobuf < MiniTest::Unit::TestCase

  pool = Google::Protobuf::DescriptorPool.new
  pool.build do
    add_message "TestMessage" do
      optional :optional_int32, :int32, 1
      optional :optional_int64, :int64, 2
      optional :optional_uint32, :uint32, 3
      optional :optional_uint64, :uint64, 4
      optional :optional_bool,   :bool,         5
      optional :optional_float,  :float,        6
      optional :optional_double, :double,       7
      optional :optional_string, :string,       8
      optional :optional_bytes,  :bytes,        9
      optional :optional_msg,    :message,      10, "TestMessage2"
      optional :optional_enum,   :enum,         11, "TestEnum"

      repeated :repeated_int32,  :int32,        12
      repeated :repeated_int64,  :int64,        13
      repeated :repeated_uint32, :uint32,       14
      repeated :repeated_uint64, :uint64,       15
      repeated :repeated_bool,   :bool,         16
      repeated :repeated_float,  :float,        17
      repeated :repeated_double, :double,       18
      repeated :repeated_string, :string,       19
      repeated :repeated_bytes,  :bytes,        20
      repeated :repeated_msg,    :message,      21, "TestMessage2"
      repeated :repeated_enum,   :enum,         22, "TestEnum"

    end
    add_message "TestMessage2" do
      optional :foo, :int32, 1
    end

    add_message "Recursive1" do
      optional :foo, :message, 1, "Recursive2"
    end
    add_message "Recursive2" do
      optional :foo, :message, 1, "Recursive1"
    end

    add_enum "TestEnum" do
      value :Default, 0
      value :A, 1
      value :B, 2
      value :C, 3
    end

    add_message "BadFieldNames" do
      optional :dup, :int32, 1
      optional :class, :int32, 2
      optional :"a.b", :int32, 3
    end

    add_message "MapMessage" do
      map :map_string_int32, :string, :int32, 1
      map :map_string_msg, :string, :message, 2, "TestMessage2"
    end
    add_message "MapMessageWireEquiv" do
      repeated :map_string_int32, :message, 1, "MapMessageWireEquiv_entry1"
      repeated :map_string_msg, :message, 2, "MapMessageWireEquiv_entry2"
    end
    add_message "MapMessageWireEquiv_entry1" do
      optional :key, :string, 1
      optional :value, :int32, 2
    end
    add_message "MapMessageWireEquiv_entry2" do
      optional :key, :string, 1
      optional :value, :message, 2, "TestMessage2"
    end

    add_message "OneofMessage" do
      oneof :my_oneof do
        optional :a, :string, 1
        optional :b, :int32, 2
        optional :c, :message, 3, "TestMessage2"
        optional :d, :enum, 4, "TestEnum"
      end
    end
  end
  TestMessage = pool.lookup("TestMessage").msgclass
  TestMessage2 = pool.lookup("TestMessage2").msgclass
  Recursive1 = pool.lookup("Recursive1").msgclass
  Recursive2 = pool.lookup("Recursive2").msgclass
  TestEnum = pool.lookup("TestEnum").enummodule
  BadFieldNames = pool.lookup("BadFieldNames").msgclass
  MapMessage = pool.lookup("MapMessage").msgclass
  MapMessageWireEquiv = pool.lookup("MapMessageWireEquiv").msgclass
  MapMessageWireEquiv_entry1 =
    pool.lookup("MapMessageWireEquiv_entry1").msgclass
  MapMessageWireEquiv_entry2 =
    pool.lookup("MapMessageWireEquiv_entry2").msgclass
  OneofMessage = pool.lookup("OneofMessage").msgclass


  def test_defaults
    m = TestMessage.new
    assert_equal 0, m.optional_int32
    assert_equal 0, m.optional_int64
    assert_equal 0, m.optional_uint32
    assert_equal 0, m.optional_uint64
    assert_equal false, m.optional_bool
    assert_equal 0.0, m.optional_float
    assert_equal 0.0, m.optional_double
    assert_equal "", m.optional_string
    assert_equal "", m.optional_bytes
  end

  def test_setters
    m = TestMessage.new
    m.optional_int32 = -42
    assert_equal(-42, m.optional_int32)
    m.optional_int64 = -0x1_0000_0000
    assert_equal(-0x1_0000_0000, m.optional_int64)
    m.optional_uint32 = 0x9000_0000
    assert_equal 0x9000_0000, m.optional_uint32
    m.optional_uint64 = 0x9000_0000_0000_0000
    assert_equal 0x9000_0000_0000_0000, m.optional_uint64
    m.optional_bool = true
    assert_equal true, m.optional_bool
    m.optional_float = 0.5
    assert_equal 0.5, m.optional_float
    m.optional_double = 0.5
    assert_equal 0.5, m.optional_double
    m.optional_string = "hello"
    assert_equal "hello", m.optional_string
    m.optional_bytes = "world".encode!('ASCII-8BIT')
    assert_equal "world", m.optional_bytes
  end

  def test_ctor_args
    m = TestMessage.new(:optional_int32 => -42,
                        :optional_msg => TestMessage2.new,
                        :optional_enum => :C,
                        :repeated_string => ["hello", "there", "world"]
                       )
    assert_equal(-42, m.optional_int32)
    assert_equal TestMessage2, m.optional_msg.class
    assert_equal 3, m.repeated_string.length
    assert_equal :C, m.optional_enum
    assert_equal "hello", m.repeated_string[0]
    assert_equal "there", m.repeated_string[1]
    assert_equal "world", m.repeated_string[2]
  end

  def test_inspect
    m = TestMessage.new(:optional_int32 => -42,
                        :optional_enum => :A,
                        :optional_msg => TestMessage2.new,
                        :repeated_string => ["hello", "there", "world"])
    expected = '#<TestProtobuf::TestMessage: optional_int32: -42, optional_int64: 0, optional_uint32: 0, optional_uint64: 0, optional_bool: false, optional_float: 0.0, optional_double: 0.0, optional_string: "", optional_bytes: "", optional_msg: #<TestProtobuf::TestMessage2: foo: 0>, optional_enum: :A, repeated_int32: [], repeated_int64: [], repeated_uint32: [], repeated_uint64: [], repeated_bool: [], repeated_float: [], repeated_double: [], repeated_string: ["hello", "there", "world"], repeated_bytes: [], repeated_msg: [], repeated_enum: []>'
    assert_equal expected, m.inspect
  end

  def test_hash
    m1 = TestMessage.new(:optional_int32 => 42)
    m2 = TestMessage.new(:optional_int32 => 102)
    refute_equal 0, m1.hash
    refute_equal 0, m2.hash
    refute_equal m1.hash, m2.hash
  end

  def test_type_errors
    m = TestMessage.new
    assert_raises TypeError do
      m.optional_int32 = "hello"
    end
    assert_raises TypeError do
      m.optional_string = 42
    end
    assert_raises TypeError do
      m.optional_string = nil
    end
    assert_raises TypeError do
      m.optional_bool = 42
    end
    assert_raises TypeError do
      m.optional_msg = TestMessage.new  # expects TestMessage2
    end

    assert_raises TypeError do
      m.repeated_int32 = []  # needs RepeatedField
    end

    assert_raises TypeError do
      m.repeated_int32.push "hello"
    end

    assert_raises TypeError do
      m.repeated_msg.push TestMessage.new
    end
  end

  def test_string_encoding
    m = TestMessage.new

    # Assigning a normal (ASCII or UTF8) string to a bytes field, or
    # ASCII-8BIT to a string field, raises an error.
    assert_raises TypeError do
      m.optional_bytes = "Test string ASCII".encode!('ASCII')
    end
    assert_raises TypeError do
      m.optional_bytes = "Test string UTF-8 \u0100".encode!('UTF-8')
    end
    assert_raises TypeError do
      m.optional_string = ["FFFF"].pack('H*')
    end

    # "Ordinary" use case.
    m.optional_bytes = ["FFFF"].pack('H*')
    m.optional_string = "\u0100"

    # strings are mutable so we can do this, but serialize should catch it.
    m.optional_string = "asdf".encode!('UTF-8')
    m.optional_string.encode!('ASCII-8BIT')
    unless jruby?
      assert_raises TypeError do
        TestMessage.encode(m)
      end
    end
  end

  def test_rptfield_int32
    l = Google::Protobuf::RepeatedField.new(:int32)
    assert_equal 0, l.count
    l = Google::Protobuf::RepeatedField.new(:int32, [1, 2, 3])
    assert_equal 3, l.count
    assert_equal l, [1, 2, 3]
    l.push 4
    assert_equal [1, 2, 3, 4], l
    dst_list = []
    l.each { |val| dst_list.push val }
    assert_equal dst_list, [1, 2, 3, 4]
    assert_equal l.to_a, [1, 2, 3, 4]
    assert_equal l[0], 1
    assert_equal l[3], 4
    l[0] = 5
    assert_equal l, [5, 2, 3, 4]

    l2 = l.dup
    assert_equal l, l2
    refute_equal l.object_id, l2.object_id
    l2.push 6
    assert_equal l.count, 4
    assert_equal l2.count, 5

    assert_equal l.inspect, '[5, 2, 3, 4]'

    l.insert(7, 8, 9)
    assert_equal [5, 2, 3, 4, 7, 8, 9], l
    assert_equal 9, l.pop
    assert_equal [5, 2, 3, 4, 7, 8], l

    assert_raises TypeError do
      m = TestMessage.new
      l.push m
    end

    m = TestMessage.new
    m.repeated_int32 = l
    assert_equal m.repeated_int32, [5, 2, 3, 4, 7, 8]
    assert_equal m.repeated_int32.object_id, l.object_id
    l.push 42
    assert_equal m.repeated_int32.pop, 42

    l3 = l + l.dup
    assert_equal l3.count, l.count * 2
    l.count.times do |i|
      assert_equal l3[i], l[i]
      assert_equal l3[l.count + i], l[i]
    end

    l.clear
    assert_equal l.count, 0
    l += [1, 2, 3, 4]
    l.replace([5, 6, 7, 8])
    assert_equal l, [5, 6, 7, 8]

    l4 = Google::Protobuf::RepeatedField.new(:int32)
    l4[5] = 42
    assert_equal [0, 0, 0, 0, 0, 42], l4

    l4 << 100
    assert_equal l4, [0, 0, 0, 0, 0, 42, 100]
    l4 << 101 << 102
    assert_equal l4, [0, 0, 0, 0, 0, 42, 100, 101, 102]
  end

  def test_rptfield_msg
    l = Google::Protobuf::RepeatedField.new(:message, TestMessage)
    l.push TestMessage.new
    assert_equal 1, l.count
    assert_raises TypeError do
      l.push TestMessage2.new
    end
    assert_raises TypeError do
      l.push 42
    end

    l2 = l.dup
    assert_equal l2[0], l[0]
    assert_equal l2[0].object_id, l[0].object_id

    l2 = Google::Protobuf.deep_copy(l)
    assert_equal l2[0], l[0]
    refute_equal l2[0].object_id, l[0].object_id

    l3 = l + l2
    assert_equal 2, l3.count
    assert_equal l3[0], l[0]
    assert_equal l3[1], l2[0]
    l3[0].optional_int32 = 1000
    assert_equal 1000, l[0].optional_int32

    new_msg = TestMessage.new(:optional_int32 => 200)
    l4 = l + [new_msg]
    assert_equal 2, l4.count
    new_msg.optional_int32 = 1000
    assert_equal 1000, l4[1].optional_int32
  end

  def test_rptfield_enum
    l = Google::Protobuf::RepeatedField.new(:enum, TestEnum)
    l.push :A
    l.push :B
    l.push :C
    assert_equal 3, l.count
    assert_raises NameError do
      l.push :D
    end
    assert_equal :A, l[0]

    l.push 4
    assert_equal 4, l[3]
  end

  def test_rptfield_initialize
    assert_raises ArgumentError do
      Google::Protobuf::RepeatedField.new
    end
    assert_raises ArgumentError do
      Google::Protobuf::RepeatedField.new(:message)
    end
    assert_raises ArgumentError do
      Google::Protobuf::RepeatedField.new([1, 2, 3])
    end
    assert_raises ArgumentError do
      Google::Protobuf::RepeatedField.new(:message, [TestMessage2.new])
    end
  end

    def test_map_basic
      # allowed key types:
      # :int32, :int64, :uint32, :uint64, :bool, :string, :bytes.

      m = Google::Protobuf::Map.new(:string, :int32)
      m["asdf"] = 1
      assert m["asdf"] == 1
      m["jkl;"] = 42
      assert m == { "jkl;" => 42, "asdf" => 1 }
      assert m.has_key?("asdf")
      assert !m.has_key?("qwerty")
      assert m.length == 2

      m2 = m.dup
      assert m == m2
      assert m.hash != 0
      assert m.hash == m2.hash

      collected = {}
      m.each { |k,v| collected[v] = k }
      assert collected == { 42 => "jkl;", 1 => "asdf" }

      assert m.delete("asdf") == 1
      assert !m.has_key?("asdf")
      assert m["asdf"] == nil
      assert !m.has_key?("asdf")

      # We only assert on inspect value when there is one map entry because the
      # order in which elements appear is unspecified (depends on the internal
      # hash function). We don't want a brittle test.
      assert m.inspect == "{\"jkl;\" => 42}"

      assert m.keys == ["jkl;"]
      assert m.values == [42]

      m.clear
      assert m.length == 0
      assert m == {}

      assert_raise TypeError do
        m[1] = 1
      end
      assert_raise RangeError do
        m["asdf"] = 0x1_0000_0000
      end
    end

    def test_map_ctor
      m = Google::Protobuf::Map.new(:string, :int32,
                                    {"a" => 1, "b" => 2, "c" => 3})
      assert m == {"a" => 1, "c" => 3, "b" => 2}
    end

    def test_map_keytypes
      m = Google::Protobuf::Map.new(:int32, :int32)
      m[1] = 42
      m[-1] = 42
      assert_raise RangeError do
        m[0x8000_0000] = 1
      end
      assert_raise TypeError do
        m["asdf"] = 1
      end

      m = Google::Protobuf::Map.new(:int64, :int32)
      m[0x1000_0000_0000_0000] = 1
      assert_raise RangeError do
        m[0x1_0000_0000_0000_0000] = 1
      end
      assert_raise TypeError do
        m["asdf"] = 1
      end

      m = Google::Protobuf::Map.new(:uint32, :int32)
      m[0x8000_0000] = 1
      assert_raise RangeError do
        m[0x1_0000_0000] = 1
      end
      assert_raise RangeError do
        m[-1] = 1
      end

      m = Google::Protobuf::Map.new(:uint64, :int32)
      m[0x8000_0000_0000_0000] = 1
      assert_raise RangeError do
        m[0x1_0000_0000_0000_0000] = 1
      end
      assert_raise RangeError do
        m[-1] = 1
      end

      m = Google::Protobuf::Map.new(:bool, :int32)
      m[true] = 1
      m[false] = 2
      assert_raise TypeError do
        m[1] = 1
      end
      assert_raise TypeError do
        m["asdf"] = 1
      end

      m = Google::Protobuf::Map.new(:string, :int32)
      m["asdf"] = 1
      assert_raise TypeError do
        m[1] = 1
      end
      assert_raise TypeError do
        bytestring = ["FFFF"].pack("H*")
        m[bytestring] = 1
      end

      m = Google::Protobuf::Map.new(:bytes, :int32)
      bytestring = ["FFFF"].pack("H*")
      m[bytestring] = 1
      assert_raise TypeError do
        m["asdf"] = 1
      end
      assert_raise TypeError do
        m[1] = 1
      end
    end

    def test_map_msg_enum_valuetypes
      m = Google::Protobuf::Map.new(:string, :message, TestMessage)
      m["asdf"] = TestMessage.new
      assert_raise TypeError do
        m["jkl;"] = TestMessage2.new
      end

      m = Google::Protobuf::Map.new(
        :string, :message, TestMessage,
        { "a" => TestMessage.new(:optional_int32 => 42),
          "b" => TestMessage.new(:optional_int32 => 84) })
      assert m.length == 2
      assert m.values.map{|msg| msg.optional_int32}.sort == [42, 84]

      m = Google::Protobuf::Map.new(:string, :enum, TestEnum,
                                    { "x" => :A, "y" => :B, "z" => :C })
      assert m.length == 3
      assert m["z"] == :C
      m["z"] = 2
      assert m["z"] == :B
      m["z"] = 4
      assert m["z"] == 4
      assert_raise RangeError do
        m["z"] = :Z
      end
      assert_raise TypeError do
        m["z"] = "z"
      end
    end

    def test_map_dup_deep_copy
      m = Google::Protobuf::Map.new(
        :string, :message, TestMessage,
        { "a" => TestMessage.new(:optional_int32 => 42),
          "b" => TestMessage.new(:optional_int32 => 84) })

      m2 = m.dup
      assert m == m2
      assert m.object_id != m2.object_id
      assert m["a"].object_id == m2["a"].object_id
      assert m["b"].object_id == m2["b"].object_id

      m2 = Google::Protobuf.deep_copy(m)
      assert m == m2
      assert m.object_id != m2.object_id
      assert m["a"].object_id != m2["a"].object_id
      assert m["b"].object_id != m2["b"].object_id
    end

    def test_map_field
      m = MapMessage.new
      assert m.map_string_int32 == {}
      assert m.map_string_msg == {}

      m = MapMessage.new(
        :map_string_int32 => {"a" => 1, "b" => 2},
        :map_string_msg => {"a" => TestMessage2.new(:foo => 1),
                            "b" => TestMessage2.new(:foo => 2)})
      assert m.map_string_int32.keys.sort == ["a", "b"]
      assert m.map_string_int32["a"] == 1
      assert m.map_string_msg["b"].foo == 2

      m.map_string_int32["c"] = 3
      assert m.map_string_int32["c"] == 3
      m.map_string_msg["c"] = TestMessage2.new(:foo => 3)
      assert m.map_string_msg["c"] == TestMessage2.new(:foo => 3)
      m.map_string_msg.delete("b")
      m.map_string_msg.delete("c")
      assert m.map_string_msg == { "a" => TestMessage2.new(:foo => 1) }

      assert_raise TypeError do
        m.map_string_msg["e"] = TestMessage.new # wrong value type
      end
      # ensure nothing was added by the above
      assert m.map_string_msg == { "a" => TestMessage2.new(:foo => 1) }

      m.map_string_int32 = Google::Protobuf::Map.new(:string, :int32)
      assert_raise TypeError do
        m.map_string_int32 = Google::Protobuf::Map.new(:string, :int64)
      end
      assert_raise TypeError do
        m.map_string_int32 = {}
      end

      assert_raise TypeError do
        m = MapMessage.new(:map_string_int32 => { 1 => "I am not a number" })
      end
    end

    def test_map_encode_decode
      m = MapMessage.new(
        :map_string_int32 => {"a" => 1, "b" => 2},
        :map_string_msg => {"a" => TestMessage2.new(:foo => 1),
                            "b" => TestMessage2.new(:foo => 2)})
      m2 = MapMessage.decode(MapMessage.encode(m))
      assert m == m2

      m3 = MapMessageWireEquiv.decode(MapMessage.encode(m))
      assert m3.map_string_int32.length == 2

      kv = {}
      m3.map_string_int32.map { |msg| kv[msg.key] = msg.value }
      assert kv == {"a" => 1, "b" => 2}

      kv = {}
      m3.map_string_msg.map { |msg| kv[msg.key] = msg.value }
      assert kv == {"a" => TestMessage2.new(:foo => 1),
                    "b" => TestMessage2.new(:foo => 2)}
    end

    def test_oneof_descriptors
      d = OneofMessage.descriptor
      o = d.lookup_oneof("my_oneof")
      assert o != nil
      assert o.class == Google::Protobuf::OneofDescriptor
      assert o.name == "my_oneof"
      oneof_count = 0
      d.each_oneof{ |oneof|
        oneof_count += 1
        assert oneof == o
      }
      assert oneof_count == 1
      assert o.count == 4
      field_names = o.map{|f| f.name}.sort
      assert field_names == ["a", "b", "c", "d"]
    end

    def test_oneof
      d = OneofMessage.new
      assert d.a == nil
      assert d.b == nil
      assert d.c == nil
      assert d.d == nil
      assert d.my_oneof == nil

      d.a = "hi"
      assert d.a == "hi"
      assert d.b == nil
      assert d.c == nil
      assert d.d == nil
      assert d.my_oneof == :a

      d.b = 42
      assert d.a == nil
      assert d.b == 42
      assert d.c == nil
      assert d.d == nil
      assert d.my_oneof == :b

      d.c = TestMessage2.new(:foo => 100)
      assert d.a == nil
      assert d.b == nil
      assert d.c.foo == 100
      assert d.d == nil
      assert d.my_oneof == :c

      d.d = :C
      assert d.a == nil
      assert d.b == nil
      assert d.c == nil
      assert d.d == :C
      assert d.my_oneof == :d

      d2 = OneofMessage.decode(OneofMessage.encode(d))
      assert d2 == d

      encoded_field_a = OneofMessage.encode(OneofMessage.new(:a => "string"))
      encoded_field_b = OneofMessage.encode(OneofMessage.new(:b => 1000))
      encoded_field_c = OneofMessage.encode(
        OneofMessage.new(:c => TestMessage2.new(:foo => 1)))
      encoded_field_d = OneofMessage.encode(OneofMessage.new(:d => :B))

      d3 = OneofMessage.decode(
        encoded_field_c + encoded_field_a + encoded_field_d)
      assert d3.a == nil
      assert d3.b == nil
      assert d3.c == nil
      assert d3.d == :B

      d4 = OneofMessage.decode(
        encoded_field_c + encoded_field_a + encoded_field_d +
        encoded_field_c)
      assert d4.a == nil
      assert d4.b == nil
      assert d4.c.foo == 1
      assert d4.d == nil

      d5 = OneofMessage.new(:a => "hello")
      assert d5.a != nil
      d5.a = nil
      assert d5.a == nil
      assert OneofMessage.encode(d5) == ''
      assert d5.my_oneof == nil
    end

  def test_enum_field
    m = TestMessage.new
    assert_equal :Default, m.optional_enum
    m.optional_enum = :A
    assert_equal :A, m.optional_enum
    assert_raises RangeError do
      m.optional_enum = :ASDF
    end
    m.optional_enum = 1
    assert_equal :A, m.optional_enum
    m.optional_enum = 100
    assert_equal 100, m.optional_enum
  end

  def test_dup
    m = TestMessage.new
    m.optional_string = "hello"
    m.optional_int32 = 42
    m.repeated_msg.push TestMessage2.new(:foo => 100)
    m.repeated_msg.push TestMessage2.new(:foo => 200)

    m2 = m.dup
    assert_equal m, m2
    m.optional_int32 += 1
    refute_equal m, m2
    assert_equal m.repeated_msg[0], m2.repeated_msg[0]
    assert_equal m.repeated_msg[0].object_id, m2.repeated_msg[0].object_id
  end

  def test_deep_copy
    m = TestMessage.new(:optional_int32 => 42,
                        :repeated_msg => [TestMessage2.new(:foo => 100)])
    m2 = Google::Protobuf.deep_copy(m)
    assert_equal m, m2
    assert_equal m.repeated_msg, m2.repeated_msg
    refute_equal m.repeated_msg.object_id, m2.repeated_msg.object_id
    refute_equal m.repeated_msg[0].object_id, m2.repeated_msg[0].object_id
  end

    def test_eq
      m = TestMessage.new(:optional_int32 => 42,
                          :repeated_int32 => [1, 2, 3])
      m2 = TestMessage.new(:optional_int32 => 43,
                           :repeated_int32 => [1, 2, 3])
      assert m != m2
    end

  def test_enum_lookup
    assert_equal 1, TestEnum::A
    assert_equal 2, TestEnum::B
    assert_equal 3, TestEnum::C

    assert_equal :A, TestEnum.lookup(1)
    assert_equal :B, TestEnum.lookup(2)
    assert_equal :C, TestEnum.lookup(3)

    assert_equal 1, TestEnum.resolve(:A)
    assert_equal 2, TestEnum.resolve(:B)
    assert_equal 3, TestEnum.resolve(:C)
  end

  def test_parse_serialize
    m = TestMessage.new(:optional_int32 => 42,
                        :optional_string => "hello world",
                        :optional_enum => :B,
                        :repeated_string => ["a", "b", "c"],
                        :repeated_int32 => [42, 43, 44],
                        #:repeated_enum => [:A, :B, :C, 100],
                        :repeated_enum => [:A, :B, :C],
                        :repeated_msg => [TestMessage2.new(:foo => 1), TestMessage2.new(:foo => 2)])
    data = TestMessage.encode m
    m2 = TestMessage.decode data
    assert_equal m, m2

    data = Google::Protobuf.encode m
    m2 = Google::Protobuf.decode(TestMessage, data)
    assert_equal m, m2
  end

  def test_def_errors
    s = Google::Protobuf::DescriptorPool.new
    assert_raises TypeError do
      s.build do
        # enum with no default (integer value 0)
        add_enum "MyEnum" do
          value :A, 1
        end
      end
    end
    assert_raises TypeError do
      s.build do
        # message with required field (unsupported in proto3)
        add_message "MyMessage" do
          required :foo, :int32, 1
        end
      end
    end
  end

  def test_corecursive
    # just be sure that we can instantiate types with corecursive field-type
    # references.
    m = Recursive1.new(:foo => Recursive2.new(:foo => Recursive1.new))
    assert_equal Recursive1.descriptor.lookup("foo").subtype,
      Recursive2.descriptor
    assert_equal Recursive2.descriptor.lookup("foo").subtype,
      Recursive1.descriptor

    serialized = Recursive1.encode(m)
    m2 = Recursive1.decode(serialized)
    # XXX StackOverFlow: cannot tell DynamicMessage.new vs DynamicMessage.defaultInstance
    unless jruby?
      assert_equal m, m2
    end
  end

  def test_serialize_cycle
    m = Recursive1.new(:foo => Recursive2.new)
    m.foo.foo = m
    unless jruby?
      assert_raises RuntimeError do
        Recursive1.encode(m)
      end
    end
  end

  def test_bad_field_names
    m = BadFieldNames.new(:dup => 1, :class => 2)
    m2 = m.dup
    assert_equal m, m2
    assert_equal 1, m['dup']
    assert m['class'] == 2
    m['dup'] = 3
    assert m['dup'] == 3
    m['a.b'] = 4
    assert m['a.b'] == 4
  end

  def test_int_ranges
    m = TestMessage.new

    m.optional_int32 = 0
    m.optional_int32 = -0x8000_0000
    m.optional_int32 = +0x7fff_ffff
    m.optional_int32 = 1.0
    m.optional_int32 = -1.0
    m.optional_int32 = 2e9
    assert_raises RangeError do
      m.optional_int32 = -0x8000_0001
    end
    assert_raises RangeError do
      m.optional_int32 = +0x8000_0000
    end
    assert_raises RangeError do
      m.optional_int32 = +0x1000_0000_0000_0000_0000_0000 # force Bignum
    end
    assert_raises RangeError do
      m.optional_int32 = 1e12
    end
    assert_raises RangeError do
      m.optional_int32 = 1.5
    end

    m.optional_uint32 = 0
    m.optional_uint32 = +0xffff_ffff
    m.optional_uint32 = 1.0
    m.optional_uint32 = 4e9
    assert_raises RangeError do
      m.optional_uint32 = -1
    end
    assert_raises RangeError do
      m.optional_uint32 = -1.5
    end
    assert_raises RangeError do
      m.optional_uint32 = -1.5e12
    end
    assert_raises RangeError do
      m.optional_uint32 = -0x1000_0000_0000_0000
    end
    assert_raises RangeError do
      m.optional_uint32 = +0x1_0000_0000
    end
    assert_raises RangeError do
      m.optional_uint32 = +0x1000_0000_0000_0000_0000_0000 # force Bignum
    end
    assert_raises RangeError do
      m.optional_uint32 = 1e12
    end
    assert_raises RangeError do
      m.optional_uint32 = 1.5
    end

    m.optional_int64 = 0
    m.optional_int64 = -0x8000_0000_0000_0000
    m.optional_int64 = +0x7fff_ffff_ffff_ffff
    m.optional_int64 = 1.0
    m.optional_int64 = -1.0
    m.optional_int64 = 8e18
    m.optional_int64 = -8e18
    assert_raises RangeError do
      m.optional_int64 = -0x8000_0000_0000_0001
    end
    assert_raises RangeError do
      m.optional_int64 = +0x8000_0000_0000_0000
    end
    assert_raises RangeError do
      m.optional_int64 = +0x1000_0000_0000_0000_0000_0000 # force Bignum
    end
    assert_raises RangeError do
      m.optional_int64 = 1e50
    end
    assert_raises RangeError do
      m.optional_int64 = 1.5
    end

    m.optional_uint64 = 0
    m.optional_uint64 = +0xffff_ffff_ffff_ffff
    m.optional_uint64 = 1.0
    m.optional_uint64 = 16e18
    assert_raises RangeError do
      m.optional_uint64 = -1
    end
    assert_raises RangeError do
      m.optional_uint64 = -1.5
    end
    assert_raises RangeError do
      m.optional_uint64 = -1.5e12
    end
    assert_raises RangeError do
      m.optional_uint64 = -0x1_0000_0000_0000_0000
    end
    assert_raises RangeError do
      m.optional_uint64 = +0x1_0000_0000_0000_0000
    end
    assert_raises RangeError do
      m.optional_uint64 = +0x1000_0000_0000_0000_0000_0000 # force Bignum
    end
    assert_raises RangeError do
      m.optional_uint64 = 1e50
    end
    assert_raises RangeError do
      m.optional_uint64 = 1.5
    end
  end

  def test_stress_test
    m = TestMessage.new
    m.optional_int32 = 42
    m.optional_int64 = 0x100000000
    m.optional_string = "hello world"
    10.times { m.repeated_msg.push TestMessage2.new(:foo => 42) }
    10.times { m.repeated_string.push "hello world" }

    data = TestMessage.encode(m)

    10_000.times do
      m = TestMessage.decode(data)
      data_new = TestMessage.encode(m)
      assert data_new == data
      data = data_new
    end
  end

  def test_reflection
    m = TestMessage.new(:optional_int32 => 1234)
    msgdef = m.class.descriptor
    assert msgdef.class == Google::Protobuf::Descriptor
    assert msgdef.any? {|field| field.name == "optional_int32"}
    optional_int32 = msgdef.lookup "optional_int32"
    assert_equal Google::Protobuf::FieldDescriptor, optional_int32.class
    refute_nil optional_int32
    assert_equal "optional_int32", optional_int32.name
    assert_equal :int32, optional_int32.type
    optional_int32.set(m, 5678)
    assert_equal 5678, m.optional_int32
    m.optional_int32 = 1000
    assert_equal 1000, optional_int32.get(m)

    optional_msg = msgdef.lookup "optional_msg"
    assert_equal optional_msg.subtype, TestMessage2.descriptor

    optional_msg.set(m, optional_msg.subtype.msgclass.new)

    assert_equal msgdef.msgclass, TestMessage

    optional_enum = msgdef.lookup "optional_enum"
    assert_equal optional_enum.subtype, TestEnum.descriptor
    assert_instance_of Google::Protobuf::EnumDescriptor, optional_enum.subtype
    optional_enum.subtype.each do |k, v|
      # set with integer, check resolution to symbolic name
      optional_enum.set(m, v)
      assert_equal k, optional_enum.get(m)
    end
  end

  def test_json
    m = TestMessage.new(:optional_int32 => 1234,
                        :optional_int64 => -0x1_0000_0000,
                        :optional_uint32 => 0x8000_0000,
                        :optional_uint64 => 0xffff_ffff_ffff_ffff,
                        :optional_bool => true,
                        :optional_float => 1.0,
                        :optional_double => -1e100,
                        :optional_string => "Test string",
                        :optional_bytes => ["FFFFFFFF"].pack('H*'),
                        :optional_msg => TestMessage2.new(:foo => 42),
                        :repeated_int32 => [1, 2, 3, 4],
                        :repeated_string => ["a", "b", "c"],
                        :repeated_bool => [true, false, true, false],
                        :repeated_msg => [TestMessage2.new(:foo => 1),
                                          TestMessage2.new(:foo => 2)])

    json_text = TestMessage.encode_json(m)
    m2 = TestMessage.decode_json(json_text)
    assert_equal m, m2
  end

  newPool = Google::Protobuf::DescriptorPool.new
  newPool.build do
    add_message "TestMessage" do
      optional :optional_int32, :int32, 1
    end

    add_enum "TestEnum" do
      value :A, 0
    end
  end

  TestM = newPool.lookup("TestMessage").msgclass
  TestE = newPool.lookup("TestEnum").enummodule
  # not in c implementation
  # insure that message class name is determined by the assignment
  def test_redefine_message
    assert_equal "TestProtobuf::TestM", TestM.name
    assert_equal "TestProtobuf::TestE", TestE.name
  end

  private
  def jruby?
    RUBY_PLATFORM == "java"
  end
end
