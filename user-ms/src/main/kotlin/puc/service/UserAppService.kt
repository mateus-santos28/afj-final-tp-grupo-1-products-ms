package puc.service

import puc.model.dto.request.ChangePasswordRequest
import puc.model.dto.request.RegisterRequest
import puc.model.dto.request.UpdateUserRequest
import puc.model.dto.response.LoginResponse
import puc.model.dto.response.RefreshTokenResponse
import puc.model.dto.response.UserResponse

interface UserAppService {
    fun register(request: RegisterRequest): UserResponse
    fun login(username: String, password: String): LoginResponse
    fun getUserInfo(username: String): UserResponse
    fun refreshToken(refreshToken: String): RefreshTokenResponse
    fun updateUser(id: Long, request: UpdateUserRequest, authUsername: String, authRoles: Set<String>): UserResponse
    fun changePassword(username: String, request: ChangePasswordRequest): UserResponse
}