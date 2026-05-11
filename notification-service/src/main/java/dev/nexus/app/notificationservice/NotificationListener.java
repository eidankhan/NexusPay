package dev.nexus.app.notificationservice;


import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
public class NotificationListener {

    @RabbitListener(queues = "payment_success_queue")
    public void handlePaymentSuccess(PaymentEvent event) {
        System.out.println("\n✅ [Notification Service] SUCCESS Event Received!");
        System.out.println("------------------------------------------------");
        System.out.println("📧 Sending Digital Receipt to: " + event.customerEmail());
        System.out.println("💰 Amount Billed: " + event.amount() + " " + event.currency());
        System.out.println("🧾 Transaction ID: " + event.transactionId());
        System.out.println("------------------------------------------------\n");

        // TODO in future: Trigger SendGrid/AWS SES API
    }

    @RabbitListener(queues = "payment_failed_queue")
    public void handlePaymentFailure(PaymentEvent event) {
        System.out.println("\n❌ [Notification Service] FAILED Event Received!");
        System.out.println("------------------------------------------------");
        System.out.println("📧 Sending 'Action Required' Email to: " + event.customerEmail());
        System.out.println("⚠️ Reason: Payment of " + event.amount() + " " + event.currency() + " was declined.");
        System.out.println("------------------------------------------------\n");

        // TODO in future: Trigger failure notification email
    }
}