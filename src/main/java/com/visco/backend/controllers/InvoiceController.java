package com.visco.backend.controllers;

import com.visco.backend.models.dtos.CreateInvoiceRequest;
import com.visco.backend.models.dtos.InvoiceResponse;
import com.visco.backend.services.InvoiceService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/invoices")
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceService invoiceService;

    @PostMapping
    public ResponseEntity<InvoiceResponse> createInvoice(
        @Valid @RequestBody CreateInvoiceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(invoiceService.createInvoice(request));
    }

    @GetMapping
    public ResponseEntity<Page<InvoiceResponse>> getAllInvoices(Pageable pageable) {
        return ResponseEntity.ok(invoiceService.getAllInvoices(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<InvoiceResponse> getInvoice(@PathVariable Long id) {
        return ResponseEntity.ok(invoiceService.getInvoiceById(id));
    }

    @GetMapping("/by-order/{orderId}")
    public ResponseEntity<List<InvoiceResponse>> getInvoicesByOrder(@PathVariable Long orderId) {
        return ResponseEntity.ok(invoiceService.getInvoicesByOrderId(orderId));
    }

    @PatchMapping("/{id}/pay")
    public ResponseEntity<InvoiceResponse> markAsPaid(
        @PathVariable Long id,
        @RequestBody Map<String, Object> body) {
        LocalDate paymentDate = body.get("paymentDate") != null
            ? LocalDate.parse(body.get("paymentDate").toString())
            : LocalDate.now();
        return ResponseEntity.ok(invoiceService.markAsPaid(id, paymentDate));
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<InvoiceResponse> cancelInvoice(@PathVariable Long id) {
        return ResponseEntity.ok(invoiceService.cancelInvoice(id));
    }
}
