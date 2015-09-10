package closure

import com.google.javascript.jscomp.CompilerOptions.LanguageMode
import com.google.javascript.jscomp._

object compiler {

  def minify(code: String): String = {
    minify("", code)
  }

  def minify(external: String, code: String): String = {
    val compiler = new Compiler
    compiler.disableThreads()

    val options = new CompilerOptions
    CompilationLevel.SIMPLE_OPTIMIZATIONS.setOptionsForCompilationLevel(options)
    options.setLanguageIn(LanguageMode.ECMASCRIPT5_STRICT)
    options.setWarningLevel(DiagnosticGroups.NON_STANDARD_JSDOC, CheckLevel.OFF)

    compiler.compile(
      SourceFile.builder.buildFromCode("external.js", external),
      SourceFile.builder.buildFromCode("input.js", code),
      options)

    compiler.toSource
  }
}
