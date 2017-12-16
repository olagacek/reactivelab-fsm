package reactive.fsm

import akka.actor.{ActorLogging, FSM}
import reactive.fsm.CartFSM.{CartData, CartState}

import scala.concurrent.duration._

class CartFSM extends FSM[CartState, CartData] with ActorLogging {
  import CartFSM._

  private val timeout = 2 minutes

  startWith(Empty, EmptyCart)

  when(Empty) {
    case Event(ItemAdded(itemId), EmptyCart)=>
      setTimer(CART_TIMER_NAME, TimerExpired, timeout)
      log.info(s"Item $itemId added")
      goto(NonEmpty) using NonEmptyCart(List(itemId))
  }

  when(NonEmpty) {
    case Event(ItemAdded(itemId), NonEmptyCart(items)) =>
      cancelTimer(CART_TIMER_NAME)
      setTimer(CART_TIMER_NAME, TimerExpired, timeout)
      log.info(s"Item $itemId added")
      stay using NonEmptyCart(itemId :: items)

    case Event(ItemRemoved(itemId), NonEmptyCart(items)) =>
      val filteredItems = items.filter(_ != itemId)
      cancelTimer(CART_TIMER_NAME)
      log.info(s"Item $itemId removed")
      if(filteredItems.isEmpty) {
        goto(Empty) using EmptyCart
      } else {
        setTimer(CART_TIMER_NAME, TimerExpired, timeout)
        stay using NonEmptyCart(filteredItems)
      }

    case Event(CheckoutStarted, NonEmptyCart(items)) =>
      log.info("checkout started")
      goto(InCheckout) using NonEmptyCart(items)
  }

  when(InCheckout) {
    case Event(CheckoutClosed, _) =>
      log.info("checkout closed")
      goto(Empty) using EmptyCart

    case Event(CheckoutCancelled, e: NonEmptyCart) =>
      log.info("checkout cancelled")
      goto(NonEmpty) using e
  }

  whenUnhandled {
    case Event(e, s) =>
      log.warning("received unhandled request {} in state {}/{}", e, stateName, s)
      stay
  }

  initialize()
}

object CartFSM {

  private val CART_TIMER_NAME = "CART_TIMER"

  sealed trait CartState

  private case object Empty extends CartState

  private case object NonEmpty extends CartState

  private case object InCheckout extends CartState

  sealed trait CartData

  private case object EmptyCart extends CartData

  private final case class NonEmptyCart(items: List[Int]) extends CartData

  sealed trait CartCommand

  final case class ItemAdded(itemId: Int) extends CartCommand

  final case class ItemRemoved(itemId: Int) extends CartCommand

  case object CheckoutStarted extends CartCommand

  case object CheckoutClosed extends CartCommand

  case object CheckoutCancelled extends CartCommand

  case object TimerExpired extends CartCommand
}