$:.unshift(File.expand_path('../../lib', __FILE__))

require 'google/protobuf'
require 'benchmark'
require 'minitest/autorun'

class StressTest < MiniTest::Unit::TestCase
  pool = Google::Protobuf::DescriptorPool.new
  pool.build do
    add_message "TestMessage" do
      optional :a,  :int32,        1
      repeated :b,  :message,      2, "M"
    end
    add_message "M" do
      optional :foo, :string, 1
    end
  end

  TestMessage = pool.lookup("TestMessage").msgclass
  M = pool.lookup("M").msgclass

  def setup
    @m = TestMessage.new(:a => 1000,
                         :b => [M.new(:foo => "hello"),
                               M.new(:foo => "world")])
  end

  def test_stress
    data = TestMessage.encode(@m)
    n = 100_000
    encode_decode = proc do
      mnew = TestMessage.decode(data)
      mnew = mnew.dup
      assert_equal mnew.inspect, @m.inspect
      assert_equal TestMessage.encode(mnew), data
    end

    # warmup
    n.times(&encode_decode)

    Benchmark.bm do |x|
      x.report do
        n.times(&encode_decode)
      end
    end
  end
end
