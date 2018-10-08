package fasterparser

import scala.annotation.unchecked.uncheckedVariance


class Parse[+T](val input: String,
                var failureStack: List[(String, Int)],
                var failureMsg: String,
                var isSuccess: Boolean,
                var logDepth: Int,
                var index: Int,
                val startIndex: Int,
                var successCut: Boolean,
                var failureCut: Boolean,
                var successValue: Any,
                var noCut: Boolean,
                val traceIndex: Int,
                var originalParser: Parse[_] => Parse[_]){
  def read[T](p: Parse[_] => Parse[T] @uncheckedVariance): Result[T] = {
    if (originalParser == null) originalParser = p
    p(this).result
  }
  // Use telescoping methods rather than default arguments to try and minimize
  // the amount of bytecode generated at the callsite.
  //
  // Because FasterParser inlines aggressively, it is very easy for a user to
  // generate huge methods, so anything we can do to reduce the size of the
  // generated code helps avoid bytecode size blowup

  def freshSuccess[V](value: V, index: Int) = prepareSuccess(value, index, cut = false)
  def freshSuccess[V](value: V) = prepareSuccess(value, index, cut = false)
  def prepareSuccess[V](value: V): Parse[V] = prepareSuccess(value, index, successCut)
  def prepareSuccess[V](value: V, index: Int): Parse[V] = prepareSuccess(value, index, successCut)
  def prepareSuccess[V](value: V, cut: Boolean): Parse[V] = prepareSuccess(value, index, cut)
  def prepareSuccess[V](value: V, index: Int, cut: Boolean): Parse[V] = {
    isSuccess = true
    successValue = value
    this.index = index
    successCut = cut
    this.asInstanceOf[Parse[V]]
  }
  def freshFailure(msg: String): Parse[Nothing] = {
    val res = prepareFailure(index, cut = false)
    this.failureMsg = msg
    res
  }
  def freshFailure(msg: String, startPos: Int): Parse[Nothing] = {
    val res = prepareFailure(startPos, cut = false)
    this.failureMsg = msg
    res
  }

  def prepareFailure(index: Int): Parse[Nothing] = prepareFailure(index, failureCut)
  def prepareFailure(index: Int, cut: Boolean): Parse[Nothing] = {
    isSuccess = false
    failureStack = Nil
    this.index = index
    failureCut = cut
    this.asInstanceOf[Parse[Nothing]]
  }

  def result: Result[T] = {
    if (isSuccess) Result.Success(successValue.asInstanceOf[T], index)
    else Result.Failure(index, (failureMsg -> index) :: failureStack, Result.Extra(input, startIndex))
  }
}
object Parse{
  def apply()(implicit i: Parse[Any]): Parse[Any] = i
  def apply(input: String, traceIndex: Int = -1) = new Parse(
    input = input,
    failureStack = List.empty,
    failureMsg = null,
    isSuccess = true,
    logDepth = 0,
    0, 0, false, false, (), false, traceIndex, null
  )
}
