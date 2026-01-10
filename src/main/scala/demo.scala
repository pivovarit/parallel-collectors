object EagerExample extends App {
  val width: Option[Int] = Some(3)
  val height: Option[Int] = Some(4)

  val result: Int = for {
    w <- width
    h <- height
  } yield w * h

  println(s"result = ${result.getOrElse(-1)}")
}
