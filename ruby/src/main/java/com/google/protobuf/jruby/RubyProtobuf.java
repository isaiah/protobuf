package com.google.protobuf.jruby;

import org.jruby.Ruby;
import org.jruby.RubyModule;
import org.jruby.anno.JRubyMethod;
import org.jruby.anno.JRubyModule;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * Created by isaiah on 12/12/14.
 */
@JRubyModule(name = "Protobuf")
public class RubyProtobuf {

    public static void createProtobuf(Ruby runtime) {
        RubyModule mGoogle = runtime.getModule("Google");
        RubyModule mProtobuf = mGoogle.defineModuleUnder("Protobuf");
        mProtobuf.defineAnnotatedMethods(RubyProtobuf.class);
    }

    @JRubyMethod(meta = true)
    public static IRubyObject encode(ThreadContext context, IRubyObject self, IRubyObject message) {
        return RubyMessage.encode(context, message.getMetaClass(), message);
    }

    @JRubyMethod(meta = true)
    public static IRubyObject decode(ThreadContext context, IRubyObject self, IRubyObject klazz, IRubyObject message) {
        return RubyMessage.decode(context, klazz, message);
    }


    @JRubyMethod(name = "encode_json", meta = true)
    public static IRubyObject encodeJson(ThreadContext context, IRubyObject self, IRubyObject message) {
        return RubyMessage.encodeJson(context, message.getMetaClass(), message);
    }

    @JRubyMethod(name = "decode_json", meta = true)
    public static IRubyObject decodeJson(ThreadContext context, IRubyObject self, IRubyObject klazz, IRubyObject message) {
        return RubyMessage.decodeJson(context, klazz, message);
    }

    @JRubyMethod(name = "deep_copy", meta = true)
    public static IRubyObject deepCopy(ThreadContext context, IRubyObject self, IRubyObject message) {
        if (message instanceof RubyMessage) {
            return ((RubyMessage) message).deepCopy(context);
        } else {
            return ((RubyRepeatedField) message).deepCopy(context);
        }
    }
}
