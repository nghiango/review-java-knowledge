package lab.jpahibernate.nplusone;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class OrderServiceTest {

    private EntityManager entityManager;
    private OrderService orderService;

    @BeforeEach
    void setUp() {
        entityManager = mock(EntityManager.class);
        orderService = new OrderService(entityManager);
    }

    @Test
    @DisplayName("findAllOrdersWithItemsJoinFetch uses JOIN FETCH to avoid N+1 queries")
    void findAllOrdersWithItemsJoinFetch_usesJoinFetchQuery() {
        @SuppressWarnings("unchecked")
        TypedQuery<Order> query = mock(TypedQuery.class);
        Order order = new Order("Alice");
        order.addItem(new OrderItem("Book", BigDecimal.valueOf(25)));

        when(entityManager.createQuery(anyString(), eq(Order.class))).thenReturn(query);
        when(query.getResultList()).thenReturn(List.of(order));

        List<Order> result = orderService.findAllOrdersWithItemsJoinFetch();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCustomerName()).isEqualTo("Alice");
        assertThat(result.get(0).getItems()).hasSize(1);
        verify(entityManager)
                .createQuery("SELECT DISTINCT o FROM Order o LEFT JOIN FETCH o.items", Order.class);
    }

    @Test
    @DisplayName("findOrderSummariesDtoProjection queries DTO projection directly")
    void findOrderSummariesDtoProjection_queriesDtoDirectly() {
        @SuppressWarnings("unchecked")
        TypedQuery<OrderSummaryDto> query = mock(TypedQuery.class);
        OrderSummaryDto dto = new OrderSummaryDto(1L, "Alice", 2, BigDecimal.valueOf(50));

        when(entityManager.createQuery(anyString(), eq(OrderSummaryDto.class))).thenReturn(query);
        when(query.getResultList()).thenReturn(List.of(dto));

        List<OrderSummaryDto> result = orderService.findOrderSummariesDtoProjection();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).customerName()).isEqualTo("Alice");
        assertThat(result.get(0).itemCount()).isEqualTo(2);
        assertThat(result.get(0).totalAmount()).isEqualTo(BigDecimal.valueOf(50));
    }
}
