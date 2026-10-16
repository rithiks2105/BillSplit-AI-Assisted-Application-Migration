package com.billsplit.web;

import com.billsplit.service.AppException;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JsonSupportTest {
    @Test
    void readsNumericAmountsFromJsonNumbers() {
        JSONObject body = new JSONObject("{\"amount\":50.5,\"id\":7,\"borrow\":10}");
        assertEquals(new BigDecimal("50.5"), JsonSupport.decimal(body, "amount", BigDecimal.ZERO));
        assertEquals(7L, JsonSupport.requiredLong(body, "id"));
        assertEquals(new BigDecimal("10"), JsonSupport.decimal(body, "borrow", BigDecimal.ZERO));
    }

    @Test
    void usesFallbackWhenAmountMissing() {
        JSONObject body = new JSONObject("{}");
        assertEquals(BigDecimal.ZERO, JsonSupport.decimal(body, "amount", BigDecimal.ZERO));
    }

    @Test
    void rejectsMissingShareId() {
        AppException ex = assertThrows(AppException.class, () -> JsonSupport.requiredLong(new JSONObject("{}"), "id"));
        assertEquals(400, ex.getStatus());
    }
}
