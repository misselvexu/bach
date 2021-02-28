package bach.info;

import com.github.sormuras.bach.Bach;
import com.github.sormuras.bach.Main;
import com.github.sormuras.bach.Options;
import com.github.sormuras.bach.Options.Flag;
import com.github.sormuras.bach.ProjectInfo;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public class CustomBach extends Bach implements MainSpaceBuilder, TestSpaceBuilder {

  public static void main(String... args) {
    var options = Options.of(args.length == 0 ? new String[] {"build"} : args);
    var bach = provider().newBach(options);
    System.exit(new Main().run(bach));
  }

  public static Provider<CustomBach> provider() {
    return options -> new CustomBach(options.with(Flag.VERBOSE));
  }

  private CustomBach(Options options) {
    super(options);
  }

  @Override
  public String computeProjectVersion(ProjectInfo info) {
    var now = LocalDateTime.now(ZoneOffset.UTC);
    var timestamp = DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(now);
    try {
      return Files.readString(Path.of("VERSION")) + "-custom+" + timestamp;
    } catch (Exception exception) {
      throw new RuntimeException("Read version failed: " + exception);
    }
  }
}
