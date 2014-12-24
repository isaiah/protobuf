package google;

import com.google.protobuf.jruby.*;
import org.jruby.Ruby;
import org.jruby.runtime.load.BasicLibraryService;

import java.io.IOException;

/**
 * Created by isaiah on 12/12/14.
 */
public class ProtobufJavaService implements BasicLibraryService {
    @Override
    public boolean basicLoad(Ruby ruby) throws IOException {
        ruby.defineModule("Google");
        RubyProtobuf.createProtobuf(ruby);
        RubyDescriptor.createRubyDescriptor(ruby);
        RubyBuilder.createRubyBuilder(ruby);
        RubyFieldDescriptor.createRubyFileDescriptor(ruby);
        RubyMessageBuilderContext.createRubyMessageBuilderContext(ruby);
        RubyEnumDescriptor.createRubyEnumDescriptor(ruby);
        RubyEnumBuilderContext.createRubyEnumBuilderContext(ruby);
        RubyDescriptorPool.createRubyDescriptorPool(ruby);
        RubyRepeatedField.createRubyRepeatedField(ruby);
        RubyFieldDescriptor.createRubyFileDescriptor(ruby);
        return true;
    }
}
