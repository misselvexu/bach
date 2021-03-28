package showcode;

import com.sun.source.util.DocTrees;
import java.io.PrintWriter;
import java.util.Set;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;

@SupportedAnnotationTypes("*")
@SupportedSourceVersion(SourceVersion.RELEASE_11)
public class ShowProcessor extends AbstractProcessor {
  PrintWriter out;
  DocTrees treeUtils;

  @Override
  public void init(ProcessingEnvironment pEnv) {
    out = new PrintWriter(System.out);
    treeUtils = DocTrees.instance(pEnv);
  }

  @Override
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
    out.printf("#%n# ShowProcessor.process%n#%n");
    new ShowCode(treeUtils).show(roundEnv.getRootElements(), out);
    out.flush();
    return false;
  }
}
