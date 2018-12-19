package examples.actors

import scala.actors.Actor
import scala.actors.Actor._

abstract class Producer[T] {

  protected def produce(x: T) {
    coordinator ! Some(x)
    receive { case Next => }
  }

  protected def produceValues: Unit

  def iterator = new Iterator[T] {
    private var current: Any = Undefined
    private def lookAhead = {
      if (current == Undefined) current = coordinator !? Next
      current
    }

    def hasNext: Boolean = lookAhead match {
      case Some(x) => true
      case None => { coordinator ! Stop; false }
    }

    def next: T = lookAhead match {
      case Some(x) => current = Undefined; x.asInstanceOf[T]
    }
  }

  private val coordinator: Actor = actor {
    loop {
      react {//the event-based pendant of receive
        case Next =>
          producer ! Next
          reply {
            receive { case x: Option[_] => x }
          }
        case Stop =>
          exit('stop)
      }
    }
  }

  private val producer: Actor = actor {
    receive {
      case Next =>
        produceValues
        coordinator ! None
    }
  }
}

object producers extends Application {

  class PreOrder(n: Tree) extends Producer[Int] {
    def produceValues = traverse(n)
    def traverse(n: Tree) {
      if (n != null) {
        produce(n.elem)
        traverse(n.left)
        traverse(n.right)
      }
    }
  }

  actor {
    print("PreOrder:")
    for (x <- new PreOrder(tree).iterator) print(" "+x)
    print("\nPostOrder:")
  }
}
