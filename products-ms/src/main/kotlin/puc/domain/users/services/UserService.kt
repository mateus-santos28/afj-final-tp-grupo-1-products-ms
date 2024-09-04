package puc.domain.users.services

import feign.FeignException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import puc.application._shared.IUserClient
import puc.domain.users.model.User
import puc.infrastructure.resilience.ResilienceStrategy

@Service
class UserService(
    val userClient: IUserClient,
    val resilienceStrategy: ResilienceStrategy<User>
) : IUserService {
    val logger = LoggerFactory.getLogger(this.javaClass)!!


    override fun getAuthenticatedUser(): User? {
        val user = resilienceStrategy.executeWithRetryAndCircuitBreaker("USER_SERVICE") {
            runCatching {
                logger.info("Retrieving authenticated user from user microservice")
                userClient.getAuthenticatedUser()
            }.getOrElse {
                logger.error("it was not possible to load user data", it)

                if (it is FeignException && it.status() in 400..499) {
                    throw UserNotFoundException("The sent user is not correct")
                }

                throw UserServiceNotReachable("It was not possible to load data from user api")
            }
        }
        logger.info("Authenticated user retrieved from user microservice: $user")
        return user
    }

}