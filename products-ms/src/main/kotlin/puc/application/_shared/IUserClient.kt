package puc.application._shared

import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.GetMapping
import puc.domain.users.model.User
import puc.infrastructure.configs.FeignConfig

@FeignClient(name="user-client", url = "\${client.user.url}", path = "\${client.user.path}", configuration = [FeignConfig::class])
interface IUserClient {

    @GetMapping("/me")
    fun getAuthenticatedUser(): User

}
