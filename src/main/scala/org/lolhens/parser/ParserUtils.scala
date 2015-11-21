package org.lolhens.parser

import java.util.regex.Pattern

import scala.util.matching.Regex
import scala.util.parsing.combinator.RegexParsers
import scala.util.parsing.input.{Position, Reader}

/**
  * Created by LolHens on 25.07.2015.
  */
class ParserUtils extends RegexParsers {
  override def skipWhitespace: Boolean = false

  def optFrame[T](pre: Parser[Any], main: Parser[T], post: Parser[Any]): Parser[T] = (pre ~> main <~ post) | main

  def lowerCase[T](parser: Parser[T]) = new Parser[T] {
    def apply(in: Input) = {
      class LowerCaseReader(val self: Reader[Char]) extends Reader[Char] {
        override def source: CharSequence = self.source.toString.toLowerCase

        override def offset: Int = self.offset

        override def first: Char = self.first.toLower

        override def atEnd: Boolean = self.atEnd

        override def pos: Position = new LowerCasePosition(self.pos)

        override def rest: Reader[Char] = new LowerCaseReader(self)

        class LowerCasePosition(val self: Position) extends Position {
          override def line: Int = self.line

          override def column: Int = self.column

          override protected def lineContents: String = self.lineContents.toLowerCase

          override def toString = self.toString()

          override def longString = self.longString

          override def <(that: Position) = self.<(that)
        }

      }

      parser.apply(new LowerCaseReader(in))
    }
  }


  def decimalNumber: Parser[String] = """-?\d+""".r

  def binaryNumber: Parser[String] = "[01]+".r

  def hexNumber: Parser[String] = "[0-9a-fA-F]+".r

  def intType: Parser[Int] = (
    "0b" ~> binaryNumber ^^ (Integer.parseInt(_, 2))
      | "0x" ~> hexNumber ^^ ((hexNum) => Integer.parseInt("0" + hexNum, 16))
      | decimalNumber ^^ (_.toInt)
    )

  def shortType: Parser[Short] = intType ^^ (_.toShort)

  def byteType: Parser[Byte] = intType ^^ (_.toByte)

  def doubleType: Parser[Double] = """-?(\d+(\.\d*)?|\d*\.\d+)([eE][+-]?\d+)?[dD]?""".r ^^ (_.toDouble)

  def floatType: Parser[Float] = """-?(\d+(\.\d*)?|\d*\.\d+)([eE][+-]?\d+)?[fF]?""".r ^^ (_.toFloat)

  def booleanType: Parser[Boolean] = ("true" | "false") ^^ {
    case "true" => true
    case "false" => false
  }

  def stringType: Parser[String] = ("\"" + """([^"\p{Cntrl}\\]|\\[\\'"bfnrt]|\\u[a-fA-F0-9]{4})*+""" + "\"").r

  def wordType: Parser[String] = """(\w)+""".r

  def textType(pre: List[String], post: List[String]): Parser[String] = s"${except(pre).getOrElse("")}${except(post).getOrElse(".")}+".r

  protected def except(list: List[String]): Option[String] = {
    val except = list.map(quote).mkString("")
    if (except == "") None else Some(s"[^$except]")
  }

  protected def quote(string: String) = if (string == "") "" else Pattern.quote(string)

  implicit def stringToStringList(string: String): List[String] = List(string)

  implicit def parserToRichParser(parser: Parser[String]) = new RichStringParser(parser)

  implicit def literalToRichParser(s: String): RichStringParser = new RichStringParser(literal(s))

  implicit def regexToRichParser(r: Regex): RichStringParser = new RichStringParser(regex(r))

  class RichStringParser(val self: Parser[String]) {
    def ~~[U](q: => Parser[U]): Parser[~[String, U]] = self ~ (whiteSpace ~> q)

    def ~~>[U](q: => Parser[U]): Parser[U] = (self ~ whiteSpace) ~> q

    def <~~[U](q: => Parser[U]): Parser[String] = self <~ (whiteSpace ~ q)
  }

}