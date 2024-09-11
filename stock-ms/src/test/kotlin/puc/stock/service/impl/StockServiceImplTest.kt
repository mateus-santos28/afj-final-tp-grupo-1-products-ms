package puc.stock.service.impl

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.test.context.junit.jupiter.SpringExtension
import puc.stock.controller.request.StockUpdateRequest
import puc.stock.controller.response.StockResponse
import puc.stock.exception.NotEnoughStockException
import puc.stock.exception.ProductAlreadyExistsException
import puc.stock.exception.ProductNotFoundException
import puc.stock.model.Stock
import puc.stock.repository.StockRepository
import puc.stock.resources.ProductResource
import puc.stock.service.ProductService

@ExtendWith(SpringExtension::class)
class StockServiceImplTest {
    private val stockRepository: StockRepository = mockk()
    private val productService: ProductService = mockk()
    private val stockService = StockServiceImpl(stockRepository, productService)

    @Test
    fun `should write down stock of product with success`() {
        // Given
        val request = StockUpdateRequest("1", 1)
        val stock = Stock(1, "1", 1)
        val stockSlot = slot<Stock>()

        every { stockRepository.findByProductId(request.productId!!) } returns stock
        every { stockRepository.save(capture(stockSlot)) } answers { stockSlot.captured }

        // When
        val actualStock = stockService.writeDownStock(request)

        // Then
        assertNotNull(actualStock)
        assertEquals(0, actualStock.quantity)
        verify { stockRepository.findByProductId(request.productId!!) }
        verify { stockRepository.save(stockSlot.captured) }
    }

    @Test
    fun `should fail write down stock if product not have enough quantity`() {
        // Given
        val request = StockUpdateRequest("1", 1)
        val stock = Stock(1, "1", 0)

        every { stockRepository.findByProductId(request.productId!!) } returns stock

        // Then
        val exception = assertThrows(NotEnoughStockException::class.java) {
            stockService.writeDownStock(request)
        }

        assertEquals("Produto com id [1] não tem estoque suficiente", exception.message)
        verify { stockRepository.findByProductId(request.productId!!) }
    }

    @Test
    fun `should add product stock when stock does not exist`() {
        // Given
        val productId = "2"
        val stockUpdateRequest = StockUpdateRequest(productId, 20)

        every { productService.findProductById(productId) } returns ProductResource("2", "test", 10.0)
        every { stockRepository.findByProductId(productId) } returns null
        every { stockRepository.save(any()) } returns Stock(id = 2L, productId = productId, quantity = 20)

        // When
        val response = stockService.addProductStock(stockUpdateRequest)

        // Then
        assertEquals(20, response.quantity)
        verify { productService.findProductById(productId) }
        verify { stockRepository.findByProductId(productId) }
        verify { stockRepository.save(any()) }
    }

    @Test
    fun `should throw ProductAlreadyExistsException when stock already exists`() {
        // Given
        val productId = "2"
        val stockUpdateRequest = StockUpdateRequest(productId, 20)
        val existingStock = Stock(id = 2L, productId = productId, quantity = 20)

        every { productService.findProductById(productId) } returns ProductResource("2", "test", 10.0)
        every { stockRepository.findByProductId(productId) } returns existingStock

        // When / Then
        val exception = assertThrows(ProductAlreadyExistsException::class.java) {
            stockService.addProductStock(stockUpdateRequest)
        }

        assertEquals("Estoque do produto com id [2] já está cadastrado", exception.message)
        verify { productService.findProductById(productId) }
        verify { stockRepository.findByProductId(productId) }
    }

    @Test
    fun `should found stock by product id with success`() {
        val stock = Stock(1, "1", 1)
        every { stockRepository.findByProductId("1") } returns stock

        // Then
        val result = stockService.findStockByProductId("1")

        assertNotNull(result)
        verify { stockRepository.findByProductId("1") }
    }

    @Test
    fun `should fail whe getting stock by product id`() {
        every { stockRepository.findByProductId("1") } returns null

        // Then
        val exception = assertThrows(ProductNotFoundException::class.java) {
            stockService.findStockByProductId("1")
        }

        assertEquals("Produto com id [1] não encontrado na base de estoques", exception.message)
        verify { stockRepository.findByProductId("1") }
    }

    @Test
    fun `should return paginated stock when repository returns data`() {
        val mockPageable = mockk<Pageable>()
        val expectedPage = Page.empty<Stock>()

        every { stockRepository.findAll(mockPageable) } returns expectedPage

        val stockPage = stockService.findStockAll(mockPageable)

        assertNotNull(stockPage)
        assertEquals(expectedPage, stockPage)
        verify { stockRepository.findAll(mockPageable) }
    }

    @Test
    fun `should throw ProductNotFoundException when repository returns null`() {
        val mockPageable = mockk<Pageable>()

        every { stockRepository.findAll(mockPageable) } returns null

        var exception = assertThrows(ProductNotFoundException::class.java) {
            stockService.findStockAll(mockPageable)
        }

        assertEquals("Erro ao buscar todos os produtos do estoque", exception.message)
        verify { stockRepository.findAll(mockPageable) }
    }

    @Test
    fun `should map returned Stock objects to StockResponse`() {
        val mockPageable = mockk<Pageable>()
        val mockStock1 = mockk<Stock>()
        val mockStock2 = mockk<Stock>()
        val expectedStockResponse1 = mockk<StockResponse>()
        val expectedStockResponse2 = mockk<StockResponse>()

        val content = listOf(mockStock1, mockStock2)
        val pageable = PageRequest.of(0, 2)
        val totalElements = 10L
        val expectedPage = PageImpl(content, pageable, totalElements)

        every { stockRepository.findAll(mockPageable) } returns expectedPage
        every { mockStock1.toResponse() } returns expectedStockResponse1
        every { mockStock2.toResponse() } returns expectedStockResponse2

        stockService.findStockAll(mockPageable)

        assertEquals(2, content.size)
        verify { mockStock1.toResponse() }
        verify { mockStock2.toResponse() }
    }
}