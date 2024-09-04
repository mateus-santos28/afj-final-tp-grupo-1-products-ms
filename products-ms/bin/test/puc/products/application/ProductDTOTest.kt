package puc.products.application

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import puc.application.dtos.ProductDTO

class ProductDTOTest {
    @Test
    fun `should create incoming product successfully`(){
        assertDoesNotThrow {
            ProductDTO("Testing product 01", 20.1)
        }
    }
}