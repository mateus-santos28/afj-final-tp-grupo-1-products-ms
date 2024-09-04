package puc.infrastructure.filters

import com.fasterxml.jackson.databind.ObjectMapper
import feign.FeignException
import io.github.resilience4j.circuitbreaker.CallNotPermittedException
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import puc.domain.users.model.User
import puc.domain.users.services.UserNotFoundException
import puc.domain.users.services.UserService
import puc.domain.users.services.UserServiceNotReachable
import puc.infrastructure.configs.HeaderContext
import java.time.LocalDate

@Component
class AuthFilter(val userService: UserService, val objectMapper: ObjectMapper): OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val user: User?
        val headerValue = request.getHeader("Authorization")
        HeaderContext.setHeader(headerValue)

        if(request.method == "GET"){
            filterChain.doFilter(request, response)
        }
        else{
            try {
                user = userService.getAuthenticatedUser()

                if (user != null) {
                    val roles = user.roles?.map { SimpleGrantedAuthority(it) }
                    val authentication = UsernamePasswordAuthenticationToken(user, null, roles)
                    SecurityContextHolder.getContext().authentication = authentication
                    filterChain.doFilter(request, response)
                } else {
                    createResponse(HttpStatus.UNAUTHORIZED, "Request with invalid token", response)
                }
            } catch (e: UserNotFoundException)  {
                createResponse(HttpStatus.FORBIDDEN, e.localizedMessage, response)
            } catch (e: UserServiceNotReachable){
                createResponse(HttpStatus.SERVICE_UNAVAILABLE, e.localizedMessage, response)
            }catch (e: FeignException) {
                val status = HttpStatus.resolve(e.status()) ?: HttpStatus.FORBIDDEN
                createResponse(status, e.localizedMessage, response)
            } catch (e: CallNotPermittedException){
                logger.error("the circuit is open. so not call is allowed")
                createResponse(HttpStatus.SERVICE_UNAVAILABLE, e.localizedMessage, response)
            }
        }

        HeaderContext.clear()
    }

    private fun createResponse(httpStatus: HttpStatus, message: String, response: HttpServletResponse) {
        response.characterEncoding = "UTF-8"
        response.contentType = "application/json"
        response.status = httpStatus.value()

        val error = createProblemDetail(httpStatus, message)
        response.writer.println(error)
    }

    private fun createProblemDetail(httpStatus: HttpStatus, message: String): String {
        val problemDetail: ProblemDetail = ProblemDetail.forStatusAndDetail(httpStatus, message)
        problemDetail.title = "Authentication error"
        problemDetail.setProperty("timestamp", LocalDate.now())
        return objectMapper.writeValueAsString(problemDetail)
    }
}