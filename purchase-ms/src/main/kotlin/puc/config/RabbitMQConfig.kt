package puc.config

import org.springframework.amqp.core.BindingBuilder
import org.springframework.amqp.core.Exchange
import org.springframework.amqp.core.ExchangeBuilder
import org.springframework.amqp.core.Queue
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory
import org.springframework.amqp.rabbit.connection.ConnectionFactory
import org.springframework.amqp.rabbit.core.RabbitAdmin
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.retry.policy.SimpleRetryPolicy
import org.springframework.retry.support.RetryTemplate
import org.springframework.amqp.core.Binding
import org.springframework.amqp.core.BindingBuilder
import org.springframework.amqp.core.DirectExchange

@Configuration
class RabbitMQConfig {

    @Value("\${rabbitmq.exchange}")
    lateinit var exchange: String

    @Value("\${rabbitmq.routingkey}")
    lateinit var routingKey: String

    @Value("\${rabbitmq.queue}")
    lateinit var purchaseQueue: String

    @Value("\${rabbitmq.dlx.exchange}")
    lateinit var dlxExchange: String

    @Value("\${rabbitmq.dlx.routingkey}")
    lateinit var dlxRoutingKey: String

    @Value("\${rabbitmq.dlx.queue}")
    lateinit var dlxPurchaseQueue: String

    @Bean
    fun purchaseQueue(): Queue {
        return Queue(purchaseQueue, true)
    }

    @Bean
    fun exchange(): Exchange {
        return ExchangeBuilder.directExchange(exchange).durable(true).build()
    }

    @Bean
    fun rabbitAdmin(connectionFactory: ConnectionFactory): RabbitAdmin {
        return RabbitAdmin(connectionFactory)
    }

    @Bean
    fun simpleRabbitListenerContainerFactory(connectionFactory: ConnectionFactory): SimpleRabbitListenerContainerFactory {
        val retryTemplate = RetryTemplate()
        val retryPolicy = SimpleRetryPolicy(3)
        retryTemplate.setRetryPolicy(retryPolicy)

        val factory = SimpleRabbitListenerContainerFactory()
        factory.setConnectionFactory(connectionFactory)
        factory.setConcurrentConsumers(3)
        factory.setMaxConcurrentConsumers(10)
        factory.setPrefetchCount(1)
        factory.setRetryTemplate(retryTemplate)
        return factory
    }

    @Bean
    fun binding(): org.springframework.amqp.core.Binding {
        return BindingBuilder
            .bind(purchaseQueue())
            .to(exchange())
            .with(routingKey)
            .noargs()
    }

    @Bean
    fun deadLetterQueue(): Queue {
        return Queue(dlxPurchaseQueue, true, false, false)
    }

    @Bean
    fun deadLetterExchange(): DirectExchange {
        return DirectExchange(dlxExchange)
    }

    @Bean
    fun dlxBinding(): Binding {
        return BindingBuilder.bind(deadLetterQueue()).to(deadLetterExchange()).with(dlxRoutingKey)
    }
}