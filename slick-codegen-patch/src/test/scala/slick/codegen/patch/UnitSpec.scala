package slick.codegen.patch

import cats.effect.testing.scalatest.AsyncIOSpec
import org.scalatest.EitherValues
import org.scalatest.Inside
import org.scalatest.OptionValues
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpec

trait UnitSpec extends AsyncWordSpec with AsyncIOSpec with Matchers with EitherValues with OptionValues with Inside
