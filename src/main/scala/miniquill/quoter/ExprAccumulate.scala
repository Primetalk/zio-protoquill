package miniquill.quoter

import scala.quoted._
import scala.quoted.matching._
import scala.collection.mutable.ArrayBuffer
import scala.quoted.util.ExprMap

object ExprAccumulate {
  def apply[T](input: Expr[Any])(matcher: PartialFunction[Expr[Any], T])(given qctx: QuoteContext): List[T] = {
    import qctx.tasty.{Type => QType, given, _}

    val buff: ArrayBuffer[T] = new ArrayBuffer[T]()
    val accum = new ExprMap {
      def transform[TF](expr: Expr[TF])(given qctx: QuoteContext, tpe: Type[TF]): Expr[TF] = {
        matcher.lift(expr) match {
          case Some(result) => 
            buff += result
            expr
          case None =>
            expr
        }

        expr.unseal match {
          // Not including this causes execption "scala.tasty.reflect.ExprCastError: Expr: [ : Nothing]" in certain situations
          case Repeated(Nil, Inferred()) => expr 
          case _ => transformChildren[TF](expr)
        }
      }
    }

    accum.transform(input)
    buff.toList
  }
}
