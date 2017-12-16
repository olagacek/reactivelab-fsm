package reactive

import akka.actor.{ActorSystem, Props}
import reactive.fsm.{CartFSM, CheckoutFSM}
import reactive.fsm.CartFSM.{CheckoutClosed, CheckoutStarted, ItemAdded, ItemRemoved}
import reactive.fsm.CheckoutFSM.{MakePayment, SelectDeliveryMethod, SelectPaymentMethod}

object Main extends App {
  private val system = ActorSystem("FSMSystem")

  val cartRef = system.actorOf(Props(new CartFSM))

  cartRef ! ItemAdded(1)

  cartRef ! ItemAdded(2)

  cartRef ! ItemRemoved(1)

  cartRef ! CheckoutStarted

  cartRef ! CheckoutClosed

  val checkoutRef = system.actorOf(Props(new CheckoutFSM))

  checkoutRef ! SelectDeliveryMethod("home_delivery")

  checkoutRef ! SelectPaymentMethod("paypal_payment")

  checkoutRef ! MakePayment
}
