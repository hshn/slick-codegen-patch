package slick.codegen.patch

import slick.model.Model

class SourceCodeGenerator(
  model: Model,
  patches: Patch*,
) extends slick.codegen.SourceCodeGenerator(model = Patch(patches: _*)(model))
