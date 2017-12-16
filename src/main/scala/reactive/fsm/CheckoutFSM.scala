package reactive.fsm

import akka.actor.{ActorLogging, FSM}
import reactive.fsm.CheckoutFSM.{CheckoutData, CheckoutState}

import scala.concurrent.duration._

class CheckoutFSM extends FSM[CheckoutState, CheckoutData] with ActorLogging {
  import CheckoutFSM._
  private val timeout = 2 minutes

  startWith(SelectingDelivery, CheckoutDetails(None, None))
  setTimer(CHECKOUT_TIMER_NAME, CancelCheckout, timeout)

  when(SelectingDelivery) {
    case Event(s : SelectDeliveryMethod, c : CheckoutDetails) =>
      cancelTimer(CHECKOUT_TIMER_NAME)
      log.info(s"Delivery method ${s.deliveryMethod} selected")
      goto(SelectingPaymentMethod) using c.copy(deliveryMethod = Some(s.deliveryMethod))
    case Event(CancelCheckout, _) =>
      log.info("Checkout cancelled")
      goto(Cancelled)
  }

  when(SelectingPaymentMethod) {
    case Event(s : SelectPaymentMethod, c : CheckoutDetails) =>
      log.info(s"Payment method ${s.paymentMethod} selected")
      goto(ProcessingPayment) using c.copy(paymentMethod = Some(s.paymentMethod))
  }

  onTransition {
    case SelectingPaymentMethod -> ProcessingPayment =>
      setTimer(PAYMENT_TIMER_NAME, CancelCheckout, timeout)
  }

  when(ProcessingPayment) {
    case Event(MakePayment, c : CheckoutDetails) =>
      log.info("Checkout closed")
      goto(Closed)

    case Event(CancelCheckout, _) =>
      log.info("Checkout cancelled")
      goto(Cancelled)
  }

  when(Closed) {
    case Event(e, s) =>
      log.warning("received unhandled request {} in state {}/{}", e, stateName, s)
      stay
  }

  when(Cancelled) {
    case Event(e, s) =>
      log.warning("received unhandled request {} in state {}/{}", e, stateName, s)
      stay
  }

  whenUnhandled {
    case Event(e, s) =>
      log.warning("received unhandled request {} in state {}/{}", e, stateName, s)
      stay
  }

  initialize()
}

object CheckoutFSM {
  private val CHECKOUT_TIMER_NAME = "CHECKOUT_TIMER"

  private val PAYMENT_TIMER_NAME = "PAYMENT_TIMER"

  sealed trait CheckoutState

  private case object SelectingDelivery extends CheckoutState

  private case object SelectingPaymentMethod extends CheckoutState

  private case object ProcessingPayment extends CheckoutState

  private case object Cancelled extends CheckoutState

  private case object Closed extends CheckoutState

  sealed trait CheckoutData

  private case class CheckoutDetails(paymentMethod: Option[String], deliveryMethod: Option[String]) extends CheckoutData

  sealed trait CheckoutCommand

  case class SelectDeliveryMethod(deliveryMethod: String) extends CheckoutCommand

  case class SelectPaymentMethod(paymentMethod: String) extends CheckoutCommand

  case object CancelCheckout extends CheckoutCommand

  case object MakePayment extends CheckoutCommand
}